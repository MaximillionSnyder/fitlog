package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.domain.HuaweiHealth
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Camino completo de la importacion: leer una exportacion como la real, convertirla en sesiones y
 * comprobar el historial.
 *
 * Los archivos imitan lo que trae Huawei Health: los entrenamientos repartidos en varios archivos
 * con registros triplicados, mas archivos de sueno y de pasos que no son entrenamientos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HuaweiImportIntegrationTest {

    private lateinit var database: FitLogDatabase
    private lateinit var repository: WorkoutRepository

    private val seedJson: String by lazy {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.assets.open("seed/catalog.json").bufferedReader().use { it.readText() }
    }

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).allowMainThreadQueries().build()

        CatalogRepository(dao = database.catalogDao(), seedJsonProvider = { seedJson })
            .ensureSeeded()

        repository = WorkoutRepository(
            dao = database.workoutDao(),
            routinesDao = database.routinesDao(),
            now = { 1_700_000_000_000 },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun activity(
        recordId: String,
        startedAt: Long,
        sportType: Int,
        distanceM: Int? = null,
        calories: Int? = null,
        heartRateAttribute: String? = null,
    ): String = buildString {
        append("{")
        append("\"recordId\": \"$recordId\",")
        append("\"startTime\": $startedAt,")
        append("\"endTime\": ${startedAt + 2_700_000},")
        append("\"sportType\": $sportType,")
        append("\"totalTime\": 2700000,")
        distanceM?.let { append("\"totalDistance\": $it,") }
        calories?.let { append("\"totalCalories\": ${it * 1000},") }
        heartRateAttribute?.let { append("\"attribute\": \"$it\",") }
        append("\"totalSteps\": 4200")
        append("}")
    }

    /** Exportacion de prueba: tres entrenamientos, con los registros repetidos entre archivos. */
    private fun exportFiles(): List<String> {
        val base = 1_690_000_000_000L
        val day = 86_400_000L

        val running = activity(
            recordId = "run-1",
            startedAt = base,
            sportType = 4,
            distanceM = 5_240,
            calories = 320,
            heartRateAttribute = "tp=lbs;k=1;lat=-34.6;lon=-58.3;tp=h-r;k=0;v=120;k=1;v=150;k=2;v=170;",
        )
        val cycling = activity(
            recordId = "bike-1",
            startedAt = base + day,
            sportType = 3,
            distanceM = 20_000,
            calories = 600,
        )
        val strength = activity(
            recordId = "gym-1",
            startedAt = base + 2 * day,
            sportType = 147,
        )

        return listOf(
            // Primer archivo: dos entrenamientos.
            "[$running, $cycling]",
            // Segundo archivo: el mismo running otra vez (la exportacion triplica) mas el de fuerza.
            "[$running, $strength]",
            // Archivo de sueno: tiene fecha y fin, pero no es un entrenamiento.
            """{"sleepRecords": [{"startTime": ${base + 3 * day}, "endTime": ${base + 3 * day + 28_800_000}, "deepSleep": 90}]}""",
            // Archivo de pasos por minuto: tampoco es un entrenamiento.
            """{"sportPerMinute": [{"startTime": ${base + 4 * day}, "steps": 120, "calories": 8}]}""",
        )
    }

    @Test
    fun `importa la exportacion completa y deja el historial con las tres sesiones`() = runTest {
        val parsed = HuaweiHealth.parse(exportFiles())
        assertEquals(3, parsed.workouts.size)

        val result = repository.importSessions(
            parsed.workouts.map { workout ->
                WorkoutRepository.ImportedSession(
                    startedAtMs = workout.startedAtMs,
                    finishedAtMs = workout.finishedAtMs,
                    notes = HuaweiHealth.noteFor(workout),
                )
            }
        )
        assertEquals(3, result.imported)
        assertEquals(0, result.skipped)

        val sessions = repository.sessions()
        assertEquals(3, sessions.size)
        // El historial viene del mas nuevo al mas viejo.
        assertEquals("Huawei Health · Entrenamiento de fuerza · 4200 pasos", sessions[0].notes)
        assertTrue(sessions[1].notes?.contains("Bicicleta") == true)
        assertTrue(sessions[2].notes?.contains("Running") == true)

        // La frecuencia cardiaca del resumen no venia: se deriva del blob de sensores.
        assertTrue(sessions[2].notes?.contains("FC 147/170") == true)

        // Una sesion importada no tiene series: Huawei no exporta peso ni reps.
        assertEquals(0, sessions[2].summary.workingSets)
        assertEquals(0.0, sessions[2].summary.totalVolumeKg, 0.001)
    }

    @Test
    fun `reimportar la misma exportacion no agrega nada`() = runTest {
        val parsed = HuaweiHealth.parse(exportFiles())
        val drafts = parsed.workouts.map { workout ->
            WorkoutRepository.ImportedSession(
                startedAtMs = workout.startedAtMs,
                finishedAtMs = workout.finishedAtMs,
                notes = HuaweiHealth.noteFor(workout),
            )
        }

        repository.importSessions(drafts)
        val second = repository.importSessions(drafts)

        assertEquals(0, second.imported)
        assertEquals(3, second.skipped)
        assertEquals(3, repository.sessions().size)
    }

    @Test
    fun `una exportacion posterior solo agrega lo nuevo`() = runTest {
        val parsed = HuaweiHealth.parse(exportFiles())
        repository.importSessions(
            parsed.workouts.map { workout ->
                WorkoutRepository.ImportedSession(
                    startedAtMs = workout.startedAtMs,
                    finishedAtMs = workout.finishedAtMs,
                    notes = HuaweiHealth.noteFor(workout),
                )
            }
        )

        // La exportacion nueva trae los mismos tres mas uno reciente.
        val nuevo = HuaweiHealth.parse(
            listOf(activity(recordId = "run-2", startedAt = 1_695_000_000_000, sportType = 4))
        )
        val result = repository.importSessions(
            nuevo.workouts.map { workout ->
                WorkoutRepository.ImportedSession(
                    startedAtMs = workout.startedAtMs,
                    finishedAtMs = workout.finishedAtMs,
                    notes = HuaweiHealth.noteFor(workout),
                )
            }
        )

        assertEquals(1, result.imported)
        assertEquals(4, repository.sessions().size)
    }
}
