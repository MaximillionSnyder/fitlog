package com.fitlog.app.domain

object Body {

    enum class Kind(val wire: String) {
        BODY_WEIGHT("body_weight"),
        BODY_FAT("body_fat"),
        WAIST("waist"),
        CHEST("chest"),
        ARM("arm"),
        THIGH("thigh"),
        HIP("hip"),
        NECK("neck"),
        OTHER("other"),
        ;

        companion object {
            fun fromWire(value: String): Kind? = entries.firstOrNull { it.wire == value }
        }
    }

    enum class Unit(val wire: String) {
        KG("kg"),
        CM("cm"),
        PERCENT("%"),
        ;

        companion object {
            fun fromWire(value: String): Unit? = entries.firstOrNull { it.wire == value }
        }
    }

    data class Point(
        val id: String,
        val kind: Kind,
        val measuredAtMs: Long,
        val value: Double,
        val unit: Unit,
        val notes: String?,
    )

    data class Stats(
        val count: Int,
        val first: Double?,
        val latest: Double?,
        val min: Double?,
        val max: Double?,
        val deltaAbs: Double?,
        val deltaPct: Double?,
    )

    fun unitForKind(kind: Kind): Unit = when (kind) {
        Kind.BODY_WEIGHT -> Unit.KG
        Kind.BODY_FAT -> Unit.PERCENT
        else -> Unit.CM
    }

    fun validate(kindWire: String, value: Double): String? {
        val kind = Kind.fromWire(kindWire)
            ?: return "Tipo de medida desconocido: $kindWire"
        if (!value.isFinite() || value <= 0) {
            return "El valor debe ser un número mayor que 0"
        }
        if (kind == Kind.BODY_FAT && value > 100) {
            return "El porcentaje de grasa debe estar entre 0 y 100"
        }
        return null
    }

    fun series(
        metrics: List<Point>,
        kind: Kind,
        range: Progress.Range? = null,
    ): List<Point> = metrics
        .filter { metric ->
            if (metric.kind != kind) return@filter false
            if (range == null) return@filter true
            val from = range.fromMs
            if (from != null && metric.measuredAtMs < from) return@filter false
            metric.measuredAtMs <= range.toMs
        }
        .sortedWith(compareBy({ it.measuredAtMs }, { it.id }))

    fun stats(points: List<Point>): Stats {
        if (points.isEmpty()) {
            return Stats(
                count = 0,
                first = null,
                latest = null,
                min = null,
                max = null,
                deltaAbs = null,
                deltaPct = null,
            )
        }

        val values = points.map { it.value }
        val first = values.first()
        val latest = values.last()
        val deltaAbs = Formulas.roundToTenth(latest - first)

        return Stats(
            count = points.size,
            first = first,
            latest = latest,
            min = values.min(),
            max = values.max(),
            deltaAbs = deltaAbs,
            deltaPct = if (first == 0.0) null else Formulas.roundToTenth(deltaAbs / first * 100),
        )
    }

    fun formatValue(value: Double, unit: Unit): String {
        val rounded = Formulas.roundToTenth(value)
        val text = if (rounded == rounded.toInt().toDouble()) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
        return "$text ${unit.wire}"
    }

    fun label(kind: Kind): String = when (kind) {
        Kind.BODY_WEIGHT -> "Peso corporal"
        Kind.BODY_FAT -> "Grasa corporal"
        Kind.WAIST -> "Cintura"
        Kind.CHEST -> "Pecho"
        Kind.ARM -> "Brazo"
        Kind.THIGH -> "Muslo"
        Kind.HIP -> "Cadera"
        Kind.NECK -> "Cuello"
        Kind.OTHER -> "Otra"
    }
}
