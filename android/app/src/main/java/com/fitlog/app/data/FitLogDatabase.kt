package com.fitlog.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase

@Dao
interface MetaDao {
    @Query("SELECT COUNT(*) FROM muscle_group")
    suspend fun muscleGroupCount(): Int
}

@Database(
    entities = [
        SchemaMigrationEntity::class,
        MuscleGroupEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        SessionEntity::class,
        SetEntryEntity::class,
        BodyMetricEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FitLogDatabase : RoomDatabase() {
    abstract fun metaDao(): MetaDao
    abstract fun catalogDao(): CatalogDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun routinesDao(): RoutinesDao
    abstract fun progressDao(): ProgressDao
    abstract fun comparisonsDao(): ComparisonsDao
    abstract fun bodyMetricsDao(): BodyMetricsDao
    abstract fun backupDao(): BackupDao
}
