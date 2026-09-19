package com.fitlog.app

import com.fitlog.app.domain.PaceSetInput
import com.fitlog.app.domain.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionPaceTest {

    private val minute = 60_000L

    @Test
    fun `mide el rango entre la primera y la ultima serie efectiva`() {
        val pace = WorkoutSummary.pace(
            listOf(
                PaceSetInput(createdAtMs = 0L, isWarmup = true),
                PaceSetInput(createdAtMs = minute, isWarmup = false),
                PaceSetInput(createdAtMs = 5 * minute, isWarmup = false),
                PaceSetInput(createdAtMs = 11 * minute, isWarmup = false),
            )
        )

        assertEquals(minute, pace.firstSetAtMs)
        assertEquals(11 * minute, pace.lastSetAtMs)
        assertEquals(10 * minute, pace.spanMs)
        assertEquals(3, pace.workingSets)
    }

    @Test
    fun `calcula el ritmo y el descanso promedio`() {
        // Tres series efectivas en diez minutos: 18 series por hora y 5 minutos entre series.
        val pace = WorkoutSummary.pace(
            listOf(
                PaceSetInput(createdAtMs = 0L, isWarmup = false),
                PaceSetInput(createdAtMs = 5 * minute, isWarmup = false),
                PaceSetInput(createdAtMs = 10 * minute, isWarmup = false),
            )
        )

        assertEquals(18.0, pace.setsPerHour ?: 0.0, 0.001)
        assertEquals(5 * minute, pace.averageRestMs)
    }

    @Test
    fun `sin series efectivas no inventa ritmo`() {
        val pace = WorkoutSummary.pace(
            listOf(PaceSetInput(createdAtMs = minute, isWarmup = true))
        )

        assertEquals(0, pace.workingSets)
        assertNull(pace.setsPerHour)
        assertNull(pace.averageRestMs)
        assertNull(pace.firstSetAtMs)
    }

    @Test
    fun `una sola serie no tiene descanso promedio ni ritmo`() {
        val pace = WorkoutSummary.pace(
            listOf(PaceSetInput(createdAtMs = minute, isWarmup = false))
        )

        assertEquals(1, pace.workingSets)
        assertEquals(0L, pace.spanMs)
        assertNull(pace.averageRestMs)
        assertNull(pace.setsPerHour)
    }
}
