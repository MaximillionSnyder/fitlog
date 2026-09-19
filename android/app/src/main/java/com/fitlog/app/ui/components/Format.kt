package com.fitlog.app.ui.components

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Formato de numeros de la interfaz.
 *
 * La app usa punto como separador decimal y espacio fino para los miles, para que los valores se
 * lean igual en cualquier configuracion regional del telefono.
 */
object Format {

    private const val THIN_SPACE = "\u2009"

    /** Entero con separador de miles. */
    fun integer(value: Long): String = groupThousands(value.toString())

    fun integer(value: Int): String = integer(value.toLong())

    fun integer(value: Double): String = integer(value.roundToLong())

    /** Peso en kg: un decimal cuando el valor no es entero. */
    fun kg(value: Double?): String {
        if (value == null) return "—"
        return decimal(value, 1)
    }

    /** Volumen en kg: sin decimales cuando supera los 100 kg, para no ensuciar el numero. */
    fun volumeKg(value: Double): String {
        if (value >= 100.0) return integer(value)
        return decimal(value, 1)
    }

    /** Numero con la cantidad de decimales pedida, sin ceros de relleno. */
    fun decimal(value: Double, decimals: Int): String {
        val factor = powerOfTen(decimals)
        val rounded = (value * factor).roundToLong() / factor.toDouble()
        val text = if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
        val parts = text.split('.')
        val whole = groupThousands(parts[0])
        val fraction = parts.getOrNull(1) ?: return whole
        return "$whole.$fraction"
    }

    /** Duracion en formato compacto: `45s`, `12m`, `1h 05m`. */
    fun duration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "$hours h ${pad(minutes)} m"
            minutes > 0 -> "$minutes" + "m"
            else -> "$seconds" + "s"
        }
    }

    /** Texto de la variacion: `+12 %`, `-4 %`, `= 0 %`. */
    fun deltaText(percent: Double): String {
        val rounded = percent.roundToInt()
        return when {
            rounded > 0 -> "+$rounded %"
            rounded < 0 -> "$rounded %"
            else -> "= 0 %"
        }
    }

    private fun groupThousands(value: String): String {
        val negative = value.startsWith('-')
        val digits = if (negative) value.substring(1) else value
        val grouped = digits.reversed().chunked(3).joinToString(THIN_SPACE).reversed()
        return if (negative) "-$grouped" else grouped
    }

    private fun powerOfTen(decimals: Int): Long {
        var result = 1L
        repeat(decimals.coerceIn(0, 6)) { result *= 10 }
        return result
    }

    private fun pad(value: Long): String = if (value < 10) "0$value" else value.toString()

}
