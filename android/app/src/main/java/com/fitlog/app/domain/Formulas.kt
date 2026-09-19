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
}
