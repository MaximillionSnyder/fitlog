package com.fitlog.app

import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogFilter
import com.fitlog.app.domain.CatalogFilters
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.domain.ExerciseKind
import com.fitlog.app.domain.MuscleGroup
import com.fitlog.app.domain.Ulid
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFilterTest {

    private val seed = Json.parseToJsonElement(TestVectors.loadText("catalog.json")).jsonObject
    private val vectors = Json.parseToJsonElement(TestVectors.loadText("catalog-filters.json")).jsonObject

    private val groups: List<MuscleGroup> = seed.getValue("muscle_groups").jsonArray.map { element ->
        val item = element.jsonObject
        MuscleGroup(
            id = item.getValue("id").jsonPrimitive.content,
            slug = item.getValue("slug").jsonPrimitive.content,
            name = item.getValue("name").jsonPrimitive.content,
            bodyRegion = item.getValue("body_region").jsonPrimitive.content,
        )
    }

    private val exercises: List<CatalogExercise> = seed.getValue("exercises").jsonArray.map { element ->
        val item = element.jsonObject
        CatalogExercise(
            id = item.getValue("id").jsonPrimitive.content,
            slug = item.getValue("slug").jsonPrimitive.content,
            name = item.getValue("name").jsonPrimitive.content,
            muscleGroupId = item.getValue("muscle_group_id").jsonPrimitive.content,
            secondaryMuscleGroupId = item.getValue("secondary_muscle_group_id")
                .jsonPrimitive
                .content
                .takeIf { it != "null" },
            equipment = item.getValue("equipment").jsonPrimitive.content,
            kind = ExerciseKind.fromWire(item.getValue("kind").jsonPrimitive.content)
                ?: error("tipo desconocido"),
            isCustom = item.getValue("is_custom").jsonPrimitive.content == "1",
        )
    }

    @Test
    fun `el catalogo base tiene al menos 10 grupos y 25 ejercicios`() {
        assertTrue(groups.size >= 10)
        assertTrue(exercises.size >= 25)
    }

    @Test
    fun `todos los identificadores del catalogo son ULID validos`() {
        (groups.map { it.id } + exercises.map { it.id }).forEach { id ->
            assertTrue("ULID invalido: $id", Ulid.isValid(id))
        }
    }

    @Test
    fun `los slugs son unicos y las referencias existen`() {
        val slugs = (groups.map { it.slug } + exercises.map { it.slug })
        assertEquals(slugs.size, slugs.toSet().size)

        val groupIds = groups.map { it.id }.toSet()
        exercises.forEach { exercise ->
            assertTrue("grupo inexistente en ${exercise.slug}", exercise.muscleGroupId in groupIds)
            exercise.secondaryMuscleGroupId?.let { secondary ->
                assertTrue("grupo secundario inexistente en ${exercise.slug}", secondary in groupIds)
            }
        }
    }

    @Test
    fun `la normalizacion coincide con los vectores compartidos`() {
        vectors.getValue("normalize").jsonArray.forEach { element ->
            val item = element.jsonObject
            val input = item.getValue("input").jsonPrimitive.content
            val expected = item.getValue("expected").jsonPrimitive.content
            assertEquals(input, expected, CatalogText.normalize(input))
        }
    }

    @Test
    fun `el slug coincide con los vectores compartidos`() {
        vectors.getValue("slugify").jsonArray.forEach { element ->
            val item = element.jsonObject
            val input = item.getValue("input").jsonPrimitive.content
            val expected = item.getValue("expected").jsonPrimitive.content
            assertEquals(input, expected, CatalogText.slugify(input))
        }
    }

    @Test
    fun `el filtrado coincide con los vectores compartidos`() {
        vectors.getValue("cases").jsonArray.forEach { element ->
            val item = element.jsonObject
            val filters = CatalogFilters(
                query = item.getValue("query").jsonPrimitive.content,
                muscleGroupSlug = item.getValue("muscle_group_slug").jsonPrimitive.content
                    .takeIf { it != "null" },
                equipment = item.getValue("equipment").jsonPrimitive.content.takeIf { it != "null" },
                kind = item.getValue("kind").jsonPrimitive.content
                    .takeIf { it != "null" }
                    ?.let { kind -> ExerciseKind.fromWire(kind) },
            )
            val expected = item.getValue("expected").jsonArray.map { it.jsonPrimitive.content }
            val actual = CatalogFilter.apply(exercises, groups, filters).map { it.slug }

            assertEquals(item.getValue("name").jsonPrimitive.content, expected, actual)
        }
    }
}
