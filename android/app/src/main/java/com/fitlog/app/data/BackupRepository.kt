package com.fitlog.app.data

import androidx.room.withTransaction
import com.fitlog.app.domain.Backup

data class BackupSectionGroup(
    val id: String,
    val label: String,
    val tables: List<String>,
)

class BackupRepository(
    private val database: FitLogDatabase,
    private val backupDao: BackupDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun readSnapshot(tables: List<String>, includeBaseExercises: Boolean = false): Backup.Snapshot {
        val snapshot = Backup.emptySnapshot()
        for (table in tables) {
            snapshot.tables[table] = readTable(table, includeBaseExercises).toMutableList()
        }
        return snapshot
    }

    suspend fun export(tables: List<String>, appVersion: String): String {
        val tablesToRead = if (tables.contains("exercise")) tables else tables + "exercise"
        val snapshot = readSnapshot(tablesToRead)
        return Backup.serialize(snapshot, tables, appVersion, now())
    }

    suspend fun import(json: String): Map<String, Backup.SectionSummary> {
        val file = Backup.parse(json)
        val local = readSnapshot(Backup.TABLES, includeBaseExercises = true)
        val merged = Backup.merge(local, file)

        database.withTransaction {
            val touchedRoutines = merged.snapshot.rows("routine_exercise")
                .mapNotNull { it.values["routine_id"]?.toString() }
                .distinct()

            if (touchedRoutines.isNotEmpty()) {
                backupDao.shiftPositions(touchedRoutines)
            }

            for (table in Backup.TABLES) {
                for (row in merged.snapshot.rows(table)) {
                    when (table) {
                        "exercise" -> backupDao.upsertExercise(row.toExerciseEntity())
                        "routine" -> backupDao.upsertRoutine(row.toRoutineEntity())
                        "session" -> backupDao.upsertSession(row.toSessionEntity())
                        "set_entry" -> backupDao.upsertSetEntry(row.toSetEntryEntity())
                        "body_metric" -> backupDao.upsertBodyMetric(row.toBodyMetricEntity())
                        "app_setting" -> backupDao.upsertSetting(row.toAppSettingEntity())
                        "routine_exercise" -> Unit
                    }
                }
            }

            for (routineId in touchedRoutines) {
                val rows = merged.snapshot.rows("routine_exercise")
                    .filter { it.values["routine_id"]?.toString() == routineId }

                val active = rows
                    .filter { it.values["deleted_at"] == null }
                    .sortedWith(
                        compareBy(
                            { (it.values["position"] as? Double)?.toInt() ?: 0 },
                            { it.values["id"]?.toString() ?: "" },
                        )
                    )
                val deleted = rows.filter { it.values["deleted_at"] != null }

                val positions = mutableMapOf<String, Int>()
                active.forEachIndexed { index, row ->
                    positions[row.values["id"]?.toString() ?: ""] = index + 1
                }
                deleted.forEachIndexed { index, row ->
                    positions[row.values["id"]?.toString() ?: ""] = -(1_000_000 + index)
                }

                for (row in rows) {
                    val id = row.values["id"]?.toString() ?: continue
                    backupDao.upsertRoutineExercise(
                        row.toRoutineExerciseEntity(position = positions[id] ?: 1)
                    )
                }
            }
        }

        return merged.summary
    }

    private suspend fun readTable(table: String, includeBaseExercises: Boolean): List<Backup.Row> =
        when (table) {
            "exercise" -> backupDao.listExercises().filter { includeBaseExercises || it.isCustom }
                .map { it.toBackupRow() }
            "routine" -> backupDao.listRoutines().map { it.toBackupRow() }
            "routine_exercise" -> backupDao.listRoutineExercises().map { it.toBackupRow() }
            "session" -> backupDao.listSessions().map { it.toBackupRow() }
            "set_entry" -> backupDao.listSetEntries().map { it.toBackupRow() }
            "body_metric" -> backupDao.listBodyMetrics().map { it.toBackupRow() }
            "app_setting" -> backupDao.listSettings().map { it.toBackupRow() }
            else -> emptyList()
        }

    companion object {
        val GROUPS = listOf(
            BackupSectionGroup("custom_exercises", "Ejercicios propios", listOf("exercise")),
            BackupSectionGroup("routines", "Rutinas", listOf("routine", "routine_exercise")),
            BackupSectionGroup("workouts", "Entrenamientos", listOf("session", "set_entry")),
            BackupSectionGroup("body_metrics", "Medidas", listOf("body_metric")),
            BackupSectionGroup("settings", "Ajustes", listOf("app_setting")),
        )

        fun tablesFor(groupIds: List<String>): List<String> =
            GROUPS.filter { it.id in groupIds }.flatMap { it.tables }
    }
}

private fun value(row: Backup.Row, column: String): Any? = row.values[column]

private fun text(row: Backup.Row, column: String): String = value(row, column)?.toString() ?: ""

private fun textOrNull(row: Backup.Row, column: String): String? = value(row, column)?.toString()

private fun number(row: Backup.Row, column: String): Long =
    (value(row, column) as? Double)?.toLong() ?: 0L

private fun numberOrNull(row: Backup.Row, column: String): Long? = (value(row, column) as? Double)?.toLong()

private fun realOrNull(row: Backup.Row, column: String): Double? = value(row, column) as? Double

private fun Backup.Row.toExerciseEntity() = ExerciseEntity(
    id = text(this, "id"),
    slug = text(this, "slug"),
    name = text(this, "name"),
    muscleGroupId = text(this, "muscle_group_id"),
    secondaryMuscleGroupId = textOrNull(this, "secondary_muscle_group_id"),
    equipment = text(this, "equipment"),
    kind = text(this, "kind").ifEmpty { "strength" },
    isCustom = (number(this, "is_custom") == 1L),
    createdAt = number(this, "created_at").takeIf { it != 0L } ?: number(this, "updated_at"),
    updatedAt = number(this, "updated_at"),
    deletedAt = numberOrNull(this, "deleted_at"),
)

private fun Backup.Row.toRoutineEntity() = RoutineEntity(
    id = text(this, "id"),
    name = text(this, "name"),
    description = textOrNull(this, "description"),
    createdAt = number(this, "created_at").takeIf { it != 0L } ?: number(this, "updated_at"),
    updatedAt = number(this, "updated_at"),
    deletedAt = numberOrNull(this, "deleted_at"),
)

private fun Backup.Row.toRoutineExerciseEntity(position: Int) = RoutineExerciseEntity(
    id = text(this, "id"),
    routineId = text(this, "routine_id"),
    exerciseId = text(this, "exercise_id"),
    position = position,
    targetSets = numberOrNull(this, "target_sets")?.toInt(),
    targetReps = numberOrNull(this, "target_reps")?.toInt(),
    targetWeightKg = realOrNull(this, "target_weight_kg"),
    restSeconds = numberOrNull(this, "rest_seconds")?.toInt(),
    notes = textOrNull(this, "notes"),
    createdAt = number(this, "created_at").takeIf { it != 0L } ?: number(this, "updated_at"),
    updatedAt = number(this, "updated_at"),
    deletedAt = numberOrNull(this, "deleted_at"),
)

private fun Backup.Row.toSessionEntity() = SessionEntity(
    id = text(this, "id"),
    routineId = textOrNull(this, "routine_id"),
    startedAt = number(this, "started_at"),
    finishedAt = numberOrNull(this, "finished_at"),
    notes = textOrNull(this, "notes"),
    createdAt = number(this, "created_at").takeIf { it != 0L } ?: number(this, "updated_at"),
    updatedAt = number(this, "updated_at"),
    deletedAt = numberOrNull(this, "deleted_at"),
)

private fun Backup.Row.toSetEntryEntity() = SetEntryEntity(
    id = text(this, "id"),
    sessionId = text(this, "session_id"),
    exerciseId = text(this, "exercise_id"),
    setIndex = number(this, "set_index").toInt(),
    weightKg = realOrNull(this, "weight_kg"),
    reps = numberOrNull(this, "reps")?.toInt(),
    rir = numberOrNull(this, "rir")?.toInt(),
    rpe = realOrNull(this, "rpe"),
    isWarmup = number(this, "is_warmup") == 1L,
    notes = textOrNull(this, "notes"),
    createdAt = number(this, "created_at").takeIf { it != 0L } ?: number(this, "updated_at"),
    updatedAt = number(this, "updated_at"),
    deletedAt = numberOrNull(this, "deleted_at"),
)

private fun Backup.Row.toBodyMetricEntity() = BodyMetricEntity(
    id = text(this, "id"),
    measuredAt = number(this, "measured_at"),
    kind = text(this, "kind").ifEmpty { "other" },
    value = realOrNull(this, "value") ?: 0.0,
    unit = text(this, "unit").ifEmpty { "cm" },
    notes = textOrNull(this, "notes"),
    createdAt = number(this, "created_at").takeIf { it != 0L } ?: number(this, "updated_at"),
    updatedAt = number(this, "updated_at"),
    deletedAt = numberOrNull(this, "deleted_at"),
)

private fun Backup.Row.toAppSettingEntity() = AppSettingEntity(
    key = text(this, "key"),
    value = text(this, "value"),
    updatedAt = number(this, "updated_at"),
)

private fun ExerciseEntity.toBackupRow() = Backup.Row(
    mapOf(
        "id" to id,
        "slug" to slug,
        "name" to name,
        "muscle_group_id" to muscleGroupId,
        "secondary_muscle_group_id" to secondaryMuscleGroupId,
        "equipment" to equipment,
        "kind" to kind,
        "is_custom" to if (isCustom) 1.0 else 0.0,
        "created_at" to createdAt.toDouble(),
        "updated_at" to updatedAt.toDouble(),
        "deleted_at" to deletedAt?.toDouble(),
    )
)

private fun RoutineEntity.toBackupRow() = Backup.Row(
    mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "created_at" to createdAt.toDouble(),
        "updated_at" to updatedAt.toDouble(),
        "deleted_at" to deletedAt?.toDouble(),
    )
)

private fun RoutineExerciseEntity.toBackupRow() = Backup.Row(
    mapOf(
        "id" to id,
        "routine_id" to routineId,
        "exercise_id" to exerciseId,
        "position" to position.toDouble(),
        "target_sets" to targetSets?.toDouble(),
        "target_reps" to targetReps?.toDouble(),
        "target_weight_kg" to targetWeightKg,
        "rest_seconds" to restSeconds?.toDouble(),
        "notes" to notes,
        "created_at" to createdAt.toDouble(),
        "updated_at" to updatedAt.toDouble(),
        "deleted_at" to deletedAt?.toDouble(),
    )
)

private fun SessionEntity.toBackupRow() = Backup.Row(
    mapOf(
        "id" to id,
        "routine_id" to routineId,
        "started_at" to startedAt.toDouble(),
        "finished_at" to finishedAt?.toDouble(),
        "notes" to notes,
        "created_at" to createdAt.toDouble(),
        "updated_at" to updatedAt.toDouble(),
        "deleted_at" to deletedAt?.toDouble(),
    )
)

private fun SetEntryEntity.toBackupRow() = Backup.Row(
    mapOf(
        "id" to id,
        "session_id" to sessionId,
        "exercise_id" to exerciseId,
        "set_index" to setIndex.toDouble(),
        "weight_kg" to weightKg,
        "reps" to reps?.toDouble(),
        "rir" to rir?.toDouble(),
        "rpe" to rpe,
        "is_warmup" to if (isWarmup) 1.0 else 0.0,
        "notes" to notes,
        "created_at" to createdAt.toDouble(),
        "updated_at" to updatedAt.toDouble(),
        "deleted_at" to deletedAt?.toDouble(),
    )
)

private fun BodyMetricEntity.toBackupRow() = Backup.Row(
    mapOf(
        "id" to id,
        "measured_at" to measuredAt.toDouble(),
        "kind" to kind,
        "value" to value,
        "unit" to unit,
        "notes" to notes,
        "created_at" to createdAt.toDouble(),
        "updated_at" to updatedAt.toDouble(),
        "deleted_at" to deletedAt?.toDouble(),
    )
)

private fun AppSettingEntity.toBackupRow() = Backup.Row(
    mapOf(
        "key" to key,
        "value" to value,
        "updated_at" to updatedAt.toDouble(),
    )
)
