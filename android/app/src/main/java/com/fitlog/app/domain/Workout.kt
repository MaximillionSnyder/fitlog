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

    fun formatDuration(startedAt: Long, finishedAt: Long?): String? {
        if (finishedAt == null) return null
        val totalMinutes = ((finishedAt - startedAt) / 60_000L).coerceAtLeast(0L).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours == 0) "$minutes min" else "$hours h $minutes min"
    }
}
