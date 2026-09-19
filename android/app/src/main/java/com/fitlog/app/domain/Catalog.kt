package com.fitlog.app.domain

data class MuscleGroup(
    val id: String,
    val slug: String,
    val name: String,
    val bodyRegion: String,
)

enum class ExerciseKind(val wire: String) {
    STRENGTH("strength"),
    CARDIO("cardio"),
    MOBILITY("mobility"),
    ;

    companion object {
        fun fromWire(value: String): ExerciseKind? = entries.firstOrNull { it.wire == value }
    }
}

data class CatalogExercise(
    val id: String,
    val slug: String,
    val name: String,
    val muscleGroupId: String,
    val secondaryMuscleGroupId: String?,
    val equipment: String,
    val kind: ExerciseKind,
    val isCustom: Boolean,
)

data class CatalogFilters(
    val query: String = "",
    val muscleGroupSlug: String? = null,
    val equipment: String? = null,
    val kind: ExerciseKind? = null,
)

data class CatalogSnapshot(
    val groups: List<MuscleGroup>,
    val exercises: List<CatalogExercise>,
)

object CatalogFilter {

    fun apply(
        exercises: List<CatalogExercise>,
        groups: List<MuscleGroup>,
        filters: CatalogFilters,
    ): List<CatalogExercise> {
        val slugById = groups.associate { group -> group.id to group.slug }
        val query = CatalogText.normalize(filters.query)

        return exercises
            .filter { exercise ->
                if (query.isNotEmpty() && !CatalogText.normalize(exercise.name).contains(query)) {
                    return@filter false
                }
                if (filters.muscleGroupSlug != null) {
                    val primary = slugById[exercise.muscleGroupId]
                    val secondary = exercise.secondaryMuscleGroupId?.let { slugById[it] }
                    if (primary != filters.muscleGroupSlug && secondary != filters.muscleGroupSlug) {
                        return@filter false
                    }
                }
                if (filters.equipment != null && exercise.equipment != filters.equipment) {
                    return@filter false
                }
                if (filters.kind != null && exercise.kind != filters.kind) {
                    return@filter false
                }
                true
            }
            .sortedWith(
                compareBy(
                    { CatalogText.normalize(it.name) },
                    { it.slug },
                )
            )
    }
}
