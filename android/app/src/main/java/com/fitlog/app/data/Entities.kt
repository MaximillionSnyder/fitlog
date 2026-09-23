package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "schema_migration")
data class SchemaMigrationEntity(
    @PrimaryKey @ColumnInfo(name = "version") val version: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "applied_at") val appliedAt: Long,
)

@Entity(tableName = "muscle_group")
data class MuscleGroupEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "body_region") val bodyRegion: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "exercise",
    foreignKeys = [
        ForeignKey(
            entity = MuscleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["muscle_group_id"],
        ),
        ForeignKey(
            entity = MuscleGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["secondary_muscle_group_id"],
        ),
    ],
    indices = [Index(value = ["muscle_group_id"], name = "idx_exercise_muscle_group")],
)
data class ExerciseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "muscle_group_id") val muscleGroupId: String,
    @ColumnInfo(name = "secondary_muscle_group_id") val secondaryMuscleGroupId: String?,
    @ColumnInfo(name = "equipment") val equipment: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "is_custom", defaultValue = "0") val isCustom: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "routine_exercise",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
        ),
    ],
    indices = [
        Index(value = ["routine_id"], name = "idx_routine_exercise_routine"),
        Index(value = ["routine_id", "position"], unique = true),
    ],
)
data class RoutineExerciseEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "target_sets") val targetSets: Int?,
    @ColumnInfo(name = "target_reps") val targetReps: Int?,
    @ColumnInfo(name = "target_weight_kg") val targetWeightKg: Double?,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "session",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
        ),
    ],
    indices = [Index(value = ["started_at"], name = "idx_session_started_at")],
)
data class SessionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    @ColumnInfo(name = "notes") val notes: String?,
    // Metricas de una sesion importada (Huawei Health o GPX): opcionales.
    @ColumnInfo(name = "distance_m") val distanceM: Double? = null,
    @ColumnInfo(name = "calories") val calories: Double? = null,
    @ColumnInfo(name = "avg_heart_rate") val avgHeartRate: Double? = null,
    @ColumnInfo(name = "max_heart_rate") val maxHeartRate: Double? = null,
    @ColumnInfo(name = "steps") val steps: Int? = null,
    @ColumnInfo(name = "elevation_gain_m") val elevationGainM: Double? = null,
    @ColumnInfo(name = "source") val source: String? = null,
    /** Ruta del recorrido en texto compacto (solo GPX). */
    @ColumnInfo(name = "route") val route: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "set_entry",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
        ),
    ],
    indices = [
        Index(value = ["session_id"], name = "idx_set_entry_session"),
        Index(value = ["exercise_id"], name = "idx_set_entry_exercise"),
    ],
)
data class SetEntryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "rir") val rir: Int?,
    @ColumnInfo(name = "rpe") val rpe: Double?,
    @ColumnInfo(name = "is_warmup", defaultValue = "0") val isWarmup: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "body_metric",
    indices = [Index(value = ["kind", "measured_at"], name = "idx_body_metric_kind_time")],
)
data class BodyMetricEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "value") val value: Double,
    @ColumnInfo(name = "unit") val unit: String,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(tableName = "app_setting")
data class AppSettingEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
