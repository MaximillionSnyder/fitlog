package com.fitlog.app.domain

/**
 * Agregacion de presentacion del panel de Inicio.
 *
 * Es una funcion pura sobre las sesiones y las medidas que ya existen: no agrega consultas nuevas al
 * esquema ni columnas derivadas persistidas (el volumen sigue siendo un valor calculado).
 */
object Home {

    const val WINDOW_DAYS = 7
    const val WEEK_MS = WINDOW_DAYS * Progress.DAY_MS

    data class SessionInput(
        val id: String,
        val startedAtMs: Long,
        val finishedAtMs: Long?,
        val workingSets: Int,
        val volumeKg: Double,
    )

    data class BodyInput(
        val measuredAtMs: Long,
        val value: Double,
    )

    data class Window(
        val sessions: Int,
        val volumeKg: Double,
        val workingSets: Int,
    ) {
        val averageVolumeKg: Double
            get() = if (sessions == 0) 0.0 else volumeKg / sessions
    }

    data class Summary(
        val current: Window,
        val previous: Window,
        val totalSessions: Int,
        val streakWeeks: Int,
        val latestBodyWeightKg: Double?,
        val latestBodyWeightAtMs: Long?,
    ) {
        val volumeDeltaPercent: Double?
            get() = Formulas.deltaPercent(current.volumeKg, previous.volumeKg)

        val sessionsDeltaPercent: Double?
            get() = Formulas.deltaPercent(current.sessions.toDouble(), previous.sessions.toDouble())
    }

    fun build(
        sessions: List<SessionInput>,
        bodyPoints: List<BodyInput>,
        nowMs: Long,
    ): Summary {
        val effective = sessions.filter { it.startedAtMs <= nowMs }
        val currentFrom = nowMs - WEEK_MS
        val previousFrom = nowMs - 2 * WEEK_MS

        val current = window(effective.filter { it.startedAtMs > currentFrom })
        val previous = window(
            effective.filter { it.startedAtMs > previousFrom && it.startedAtMs <= currentFrom }
        )

        val latestBody = bodyPoints
            .filter { it.measuredAtMs <= nowMs }
            .maxByOrNull { it.measuredAtMs }

        return Summary(
            current = current,
            previous = previous,
            totalSessions = effective.size,
            streakWeeks = streakWeeks(effective, nowMs),
            latestBodyWeightKg = latestBody?.value,
            latestBodyWeightAtMs = latestBody?.measuredAtMs,
        )
    }

    private fun window(sessions: List<SessionInput>): Window = Window(
        sessions = sessions.size,
        volumeKg = sessions.sumOf { it.volumeKg },
        workingSets = sessions.sumOf { it.workingSets },
    )

    /**
     * Semanas consecutivas con al menos una sesion, contando hacia atras desde la semana en curso.
     * Si la semana en curso todavia no tiene sesiones, la racha se mide desde la semana anterior.
     */
    private fun streakWeeks(sessions: List<SessionInput>, nowMs: Long): Int {
        if (sessions.isEmpty()) return 0
        val weeksWithSession = sessions
            .map { ((nowMs - it.startedAtMs) / WEEK_MS).toInt() }
            .filter { it >= 0 }
            .toSet()

        var streak = 0
        var index = if (weeksWithSession.contains(0)) 0 else 1
        while (weeksWithSession.contains(index)) {
            streak += 1
            index += 1
        }
        return streak
    }
}
