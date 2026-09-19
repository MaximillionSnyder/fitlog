package com.fitlog.app

import com.fitlog.app.domain.Home
import com.fitlog.app.domain.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeSummaryTest {

    private val now = 1_800_000_000_000L
    private val day = Progress.DAY_MS

    private fun session(daysAgo: Long, volume: Double, sets: Int = 4) = Home.SessionInput(
        id = "s-$daysAgo",
        startedAtMs = now - daysAgo * day,
        finishedAtMs = now - daysAgo * day + 3_600_000L,
        workingSets = sets,
        volumeKg = volume,
    )

    @Test
    fun `suma la ventana actual y la anterior por separado`() {
        val summary = Home.build(
            sessions = listOf(
                session(daysAgo = 1, volume = 1_000.0),
                session(daysAgo = 6, volume = 500.0),
                session(daysAgo = 9, volume = 300.0),
            ),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertEquals(2, summary.current.sessions)
        assertEquals(1_500.0, summary.current.volumeKg, 0.001)
        assertEquals(1, summary.previous.sessions)
        assertEquals(300.0, summary.previous.volumeKg, 0.001)
        assertEquals(3, summary.totalSessions)
    }

    @Test
    fun `calcula la variacion de volumen contra la ventana anterior`() {
        val summary = Home.build(
            sessions = listOf(
                session(daysAgo = 1, volume = 1_500.0),
                session(daysAgo = 8, volume = 1_000.0),
            ),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertEquals(50.0, summary.volumeDeltaPercent ?: 0.0, 0.001)
    }

    @Test
    fun `sin ventana anterior no inventa una variacion`() {
        val summary = Home.build(
            sessions = listOf(session(daysAgo = 2, volume = 900.0)),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertNull(summary.volumeDeltaPercent)
        assertNull(summary.sessionsDeltaPercent)
    }

    @Test
    fun `cuenta semanas consecutivas con sesiones`() {
        val summary = Home.build(
            sessions = listOf(
                session(daysAgo = 1, volume = 100.0),
                session(daysAgo = 9, volume = 100.0),
                session(daysAgo = 16, volume = 100.0),
                session(daysAgo = 40, volume = 100.0),
            ),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertEquals(3, summary.streakWeeks)
    }

    @Test
    fun `la racha arranca en la semana anterior si la actual esta vacia`() {
        val summary = Home.build(
            sessions = listOf(
                session(daysAgo = 9, volume = 100.0),
                session(daysAgo = 16, volume = 100.0),
            ),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertEquals(2, summary.streakWeeks)
    }

    @Test
    fun `toma el ultimo peso corporal registrado`() {
        val summary = Home.build(
            sessions = emptyList(),
            bodyPoints = listOf(
                Home.BodyInput(measuredAtMs = now - 10 * day, value = 81.4),
                Home.BodyInput(measuredAtMs = now - 2 * day, value = 79.8),
            ),
            nowMs = now,
        )

        assertEquals(79.8, summary.latestBodyWeightKg ?: 0.0, 0.001)
        assertEquals(now - 2 * day, summary.latestBodyWeightAtMs)
    }

    @Test
    fun `sin datos el panel queda en cero y sin error`() {
        val summary = Home.build(sessions = emptyList(), bodyPoints = emptyList(), nowMs = now)

        assertEquals(0, summary.current.sessions)
        assertEquals(0.0, summary.current.volumeKg, 0.0)
        assertEquals(0, summary.streakWeeks)
        assertNull(summary.latestBodyWeightKg)
    }

    @Test
    fun `agrupa la actividad de hoy segun el inicio del dia local`() {
        val summary = Home.build(
            sessions = listOf(
                session(daysAgo = 0, volume = 400.0, sets = 3),
                // Seis horas antes de "ahora": cae fuera del dia cuando el dia empezo hace cinco.
                Home.SessionInput(
                    id = "s-ayer",
                    startedAtMs = now - 6 * 3_600_000L,
                    finishedAtMs = now - 5 * 3_600_000L,
                    workingSets = 4,
                    volumeKg = 900.0,
                ),
            ),
            bodyPoints = emptyList(),
            nowMs = now,
            startOfTodayMs = now - 5 * 3_600_000L,
        )

        assertEquals(1, summary.today.sessions)
        assertEquals(400.0, summary.today.volumeKg, 0.001)
    }

    @Test
    fun `agrupa la actividad de hoy`() {
        val summary = Home.build(
            sessions = listOf(
                session(daysAgo = 0, volume = 400.0, sets = 3),
                session(daysAgo = 1, volume = 900.0),
                session(daysAgo = 9, volume = 300.0),
            ),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertEquals(1, summary.today.sessions)
        assertEquals(400.0, summary.today.volumeKg, 0.001)
        assertEquals(3, summary.today.workingSets)
    }

    @Test
    fun `los primeros pasos se completan con rutina, entrenamiento y medida`() {
        val vacio = Home.steps(routineCount = 0, sessionCount = 0, bodyMetricCount = 0)
        assertEquals(0, vacio.doneCount)
        assertEquals(3, vacio.total)
        assertEquals(false, vacio.isComplete)

        val conRutina = Home.steps(routineCount = 1, sessionCount = 0, bodyMetricCount = 0)
        assertEquals(1, conRutina.doneCount)
        assertEquals(true, conRutina.items.first { it.id == Home.Step.Id.ROUTINE }.done)

        val completo = Home.steps(routineCount = 2, sessionCount = 5, bodyMetricCount = 3)
        assertEquals(true, completo.isComplete)
    }

    @Test
    fun `ignora sesiones con fecha futura`() {
        val summary = Home.build(
            sessions = listOf(session(daysAgo = -3, volume = 500.0)),
            bodyPoints = emptyList(),
            nowMs = now,
        )

        assertEquals(0, summary.totalSessions)
    }
}
