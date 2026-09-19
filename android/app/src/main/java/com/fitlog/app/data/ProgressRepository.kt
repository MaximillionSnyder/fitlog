package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import com.fitlog.app.domain.Progress

data class ProgressSetRow(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "started_at") val startedAtMs: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "is_warmup") val isWarmup: Boolean,
)

@Dao
abstract class ProgressDao {

    @Query(
        """
        SELECT s.session_id AS session_id, se.started_at AS started_at, s.weight_kg AS weight_kg,
               s.reps AS reps, s.is_warmup AS is_warmup
        FROM set_entry s
        INNER JOIN session se ON se.id = s.session_id
        WHERE s.exercise_id = :exerciseId AND s.deleted_at IS NULL AND se.deleted_at IS NULL
        ORDER BY se.started_at ASC
        """
    )
    abstract suspend fun listProgressSets(exerciseId: String): List<ProgressSetRow>
}

class ProgressRepository(
    private val dao: ProgressDao,
) {

    suspend fun series(
        exerciseId: String,
        range: Progress.Range?,
    ): List<Progress.Point> = Progress.buildSeries(
        sets = dao.listProgressSets(exerciseId).map { row ->
            Progress.SetInput(
                sessionId = row.sessionId,
                startedAtMs = row.startedAtMs,
                weightKg = row.weightKg,
                reps = row.reps,
                isWarmup = row.isWarmup,
            )
        },
        range = range,
    )
}
