package com.fitlog.app

import com.fitlog.app.domain.Body
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMetricsDomainTest {

    private val vectors = TestVectors.load("body-metrics.json").jsonObject

    @Test
    fun `las unidades coinciden con los vectores compartidos`() {
        vectors.getValue("unit_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val kind = Body.Kind.fromWire(testCase.getValue("kind").jsonPrimitive.content)
            assertNotNull(kind)
            assertEquals(
                testCase.getValue("expected").jsonPrimitive.content,
                Body.unitForKind(kind!!).wire,
            )
        }
    }

    @Test
    fun `las validaciones coinciden con los vectores compartidos`() {
        vectors.getValue("validation_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val kind = testCase.getValue("kind").jsonPrimitive.content
            val value = testCase.getValue("value").jsonPrimitive.doubleOrNull ?: 0.0
            val valid = testCase.getValue("valid").jsonPrimitive.content.toBoolean()

            val error = Body.validate(kind, value)
            if (valid) {
                assertNull(testCase.getValue("name").jsonPrimitive.content, error)
            } else {
                assertNotNull(testCase.getValue("name").jsonPrimitive.content, error)
            }
        }
    }

    @Test
    fun `las series coinciden con los vectores compartidos`() {
        vectors.getValue("series_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val kind = Body.Kind.fromWire(testCase.getValue("kind").jsonPrimitive.content)!!
            val metrics = testCase.getValue("metrics").jsonArray.map { metricElement ->
                val item = metricElement.jsonObject
                val metricKind = Body.Kind.fromWire(item.getValue("kind").jsonPrimitive.content)!!
                Body.Point(
                    id = item.getValue("id").jsonPrimitive.content,
                    kind = metricKind,
                    measuredAtMs = item.getValue("measured_at_ms").jsonPrimitive.content.toLong(),
                    value = item.getValue("value").jsonPrimitive.doubleOrNull ?: 0.0,
                    unit = Body.unitForKind(metricKind),
                    notes = null,
                )
            }
            val range = if (testCase.containsKey("from_ms")) {
                Progress.Range(
                    fromMs = testCase.getValue("from_ms").jsonPrimitive.doubleOrNull?.toLong(),
                    toMs = testCase.getValue("to_ms").jsonPrimitive.doubleOrNull?.toLong() ?: Long.MAX_VALUE,
                )
            } else {
                null
            }
            val expected = testCase.getValue("expected").jsonArray.map { it.jsonPrimitive.content }

            val ids = Body.series(metrics, kind, range).map { it.id }
            assertEquals(testCase.getValue("name").jsonPrimitive.content, expected, ids)
        }
    }

    @Test
    fun `las estadisticas coinciden con los vectores compartidos`() {
        vectors.getValue("stats_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val points = testCase.getValue("values").jsonArray.mapIndexed { index, valueElement ->
                Body.Point(
                    id = "m$index",
                    kind = Body.Kind.BODY_WEIGHT,
                    measuredAtMs = 1_000L + index,
                    value = valueElement.jsonPrimitive.doubleOrNull ?: 0.0,
                    unit = Body.Unit.KG,
                    notes = null,
                )
            }
            val expected = testCase.getValue("expected").jsonObject
            val stats = Body.stats(points)

            assertEquals(expected.getValue("count").jsonPrimitive.intOrNull, stats.count)
            assertNullable(expected.getValue("first"), stats.first)
            assertNullable(expected.getValue("latest"), stats.latest)
            assertNullable(expected.getValue("min"), stats.min)
            assertNullable(expected.getValue("max"), stats.max)
            assertNullable(expected.getValue("delta_abs"), stats.deltaAbs)
            assertNullable(expected.getValue("delta_pct"), stats.deltaPct)
        }
    }

    @Test
    fun `el formato de valores coincide`() {
        assertEquals("78.5 kg", Body.formatValue(78.5, Body.Unit.KG))
        assertEquals("80 cm", Body.formatValue(80.0, Body.Unit.CM))
        assertEquals("18.4 %", Body.formatValue(18.4, Body.Unit.PERCENT))
    }

    private fun assertNullable(
        expected: kotlinx.serialization.json.JsonElement,
        actual: Double?,
    ) {
        if (expected is JsonNull) {
            assertNull(actual)
        } else {
            assertEquals(expected.jsonPrimitive.doubleOrNull ?: 0.0, actual ?: 0.0, 1e-9)
        }
    }
}
