package com.fitlog.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class BackupDao {

    @Query("SELECT * FROM exercise")
    abstract suspend fun listExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM routine")
    abstract suspend fun listRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_exercise")
    abstract suspend fun listRoutineExercises(): List<RoutineExerciseEntity>

    @Query("SELECT * FROM session")
    abstract suspend fun listSessions(): List<SessionEntity>

    @Query("SELECT * FROM set_entry")
    abstract suspend fun listSetEntries(): List<SetEntryEntity>

    @Query("SELECT * FROM body_metric")
    abstract suspend fun listBodyMetrics(): List<BodyMetricEntity>

    @Query("SELECT * FROM app_setting")
    abstract suspend fun listSettings(): List<AppSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertExercise(entity: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRoutine(entity: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertRoutineExercise(entity: RoutineExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSession(entity: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSetEntry(entity: SetEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertBodyMetric(entity: BodyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSetting(entity: AppSettingEntity)

    @Query("UPDATE routine_exercise SET position = -1 * (position + 1) WHERE routine_id IN (:routineIds)")
    abstract suspend fun shiftPositions(routineIds: List<String>)
}
