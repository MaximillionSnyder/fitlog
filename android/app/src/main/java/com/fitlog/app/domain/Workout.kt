package com.fitlog.app.domain

data class SessionSetInput(
    val exerciseId: String,
    val weightKg: Double?,
    val reps: Int?,
    val isWarmup: Boolean,
)

data class SessionSummary(
    val totalSets: Int,
    val workingSets: Int,
    val totalVolumeKg: Double,
    val volumeByExercise: Map<String, Double>,
)

/** Serie con su marca de tiempo, para medir el ritmo de la sesion. */
data class PaceSetInput(
    val createdAtMs: Long,
    val isWarmup: Boolean,
)

/**
 * Ritmo de una sesion: cuanto duro de verdad (de la primera a la ultima serie efectiva), a que
 * velocidad se registraron las series y cuanto se descanso en promedio entre ellas.
 *
 * El descanso promedio es el intervalo entre series consecutivas, asi que solo tiene sentido con
 * dos o mas series; el ritmo necesita un rango mayor a cero.
 */
data class SessionPace(
    val firstSetAtMs: Long?,
    val lastSetAtMs: Long?,
    val spanMs: Long,
    val workingSets: Int,
    val setsPerHour: Double?,
    val averageRestMs: Long?,
)

object WorkoutSummary {

    fun summarize(sets: List<SessionSetInput>): SessionSummary {
        var workingSets = 0
        var totalVolumeKg = 0.0
        val volumeByExercise = mutableMapOf<String, Double>()

        for (set in sets) {
            if (set.isWarmup) continue
            workingSets += 1

            val weight = set.weightKg ?: continue
            val reps = set.reps ?: continue
            val volume = weight * reps
            totalVolumeKg += volume
            volumeByExercise[set.exerciseId] = (volumeByExercise[set.exerciseId] ?: 0.0) + volume
        }

        return SessionSummary(
            totalSets = sets.size,
            workingSets = workingSets,
            totalVolumeKg = totalVolumeKg,
            volumeByExercise = volumeByExercise,
        )
    }

    fun pace(sets: List<PaceSetInput>): SessionPace {
        val effective = sets.filter { !it.isWarmup }.sortedBy { it.createdAtMs }
        if (effective.isEmpty()) {
            return SessionPace(
                firstSetAtMs = null,
                lastSetAtMs = null,
                spanMs = 0L,
                workingSets = 0,
                setsPerHour = null,
                averageRestMs = null,
            )
        }

        val first = effective.first().createdAtMs
        val last = effective.last().createdAtMs
        val span = (last - first).coerceAtLeast(0L)
        val count = effective.size

        return SessionPace(
            firstSetAtMs = first,
            lastSetAtMs = last,
            spanMs = span,
            workingSets = count,
            setsPerHour = if (span > 0L) count * 3_600_000.0 / span else null,
            averageRestMs = if (count > 1) span / (count - 1) else null,
        )
    }

    fun formatDuration(startedAt: Long, finishedAt: Long?): String? {
        if (finishedAt == null) return null
        val totalMinutes = ((finishedAt - startedAt) / 60_000L).coerceAtLeast(0L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours == 0) "$minutes min" else "$hours h $minutes min"
    }
}
