package com.fitlog.app.data

import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogSnapshot
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.domain.ExerciseKind
import com.fitlog.app.domain.MuscleGroup
import com.fitlog.app.domain.Ulid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CatalogErrorCode {
    INVALID_INPUT,
    DUPLICATE_SLUG,
    MUSCLE_GROUP_NOT_FOUND,
    EXERCISE_NOT_FOUND,
    BASE_CATALOG_PROTECTED,
}

class CatalogException(
    val code: CatalogErrorCode,
    message: String,
) : Exception(message)

data class CreateCustomExerciseInput(
    val name: String,
    val muscleGroupId: String,
    val equipment: String,
    val kind: ExerciseKind,
)

data class SeedResult(
    val seeded: Boolean,
    val muscleGroups: Int,
    val exercises: Int,
)

class CatalogSeeder(
    private val dao: CatalogDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun seed(catalogJson: String): SeedResult {
        if (dao.countMuscleGroups() > 0) {
            return SeedResult(seeded = false, muscleGroups = 0, exercises = 0)
        }

        val seed = CatalogSeedParser.parse(catalogJson)
        val timestamp = now()

        dao.insertSeed(
            groups = seed.muscleGroups.map { group ->
                MuscleGroupEntity(
                    id = group.id,
                    slug = group.slug,
                    name = group.name,
                    bodyRegion = group.bodyRegion,
                    createdAt = group.createdAt,
                    updatedAt = group.updatedAt,
                    deletedAt = null,
                )
            },
            exercises = seed.exercises.map { exercise ->
                ExerciseEntity(
                    id = exercise.id,
                    slug = exercise.slug,
                    name = exercise.name,
                    muscleGroupId = exercise.muscleGroupId,
                    secondaryMuscleGroupId = exercise.secondaryMuscleGroupId,
                    equipment = exercise.equipment,
                    kind = exercise.kind,
                    isCustom = exercise.isCustom,
                    createdAt = exercise.createdAt,
                    updatedAt = exercise.updatedAt,
                    deletedAt = null,
                )
            },
            setting = AppSettingEntity(
                key = CATALOG_SEEDED_AT,
                value = timestamp.toString(),
                updatedAt = timestamp,
            ),
        )

        return SeedResult(
            seeded = true,
            muscleGroups = seed.muscleGroups.size,
            exercises = seed.exercises.size,
        )
    }

    companion object {
        const val CATALOG_SEEDED_AT = "catalog_seeded_at"
    }
}

class CatalogRepository(
    private val dao: CatalogDao,
    private val seedJsonProvider: () -> String,
    private val idGenerator: () -> String = { Ulid.generate() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val seedMutex = Mutex()
    private var seeded = false

    suspend fun ensureSeeded(): SeedResult {
        if (seeded) {
            return SeedResult(seeded = false, muscleGroups = 0, exercises = 0)
        }
        return seedMutex.withLock {
            if (seeded) {
                SeedResult(seeded = false, muscleGroups = 0, exercises = 0)
            } else {
                val result = CatalogSeeder(dao, now).seed(seedJsonProvider())
                seeded = true
                result
            }
        }
    }

    suspend fun loadCatalog(): CatalogSnapshot {
        ensureSeeded()
        return CatalogSnapshot(
            groups = dao.listMuscleGroups().map { it.toDomain() },
            exercises = dao.listExercises().map { it.toDomain() },
        )
    }

    suspend fun createCustomExercise(input: CreateCustomExerciseInput): CatalogExercise {
        ensureSeeded()

        val name = input.name.trim()
        if (name.isEmpty()) {
            throw CatalogException(CatalogErrorCode.INVALID_INPUT, "El nombre del ejercicio no puede estar vacío")
        }
        if (dao.findActiveMuscleGroupById(input.muscleGroupId) == null) {
            throw CatalogException(CatalogErrorCode.MUSCLE_GROUP_NOT_FOUND, "El grupo muscular no existe")
        }

        val slug = CatalogText.slugify(name)
        if (dao.findExerciseBySlug(slug) != null) {
            throw CatalogException(CatalogErrorCode.DUPLICATE_SLUG, "Ya existe un ejercicio con ese nombre")
        }

        val timestamp = now()
        val entity = ExerciseEntity(
            id = idGenerator(),
            slug = slug,
            name = name,
            muscleGroupId = input.muscleGroupId,
            secondaryMuscleGroupId = null,
            equipment = input.equipment,
            kind = input.kind.wire,
            isCustom = true,
            createdAt = timestamp,
            updatedAt = timestamp,
            deletedAt = null,
        )

        try {
            dao.insertExercises(listOf(entity))
        } catch (error: Exception) {
            if (error.message?.contains("UNIQUE") == true) {
                throw CatalogException(CatalogErrorCode.DUPLICATE_SLUG, "Ya existe un ejercicio con ese nombre")
            }
            throw error
        }

        return entity.toDomain()
    }

    suspend fun deleteCustomExercise(id: String) {
        ensureSeeded()

        val exercise = dao.findActiveExerciseById(id)
            ?: throw CatalogException(CatalogErrorCode.EXERCISE_NOT_FOUND, "El ejercicio no existe")
        if (!exercise.isCustom) {
            throw CatalogException(
                CatalogErrorCode.BASE_CATALOG_PROTECTED,
                "Los ejercicios del catálogo base no se pueden eliminar",
            )
        }

        val timestamp = now()
        dao.softDeleteExercise(id = id, deletedAt = timestamp, updatedAt = timestamp)
    }

    private fun MuscleGroupEntity.toDomain() = MuscleGroup(
        id = id,
        slug = slug,
        name = name,
        bodyRegion = bodyRegion,
    )

    private fun ExerciseEntity.toDomain() = CatalogExercise(
        id = id,
        slug = slug,
        name = name,
        muscleGroupId = muscleGroupId,
        secondaryMuscleGroupId = secondaryMuscleGroupId,
        equipment = equipment,
        kind = ExerciseKind.fromWire(kind)
            ?: throw IllegalStateException("Tipo de ejercicio desconocido: $kind"),
        isCustom = isCustom,
    )
}
