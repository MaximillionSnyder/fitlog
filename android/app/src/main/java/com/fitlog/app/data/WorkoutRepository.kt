package com.fitlog.app.data

import com.fitlog.app.domain.SessionSetInput
import com.fitlog.app.domain.SessionSummary
import com.fitlog.app.domain.Ulid
import com.fitlog.app.domain.WorkoutSummary

enum class WorkoutErrorCode {
    INVALID_INPUT,
    EXERCISE_NOT_FOUND,
    SESSION_NOT_FOUND,
    SESSION_NOT_ACTIVE,
    SESSION_ALREADY_ACTIVE,
    SET_NOT_FOUND,
}

class WorkoutException(
    val code: WorkoutErrorCode,
    message: String,
) : Exception(message)

data class WorkoutSet(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val exerciseName: String,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val isWarmup: Boolean,
    val notes: String?,
)

data class WorkoutSession(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val notes: String?,
    val summary: SessionSummary,
)

data class SessionDetail(
    val session: WorkoutSession,
    val sets: List<WorkoutSet>,
)

data class AddSetInput(
    val sessionId: String,
    val exerciseId: String,
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val notes: String?,
    val isWarmup: Boolean,
)

data class UpdateSetInput(
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val notes: String?,
)

class WorkoutRepository(
    private val dao: WorkoutDao,
    private val idGenerator: () -> String = { Ulid.generate() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun activeSession(): WorkoutSession? {
        val session = dao.findActiveSession() ?: return null
        return session.toDomain(dao.listSetsForSession(session.id))
    }

    suspend fun startSession(): WorkoutSession {
        if (dao.findActiveSession() != null) {
            throw WorkoutException(
                WorkoutErrorCode.SESSION_ALREADY_ACTIVE,
                "Ya hay una sesión de entrenamiento en curso",
            )
        }

        val timestamp = now()
        val session = SessionEntity(
            id = idGenerator(),
            routineId = null,
            startedAt = timestamp,
            finishedAt = null,
            notes = null,
            createdAt = timestamp,
            updatedAt = timestamp,
            deletedAt = null,
        )
        dao.insertSession(session)
        return session.toDomain(emptyList())
    }

    suspend fun finishSession(sessionId: String) {
        val session = dao.findSessionById(sessionId)
            ?: throw WorkoutException(WorkoutErrorCode.SESSION_NOT_FOUND, "La sesión no existe")
        if (session.finishedAt != null) {
            throw WorkoutException(WorkoutErrorCode.SESSION_NOT_ACTIVE, "La sesión ya está finalizada")
        }

        val timestamp = now()
        dao.finishSession(id = sessionId, finishedAt = timestamp, updatedAt = timestamp)
    }

    suspend fun addSet(input: AddSetInput): WorkoutSet {
        validate(input.weightKg, input.reps, input.rir)

        val session = dao.findSessionById(input.sessionId)
            ?: throw WorkoutException(WorkoutErrorCode.SESSION_NOT_FOUND, "La sesión no existe")
        if (session.finishedAt != null) {
            throw WorkoutException(WorkoutErrorCode.SESSION_NOT_ACTIVE, "La sesión ya está finalizada")
        }

        val exercise = dao.findActiveExerciseById(input.exerciseId)
            ?: throw WorkoutException(
                WorkoutErrorCode.EXERCISE_NOT_FOUND,
                "El ejercicio no está disponible",
            )

        val timestamp = now()
        val id = idGenerator()
        dao.insertSet(
            id = id,
            sessionId = input.sessionId,
            exerciseId = input.exerciseId,
            weightKg = input.weightKg,
            reps = input.reps,
            rir = input.rir,
            isWarmup = input.isWarmup,
            notes = input.notes,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

        val created = dao.listSetsForSession(input.sessionId).firstOrNull { it.id == id }
            ?: throw IllegalStateException("La serie recién creada no se encontró")

        return created.toDomain(exerciseName = exercise.name)
    }

    suspend fun updateSet(setId: String, input: UpdateSetInput) {
        validate(input.weightKg, input.reps, input.rir)

        if (dao.findSetById(setId) == null) {
            throw WorkoutException(WorkoutErrorCode.SET_NOT_FOUND, "La serie no existe")
        }

        dao.updateSet(
            id = setId,
            weightKg = input.weightKg,
            reps = input.reps,
            rir = input.rir,
            notes = input.notes,
            updatedAt = now(),
        )
    }

    suspend fun deleteSet(setId: String) {
        if (dao.findSetById(setId) == null) {
            throw WorkoutException(WorkoutErrorCode.SET_NOT_FOUND, "La serie no existe")
        }
        val timestamp = now()
        dao.softDeleteSet(id = setId, deletedAt = timestamp, updatedAt = timestamp)
    }

    suspend fun sessions(): List<WorkoutSession> {
        val sessions = dao.listSessions()
        if (sessions.isEmpty()) return emptyList()

        val setsBySession = dao.listAllSets().groupBy { it.sessionId }
        return sessions.map { session -> session.toDomain(setsBySession[session.id].orEmpty()) }
    }

    suspend fun sessionDetail(sessionId: String): SessionDetail {
        val session = dao.findSessionById(sessionId)
            ?: throw WorkoutException(WorkoutErrorCode.SESSION_NOT_FOUND, "La sesión no existe")
        val sets = dao.listSetsForSession(sessionId)
        return SessionDetail(session = session.toDomain(sets), sets = sets.map { it.toDomain() })
    }

    private fun validate(weightKg: Double?, reps: Int?, rir: Int?) {
        if (weightKg != null && (!weightKg.isFinite() || weightKg < 0)) {
            throw WorkoutException(
                WorkoutErrorCode.INVALID_INPUT,
                "El peso debe ser un número mayor o igual a 0",
            )
        }
        if (reps != null && reps < 0) {
            throw WorkoutException(
                WorkoutErrorCode.INVALID_INPUT,
                "Las repeticiones deben ser un entero mayor o igual a 0",
            )
        }
        if (rir != null && (rir < 0 || rir > 10)) {
            throw WorkoutException(
                WorkoutErrorCode.INVALID_INPUT,
                "El RIR debe ser un entero entre 0 y 10",
            )
        }
    }

    private fun SessionEntity.toDomain(sets: List<WorkoutSetRow>) = WorkoutSession(
        id = id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        notes = notes,
        summary = WorkoutSummary.summarize(
            sets.map { set ->
                SessionSetInput(
                    exerciseId = set.exerciseId,
                    weightKg = set.weightKg,
                    reps = set.reps,
                    isWarmup = set.isWarmup,
                )
            }
        ),
    )

    private fun WorkoutSetRow.toDomain(exerciseName: String = this.exerciseName) = WorkoutSet(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        setIndex = setIndex,
        weightKg = weightKg,
        reps = reps,
        rir = rir,
        isWarmup = isWarmup,
        notes = notes,
    )
}
