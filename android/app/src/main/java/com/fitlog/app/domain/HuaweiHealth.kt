package com.fitlog.app.domain

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Lectura de la exportacion de Huawei Health.
 *
 * Huawei exporta un JSON por entrenamiento grabado (carpeta `Motion path detail data`), con el tipo
 * de deporte, el inicio, el fin, la duracion, las calorias, la distancia y la frecuencia cardiaca.
 * No exporta series con peso y reps: esos entrenamientos entran como sesiones sin series.
 *
 * El lector es tolerante a proposito: la exportacion trae tambien archivos de sueno, pasos y
 * estres, y el formato varia entre versiones de la app. Todo lo que no se reconoce se ignora.
 */
object HuaweiHealth {

    /** Entrenamiento reconocido en la exportacion. */
    data class Workout(
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
    ) {
        /** Clave natural para deduplicar dentro de la exportacion. */
        val key: String get() = recordId ?: "$startedAtMs-$sportName"
    }

    /** Resultado de leer un conjunto de archivos. */
    data class ParseResult(
        val workouts: List<Workout>,
        val filesRead: Int,
        val filesSkipped: Int,
    ) {
        val isEmpty: Boolean get() = workouts.isEmpty()
    }

    /** Nombres de los tipos de deporte que usa la exportacion. */
    private val SPORT_NAMES = mapOf(
        2 to "Senderismo",
        3 to "Bicicleta",
        4 to "Running",
        5 to "Caminata",
        102 to "Natación en pileta",
        103 to "Bicicleta fija",
        104 to "Natación en aguas abiertas",
        112 to "Remo",
        117 to "Entrenamiento",
        118 to "Running",
        147 to "Entrenamiento de fuerza",
        281 to "Caminata bajo techo",
        282 to "Senderismo",
    )

    /** Escala de la exportacion: las calorias vienen multiplicadas por mil. */
    private const val CALORIE_SCALE = 1000.0

    /** Lee varios archivos y devuelve los entrenamientos reconocidos, sin repetidos. */
    fun parse(contents: List<String>): ParseResult {
        val byKey = linkedMapOf<String, Workout>()
        var read = 0
        var skipped = 0

        for (content in contents) {
            val workouts = runCatching { parseFile(content) }.getOrNull()
            if (workouts == null) {
                skipped += 1
                continue
            }
            read += 1
            for (workout in workouts) {
                // La exportacion triplica los registros: el primero que aparece gana.
                byKey.putIfAbsent(workout.key, workout)
            }
        }

        return ParseResult(
            workouts = byKey.values.sortedByDescending { it.startedAtMs },
            filesRead = read,
            filesSkipped = skipped,
        )
    }

    /** Lee un archivo: lista, objeto con la lista adentro, o un JSON por linea. */
    fun parseFile(content: String): List<Workout> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        val workouts = mutableListOf<Workout>()
        val root = runCatching { JSONTokener(trimmed).nextValue() }.getOrNull()

        when (root) {
            is JSONArray -> collect(root, workouts)
            is JSONObject -> collect(root, workouts)
        }

        // La exportacion tambien tiene archivos con un objeto por linea: se suman a lo anterior y
        // la deduplicacion por registro se encarga de que no queden repetidos.
        if (trimmed.lineSequence().count { it.trimStart().startsWith("{") } > 1) {
            trimmed.lineSequence()
                .filter { it.trimStart().startsWith("{") }
                .forEach { line ->
                    runCatching { JSONObject(line) }.getOrNull()?.let { collect(it, workouts) }
                }
        }

        return workouts
    }

    private fun collect(node: Any?, out: MutableList<Workout>) {
        when (node) {
            is JSONArray -> for (index in 0 until node.length()) collect(node.opt(index), out)
            is JSONObject -> {
                toWorkout(node)?.let { out += it }
                node.keys().forEach { key ->
                    val value = node.opt(key)
                    if (value is JSONArray || value is JSONObject) collect(value, out)
                }
            }
        }
    }

    /**
     * Convierte un objeto en entrenamiento, o `null` si no lo parece.
     *
     * No alcanza con tener fecha: la exportacion tambien trae sueno, pasos y estres con sus propias
     * marcas de tiempo. Se pide ademas alguna senal de deporte (tipo, nombre, distancia, calorias o
     * frecuencia cardiaca), que es lo que distingue un entrenamiento del resto.
     */
    private fun toWorkout(node: JSONObject): Workout? {
        val startedAt = readTimestamp(node, "startTime", "start_time", "beginTime", "start")
            ?: return null
        if (!looksLikeWorkout(node)) return null
        val durationMs = readDuration(node)
        val finishedAt = readTimestamp(node, "endTime", "end_time", "finishTime", "end")
            ?: durationMs?.let { startedAt + it }

        val sportType = readInt(node, "sportType", "sport_type", "activityType", "exerciseType")
        val sportName = readString(node, "sportName", "activityName", "name")
            ?: sportType?.let { SPORT_NAMES[it] }
            ?: "Entrenamiento"

        return Workout(
            recordId = readString(node, "recordId", "record_id", "id"),
            startedAtMs = startedAt,
            finishedAtMs = finishedAt,
            sportType = sportType,
            sportName = sportName,
            durationMs = durationMs ?: finishedAt?.let { (it - startedAt).coerceAtLeast(0L) },
            distanceM = readDouble(node, "totalDistance", "distance", "total_distance"),
            calories = readDouble(node, "totalCalories", "calories", "total_calories")
                ?.let { it / CALORIE_SCALE },
            steps = readInt(node, "totalSteps", "steps"),
            averageHeartRate = readDouble(node, "avgHeartRate", "averageHeartRate", "avg_heart_rate"),
            maxHeartRate = readDouble(node, "maxHeartRate", "max_heart_rate"),
        )
    }

    /** Senales de que el objeto es un entrenamiento y no otra serie de datos de la exportacion. */
    private fun looksLikeWorkout(node: JSONObject): Boolean {
        if (readDouble(node, "sportType", "sport_type", "activityType", "exerciseType") != null) {
            return true
        }
        if (readString(node, "sportName", "activityName") != null) return true
        if (readDouble(node, "totalDistance", "distance", "total_distance") != null) return true
        if (readDouble(node, "totalCalories", "calories", "total_calories") != null) return true
        return readDouble(node, "avgHeartRate", "averageHeartRate", "maxHeartRate") != null
    }

    /**
     * Lee una fecha: epoch en milisegundos o segundos, o texto ISO / `yyyy-MM-dd HH:mm:ss`.
     *
     * La exportacion usa milisegundos, pero algunas versiones exportan segundos; se distingue por
     * magnitud (un epoch en segundos de este siglo tiene diez digitos, uno en milisegundos trece).
     */
    private fun readTimestamp(node: JSONObject, vararg keys: String): Long? {
        for (key in keys) {
            if (!node.has(key)) continue
            val raw = node.opt(key)
            when (raw) {
                is Number -> return normalizeEpoch(raw.toLong())
                is String -> {
                    val text = raw.trim()
                    if (text.isEmpty()) continue
                    text.toLongOrNull()?.let { return normalizeEpoch(it) }
                    text.toDoubleOrNull()?.let { return normalizeEpoch(it.toLong()) }
                    parseDateText(text)?.let { return it }
                }
            }
        }
        return null
    }

    private fun normalizeEpoch(value: Long): Long =
        if (value in 1..MAX_SECONDS_EPOCH) value * 1000L else value

    private fun parseDateText(text: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(text)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    /**
     * Duracion en milisegundos.
     *
     * `totalTime` ya viene en milisegundos (la exportacion divide por 1000 para obtener segundos);
     * las claves alternativas (`duration`, `exerciseTime`) suelen venir en segundos, asi que se
     * distinguen por magnitud: menos de un dia se interpreta como segundos.
     */
    private fun readDuration(node: JSONObject): Long? {
        readDouble(node, "totalTime", "total_time")?.let { return it.toLong() }
        readDouble(node, "durationMs", "duration_ms")?.let { return it.toLong() }
        readDouble(node, "duration", "exerciseTime")?.let { raw ->
            return if (raw < SECONDS_PER_DAY) (raw * 1000).toLong() else raw.toLong()
        }
        return null
    }

    private fun readString(node: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            val value = node.opt(key)
            if (value is String && value.isNotBlank()) return value.trim()
            if (value is Number) return value.toString()
        }
        return null
    }

    private fun readInt(node: JSONObject, vararg keys: String): Int? =
        readDouble(node, *keys)?.roundToInt()

    private fun readDouble(node: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            val value = node.opt(key)
            when (value) {
                is Number -> return value.toDouble()
                is String -> value.trim().replace(',', '.').toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    /** Epoch en segundos del siglo XXI: por debajo de este valor se asume segundos, no milisegundos. */
    private const val MAX_SECONDS_EPOCH = 4_102_444_800L

    /** Segundos de un dia: separa una duracion en segundos de una en milisegundos. */
    private const val SECONDS_PER_DAY = 86_400.0

    /**
     * Nota de la sesion importada: origen y los datos que Huawei si registro.
     *
     * Es lo unico que queda del entrenamiento ademas de las fechas, porque la exportacion no trae
     * series con peso y reps.
     */
    fun noteFor(workout: Workout): String {
        val parts = mutableListOf("Huawei Health", workout.sportName)

        workout.distanceM?.takeIf { it > 0 }?.let { meters ->
            val km = meters / 1000.0
            parts += if (km >= 1.0) "${formatOneDecimal(km)} km" else "${meters.roundToInt()} m"
        }
        workout.calories?.takeIf { it > 0 }?.let { parts += "${it.roundToInt()} kcal" }
        val average = workout.averageHeartRate
        val max = workout.maxHeartRate
        if (average != null && max != null) {
            parts += "FC ${average.roundToInt()}/${max.roundToInt()}"
        } else if (average != null) {
            parts += "FC media ${average.roundToInt()}"
        }
        workout.steps?.takeIf { it > 0 }?.let { parts += "$it pasos" }

        return parts.joinToString(" · ")
    }

    private fun formatOneDecimal(value: Double): String {
        val rounded = (value * 100).roundToInt() / 100.0
        return rounded.toString()
    }
}
