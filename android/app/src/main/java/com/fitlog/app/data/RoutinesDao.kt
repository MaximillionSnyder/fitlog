package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class RoutineExerciseRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int?,
    @ColumnInfo(name = "target_reps") val targetReps: Int?,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "notes") val notes: String?,
)

@Dao
abstract class RoutinesDao {

    @Query("SELECT * FROM routine WHERE deleted_at IS NULL ORDER BY name ASC")
    abstract suspend fun listRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findRoutineById(id: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRoutine(routine: RoutineEntity)

    @Query("UPDATE routine SET name = :name, description = :description, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun updateRoutine(id: String, name: String, description: String?, updatedAt: Long)

    @Query("UPDATE routine SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun softDeleteRoutine(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        """
        SELECT re.id AS id, re.routine_id AS routine_id, re.exercise_id AS exercise_id,
               e.name AS exercise_name, re.position AS position, re.target_sets AS target_sets,
               re.target_reps AS target_reps, re.target_weight_kg AS target_weight_kg,
               re.rest_seconds AS rest_seconds, re.notes AS notes
        FROM routine_exercise re
        INNER JOIN exercise e ON e.id = re.exercise_id
        WHERE re.routine_id = :routineId AND re.deleted_at IS NULL
        ORDER BY re.position ASC
        """
    )
    abstract suspend fun listRoutineExercises(routineId: String): List<RoutineExerciseRow>

    @Query(
        """
        SELECT re.id AS id, re.routine_id AS routine_id, re.exercise_id AS exercise_id,
               e.name AS exercise_name, re.position AS position, re.target_sets AS target_sets,
               re.target_reps AS target_reps, re.target_weight_kg AS target_weight_kg,
               re.rest_seconds AS rest_seconds, re.notes AS notes
        FROM routine_exercise re
        INNER JOIN exercise e ON e.id = re.exercise_id
        WHERE re.deleted_at IS NULL
        ORDER BY re.routine_id ASC, re.position ASC
        """
    )
    abstract suspend fun listAllRoutineExercises(): List<RoutineExerciseRow>

    @Query("SELECT * FROM routine_exercise WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findRoutineExerciseById(id: String): RoutineExerciseEntity?

    @Query(
        """
        SELECT COUNT(*) FROM routine_exercise
        WHERE routine_id = :routineId AND exercise_id = :exerciseId AND deleted_at IS NULL
        """
    )
    abstract suspend fun countExerciseInRoutine(routineId: String, exerciseId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM routine_exercise
        WHERE routine_id = :routineId AND deleted_at IS NULL
        """
    )
    abstract suspend fun countRoutineExercises(routineId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertRoutineExercise(routineExercise: RoutineExerciseEntity)

    @Query("UPDATE routine_exercise SET position = :position, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun updatePosition(id: String, position: Int, updatedAt: Long)

    @Query(
        """
        UPDATE routine_exercise
        SET position = (SELECT COALESCE(MIN(position), 0) - 1 FROM routine_exercise WHERE routine_id = :routineId),
            deleted_at = :deletedAt,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    abstract suspend fun softDeleteRoutineExercise(
        id: String,
        routineId: String,
        deletedAt: Long,
        updatedAt: Long,
    )

    @Query("UPDATE routine_exercise SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE routine_id = :routineId AND deleted_at IS NULL")
    abstract suspend fun softDeleteRoutineExercises(routineId: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT * FROM exercise WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findActiveExerciseById(id: String): ExerciseEntity?

    @Transaction
    open suspend fun deleteRoutineWithExercises(routineId: String, timestamp: Long) {
        softDeleteRoutineExercises(routineId, timestamp, timestamp)
        softDeleteRoutine(routineId, timestamp, timestamp)
    }

    @Transaction
    open suspend fun renumber(routineId: String, orderedIds: List<String>, timestamp: Long) {
        val positions = orderedIds.mapIndexed { index, id -> id to index + 1 }
        for ((id, position) in positions) {
            updatePosition(id, -(1_000_000 + position), timestamp)
        }
        for ((id, position) in positions) {
            updatePosition(id, position, timestamp)
        }
    }
}
