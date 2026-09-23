package com.fitlog.app.domain

/**
 * Ruta de un entrenamiento con GPS: los puntos del archivo, ya recortados.
 *
 * Un GPX no trae un mapa (el mapa son imagenes de un servicio de teselas): trae las coordenadas. Con
 * ellas se dibuja el trazado y los perfiles de altura y pulso, sin conexion.
 */
object Route {

    /** Tope de puntos guardados: alcanza para el trazado y no infla la base. */
    const val MAX_POINTS = 300

    data class Point(
        val latitude: Double,
        val longitude: Double,
        val elevation: Double? = null,
        val heartRate: Double? = null,
    )

    /** Recorta la ruta a [MAX_POINTS] puntos, repartidos parejo y conservando el primero y el ultimo. */
    fun simplify(points: List<Point>): List<Point> {
        val valid = points.filter { it.latitude.isFinite() && it.longitude.isFinite() }
        if (valid.size <= MAX_POINTS) return valid

        val step = (valid.size - 1).toDouble() / (MAX_POINTS - 1)
        return (0 until MAX_POINTS).map { index ->
            valid[(index * step).toInt().coerceAtMost(valid.size - 1)]
        }
    }

    /**
     * Texto compacto de la ruta: `lat,lon,ele,hr;lat,lon,ele,hr`.
     *
     * Los campos vacios son `null`. Es el mismo formato en Android y en la web, y no necesita
     * ninguna libreria para leerse.
     */
    fun encode(points: List<Point>): String = points.joinToString(";") { point ->
        listOf(
            format(point.latitude, 5),
            format(point.longitude, 5),
            point.elevation?.let { format(it, 1) } ?: "",
            point.heartRate?.let { format(it, 0) } ?: "",
        ).joinToString(",")
    }

    fun decode(text: String?): List<Point> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(';').mapNotNull { chunk ->
            val fields = chunk.split(',')
            val latitude = fields.getOrNull(0)?.toDoubleOrNull() ?: return@mapNotNull null
            val longitude = fields.getOrNull(1)?.toDoubleOrNull() ?: return@mapNotNull null
            Point(
                latitude = latitude,
                longitude = longitude,
                elevation = fields.getOrNull(2)?.toDoubleOrNull(),
                heartRate = fields.getOrNull(3)?.toDoubleOrNull(),
            )
        }
    }

    /** Distancia acumulada de la ruta, en metros (semiverseno). */
    fun distanceM(points: List<Point>): Double {
        var total = 0.0
        for (index in 1 until points.size) {
            total += Formulas.haversineM(
                points[index - 1].latitude,
                points[index - 1].longitude,
                points[index].latitude,
                points[index].longitude,
            )
        }
        return total
    }

    private fun format(value: Double, decimals: Int): String =
        java.math.BigDecimal(value).setScale(decimals, java.math.RoundingMode.HALF_UP).toPlainString()
}
