package com.fitlog.app

import com.fitlog.app.domain.Progress
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressSeriesTest {

    private val vectors = TestVectors.load("progress-series.json").jsonObject

    private fun setInput(element: kotlinx.serialization.json.JsonElement): Progress.SetInput {
        val item = element.jsonObject
        return Progress.SetInput(
            sessionId = item.getValue("session_id").jsonPrimitive.content,
            startedAtMs = item.getValue("started_at_ms").jsonPrimitive.content.toLong(),
            weightKg = item.getValue("weight_kg").jsonPrimitive.doubleOrNull,
            reps = item.getValue("reps").jsonPrimitive.intOrNull,
            isWarmup = item.getValue("is_warmup").jsonPrimitive.intOrNull == 1,
        )
    }

    @Test
    fun `los rangos coinciden con los vectores compartidos`() {
        vectors.getValue("range_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val preset = when (testCase.getValue("preset").jsonPrimitive.content) {
                "30d" -> Progress.RangePreset.LAST_30_DAYS
                "90d" -> Progress.RangePreset.LAST_90_DAYS
                else -> Progress.RangePreset.ALL
            }
            val nowMs = testCase.getValue("now_ms").jsonPrimitive.content.toLong()
            val expected = testCase.getValue("expected")

            val range = Progress.rangeFor(preset, nowMs)
            if (expected is kotlinx.serialization.json.JsonNull) {
                assertNull(range)
            } else {
                val expectedObject = expected.jsonObject
                assertEquals(
                    expectedObject.getValue("from_ms").jsonPrimitive.content.toLong(),
                    range?.fromMs,
                )
                assertEquals(
                    expectedObject.getValue("to_ms").jsonPrimitive.content.toLong(),
                    range?.toMs,
                )
            }
        }
    }

    @Test
    fun `las series coinciden con los vectores compartidos`() {
        vectors.getValue("series_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val sets = testCase.getValue("sets").jsonArray.map { setInput(it) }
            val expected = testCase.getValue("expected").jsonArray

            val points = Progress.buildSeries(sets)
            assertEquals("$name: cantidad de puntos", expected.size, points.size)

            expected.forEachIndexed { index, expectedElement ->
                val item = expectedElement.jsonObject
                val point = points[index]
                assertEquals(item.getValue("session_id").jsonPrimitive.content, point.sessionId)
                assertEquals(
                    item.getValue("started_at_ms").jsonPrimitive.content.toLong(),
                    point.startedAtMs,
                )
                assertEquals(
                    item.getValue("max_weight_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    point.maxWeightKg,
                    1e-9,
                )
                assertEquals(
                    item.getValue("volume_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    point.volumeKg,
                    1e-9,
                )
                assertEquals(
                    item.getValue("best_one_rep_max_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                    point.bestOneRepMaxKg,
                    1e-9,
                )
                assertEquals(item.getValue("working_sets").jsonPrimitive.intOrNull, point.workingSets)
            }
        }
    }

    @Test
    fun `los filtros por rango coinciden con los vectores compartidos`() {
        val filterSets = vectors.getValue("filter_sets").jsonArray.map { setInput(it) }

        vectors.getValue("filter_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val fromMs = testCase.getValue("from_ms").jsonPrimitive.doubleOrNull?.toLong()
            val toMs = testCase.getValue("to_ms").jsonPrimitive.doubleOrNull?.toLong() ?: Long.MAX_VALUE
            val expected = testCase.getValue("expected").jsonArray.map { it.jsonPrimitive.content }

            val points = Progress.buildSeries(filterSets, Progress.Range(fromMs = fromMs, toMs = toMs))
            assertEquals(name, expected, points.map { it.sessionId })
        }
    }

    @Test
    fun `la metrica elegida se lee del punto`() {
        val point = Progress.Point(
            sessionId = "s1",
            startedAtMs = 1000,
            maxWeightKg = 100.0,
            volumeKg = 800.0,
            bestOneRepMaxKg = 126.7,
            workingSets = 2,
        )

        assertEquals(100.0, Progress.metricValue(point, Progress.Metric.MAX_WEIGHT), 1e-9)
        assertEquals(800.0, Progress.metricValue(point, Progress.Metric.VOLUME), 1e-9)
        assertEquals(126.7, Progress.metricValue(point, Progress.Metric.BEST_ONE_REP_MAX), 1e-9)
    }
}
