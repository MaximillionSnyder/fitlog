package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class WorkoutSetRow(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "rir") val rir: Int?,
    @ColumnInfo(name = "is_warmup") val isWarmup: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
)

@Dao
abstract class WorkoutDao {

    @Query(
        "SELECT * FROM session WHERE finished_at IS NULL AND deleted_at IS NULL " +
            "ORDER BY started_at DESC LIMIT 1"
    )
    abstract suspend fun findActiveSession(): SessionEntity?

    @Query("SELECT * FROM session WHERE deleted_at IS NULL ORDER BY started_at DESC")
    abstract suspend fun listSessions(): List<SessionEntity>

    @Query("SELECT * FROM session WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findSessionById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE session SET finished_at = :finishedAt, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun finishSession(id: String, finishedAt: Long, updatedAt: Long)

    @Query(
        """
        INSERT INTO set_entry
            (id, session_id, exercise_id, set_index, weight_kg, reps, rir, rpe, is_warmup, notes, created_at, updated_at, deleted_at)
        VALUES
            (:id, :sessionId, :exerciseId,
             (SELECT COALESCE(MAX(set_index), 0) + 1 FROM set_entry
               WHERE session_id = :sessionId AND exercise_id = :exerciseId AND deleted_at IS NULL),
             :weightKg, :reps, :rir, NULL, :isWarmup, :notes, :createdAt, :updatedAt, NULL)
        """
    )
    abstract suspend fun insertSet(
        id: String,
        sessionId: String,
        exerciseId: String,
        weightKg: Double?,
        reps: Int?,
        rir: Int?,
        isWarmup: Boolean,
        notes: String?,
        createdAt: Long,
        updatedAt: Long,
    )

    @Query(
        """
        SELECT s.id AS id, s.session_id AS session_id, s.exercise_id AS exercise_id,
               e.name AS exercise_name, s.set_index AS set_index, s.weight_kg AS weight_kg,
               s.reps AS reps, s.rir AS rir, s.is_warmup AS is_warmup, s.notes AS notes
        FROM set_entry s
        INNER JOIN exercise e ON e.id = s.exercise_id
        WHERE s.session_id = :sessionId AND s.deleted_at IS NULL
        ORDER BY e.name ASC, s.set_index ASC
        """
    )
    abstract suspend fun listSetsForSession(sessionId: String): List<WorkoutSetRow>

    @Query(
        """
        SELECT s.id AS id, s.session_id AS session_id, s.exercise_id AS exercise_id,
               e.name AS exercise_name, s.set_index AS set_index, s.weight_kg AS weight_kg,
               s.reps AS reps, s.rir AS rir, s.is_warmup AS is_warmup, s.notes AS notes
        FROM set_entry s
        INNER JOIN exercise e ON e.id = s.exercise_id
        WHERE s.deleted_at IS NULL
        ORDER BY e.name ASC, s.set_index ASC
        """
    )
    abstract suspend fun listAllSets(): List<WorkoutSetRow>

    @Query("SELECT * FROM set_entry WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findSetById(id: String): SetEntryEntity?

    @Query(
        "UPDATE set_entry SET weight_kg = :weightKg, reps = :reps, rir = :rir, notes = :notes, " +
            "updated_at = :updatedAt WHERE id = :id"
    )
    abstract suspend fun updateSet(
        id: String,
        weightKg: Double?,
        reps: Int?,
        rir: Int?,
        notes: String?,
        updatedAt: Long,
    )

    @Query("UPDATE set_entry SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun softDeleteSet(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT * FROM exercise WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findActiveExerciseById(id: String): ExerciseEntity?
}
