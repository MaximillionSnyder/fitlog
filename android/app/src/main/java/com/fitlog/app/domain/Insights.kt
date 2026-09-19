package com.fitlog.app.domain

object Insights {

    enum class Kind { CONSISTENCY, INCOMPLETE_DATA, IMBALANCE, PROGRESS, STAGNATION }

    enum class Severity { WARNING, INFO, SUCCESS }

    data class Tip(
        val kind: Kind,
        val severity: Severity,
        val subject: String?,
        val value: Double,
        val message: String,
    )

    data class Input(
        val sets: List<Comparisons.SetInput>,
        val groupByExercise: Map<String, String>,
        val range: Progress.Range,
        val periodDays: Int,
    )

    const val TIP_LIMIT = 6

    const val PROGRESS_MIN_SESSIONS = 3
    const val STAGNATION_MIN_SESSIONS = 4
    const val STAGNATION_MAX_VARIATION_PCT = 0.5
    const val IMBALANCE_MIN_SHARE_PCT = 50.0
    const val CONSISTENCY_HIGH_PER_WEEK = 3.0
    const val CONSISTENCY_LOW_PER_WEEK = 1.5
    const val INCOMPLETE_MIN_SETS = 5
    const val INCOMPLETE_MIN_SHARE_PCT = 30.0

    fun build(input: Input): List<Tip> {
        val effective = input.sets.filter { set ->
            !set.isWarmup && inRange(set, input.range)
        }
        if (effective.isEmpty()) return emptyList()

        val tips = mutableListOf<Tip>()
        val sessionStartById = mutableMapOf<String, Long>()
        for (set in effective) {
            val current = sessionStartById[set.sessionId]
            if (current == null || set.startedAtMs < current) {
                sessionStartById[set.sessionId] = set.startedAtMs
            }
        }
        val sessions = sessionStartById.size

        val weeks = maxOf(1.0, input.periodDays / 7.0)
        val perWeek = sessions / weeks
        if (perWeek >= CONSISTENCY_HIGH_PER_WEEK) {
            tips += Tip(
                kind = Kind.CONSISTENCY,
                severity = Severity.SUCCESS,
                subject = null,
                value = Formulas.roundToTenth(perWeek),
                message = "Buena frecuencia: ${formatNumber(perWeek)} sesiones por semana",
            )
        } else if (perWeek < CONSISTENCY_LOW_PER_WEEK) {
            tips += Tip(
                kind = Kind.CONSISTENCY,
                severity = Severity.WARNING,
                subject = null,
                value = sessions.toDouble(),
                message = if (sessions == 1) {
                    "Poca frecuencia: 1 sesión en el periodo"
                } else {
                    "Poca frecuencia: $sessions sesiones en el periodo"
                },
            )
        }

        val incomplete = effective.count { it.weightKg == null || it.reps == null }
        val incompleteShare = incomplete.toDouble() / effective.size * 100
        if (effective.size >= INCOMPLETE_MIN_SETS && incompleteShare >= INCOMPLETE_MIN_SHARE_PCT) {
            tips += Tip(
                kind = Kind.INCOMPLETE_DATA,
                severity = Severity.INFO,
                subject = null,
                value = Formulas.roundToTenth(incompleteShare),
                message = "${formatNumber(incompleteShare)}% de las series no tienen peso y repeticiones",
            )
        }

        val balance = Comparisons.muscleBalance(input.sets, input.groupByExercise, input.range)
        if (balance.size >= 2) {
            val top = balance.first()
            if (top.sharePct >= IMBALANCE_MIN_SHARE_PCT) {
                tips += Tip(
                    kind = Kind.IMBALANCE,
                    severity = Severity.WARNING,
                    subject = top.muscleGroupSlug,
                    value = top.sharePct,
                    message = "Un grupo concentra ${formatNumber(top.sharePct)}% del volumen del periodo",
                )
            }
        }

        val oneRepMaxByExercise = linkedMapOf<String, MutableMap<String, Double>>()
        for (set in effective) {
            val weight = set.weightKg ?: continue
            val reps = set.reps ?: continue
            val bySession = oneRepMaxByExercise.getOrPut(set.exerciseId) { mutableMapOf() }
            val estimate = Formulas.roundToTenth(weight * (1 + reps / 30.0))
            val current = bySession[set.sessionId]
            if (current == null || estimate > current) {
                bySession[set.sessionId] = estimate
            }
        }

        for ((exerciseId, bySession) in oneRepMaxByExercise) {
            val ordered = bySession.entries
                .sortedWith(
                    compareBy(
                        { sessionStartById[it.key] ?: 0L },
                        { it.key },
                    )
                )
                .map { it.value }

            if (ordered.size < PROGRESS_MIN_SESSIONS) continue

            val first = ordered.first()
            val last = ordered.last()
            if (first == 0.0) continue

            val variationPct = Formulas.roundToTenth((last - first) / first * 100)

            if (ordered.size >= STAGNATION_MIN_SESSIONS &&
                kotlin.math.abs(variationPct) <= STAGNATION_MAX_VARIATION_PCT
            ) {
                tips += Tip(
                    kind = Kind.STAGNATION,
                    severity = Severity.WARNING,
                    subject = exerciseId,
                    value = ordered.size.toDouble(),
                    message = "Sin progreso de 1RM en las últimas ${ordered.size} sesiones",
                )
            } else if (variationPct > 0) {
                tips += Tip(
                    kind = Kind.PROGRESS,
                    severity = Severity.SUCCESS,
                    subject = exerciseId,
                    value = variationPct,
                    message = "Tu 1RM estimado subió ${formatNumber(variationPct)}% en ${ordered.size} sesiones",
                )
            }
        }

        return tips.sortedWith(
            compareBy(
                { severityOrder(it.severity) },
                { it.kind.ordinal },
                { it.subject ?: "" },
            )
        )
    }

    fun formatNumber(value: Double): String {
        val rounded = Formulas.roundToTenth(value)
        return if (rounded == rounded.toInt().toDouble()) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }

    private fun severityOrder(severity: Severity): Int = when (severity) {
        Severity.WARNING -> 0
        Severity.INFO -> 1
        Severity.SUCCESS -> 2
    }

    private fun inRange(set: Comparisons.SetInput, range: Progress.Range): Boolean {
        val from = range.fromMs
        if (from != null && set.startedAtMs < from) return false
        return set.startedAtMs <= range.toMs
    }
}
