package com.fitlog.app.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lectura de archivos GPX.
 *
 * Un GPX es una ruta con puntos: no trae series con peso y reps, pero si el inicio, el fin, la
 * distancia (sumando los tramos), la frecuencia cardiaca de cada punto y el desnivel. Se lee con un
 * recorrido propio de etiquetas, tolerante a espacios, atributos con comillas simples o dobles y
 * prefijos de namespace (`gpxtpx:hr`), porque los GPX los genera cada reloj a su manera.
 */
object Gpx {

    /** `true` si el texto parece un GPX y no un JSON de Huawei Health. */
    fun looksLikeGpx(text: String): Boolean {
        val head = text.trimStart().removePrefix(BOM).take(HEAD_LENGTH)
        return head.contains("<gpx", ignoreCase = true) || head.trimStart().startsWith("<?xml")
    }

    /** Lee un archivo GPX; devuelve como mucho un entrenamiento. */
    fun parse(content: String, fileName: String? = null): List<ImportedWorkout> {
        val text = content.trim().removePrefix(BOM)
        if (!looksLikeGpx(text)) return emptyList()

        val track = readTrack(text) ?: return emptyList()
        if (track.points.isEmpty()) return emptyList()

        // Con dos marcas de tiempo hay entrenamiento completo. Si el archivo no las trae (o trae
        // una sola, que no alcanza para medir nada) pero si una fecha en el metadata, se importa con
        // esa fecha y sin duracion: es mejor que perderlo.
        val times = track.points.mapNotNull { it.timeMs }.sorted()
        val complete = times.size >= 2
        val startedAt = if (complete) times.first() else track.metadataTimeMs ?: return emptyList()
        val finishedAt = if (complete) times.last() else startedAt
        val heartRates = track.points.mapNotNull { it.heartRate }

        return listOf(
            ImportedWorkout(
                recordId = null,
                startedAtMs = startedAt,
                finishedAtMs = finishedAt,
                sportType = null,
                sportName = track.sportName ?: fileName?.let { nameWithoutExtension(it) } ?: "Entrenamiento",
                durationMs = finishedAt?.let { it - startedAt },
                distanceM = track.distanceM().takeIf { it > 0 },
                calories = null,
                steps = null,
                averageHeartRate = heartRates.takeIf { it.isNotEmpty() }?.average(),
                maxHeartRate = heartRates.maxOrNull(),
                elevationGainM = track.elevationGainM(),
                source = track.source,
            )
        )
    }

    private const val HEAD_LENGTH = 2048

    /** Marca de orden de bytes: algunos GPX la traen al principio. */
    private const val BOM = "\uFEFF"

    private data class Point(
        val latitude: Double?,
        val longitude: Double?,
        val elevation: Double?,
        val timeMs: Long?,
        val heartRate: Double?,
    )

    private data class Track(
        val points: List<Point>,
        val sportName: String?,
        val creator: String?,
        val metadataTimeMs: Long?,
    ) {
        val source: String
            get() = if (creator?.contains("huawei", ignoreCase = true) == true) {
                ImportedWorkoutNotes.HUAWEI
            } else {
                ImportedWorkoutNotes.GPX
            }

        /** Distancia acumulada entre puntos consecutivos con coordenadas. */
        fun distanceM(): Double {
            var total = 0.0
            var previous: Point? = null
            for (point in points) {
                val last = previous
                if (last != null &&
                    point.latitude != null && point.longitude != null &&
                    last.latitude != null && last.longitude != null
                ) {
                    total += haversineM(
                        last.latitude,
                        last.longitude,
                        point.latitude,
                        point.longitude,
                    )
                }
                previous = point
            }
            return total
        }

        /** Desnivel positivo, ignorando el ruido del altimetro. */
        fun elevationGainM(): Double? {
            val elevations = points.mapNotNull { it.elevation }
            if (elevations.size < 2) return null
            var gain = 0.0
            var reference = elevations.first()
            for (elevation in elevations.drop(1)) {
                val difference = elevation - reference
                if (difference > ELEVATION_NOISE_M) {
                    gain += difference
                    reference = elevation
                } else if (difference < -ELEVATION_NOISE_M) {
                    reference = elevation
                }
            }
            return if (gain > 0) gain else null
        }
    }

    private const val ELEVATION_NOISE_M = 2.0

    /**
     * Recorre las etiquetas del GPX y arma el track.
     *
     * No es un parser XML completo: solo entiende lo que un GPX usa (etiquetas, atributos, texto y
     * comentarios), que es suficiente y evita depender de una libreria.
     */
    private fun readTrack(content: String): Track? {
        val points = mutableListOf<Point>()
        var current: Point? = null
        var sportName: String? = null
        var creator: String? = null
        var metadataTime: Long? = null
        var inMetadata = false
        var textTarget: String? = null
        var buffer = StringBuilder()
        var sawTrack = false

        var index = 0
        while (index < content.length) {
            val open = content.indexOf('<', index)
            if (open < 0) break

            // Texto entre etiquetas: se guarda si estabamos leyendo un campo conocido.
            if (open > index && textTarget != null) buffer.append(content, index, open)

            if (content.startsWith("<!--", open)) {
                val end = content.indexOf("-->", open)
                index = if (end < 0) content.length else end + 3
                continue
            }
            if (content.startsWith("<![CDATA[", open)) {
                val end = content.indexOf("]]>", open)
                if (end > 0 && textTarget != null) buffer.append(content, open + 9, end)
                index = if (end < 0) content.length else end + 3
                continue
            }

            val close = content.indexOf('>', open)
            if (close < 0) break
            val raw = content.substring(open + 1, close).trim()
            index = close + 1
            if (raw.isEmpty()) continue

            val closing = raw.startsWith("/")
            val selfClosing = raw.endsWith("/")
            val body = raw.removePrefix("/").removeSuffix("/").trim()
            val name = body.substringBefore(' ').substringBefore('/').lowercase()
            val localName = name.substringAfterLast(':')
            val attributes = readAttributes(body)

            when {
                closing -> {
                    val text = buffer.toString().trim()
                    when (localName) {
                        "trkpt" -> current?.let { points += it }
                        "time" -> if (textTarget == "time" && text.isNotEmpty()) {
                            parseTime(text)?.let { parsed ->
                                if (current != null) {
                                    current = current?.copy(timeMs = parsed)
                                } else if (inMetadata) {
                                    metadataTime = parsed
                                }
                            }
                        }
                        "metadata" -> inMetadata = false
                        "ele" -> if (textTarget == "ele" && text.isNotEmpty()) {
                            current = current?.copy(elevation = text.toDoubleOrNull())
                        }
                        "hr" -> if (textTarget == "hr" && text.isNotEmpty()) {
                            current = current?.copy(heartRate = text.toDoubleOrNull())
                        }
                        "name" -> if (textTarget == "name" && text.isNotEmpty() && sportName == null) {
                            sportName = text
                        }
                        "type" -> if (textTarget == "type" && text.isNotEmpty()) {
                            sportName = sportLabel(text) ?: sportName
                        }
                    }
                    if (textTarget == localName) textTarget = null
                    buffer = StringBuilder()
                }

                else -> {
                    if (localName == "gpx") {
                        creator = attributes["creator"]
                        sawTrack = sawTrack || false
                    }
                    if (localName == "metadata") inMetadata = true
                    if (localName == "trkpt") {
                        sawTrack = true
                        current = Point(
                            latitude = attributes["lat"]?.toDoubleOrNull(),
                            longitude = attributes["lon"]?.toDoubleOrNull(),
                            elevation = null,
                            timeMs = null,
                            heartRate = null,
                        )
                    }
                    if (localName == "time" || localName == "ele" || localName == "hr" ||
                        localName == "name" || localName == "type"
                    ) {
                        textTarget = localName
                        buffer = StringBuilder()
                    }
                    if (selfClosing) {
                        if (localName == "trkpt") {
                            points += current ?: Point(null, null, null, null, null)
                            current = null
                        }
                        if (textTarget == localName) textTarget = null
                    }
                }
            }
        }

        if (!sawTrack && points.isEmpty()) return null
        return Track(
            points = points,
            sportName = sportName,
            creator = creator,
            metadataTimeMs = metadataTime,
        )
    }

    private fun readAttributes(body: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        var index = 0
        while (index < body.length) {
            val equals = body.indexOf('=', index)
            if (equals < 0) break
            val name = body.substring(index, equals).trim().substringAfterLast(' ').lowercase()
            var cursor = equals + 1
            while (cursor < body.length && body[cursor].isWhitespace()) cursor += 1
            if (cursor >= body.length) break
            val quote = body[cursor]
            if (quote != '"' && quote != '\'') {
                index = cursor
                continue
            }
            val end = body.indexOf(quote, cursor + 1)
            if (end < 0) break
            if (name.isNotEmpty()) attributes[name] = body.substring(cursor + 1, end)
            index = end + 1
        }
        return attributes
    }

    /** Fecha del GPX: ISO 8601 en UTC, con o sin milisegundos. */
    private fun parseTime(text: String): Long? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(text)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    /** Tipo de deporte del GPX a partir de su etiqueta `type` o `name`. */
    private fun sportLabel(raw: String): String? {
        val value = raw.trim().lowercase()
        if (value.isEmpty()) return null
        return when {
            value.contains("run") -> "Running"
            value.contains("cycl") || value.contains("bike") || value.contains("bici") -> "Bicicleta"
            value.contains("walk") || value.contains("camin") || value.contains("hik") -> "Caminata"
            value.contains("swim") || value.contains("natac") -> "Natación"
            value.contains("row") || value.contains("remo") -> "Remo"
            else -> raw.trim()
        }
    }

    private fun nameWithoutExtension(fileName: String): String =
        fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
            .replaceFirstChar { it.uppercase() }

    /** Distancia entre dos coordenadas por la formula del semiverseno. */
    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusM = 6_371_000.0
        val deltaLat = (lat2 - lat1) * PI / 180.0
        val deltaLon = (lon2 - lon1) * PI / 180.0
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }
}
