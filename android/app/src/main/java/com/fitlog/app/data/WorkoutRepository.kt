package com.fitlog.app.data

import com.fitlog.app.domain.ImportedWorkoutNotes
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
    ROUTINE_NOT_FOUND,
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
    val createdAtMs: Long,
)

data class WorkoutSession(
    val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val notes: String?,
    val routineId: String?,
    val routineName: String?,
    val summary: SessionSummary,
    /** Metricas de una sesion importada; en una sesion propia quedan en `null`. */
    val activity: ImportedActivity? = null,
)

/** Metricas que trae una sesion importada de otra app (Huawei Health o GPX). */
data class ImportedActivity(
    val distanceM: Double?,
    val calories: Double?,
    val averageHeartRate: Double?,
    val maxHeartRate: Double?,
    val steps: Int?,
    val elevationGainM: Double?,
    val source: String?,
) {
    val isEmpty: Boolean
        get() = distanceM == null && calories == null && averageHeartRate == null &&
            maxHeartRate == null && steps == null && elevationGainM == null
}

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
    private val routinesDao: RoutinesDao,
    private val idGenerator: () -> String = { Ulid.generate() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun activeSession(): WorkoutSession? {
        val session = dao.findActiveSession() ?: return null
        return session.toDomain(
            sets = dao.listSetsForSession(session.id),
            routineName = routineNameOf(session.routineId),
        )
    }

    /** Entrenamiento a importar desde otra app: fechas, nota y metricas ya resueltas. */
    data class ImportedSession(
        val startedAtMs: Long,
        val finishedAtMs: Long?,
        val notes: String?,
        val activity: ImportedActivity? = null,
    )

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
    )

    /**
     * Importa entrenamientos de otra app como sesiones, salteando los que ya existen.
     *
     * La clave es la fecha de inicio: dos entrenamientos no empiezan en el mismo milisegundo, asi
     * que repetir la importacion no duplica nada y no hace falta guardar el id de la app de origen.
     */
    suspend fun importSessions(sessions: List<ImportedSession>): ImportResult {
        if (sessions.isEmpty()) return ImportResult(imported = 0, skipped = 0)

        val existing = dao.listSessions().map { it.startedAt }.toHashSet()
        val timestamp = now()
        var imported = 0
        var skipped = 0

        for (session in sessions.sortedBy { it.startedAtMs }) {
            if (!existing.add(session.startedAtMs)) {
                skipped += 1
                continue
            }
            dao.insertSession(
                SessionEntity(
                    id = idGenerator(),
                    routineId = null,
                    startedAt = session.startedAtMs,
                    finishedAt = session.finishedAtMs,
                    notes = session.notes,
                    distanceM = session.activity?.distanceM,
                    calories = session.activity?.calories,
                    avgHeartRate = session.activity?.averageHeartRate,
                    maxHeartRate = session.activity?.maxHeartRate,
                    steps = session.activity?.steps,
                    elevationGainM = session.activity?.elevationGainM,
                    source = session.activity?.source,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    deletedAt = null,
                )
            )
            imported += 1
        }

        return ImportResult(imported = imported, skipped = skipped)
    }

    suspend fun startSession(routineId: String? = null): WorkoutSession {
        if (dao.findActiveSession() != null) {
            throw WorkoutException(
                WorkoutErrorCode.SESSION_ALREADY_ACTIVE,
                "Ya hay una sesión de entrenamiento en curso",
            )
        }

        val routineName = if (routineId == null) {
            null
        } else {
            routinesDao.findRoutineById(routineId)?.name
                ?: throw WorkoutException(
                    WorkoutErrorCode.ROUTINE_NOT_FOUND,
                    "La rutina no existe",
                )
        }

        val timestamp = now()
        val session = SessionEntity(
            id = idGenerator(),
            routineId = routineId,
            startedAt = timestamp,
            finishedAt = null,
            notes = null,
            createdAt = timestamp,
            updatedAt = timestamp,
            deletedAt = null,
        )
        dao.insertSession(session)
        return session.toDomain(emptyList(), routineName)
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
        return sessions.map { session ->
            session.toDomain(
                sets = setsBySession[session.id].orEmpty(),
                routineName = routineNameOf(session.routineId),
            )
        }
    }

    /**
     * Descanso objetivo por ejercicio de una rutina (exerciseId -> segundos).
     *
     * Lo usa la pantalla de Entrenar para mostrar la cuenta regresiva entre series cuando la sesion
     * se arranco desde una rutina; sin rutina no hay objetivo y solo se muestra el tiempo pasado.
     */
    suspend fun restTargets(routineId: String?): Map<String, Int> {
        if (routineId == null) return emptyMap()
        return routinesDao.listRoutineExercises(routineId)
            .mapNotNull { row -> row.restSeconds?.let { seconds -> row.exerciseId to seconds } }
            .toMap()
    }

    suspend fun sessionDetail(sessionId: String): SessionDetail {
        val session = dao.findSessionById(sessionId)
            ?: throw WorkoutException(WorkoutErrorCode.SESSION_NOT_FOUND, "La sesión no existe")
        val sets = dao.listSetsForSession(sessionId)
        return SessionDetail(
            session = session.toDomain(sets, routineNameOf(session.routineId)),
            sets = sets.map { it.toDomain() },
        )
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

    private suspend fun routineNameOf(routineId: String?): String? =
        routineId?.let { routinesDao.findRoutineById(it)?.name }

    /** Metricas guardadas en las columnas propias, o `null` si la sesion no tiene ninguna. */
    private fun SessionEntity.storedActivity(): ImportedActivity? = ImportedActivity(
        distanceM = distanceM,
        calories = calories,
        averageHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        steps = steps,
        elevationGainM = elevationGainM,
        source = source,
    ).takeIf { !it.isEmpty }

    /** Metricas recuperadas de la nota, para las sesiones importadas antes de las columnas. */
    private fun ImportedWorkoutNotes.ParsedActivity.toActivity(): ImportedActivity = ImportedActivity(
        distanceM = distanceM,
        calories = calories,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        steps = steps,
        elevationGainM = elevationGainM,
        source = source,
    )

    private fun SessionEntity.toDomain(
        sets: List<WorkoutSetRow>,
        routineName: String? = null,
    ) = WorkoutSession(
        id = id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        notes = notes,
        routineId = routineId,
        routineName = routineName,
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
        activity = storedActivity() ?: ImportedWorkoutNotes.parseActivity(notes)?.toActivity(),
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
        createdAtMs = createdAt,
    )
}
