package com.fitlog.app.domain

object Comparisons {

    enum class Preset { LAST_30_DAYS, LAST_90_DAYS }

    data class SetInput(
        val exerciseId: String,
        val sessionId: String,
        val startedAtMs: Long,
        val weightKg: Double?,
        val reps: Int?,
        val isWarmup: Boolean,
    )

    data class PersonalRecord(
        val exerciseId: String,
        val bestWeightKg: Double,
        val bestWeightAtMs: Long?,
        val bestOneRepMaxKg: Double,
        val bestOneRepMaxAtMs: Long?,
        val bestSessionVolumeKg: Double,
        val bestSessionVolumeAtMs: Long?,
        val bestReps: Int,
        val bestRepsAtMs: Long?,
    )

    data class PeriodTotals(
        val volumeKg: Double,
        val workingSets: Int,
        val sessions: Int,
    )

    data class PeriodComparison(
        val current: PeriodTotals,
        val previous: PeriodTotals,
        val volumeDeltaPct: Double?,
        val setsDeltaPct: Double?,
        val sessionsDeltaPct: Double?,
    )

    data class MuscleBalanceEntry(
        val muscleGroupSlug: String,
        val volumeKg: Double,
        val sharePct: Double,
    )

    data class Ranges(
        val current: Progress.Range,
        val previous: Progress.Range,
    )

    private data class SessionVolume(var volumeKg: Double, val atMs: Long)

    private class Accumulator {
        var bestWeightKg: Double? = null
        var bestWeightAtMs: Long? = null
        var bestOneRepMaxKg: Double? = null
        var bestOneRepMaxAtMs: Long? = null
        var bestReps: Int? = null
        var bestRepsAtMs: Long? = null
        val sessions = mutableMapOf<String, SessionVolume>()
    }

    fun comparisonRanges(preset: Preset, nowMs: Long): Ranges {
        val days = if (preset == Preset.LAST_30_DAYS) 30 else 90
        val spanMs = days * Progress.DAY_MS
        return Ranges(
            current = Progress.Range(fromMs = nowMs - spanMs, toMs = nowMs),
            previous = Progress.Range(fromMs = nowMs - 2 * spanMs, toMs = nowMs - spanMs - 1),
        )
    }

    fun personalRecords(sets: List<SetInput>): List<PersonalRecord> {
        val byExercise = linkedMapOf<String, Accumulator>()

        for (set in effectiveSets(sets)) {
            val accumulator = byExercise.getOrPut(set.exerciseId) { Accumulator() }
            val reps = set.reps
            val weight = set.weightKg

            if (reps != null && (accumulator.bestReps == null || reps > accumulator.bestReps!!)) {
                accumulator.bestReps = reps
                accumulator.bestRepsAtMs = set.startedAtMs
            }

            if (weight == null || reps == null) continue

            if (accumulator.bestWeightKg == null || weight > accumulator.bestWeightKg!!) {
                accumulator.bestWeightKg = weight
                accumulator.bestWeightAtMs = set.startedAtMs
            }

            val oneRepMax = Formulas.roundToTenth(weight * (1 + reps / 30.0))
            if (accumulator.bestOneRepMaxKg == null || oneRepMax > accumulator.bestOneRepMaxKg!!) {
                accumulator.bestOneRepMaxKg = oneRepMax
                accumulator.bestOneRepMaxAtMs = set.startedAtMs
            }

            val session = accumulator.sessions[set.sessionId]
            if (session == null) {
                accumulator.sessions[set.sessionId] = SessionVolume(weight * reps, set.startedAtMs)
            } else {
                session.volumeKg += weight * reps
            }
        }

        val records = byExercise.map { (exerciseId, accumulator) ->
            var bestSessionVolumeKg = 0.0
            var bestSessionVolumeAtMs: Long? = null
            for (session in accumulator.sessions.values) {
                if (bestSessionVolumeAtMs == null || session.volumeKg > bestSessionVolumeKg) {
                    bestSessionVolumeKg = session.volumeKg
                    bestSessionVolumeAtMs = session.atMs
                }
            }

            PersonalRecord(
                exerciseId = exerciseId,
                bestWeightKg = accumulator.bestWeightKg ?: 0.0,
                bestWeightAtMs = accumulator.bestWeightAtMs,
                bestOneRepMaxKg = accumulator.bestOneRepMaxKg ?: 0.0,
                bestOneRepMaxAtMs = accumulator.bestOneRepMaxAtMs,
                bestSessionVolumeKg = bestSessionVolumeKg,
                bestSessionVolumeAtMs = bestSessionVolumeAtMs,
                bestReps = accumulator.bestReps ?: 0,
                bestRepsAtMs = accumulator.bestRepsAtMs,
            )
        }

        return records.sortedWith(
            compareByDescending<PersonalRecord> { it.bestOneRepMaxKg }.thenBy { it.exerciseId }
        )
    }

    fun comparePeriods(
        sets: List<SetInput>,
        current: Progress.Range,
        previous: Progress.Range,
    ): PeriodComparison {
        val currentTotals = totalsFor(sets, current)
        val previousTotals = totalsFor(sets, previous)

        return PeriodComparison(
            current = currentTotals,
            previous = previousTotals,
            volumeDeltaPct = deltaPct(currentTotals.volumeKg, previousTotals.volumeKg),
            setsDeltaPct = deltaPct(
                currentTotals.workingSets.toDouble(),
                previousTotals.workingSets.toDouble(),
            ),
            sessionsDeltaPct = deltaPct(
                currentTotals.sessions.toDouble(),
                previousTotals.sessions.toDouble(),
            ),
        )
    }

    fun muscleBalance(
        sets: List<SetInput>,
        groupByExercise: Map<String, String>,
        range: Progress.Range? = null,
    ): List<MuscleBalanceEntry> {
        val volumeByGroup = mutableMapOf<String, Double>()
        var totalVolume = 0.0

        for (set in sets) {
            if (set.isWarmup) continue
            val weight = set.weightKg ?: continue
            val reps = set.reps ?: continue
            if (range != null) {
                val from = range.fromMs
                if (from != null && set.startedAtMs < from) continue
                if (set.startedAtMs > range.toMs) continue
            }

            val slug = groupByExercise[set.exerciseId] ?: "sin-grupo"
            val volume = weight * reps
            volumeByGroup[slug] = (volumeByGroup[slug] ?: 0.0) + volume
            totalVolume += volume
        }

        if (totalVolume == 0.0) return emptyList()

        return volumeByGroup.map { (slug, volume) ->
            MuscleBalanceEntry(
                muscleGroupSlug = slug,
                volumeKg = volume,
                sharePct = Formulas.roundToTenth(volume / totalVolume * 100),
            )
        }.sortedWith(
            compareByDescending<MuscleBalanceEntry> { it.volumeKg }.thenBy { it.muscleGroupSlug }
        )
    }

    fun formatDelta(deltaPct: Double?): String {
        if (deltaPct == null) return "sin datos"
        val rounded = Formulas.roundToTenth(deltaPct)
        val text = if (rounded == rounded.toInt().toDouble()) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
        val sign = if (rounded > 0) "+" else ""
        return "$sign$text%"
    }

    private fun effectiveSets(sets: List<SetInput>): List<SetInput> =
        sets.filter { !it.isWarmup }
            .sortedWith(compareBy({ it.startedAtMs }, { it.sessionId }))

    private fun totalsFor(sets: List<SetInput>, range: Progress.Range): PeriodTotals {
        val sessions = mutableSetOf<String>()
        var volumeKg = 0.0
        var workingSets = 0

        for (set in sets) {
            if (set.isWarmup) continue
            val from = range.fromMs
            if (from != null && set.startedAtMs < from) continue
            if (set.startedAtMs > range.toMs) continue

            workingSets += 1
            sessions.add(set.sessionId)
            val weight = set.weightKg
            val reps = set.reps
            if (weight != null && reps != null) {
                volumeKg += weight * reps
            }
        }

        return PeriodTotals(volumeKg = volumeKg, workingSets = workingSets, sessions = sessions.size)
    }

    private fun deltaPct(current: Double, previous: Double): Double? {
        if (previous == 0.0) return null
        return Formulas.roundToTenth((current - previous) / previous * 100)
    }
}
