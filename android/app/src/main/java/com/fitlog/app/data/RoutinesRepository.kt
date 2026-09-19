package com.fitlog.app.data

import com.fitlog.app.domain.RoutineOrder
import com.fitlog.app.domain.Ulid

enum class RoutineErrorCode {
    INVALID_INPUT,
    ROUTINE_NOT_FOUND,
    EXERCISE_NOT_FOUND,
    EXERCISE_ALREADY_IN_ROUTINE,
    ROUTINE_EXERCISE_NOT_FOUND,
    INVALID_MOVE,
}

class RoutineException(
    val code: RoutineErrorCode,
    message: String,
) : Exception(message)

data class RoutineExercise(
    val id: String,
    val routineId: String,
    val exerciseId: String,
    val exerciseName: String,
    val position: Int,
    val targetSets: Int?,
    val targetReps: Int?,
    val targetWeightKg: Double?,
    val restSeconds: Int?,
    val notes: String?,
)

data class Routine(
    val id: String,
    val name: String,
    val description: String?,
    val exercises: List<RoutineExercise>,
)

data class RoutineExerciseInput(
    val exerciseId: String,
    val targetSets: Int?,
    val targetReps: Int?,
    val targetWeightKg: Double?,
    val restSeconds: Int?,
    val notes: String?,
)

class RoutinesRepository(
    private val dao: RoutinesDao,
    private val idGenerator: () -> String = { Ulid.generate() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun routines(): List<Routine> {
        val routines = dao.listRoutines()
        if (routines.isEmpty()) return emptyList()

        val exercisesByRoutine = dao.listAllRoutineExercises().groupBy { it.routineId }
        return routines.map { routine ->
            routine.toDomain(exercisesByRoutine[routine.id].orEmpty())
        }
    }

    suspend fun routine(routineId: String): Routine {
        val routine = requireRoutine(routineId)
        return routine.toDomain(dao.listRoutineExercises(routineId))
    }

    suspend fun createRoutine(name: String, description: String?): Routine {
        val cleanName = validateName(name)
        val timestamp = now()
        val routine = RoutineEntity(
            id = idGenerator(),
            name = cleanName,
            description = description?.trim()?.ifEmpty { null },
            createdAt = timestamp,
            updatedAt = timestamp,
            deletedAt = null,
        )
        dao.insertRoutine(routine)
        return routine.toDomain(emptyList())
    }

    suspend fun updateRoutine(routineId: String, name: String, description: String?) {
        requireRoutine(routineId)
        dao.updateRoutine(
            id = routineId,
            name = validateName(name),
            description = description?.trim()?.ifEmpty { null },
            updatedAt = now(),
        )
    }

    suspend fun deleteRoutine(routineId: String) {
        requireRoutine(routineId)
        dao.deleteRoutineWithExercises(routineId, now())
    }

    suspend fun addExercise(routineId: String, input: RoutineExerciseInput): RoutineExercise {
        requireRoutine(routineId)

        val exercise = dao.findActiveExerciseById(input.exerciseId)
            ?: throw RoutineException(
                RoutineErrorCode.EXERCISE_NOT_FOUND,
                "El ejercicio no está disponible",
            )

        if (dao.countExerciseInRoutine(routineId, input.exerciseId) > 0) {
            throw RoutineException(
                RoutineErrorCode.EXERCISE_ALREADY_IN_ROUTINE,
                "El ejercicio ya está en la rutina",
            )
        }

        val timestamp = now()
        val entity = RoutineExerciseEntity(
            id = idGenerator(),
            routineId = routineId,
            exerciseId = input.exerciseId,
            position = dao.countRoutineExercises(routineId) + 1,
            targetSets = input.targetSets,
            targetReps = input.targetReps,
            targetWeightKg = input.targetWeightKg,
            restSeconds = input.restSeconds,
            notes = input.notes,
            createdAt = timestamp,
            updatedAt = timestamp,
            deletedAt = null,
        )
        dao.insertRoutineExercise(entity)

        return entity.toDomain(exerciseName = exercise.name)
    }

    suspend fun removeExercise(routineExerciseId: String) {
        val routineExercise = requireRoutineExercise(routineExerciseId)
        val timestamp = now()

        dao.softDeleteRoutineExercise(
            id = routineExerciseId,
            routineId = routineExercise.routineId,
            deletedAt = timestamp,
            updatedAt = timestamp,
        )

        val remaining = dao.listRoutineExercises(routineExercise.routineId)
        dao.renumber(routineExercise.routineId, remaining.map { it.id }, timestamp)
    }

    suspend fun moveExercise(routineExerciseId: String, direction: MoveDirection) {
        val routineExercise = requireRoutineExercise(routineExerciseId)
        val current = dao.listRoutineExercises(routineExercise.routineId)
        val orderedIds = current.map { it.id }
        val fromIndex = orderedIds.indexOf(routineExerciseId)
        val toIndex = if (direction == MoveDirection.UP) fromIndex - 1 else fromIndex + 1

        val moved = try {
            RoutineOrder.moveItem(orderedIds, fromIndex, toIndex)
        } catch (error: IllegalArgumentException) {
            throw RoutineException(
                RoutineErrorCode.INVALID_MOVE,
                "No se puede mover el ejercicio en esa dirección",
            )
        }

        dao.renumber(routineExercise.routineId, moved, now())
    }

    enum class MoveDirection { UP, DOWN }

    private suspend fun requireRoutine(routineId: String): RoutineEntity =
        dao.findRoutineById(routineId)
            ?: throw RoutineException(RoutineErrorCode.ROUTINE_NOT_FOUND, "La rutina no existe")

    private suspend fun requireRoutineExercise(id: String): RoutineExerciseEntity =
        dao.findRoutineExerciseById(id)
            ?: throw RoutineException(
                RoutineErrorCode.ROUTINE_EXERCISE_NOT_FOUND,
                "El ejercicio no está en la rutina",
            )

    private fun validateName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw RoutineException(
                RoutineErrorCode.INVALID_INPUT,
                "El nombre de la rutina no puede estar vacío",
            )
        }
        return trimmed
    }

    private fun RoutineEntity.toDomain(exercises: List<RoutineExerciseRow>) = Routine(
        id = id,
        name = name,
        description = description,
        exercises = exercises.map { it.toDomain() },
    )

    private fun RoutineExerciseEntity.toDomain(exerciseName: String) = RoutineExercise(
        id = id,
        routineId = routineId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        position = position,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        restSeconds = restSeconds,
        notes = notes,
    )

    private fun RoutineExerciseRow.toDomain() = RoutineExercise(
        id = id,
        routineId = routineId,
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        position = position,
        targetSets = targetSets,
        targetReps = targetReps,
        targetWeightKg = targetWeightKg,
        restSeconds = restSeconds,
        notes = notes,
    )
}
