package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import com.fitlog.app.domain.Comparisons

data class ComparisonSetRow(
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "started_at") val startedAtMs: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "is_warmup") val isWarmup: Boolean,
)

data class ExerciseGroupRow(
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "group_slug") val groupSlug: String,
)

@Dao
abstract class ComparisonsDao {

    @Query(
        """
        SELECT s.exercise_id AS exercise_id, s.session_id AS session_id, se.started_at AS started_at,
               s.weight_kg AS weight_kg, s.reps AS reps, s.is_warmup AS is_warmup
        FROM set_entry s
        INNER JOIN session se ON se.id = s.session_id
        WHERE s.deleted_at IS NULL AND se.deleted_at IS NULL
        ORDER BY se.started_at ASC
        """
    )
    abstract suspend fun listComparisonSets(): List<ComparisonSetRow>

    @Query(
        """
        SELECT e.id AS exercise_id, mg.slug AS group_slug
        FROM exercise e
        INNER JOIN muscle_group mg ON mg.id = e.muscle_group_id
        WHERE e.deleted_at IS NULL
        """
    )
    abstract suspend fun listExerciseGroups(): List<ExerciseGroupRow>
}

class ComparisonsRepository(
    private val dao: ComparisonsDao,
) {

    suspend fun sets(): List<Comparisons.SetInput> = dao.listComparisonSets().map { row ->
        Comparisons.SetInput(
            exerciseId = row.exerciseId,
            sessionId = row.sessionId,
            startedAtMs = row.startedAtMs,
            weightKg = row.weightKg,
            reps = row.reps,
            isWarmup = row.isWarmup,
        )
    }

    suspend fun groups(): Map<String, String> =
        dao.listExerciseGroups().associate { it.exerciseId to it.groupSlug }
}
