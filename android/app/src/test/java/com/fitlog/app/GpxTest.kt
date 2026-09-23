package com.fitlog.app

import com.fitlog.app.domain.Gpx
import com.fitlog.app.domain.ImportedWorkoutNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxTest {

    /** GPX como el que exporta un reloj: dos puntos con tiempo, altura y frecuencia cardiaca. */
    private fun gpx(
        type: String = "running",
        withTime: Boolean = true,
        withHeartRate: Boolean = true,
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="Huawei Health" xmlns="http://www.topografix.com/GPX/1/1">
          <metadata>
            <time>2023-11-14T22:13:20Z</time>
          </metadata>
          <trk>
            <name>Entrenamiento de la mañana</name>
            <type>$type</type>
            <trkseg>
              <trkpt lat="-34.6037" lon="-58.3816">
                <ele>25.0</ele>
                ${if (withTime) "<time>2023-11-14T22:13:20Z</time>" else ""}
                ${if (withHeartRate) "<extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>120</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions>" else ""}
              </trkpt>
              <trkpt lat="-34.6047" lon="-58.3826">
                <ele>32.0</ele>
                ${if (withTime) "<time>2023-11-14T22:43:20Z</time>" else ""}
                ${if (withHeartRate) "<extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>170</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions>" else ""}
              </trkpt>
            </trkseg>
          </trk>
        </gpx>
    """.trimIndent()

    @Test
    fun `reconoce un gpx y no un json`() {
        assertTrue(Gpx.looksLikeGpx(gpx()))
        assertTrue(!Gpx.looksLikeGpx("""{"recordId": "x", "startTime": 1}"""))
    }

    @Test
    fun `lee el entrenamiento con su duracion, distancia y frecuencia cardiaca`() {
        val workout = Gpx.parse(gpx()).first()

        assertEquals(1_700_000_000_000L, workout.startedAtMs)
        assertEquals(1_700_001_800_000L, workout.finishedAtMs)
        assertEquals(1_800_000L, workout.durationMs)
        assertEquals("Running", workout.sportName)
        assertEquals(145.0, workout.averageHeartRate ?: 0.0, 0.001)
        assertEquals(170.0, workout.maxHeartRate ?: 0.0, 0.001)
        // Dos puntos a ~140 m: la distancia se calcula sumando los tramos.
        assertTrue((workout.distanceM ?: 0.0) in 100.0..200.0)
        // El desnivel positivo entre 25 y 32 m.
        assertEquals(7.0, workout.elevationGainM ?: 0.0, 0.001)
    }

    @Test
    fun `la nota dice que vino de un gpx y de que reloj`() {
        val note = ImportedWorkoutNotes.noteFor(Gpx.parse(gpx()).first())

        assertTrue(note.startsWith("Huawei Health"))
        assertTrue(note.contains("Running"))
        // Dos puntos separados ~140 m: la distancia se muestra en metros.
        assertTrue(note.contains("144 m"))
        assertTrue(note.contains("FC 145/170"))
        assertTrue(note.contains("desnivel 7 m"))
        assertTrue(ImportedWorkoutNotes.isImported(note))
    }

    @Test
    fun `el resumen sin origen sirve para el historial`() {
        val note = ImportedWorkoutNotes.noteFor(Gpx.parse(gpx()).first())
        val summary = ImportedWorkoutNotes.dataSummary(note)

        assertTrue(summary?.startsWith("Running") == true)
        assertTrue(summary?.contains("FC 145/170") == true)
        // Una sesion sin nota de importacion no tiene resumen.
        assertEquals(null, ImportedWorkoutNotes.dataSummary("Entrenamiento libre"))
    }

    @Test
    fun `recupera las metricas de la nota de una sesion ya importada`() {
        // Las sesiones importadas antes de las columnas propias solo tienen la nota.
        val parsed = ImportedWorkoutNotes.parseActivity(
            "Huawei Health · Running · 5.24 km · 320 kcal · FC 147/170 · desnivel 12 m · 6800 pasos"
        )

        assertEquals("Huawei Health", parsed?.source)
        assertEquals(5_240.0, parsed?.distanceM ?: 0.0, 0.001)
        assertEquals(320.0, parsed?.calories ?: 0.0, 0.001)
        assertEquals(147.0, parsed?.averageHeartRate ?: 0.0, 0.001)
        assertEquals(170.0, parsed?.maxHeartRate ?: 0.0, 0.001)
        assertEquals(12.0, parsed?.elevationGainM ?: 0.0, 0.001)
        assertEquals(6_800, parsed?.steps)
    }

    @Test
    fun `la nota de ida y vuelta conserva las metricas`() {
        val workout = Gpx.parse(gpx()).first()
        val parsed = ImportedWorkoutNotes.parseActivity(ImportedWorkoutNotes.noteFor(workout))

        // La nota es un resumen redondeado: la distancia puede diferir en metros.
        assertTrue(kotlin.math.abs((parsed?.distanceM ?: 0.0) - (workout.distanceM ?: 0.0)) < 10.0)
        assertEquals(workout.averageHeartRate ?: 0.0, parsed?.averageHeartRate ?: 0.0, 0.5)
        assertEquals(workout.maxHeartRate ?: 0.0, parsed?.maxHeartRate ?: 0.0, 0.5)
        assertEquals(workout.elevationGainM ?: 0.0, parsed?.elevationGainM ?: 0.0, 0.5)
    }

    @Test
    fun `un nombre de archivo con distancia no se confunde con una metrica`() {
        // Un GPX llamado "salida 5 km.gpx" produce el deporte "Salida 5 km": no debe leerse como
        // una distancia de 5 km al recuperar las metricas de la nota.
        val sinTipo = gpx().replace("<type>running</type>", "").replace(
            "<name>Entrenamiento de la mañana</name>",
            "",
        )
        val workout = Gpx.parse(sinTipo, "salida 5 km.gpx").first()
        val note = ImportedWorkoutNotes.noteFor(workout)

        assertTrue(note.startsWith("Huawei Health · Salida 5 km"))
        // La distancia de la nota es la real (144 m), no el "5" del nombre.
        val parsed = ImportedWorkoutNotes.parseActivity(note)
        assertTrue((parsed?.distanceM ?: 0.0) < 1_000.0)
    }

    @Test
    fun `una nota que no es de importacion no tiene metricas`() {
        assertEquals(null, ImportedWorkoutNotes.parseActivity("Entrenamiento libre"))
        assertEquals(null, ImportedWorkoutNotes.parseActivity(null))
    }

    @Test
    fun `sin creador conocido la nota dice GPX`() {
        val sinCreador = gpx().replace(""" creator="Huawei Health"""", "")
        val note = ImportedWorkoutNotes.noteFor(Gpx.parse(sinCreador).first())

        assertTrue(note.startsWith("GPX · Running"))
    }

    @Test
    fun `un gpx sin ninguna fecha no se importa`() {
        val sinFechas = gpx(withTime = false).replace("<time>2023-11-14T22:13:20Z</time>", "")

        assertTrue(Gpx.parse(sinFechas).isEmpty())
    }

    @Test
    fun `un gpx con una sola marca de tiempo y sin metadata no se importa`() {
        val unPunto = """
            <gpx version="1.1"><trk><trkseg>
              <trkpt lat="-34.6" lon="-58.3"><time>2023-11-14T22:13:20Z</time></trkpt>
            </trkseg></trk></gpx>
        """.trimIndent()

        assertTrue(Gpx.parse(unPunto).isEmpty())
    }

    @Test
    fun `un gpx con una sola marca de tiempo usa la del metadata`() {
        val unPunto = gpx(withTime = false).replace(
            "<trkpt lat=\"-34.6037\" lon=\"-58.3816\">",
            "<trkpt lat=\"-34.6037\" lon=\"-58.3816\"><time>2023-11-14T22:13:20Z</time>",
        ).replace("<trkpt lat=\"-34.6047\" lon=\"-58.3826\">", "<trkpt lat=\"-34.6047\" lon=\"-58.3826\">")

        val workout = Gpx.parse(unPunto).first()
        assertEquals(1_700_000_000_000L, workout.startedAtMs)
        assertEquals(0L, workout.durationMs)
    }

    @Test
    fun `sin tiempos por punto usa la fecha del metadata, sin duracion`() {
        // Un GPX sin marcas de tiempo en los puntos pero con fecha en el metadata: se importa con
        // esa fecha y sin duracion, en lugar de perderlo.
        val workout = Gpx.parse(gpx(withTime = false)).first()

        assertEquals(1_700_000_000_000L, workout.startedAtMs)
        assertEquals(1_700_000_000_000L, workout.finishedAtMs)
        assertEquals(0L, workout.durationMs)
    }

    @Test
    fun `un gpx con BOM se lee igual`() {
        val conBom = "\uFEFF" + gpx()

        assertTrue(Gpx.looksLikeGpx(conBom))
        assertEquals(1, Gpx.parse(conBom).size)
    }

    @Test
    fun `un gpx sin frecuencia cardiaca no inventa el dato`() {
        val workout = Gpx.parse(gpx(withHeartRate = false)).first()

        assertNull(workout.averageHeartRate)
        assertNull(workout.maxHeartRate)
    }

    @Test
    fun `traduce el tipo de deporte del gpx`() {
        assertEquals("Bicicleta", Gpx.parse(gpx(type = "cycling")).first().sportName)
        assertEquals("Caminata", Gpx.parse(gpx(type = "hiking")).first().sportName)
    }

    @Test
    fun `sin tipo usa el nombre del archivo`() {
        val sinTipo = gpx().replace("<type>running</type>", "").replace(
            "<name>Entrenamiento de la mañana</name>",
            "",
        )

        assertEquals("Salida sabado", Gpx.parse(sinTipo, "salida_sabado.gpx").first().sportName)
    }
}
