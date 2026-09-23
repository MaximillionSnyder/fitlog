package com.fitlog.app.domain

import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.math.RoundingMode

object Formulas {

    fun volumeKg(weightKg: Double?, reps: Int?): Double? {
        if (weightKg == null || reps == null) return null
        return weightKg * reps
    }

    fun estimatedOneRepMaxKg(weightKg: Double?, reps: Int?): Double? {
        if (weightKg == null || reps == null || reps <= 0) return null
        return roundToTenth(weightKg * (1 + reps / 30.0))
    }

    /** Distancia entre dos coordenadas por la formula del semiverseno, en metros. */
    fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val deltaLat = (lat2 - lat1) * PI / 180.0
        val deltaLon = (lon2 - lon1) * PI / 180.0
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        return EARTH_RADIUS_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Radio medio de la Tierra, en metros. */
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun roundToTenth(value: Double): Double =
        BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toDouble()

    /**
     * Variacion porcentual entre dos valores, o `null` cuando no hay base de comparacion
     * (el periodo anterior fue cero): sin base, un porcentaje seria enganoso.
     */
    fun deltaPercent(current: Double, previous: Double): Double? {
        if (previous <= 0.0) return null
        return (current - previous) / previous * 100.0
    }
}
