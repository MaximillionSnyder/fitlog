package com.fitlog.app.domain

object Progress {

    const val DAY_MS = 86_400_000L

    enum class Metric { MAX_WEIGHT, VOLUME, BEST_ONE_REP_MAX }

    enum class RangePreset { LAST_30_DAYS, LAST_90_DAYS, ALL }

    data class Range(val fromMs: Long?, val toMs: Long)

    data class SetInput(
        val sessionId: String,
        val startedAtMs: Long,
        val weightKg: Double?,
        val reps: Int?,
        val isWarmup: Boolean,
    )

    data class Point(
        val sessionId: String,
        val startedAtMs: Long,
        val maxWeightKg: Double,
        val volumeKg: Double,
        val bestOneRepMaxKg: Double,
        val workingSets: Int,
    )

    fun rangeFor(preset: RangePreset, nowMs: Long): Range? = when (preset) {
        RangePreset.ALL -> null
        RangePreset.LAST_30_DAYS -> Range(fromMs = nowMs - 30 * DAY_MS, toMs = nowMs)
        RangePreset.LAST_90_DAYS -> Range(fromMs = nowMs - 90 * DAY_MS, toMs = nowMs)
    }

    fun buildSeries(sets: List<SetInput>, range: Range? = null): List<Point> {
        val buckets = linkedMapOf<String, Point>()

        for (set in sets) {
            if (set.isWarmup) continue
            if (range != null) {
                val from = range.fromMs
                if (from != null && set.startedAtMs < from) continue
                if (set.startedAtMs > range.toMs) continue
            }

            val existing = buckets[set.sessionId]
            val workingSets = (existing?.workingSets ?: 0) + 1
            val weight = set.weightKg
            val reps = set.reps
            val complete = weight != null && reps != null

            val maxWeightKg = if (complete) {
                maxOf(existing?.maxWeightKg ?: 0.0, weight)
            } else {
                existing?.maxWeightKg ?: 0.0
            }

            val volumeKg = if (complete) {
                (existing?.volumeKg ?: 0.0) + weight * reps
            } else {
                existing?.volumeKg ?: 0.0
            }

            val bestOneRepMaxKg = if (complete) {
                maxOf(
                    existing?.bestOneRepMaxKg ?: 0.0,
                    Formulas.roundToTenth(weight * (1 + reps / 30.0)),
                )
            } else {
                existing?.bestOneRepMaxKg ?: 0.0
            }

            buckets[set.sessionId] = Point(
                sessionId = set.sessionId,
                startedAtMs = existing?.startedAtMs ?: set.startedAtMs,
                maxWeightKg = maxWeightKg,
                volumeKg = volumeKg,
                bestOneRepMaxKg = bestOneRepMaxKg,
                workingSets = workingSets,
            )
        }

        return buckets.values.sortedWith(
            compareBy({ it.startedAtMs }, { it.sessionId })
        )
    }

    fun metricValue(point: Point, metric: Metric): Double = when (metric) {
        Metric.MAX_WEIGHT -> point.maxWeightKg
        Metric.VOLUME -> point.volumeKg
        Metric.BEST_ONE_REP_MAX -> point.bestOneRepMaxKg
    }
}
