package com.fitlog.app.data

import org.json.JSONObject

data class CatalogSeedGroup(
    val id: String,
    val slug: String,
    val name: String,
    val bodyRegion: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CatalogSeedExercise(
    val id: String,
    val slug: String,
    val name: String,
    val muscleGroupId: String,
    val secondaryMuscleGroupId: String?,
    val equipment: String,
    val kind: String,
    val isCustom: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CatalogSeed(
    val muscleGroups: List<CatalogSeedGroup>,
    val exercises: List<CatalogSeedExercise>,
)

object CatalogSeedParser {

    fun parse(json: String): CatalogSeed {
        val root = JSONObject(json)
        val groups = root.getJSONArray("muscle_groups").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                CatalogSeedGroup(
                    id = item.getString("id"),
                    slug = item.getString("slug"),
                    name = item.getString("name"),
                    bodyRegion = item.getString("body_region"),
                    createdAt = item.getLong("created_at"),
                    updatedAt = item.getLong("updated_at"),
                )
            }
        }
        val exercises = root.getJSONArray("exercises").let { array ->
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                CatalogSeedExercise(
                    id = item.getString("id"),
                    slug = item.getString("slug"),
                    name = item.getString("name"),
                    muscleGroupId = item.getString("muscle_group_id"),
                    secondaryMuscleGroupId = if (item.isNull("secondary_muscle_group_id")) {
                        null
                    } else {
                        item.getString("secondary_muscle_group_id")
                    },
                    equipment = item.getString("equipment"),
                    kind = item.getString("kind"),
                    isCustom = item.getInt("is_custom") == 1,
                    createdAt = item.getLong("created_at"),
                    updatedAt = item.getLong("updated_at"),
                )
            }
        }
        return CatalogSeed(muscleGroups = groups, exercises = exercises)
    }
}
