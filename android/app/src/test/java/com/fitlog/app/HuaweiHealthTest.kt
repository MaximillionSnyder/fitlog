package com.fitlog.app

import com.fitlog.app.domain.HuaweiHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HuaweiHealthTest {

    private val start = 1_700_000_000_000L

    private fun activity(
        recordId: String = "rec-1",
        startTime: Long = start,
        endTime: Long = start + 1_800_000,
        sportType: Int = 4,
        totalTime: Long = 1_800_000,
        totalCalories: Long = 320_000,
        totalDistance: Long = 5_240,
    ) = """
        {
          "recordId": "$recordId",
          "startTime": $startTime,
          "endTime": $endTime,
          "sportType": $sportType,
          "totalTime": $totalTime,
          "totalCalories": $totalCalories,
          "totalDistance": $totalDistance,
          "totalSteps": 6800,
          "avgHeartRate": 145,
          "maxHeartRate": 172,
          "attribute": "tp=lbs;k=1;lat=1.0;lon=2.0;alt=3.0;t=1.0;"
        }
    """.trimIndent()

    @Test
    fun `lee un entrenamiento con sus datos`() {
        val result = HuaweiHealth.parse(listOf(activity()))

        assertEquals(1, result.workouts.size)
        val workout = result.workouts.first()
        assertEquals("rec-1", workout.recordId)
        assertEquals(start, workout.startedAtMs)
        assertEquals(start + 1_800_000, workout.finishedAtMs)
        assertEquals("Running", workout.sportName)
        assertEquals(1_800_000L, workout.durationMs)
        assertEquals(5_240.0, workout.distanceM ?: 0.0, 0.001)
        assertEquals(320.0, workout.calories ?: 0.0, 0.001)
        assertEquals(6_800, workout.steps)
        assertEquals(145.0, workout.averageHeartRate ?: 0.0, 0.001)
    }

    @Test
    fun `la exportacion triplica los registros y se cuenta una sola vez`() {
        val result = HuaweiHealth.parse(
            listOf(activity(), activity(), activity(recordId = "rec-1"))
        )

        assertEquals(1, result.workouts.size)
    }

    @Test
    fun `lee la raiz como lista y como objeto con la lista adentro`() {
        val asList = HuaweiHealth.parse(listOf("[${activity()}]"))
        assertEquals(1, asList.workouts.size)

        val wrapped = HuaweiHealth.parse(listOf("""{"sportRecords": [${activity()}]}"""))
        assertEquals(1, wrapped.workouts.size)
    }

    private fun compactActivity(recordId: String, startTime: Long) =
        """{"recordId": "$recordId", "startTime": $startTime, "endTime": ${startTime + 1_800_000}, """ +
            """"sportType": 4, "totalTime": 1800000, "totalDistance": 5240}"""

    @Test
    fun `lee un archivo con un objeto por linea`() {
        val content = compactActivity("a", start) + "\n" + compactActivity("b", start + 86_400_000)

        val result = HuaweiHealth.parse(listOf(content))

        assertEquals(2, result.workouts.size)
    }

    @Test
    fun `un registro sin fecha se descarta`() {
        val result = HuaweiHealth.parse(listOf("""{"recordId": "x", "sportType": 4}"""))

        assertTrue(result.isEmpty)
    }

    @Test
    fun `acepta epoch en segundos y fechas en texto`() {
        val seconds = HuaweiHealth.parse(listOf(activity(startTime = start / 1000, endTime = (start / 1000) + 1800)))
        assertEquals(start, seconds.workouts.first().startedAtMs)

        val text = HuaweiHealth.parse(
            listOf("""{"startTime": "2023-11-14T22:13:20", "totalTime": 600000, "sportType": 5}""")
        )
        assertEquals(start, text.workouts.first().startedAtMs)
        assertEquals(600_000L, text.workouts.first().durationMs)
    }

    @Test
    fun `los archivos que no son entrenamientos se ignoran sin error`() {
        val result = HuaweiHealth.parse(
            listOf(
                activity(),
                // Con inicio y fin, como el sueno real: lo que lo descarta es no tener senal de deporte.
                """{"sleepRecords": [{"startTime": $start, "endTime": ${start + 28_800_000}, "deepSleep": 10}]}""",
                "no es json",
            )
        )

        assertEquals(1, result.workouts.size)
        assertEquals(3, result.filesRead + result.filesSkipped)
    }

    @Test
    fun `la nota resume el origen y los datos disponibles`() {
        val workout = HuaweiHealth.parse(listOf(activity())).workouts.first()
        val note = HuaweiHealth.noteFor(workout)

        assertTrue(note.startsWith("Huawei Health · Running"))
        assertTrue(note.contains("5.24 km"))
        assertTrue(note.contains("320 kcal"))
        assertTrue(note.contains("FC 145/172"))
        assertTrue(note.contains("6800 pasos"))
    }

    @Test
    fun `la nota se limita a lo que existe`() {
        val workout = HuaweiHealth.parse(
            listOf("""{"recordId": "solo", "startTime": $start, "totalTime": 600000, "sportType": 147}""")
        ).workouts.first()

        val note = HuaweiHealth.noteFor(workout)
        assertEquals("Huawei Health · Entrenamiento de fuerza", note)
        assertNull(workout.distanceM)
    }
}
