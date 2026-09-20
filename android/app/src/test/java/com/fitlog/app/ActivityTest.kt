package com.fitlog.app

import com.fitlog.app.domain.Activity
import com.fitlog.app.domain.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityTest {

    private val now = 1_800_000_000_000L
    private val day = Progress.DAY_MS

    private fun input(
        daysAgo: Long,
        distanceM: Double? = null,
        durationMs: Long = 1_800_000,
        heartRate: Double? = null,
    ) = Activity.Input(
        startedAtMs = now - daysAgo * day,
        finishedAtMs = now - daysAgo * day + durationMs,
        distanceM = distanceM,
        averageHeartRate = heartRate,
    )

    @Test
    fun `arma la serie en orden cronologico y dentro del rango`() {
        val series = Activity.series(
            listOf(
                input(daysAgo = 2, distanceM = 5_000.0),
                input(daysAgo = 40, distanceM = 8_000.0),
                input(daysAgo = 1, distanceM = 3_000.0),
            ),
            Progress.rangeFor(Progress.RangePreset.LAST_30_DAYS, now),
        )

        assertEquals(2, series.size)
        // Cronologico: primero la mas vieja del rango.
        assertEquals(5_000.0, series.first().distanceM, 0.001)
        assertEquals(3_000.0, series.last().distanceM, 0.001)
    }

    @Test
    fun `sin rango entran todas las sesiones`() {
        val series = Activity.series(
            listOf(input(daysAgo = 1, distanceM = 1_000.0), input(daysAgo = 400, distanceM = 2_000.0)),
            null,
        )

        assertEquals(2, series.size)
    }

    @Test
    fun `la duracion sale de las fechas de la sesion`() {
        val series = Activity.series(listOf(input(daysAgo = 1, durationMs = 2_700_000)), null)

        assertEquals(2_700_000L, series.first().durationMs)
    }

    @Test
    fun `solo se grafican los entrenamientos que traen la metrica`() {
        val series = Activity.series(
            listOf(
                input(daysAgo = 2, distanceM = 5_000.0, heartRate = 145.0),
                input(daysAgo = 1, distanceM = null, heartRate = null),
            ),
            null,
        )

        assertEquals(1, Activity.values(series, Activity.Metric.DISTANCE).size)
        assertEquals(1, Activity.values(series, Activity.Metric.HEART_RATE).size)
        // La duracion siempre esta: sale de las fechas.
        assertEquals(2, Activity.values(series, Activity.Metric.DURATION).size)
        assertNull(Activity.value(series.last(), Activity.Metric.DISTANCE))
    }

    @Test
    fun `suma distancia y tiempo del periodo`() {
        val series = Activity.series(
            listOf(
                input(daysAgo = 2, distanceM = 5_000.0, durationMs = 1_800_000),
                input(daysAgo = 1, distanceM = 3_000.0, durationMs = 1_200_000),
            ),
            null,
        )

        val totals = Activity.totals(series)
        assertEquals(2, totals.sessions)
        assertEquals(8_000.0, totals.distanceM, 0.001)
        assertEquals(3_000_000L, totals.durationMs)
    }
}
