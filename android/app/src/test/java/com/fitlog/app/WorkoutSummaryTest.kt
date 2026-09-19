package com.fitlog.app

import com.fitlog.app.domain.SessionSetInput
import com.fitlog.app.domain.WorkoutSummary
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSummaryTest {

    private val vectors = TestVectors.load("session-summary.json").jsonObject

    @Test
    fun `el resumen coincide con los vectores compartidos`() {
        vectors.getValue("cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content

            val sets = testCase.getValue("sets").jsonArray.map { setElement ->
                val set = setElement.jsonObject
                SessionSetInput(
                    exerciseId = set.getValue("exercise_id").jsonPrimitive.content,
                    weightKg = set.getValue("weight_kg").jsonPrimitive.doubleOrNull,
                    reps = set.getValue("reps").jsonPrimitive.intOrNull,
                    isWarmup = set.getValue("is_warmup").jsonPrimitive.intOrNull == 1,
                )
            }

            val expected = testCase.getValue("expected").jsonObject
            val summary = WorkoutSummary.summarize(sets)

            assertEquals(name, expected.getValue("total_sets").jsonPrimitive.intOrNull, summary.totalSets)
            assertEquals(
                name,
                expected.getValue("working_sets").jsonPrimitive.intOrNull,
                summary.workingSets,
            )
            assertEquals(
                name,
                expected.getValue("total_volume_kg").jsonPrimitive.doubleOrNull ?: 0.0,
                summary.totalVolumeKg,
                1e-9,
            )

            val expectedByExercise = expected.getValue("volume_by_exercise").jsonObject
            assertEquals(name, expectedByExercise.keys.sorted(), summary.volumeByExercise.keys.sorted())
            expectedByExercise.forEach { (exerciseId, expectedVolume) ->
                assertEquals(
                    "$name · $exerciseId",
                    expectedVolume.jsonPrimitive.doubleOrNull ?: 0.0,
                    summary.volumeByExercise[exerciseId] ?: 0.0,
                    1e-9,
                )
            }
        }
    }

    @Test
    fun `la duracion se calcula sobre sesiones finalizadas`() {
        assertNull(WorkoutSummary.formatDuration(0, null))
        assertEquals("45 min", WorkoutSummary.formatDuration(0, 45 * 60_000L))
        assertEquals("1 h 30 min", WorkoutSummary.formatDuration(0, 90 * 60_000L))
    }
}
