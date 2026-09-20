package com.fitlog.app.domain

import kotlin.math.roundToInt

/**
 * Entrenamiento importado desde otra app, ya normalizado.
 *
 * Es el modelo comun de todas las fuentes (la exportacion de Huawei Health y los archivos GPX), para
 * que la pantalla de importacion y la importacion en si no sepan de formatos.
 */
data class ImportedWorkout(
    val recordId: String?,
    val startedAtMs: Long,
    val finishedAtMs: Long?,
    val sportType: Int?,
    val sportName: String,
    val durationMs: Long?,
    val distanceM: Double?,
    val calories: Double?,
    val steps: Int?,
    val averageHeartRate: Double?,
    val maxHeartRate: Double?,
    val elevationGainM: Double? = null,
    val source: String = ImportedWorkoutNotes.HUAWEI,
) {
    /** Clave natural para deduplicar dentro de una importacion. */
    val key: String get() = recordId ?: "$startedAtMs-$sportName"
}

/** Armado de la nota de origen y deteccion de sesiones importadas. */
object ImportedWorkoutNotes {

    const val HUAWEI = "Huawei Health"
    const val GPX = "GPX"

    /** Marcas de origen que puede tener la nota de una sesion importada. */
    val SOURCES = listOf(HUAWEI, GPX)

    /** `true` si la sesion vino de una importacion (el origen queda en la nota). */
    fun isImported(notes: String?): Boolean =
        notes != null && SOURCES.any { notes.startsWith(it) }

    /**
     * Parte de la nota sin el origen: los datos que registro la app de origen.
     *
     * La usa el historial para mostrar lo que si tiene una sesion importada (deporte, distancia,
     * frecuencia cardiaca) en lugar de "0 series · 0 kg".
     */
    fun dataSummary(notes: String?): String? {
        if (notes == null) return null
        val source = SOURCES.firstOrNull { notes.startsWith(it) } ?: return null
        return notes.removePrefix(source).removePrefix(" · ").trim().ifEmpty { null }
    }

    /**
     * Nota de la sesion importada: origen y los datos que la fuente si registro.
     *
     * Es lo unico que queda del entrenamiento ademas de las fechas, porque ninguna de las fuentes
     * exporta series con peso y reps.
     */
    fun noteFor(workout: ImportedWorkout): String {
        val parts = mutableListOf(workout.source, workout.sportName)

        workout.distanceM?.takeIf { it > 0 }?.let { meters ->
            val km = meters / 1000.0
            parts += if (km >= 1.0) "${formatDistance(km)} km" else "${meters.roundToInt()} m"
        }
        workout.calories?.takeIf { it > 0 }?.let { parts += "${it.roundToInt()} kcal" }
        val average = workout.averageHeartRate
        val max = workout.maxHeartRate
        if (average != null && max != null) {
            parts += "FC ${average.roundToInt()}/${max.roundToInt()}"
        } else if (average != null) {
            parts += "FC media ${average.roundToInt()}"
        }
        workout.elevationGainM?.takeIf { it >= 5.0 }?.let { parts += "desnivel ${it.roundToInt()} m" }
        workout.steps?.takeIf { it > 0 }?.let { parts += "$it pasos" }

        return parts.joinToString(" · ")
    }

    /** Hasta dos decimales, sin ceros de relleno: `5.24`, `20`. */
    private fun formatDistance(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
