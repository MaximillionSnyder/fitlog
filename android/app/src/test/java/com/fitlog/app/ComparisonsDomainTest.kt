package com.fitlog.app

import com.fitlog.app.domain.Comparisons
import com.fitlog.app.domain.Progress
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComparisonsDomainTest {

    private val vectors = TestVectors.load("comparisons.json").jsonObject

    private fun setInput(element: kotlinx.serialization.json.JsonElement): Comparisons.SetInput {
        val item = element.jsonObject
        return Comparisons.SetInput(
            exerciseId = item.getValue("exercise_id").jsonPrimitive.content,
            sessionId = item.getValue("session_id").jsonPrimitive.content,
            startedAtMs = item.getValue("started_at_ms").jsonPrimitive.content.toLong(),
            weightKg = item.getValue("weight_kg").jsonPrimitive.doubleOrNull,
            reps = item.getValue("reps").jsonPrimitive.intOrNull,
            isWarmup = item.getValue("is_warmup").jsonPrimitive.intOrNull == 1,
        )
    }

    private fun setsOf(element: kotlinx.serialization.json.JsonElement): List<Comparisons.SetInput> =
        element.jsonArray.map { setInput(it) }

    @Test
    fun `los rangos de comparacion coinciden con los vectores`() {
        vectors.getValue("range_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val preset = if (testCase.getValue("preset").jsonPrimitive.content == "30d") {
                Comparisons.Preset.LAST_30_DAYS
            } else {
                Comparisons.Preset.LAST_90_DAYS
            }
            val ranges = Comparisons.comparisonRanges(
                preset,
                testCase.getValue("now_ms").jsonPrimitive.content.toLong(),
            )
            val expected = testCase.getValue("expected").jsonObject

            assertEquals(
                expected.getValue("current").jsonObject.getValue("from_ms").jsonPrimitive.content.toLong(),
                ranges.current.fromMs,
            )
            assertEquals(
                expected.getValue("current").jsonObject.getValue("to_ms").jsonPrimitive.content.toLong(),
                ranges.current.toMs,
            )
            assertEquals(
                expected.getValue("previous").jsonObject.getValue("from_ms").jsonPrimitive.content.toLong(),
                ranges.previous.fromMs,
            )
            assertEquals(
                expected.getValue("previous").jsonObject.getValue("to_ms").jsonPrimitive.content.toLong(),
                ranges.previous.toMs,
            )
        }
    }

    @Test
    fun `las marcas personales coinciden con los vectores`() {
        vectors.getValue("pr_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val records = Comparisons.personalRecords(setsOf(testCase.getValue("sets")))
            val expected = testCase.getValue("expected").jsonArray

            assertEquals("$name: cantidad", expected.size, records.size)
            expected.forEachIndexed { index, expectedElement ->
                val item = expectedElement.jsonObject
                val record = records[index]
                assertEquals(item.getValue("exercise_id").jsonPrimitive.content, record.exerciseId)
                assertEquals(
                    item.getValue("best_weight_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    record.bestWeightKg,
                    1e-9,
                )
                assertEquals(
                    item.getValue("best_weight_at_ms").jsonPrimitive.doubleOrNull?.toLong(),
                    record.bestWeightAtMs,
                )
                assertEquals(
                    item.getValue("best_one_rep_max_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    record.bestOneRepMaxKg,
                    1e-9,
                )
                assertEquals(
                    item.getValue("best_one_rep_max_at_ms").jsonPrimitive.doubleOrNull?.toLong(),
                    record.bestOneRepMaxAtMs,
                )
                assertEquals(
                    item.getValue("best_session_volume_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    record.bestSessionVolumeKg,
                    1e-9,
                )
                assertEquals(
                    item.getValue("best_session_volume_at_ms").jsonPrimitive.doubleOrNull?.toLong(),
                    record.bestSessionVolumeAtMs,
                )
                assertEquals(item.getValue("best_reps").jsonPrimitive.intOrNull, record.bestReps)
                assertEquals(
                    item.getValue("best_reps_at_ms").jsonPrimitive.doubleOrNull?.toLong(),
                    record.bestRepsAtMs,
                )
            }
        }
    }

    @Test
    fun `la comparacion de periodos coincide con los vectores`() {
        vectors.getValue("comparison_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val comparison = Comparisons.comparePeriods(
                sets = setsOf(testCase.getValue("sets")),
                current = rangeOf(testCase.getValue("current")),
                previous = rangeOf(testCase.getValue("previous")),
            )
            val expected = testCase.getValue("expected").jsonObject

            assertEquals(name, expected.getValue("current").jsonObject.getValue("volume_kg").jsonPrimitive.doubleOrNull ?: 0.0, comparison.current.volumeKg, 1e-9)
            assertEquals(name, expected.getValue("current").jsonObject.getValue("working_sets").jsonPrimitive.intOrNull, comparison.current.workingSets)
            assertEquals(name, expected.getValue("current").jsonObject.getValue("sessions").jsonPrimitive.intOrNull, comparison.current.sessions)
            assertEquals(name, expected.getValue("previous").jsonObject.getValue("volume_kg").jsonPrimitive.doubleOrNull ?: 0.0, comparison.previous.volumeKg, 1e-9)
            assertEquals(name, expected.getValue("previous").jsonObject.getValue("working_sets").jsonPrimitive.intOrNull, comparison.previous.workingSets)
            assertEquals(name, expected.getValue("previous").jsonObject.getValue("sessions").jsonPrimitive.intOrNull, comparison.previous.sessions)

            assertDelta("$name: volumen", expected.getValue("volume_delta_pct"), comparison.volumeDeltaPct)
            assertDelta("$name: series", expected.getValue("sets_delta_pct"), comparison.setsDeltaPct)
            assertDelta("$name: sesiones", expected.getValue("sessions_delta_pct"), comparison.sessionsDeltaPct)
        }
    }

    @Test
    fun `el balance muscular coincide con los vectores`() {
        vectors.getValue("balance_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val groups = testCase.getValue("groups").jsonObject.mapValues { it.value.jsonPrimitive.content }

            val balance = Comparisons.muscleBalance(setsOf(testCase.getValue("sets")), groups)
            val expected = testCase.getValue("expected").jsonArray

            assertEquals("$name: cantidad", expected.size, balance.size)
            expected.forEachIndexed { index, expectedElement ->
                val item = expectedElement.jsonObject
                val entry = balance[index]
                assertEquals(item.getValue("muscle_group_slug").jsonPrimitive.content, entry.muscleGroupSlug)
                assertEquals(
                    item.getValue("volume_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    entry.volumeKg,
                    1e-9,
                )
                assertEquals(
                    item.getValue("share_pct").jsonPrimitive.doubleOrNull ?: 0.0,
                    entry.sharePct,
                    1e-9,
                )
            }
        }
    }

    @Test
    fun `el formato de delta muestra porcentajes y ausencia de datos`() {
        assertEquals("+25%", Comparisons.formatDelta(25.0))
        assertEquals("-10.5%", Comparisons.formatDelta(-10.5))
        assertEquals("sin datos", Comparisons.formatDelta(null))
    }

    private fun rangeOf(element: kotlinx.serialization.json.JsonElement): Progress.Range {
        val item = element.jsonObject
        return Progress.Range(
            fromMs = item.getValue("from_ms").jsonPrimitive.doubleOrNull?.toLong(),
            toMs = item.getValue("to_ms").jsonPrimitive.content.toLong(),
        )
    }

    private fun assertDelta(
        label: String,
        expected: kotlinx.serialization.json.JsonElement,
        actual: Double?,
    ) {
        if (expected is JsonNull) {
            assertNull(label, actual)
        } else {
            assertEquals(label, expected.jsonPrimitive.doubleOrNull ?: 0.0, actual ?: 0.0, 1e-9)
        }
    }
}
