package com.fitlog.app.domain

/**
 * Actividad importada de otra app: la serie de entrenamientos que traen distancia, duracion y
 * frecuencia cardiaca (Huawei Health o GPX).
 *
 * Es distinta del progreso por ejercicio: una sesion importada no tiene series con peso y reps, asi
 * que sus graficos son de actividad (distancia, tiempo, pulso) y no de volumen.
 */
object Activity {

    enum class Metric(val label: String) {
        DISTANCE("Distancia"),
        DURATION("Duración"),
        HEART_RATE("FC media"),
    }

    data class Input(
        val startedAtMs: Long,
        val finishedAtMs: Long?,
        val distanceM: Double?,
        val averageHeartRate: Double?,
    )

    data class Point(
        val startedAtMs: Long,
        val distanceM: Double,
        val durationMs: Long,
        val averageHeartRate: Double?,
    )

    data class Totals(
        val sessions: Int,
        val distanceM: Double,
        val durationMs: Long,
    )

    /** Serie de actividad dentro del rango, de la mas vieja a la mas nueva. */
    fun series(inputs: List<Input>, range: Progress.Range? = null): List<Point> =
        inputs
            .filter { input -> inRange(input.startedAtMs, range) }
            .sortedBy { it.startedAtMs }
            .map { input ->
                Point(
                    startedAtMs = input.startedAtMs,
                    distanceM = input.distanceM ?: 0.0,
                    durationMs = input.finishedAtMs
                        ?.let { (it - input.startedAtMs).coerceAtLeast(0L) }
                        ?: 0L,
                    averageHeartRate = input.averageHeartRate,
                )
            }

    /** Valor de la metrica elegida para un punto, o `null` si el entrenamiento no la trae. */
    fun value(point: Point, metric: Metric): Double? = when (metric) {
        Metric.DISTANCE -> point.distanceM.takeIf { it > 0 }
        Metric.DURATION -> point.durationMs.takeIf { it > 0 }?.toDouble()
        Metric.HEART_RATE -> point.averageHeartRate
    }

    /** Puntos con valor para la metrica elegida: los que no la traen no se grafican. */
    fun values(points: List<Point>, metric: Metric): List<Double> =
        points.mapNotNull { value(it, metric) }

    fun totals(points: List<Point>): Totals = Totals(
        sessions = points.size,
        distanceM = points.sumOf { it.distanceM },
        durationMs = points.sumOf { it.durationMs },
    )

    private fun inRange(timestampMs: Long, range: Progress.Range?): Boolean {
        if (range == null) return true
        val from = range.fromMs
        if (from != null && timestampMs < from) return false
        return timestampMs <= range.toMs
    }
}
