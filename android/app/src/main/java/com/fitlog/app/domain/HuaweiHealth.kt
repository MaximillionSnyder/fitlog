package com.fitlog.app.domain

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    /** Resultado de leer un conjunto de archivos. */
    data class ParseResult(
        val workouts: List<ImportedWorkout>,
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
        val byKey = linkedMapOf<String, ImportedWorkout>()
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

    /**
     * Lee un archivo de la exportacion.
     *
     * Primero intenta el camino normal (lista u objeto). Si no encuentra entrenamientos, recorre el
     * texto buscando objetos balanceados: asi entran los archivos con varios objetos concatenados
     * (la exportacion los parte con marcadores de resincronizacion) y los que traen una comilla
     * suelta dentro del blob de sensores, que rompe el JSON.
     */
    fun parseFile(content: String): List<ImportedWorkout> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Camino rapido: el archivo entero es JSON valido (una lista o un objeto).
        val root = runCatching { JSONTokener(trimmed).nextValue() }.getOrNull()
        val direct = mutableListOf<ImportedWorkout>()
        when (root) {
            is JSONArray -> collect(root, direct)
            is JSONObject -> collect(root, direct)
        }

        val candidates = balancedObjects(trimmed)
        if (direct.isNotEmpty() && candidates.size <= 1) return direct

        // Varios objetos seguidos (o JSON roto por una comilla suelta): se leen uno por uno.
        val scanned = mutableListOf<ImportedWorkout>()
        for (candidate in candidates) {
            val parsed = runCatching { JSONObject(candidate) }.getOrNull()
                ?: runCatching { JSONObject(repairAttributeQuotes(candidate)) }.getOrNull()
                ?: continue
            collect(parsed, scanned)
        }

        val result = if (scanned.isNotEmpty()) scanned else direct
        return result.distinctBy { it.key }
    }

    /**
     * Objetos JSON balanceados del texto, respetando los literales de texto.
     *
     * Cubre archivos con varios objetos seguidos, con o sin saltos de linea, y listas: en todos los
     * casos los objetos se recortan por profundidad de llaves.
     */
    private fun balancedObjects(text: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false

        for (index in text.indices) {
            val char = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) start = index
                    depth += 1
                }
                '}' -> {
                    depth -= 1
                    if (depth <= 0 && start >= 0) {
                        objects += text.substring(start, index + 1)
                        start = -1
                        depth = 0
                    }
                }
            }
        }
        return objects
    }

    /**
     * Quita las comillas sueltas dentro del campo `attribute`.
     *
     * La exportacion guarda ahi la telemetria como texto y a veces aparece una comilla sin escapar,
     * que invalida el archivo entero. El valor real termina en la comilla seguida de coma o cierre.
     */
    fun repairAttributeQuotes(text: String): String {
        if (!text.contains(ATTRIBUTE_FIELD)) return text

        val builder = StringBuilder()
        var index = 0
        while (index < text.length) {
            val match = ATTRIBUTE_FIELD.find(text, index) ?: break
            val valueStart = match.range.last + 1
            builder.append(text, index, valueStart)

            var cursor = valueStart
            var end = -1
            while (cursor < text.length) {
                if (text[cursor] == '"') {
                    var probe = cursor + 1
                    while (probe < text.length && text[probe].isWhitespace()) probe += 1
                    val closes = probe >= text.length ||
                        text[probe] == ',' || text[probe] == '}' || text[probe] == ']'
                    if (closes) {
                        end = cursor
                        break
                    }
                }
                cursor += 1
            }
            if (end < 0) {
                index = valueStart
                continue
            }
            builder.append(text.substring(valueStart, end).replace("\"", ""))
            index = end
        }
        builder.append(text, index, text.length)
        return builder.toString()
    }

    private val ATTRIBUTE_FIELD = Regex("\"attribute\"\\s*:\\s*\"", RegexOption.IGNORE_CASE)

    private fun collect(node: Any?, out: MutableList<ImportedWorkout>) {
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
    private fun toWorkout(node: JSONObject): ImportedWorkout? {
        val startedAt = readTimestamp(node, "startTime", "start_time", "beginTime", "start")
            ?: return null
        if (!looksLikeWorkout(node)) return null

        val durationMs = readDuration(node)
        val explicitEnd = readTimestamp(node, "endTime", "end_time", "finishTime", "end")
        // Un entrenamiento dura: los archivos por minuto traen fecha y calorias, pero no duracion.
        if (durationMs == null && explicitEnd == null) return null
        val finishedAt = explicitEnd ?: durationMs?.let { startedAt + it }

        val attribute = readString(node, "attribute", "attributes")
        val summaryHeartRate = readDouble(node, "avgHeartRate", "averageHeartRate", "avg_heart_rate")
        val track = if (summaryHeartRate == null) trackHeartRate(attribute) else null

        val sportType = readInt(node, "sportType", "sport_type", "activityType", "exerciseType")
        val sportName = readString(node, "sportName", "activityName", "name")
            ?: sportType?.let { SPORT_NAMES[it] }
            ?: "Entrenamiento"

        return ImportedWorkout(
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
            averageHeartRate = summaryHeartRate ?: track?.first,
            maxHeartRate = readDouble(node, "maxHeartRate", "max_heart_rate") ?: track?.second,
        )
    }

    /**
     * Frecuencia cardiaca derivada del blob de sensores (`attribute`).
     *
     * La exportacion documenta `avgHeartRate` y `maxHeartRate`, pero en la practica vienen vacios:
     * el dato real esta en los segmentos `tp=h-r;k=<minuto>;v=<pulsaciones>;` del blob. Devuelve
     * (promedio, maximo), o `null` si no hay lecturas.
     */
    fun trackHeartRate(attribute: String?): Pair<Double, Double>? {
        if (attribute.isNullOrBlank()) return null

        val readings = mutableListOf<Double>()
        var index = 0
        while (index < attribute.length) {
            val segmentStart = attribute.indexOf(HR_TAG, index, ignoreCase = true)
            if (segmentStart < 0) break
            val contentStart = segmentStart + HR_TAG.length
            val nextSegment = attribute.indexOf("tp=", contentStart, ignoreCase = true)
            val end = if (nextSegment < 0) attribute.length else nextSegment
            HEART_RATE_PAIR.findAll(attribute.substring(contentStart, end)).forEach { match ->
                match.groupValues[2].toDoubleOrNull()?.let { readings += it }
            }
            index = end
        }

        if (readings.isEmpty()) return null
        return readings.average() to readings.max()
    }

    private const val HR_TAG = "tp=h-r;"

    /** Lectura de frecuencia cardiaca dentro de un segmento `h-r`: `k=<minuto>;v=<pulsaciones>;`. */
    private val HEART_RATE_PAIR = Regex("k=([-\\d.]+);v=([-\\d.]+);", RegexOption.IGNORE_CASE)

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

    /** Marca de origen en la nota de una sesion importada. */
    const val NOTE_PREFIX = ImportedWorkoutNotes.HUAWEI

    /** `true` si la sesion vino de una importacion (el origen queda en la nota). */
    fun isImportedNote(notes: String?): Boolean = ImportedWorkoutNotes.isImported(notes)

    /** Nota de la sesion importada: origen y los datos que Huawei si registro. */
    fun noteFor(workout: ImportedWorkout): String = ImportedWorkoutNotes.noteFor(workout)
}
