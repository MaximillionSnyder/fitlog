package com.fitlog.app.domain

import java.math.BigDecimal
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
