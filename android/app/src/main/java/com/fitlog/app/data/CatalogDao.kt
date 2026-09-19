package com.fitlog.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class CatalogDao {

    @Query("SELECT COUNT(*) FROM muscle_group")
    abstract suspend fun countMuscleGroups(): Int

    @Query("SELECT * FROM muscle_group WHERE deleted_at IS NULL ORDER BY name")
    abstract suspend fun listMuscleGroups(): List<MuscleGroupEntity>

    @Query("SELECT * FROM exercise WHERE deleted_at IS NULL ORDER BY name")
    abstract suspend fun listExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE slug = :slug LIMIT 1")
    abstract suspend fun findExerciseBySlug(slug: String): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findActiveExerciseById(id: String): ExerciseEntity?

    @Query("SELECT * FROM muscle_group WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findActiveMuscleGroupById(id: String): MuscleGroupEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMuscleGroups(groups: List<MuscleGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertSetting(setting: AppSettingEntity)

    @Query("UPDATE exercise SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun softDeleteExercise(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM exercise")
    abstract suspend fun countExercises(): Int

    @Transaction
    open suspend fun insertSeed(
        groups: List<MuscleGroupEntity>,
        exercises: List<ExerciseEntity>,
        setting: AppSettingEntity,
    ) {
        insertMuscleGroups(groups)
        insertExercises(exercises)
        insertSetting(setting)
    }
}
