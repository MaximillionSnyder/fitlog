package com.fitlog.app

import com.fitlog.app.domain.Comparisons
import com.fitlog.app.domain.Insights
import com.fitlog.app.domain.Progress
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsDomainTest {

    private val vectors = TestVectors.load("insights.json").jsonObject

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

    @Test
    fun `los consejos coinciden con los vectores compartidos`() {
        vectors.getValue("cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val groups = testCase.getValue("groups").jsonObject.mapValues { it.value.jsonPrimitive.content }
            val rangeObject = testCase.getValue("range").jsonObject

            val tips = Insights.build(
                Insights.Input(
                    sets = testCase.getValue("sets").jsonArray.map { setInput(it) },
                    groupByExercise = groups,
                    range = Progress.Range(
                        fromMs = rangeObject.getValue("from_ms").jsonPrimitive.doubleOrNull?.toLong(),
                        toMs = rangeObject.getValue("to_ms").jsonPrimitive.content.toLong(),
                    ),
                    periodDays = testCase.getValue("period_days").jsonPrimitive.intOrNull ?: 30,
                )
            )

            val expected = testCase.getValue("expected").jsonArray
            assertEquals("$name: cantidad de consejos", expected.size, tips.size)

            expected.forEachIndexed { index, expectedElement ->
                val item = expectedElement.jsonObject
                val tip = tips[index]

                assertEquals("$name: kind", item.getValue("kind").jsonPrimitive.content, tip.kind.name.lowercase())
                assertEquals(
                    "$name: severity",
                    item.getValue("severity").jsonPrimitive.content,
                    tip.severity.name.lowercase(),
                )
                assertEquals(
                    "$name: subject",
                    if (item.getValue("subject") is JsonNull) {
                        null
                    } else {
                        item.getValue("subject").jsonPrimitive.content
                    },
                    tip.subject,
                )
                assertEquals(
                    "$name: value",
                    item.getValue("value").jsonPrimitive.doubleOrNull ?: 0.0,
                    tip.value,
                    1e-9,
                )
                assertEquals("$name: message", item.getValue("message").jsonPrimitive.content, tip.message)
            }
        }
    }

    @Test
    fun `el formato de numeros coincide con la regla compartida`() {
        assertEquals("25", Insights.formatNumber(25.0))
        assertEquals("57.1", Insights.formatNumber(57.14))
        assertEquals("33.3", Insights.formatNumber(33.333))
    }

    @Test
    fun `los umbrales documentados estan fijados`() {
        assertEquals(3, Insights.PROGRESS_MIN_SESSIONS)
        assertEquals(0.5, Insights.STAGNATION_MAX_VARIATION_PCT, 1e-9)
        assertEquals(50.0, Insights.IMBALANCE_MIN_SHARE_PCT, 1e-9)
        assertEquals(3.0, Insights.CONSISTENCY_HIGH_PER_WEEK, 1e-9)
        assertEquals(1.5, Insights.CONSISTENCY_LOW_PER_WEEK, 1e-9)
        assertEquals(30.0, Insights.INCOMPLETE_MIN_SHARE_PCT, 1e-9)
        assertTrue(Insights.TIP_LIMIT > 0)
    }
}
