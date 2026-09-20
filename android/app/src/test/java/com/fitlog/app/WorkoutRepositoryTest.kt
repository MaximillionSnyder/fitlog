package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.AddSetInput
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.UpdateSetInput
import com.fitlog.app.data.WorkoutErrorCode
import com.fitlog.app.data.WorkoutException
import com.fitlog.app.data.WorkoutRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var repository: WorkoutRepository
    private lateinit var exerciseA: String
    private lateinit var exerciseB: String

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

        val catalog = CatalogRepository(
            dao = database.catalogDao(),
            seedJsonProvider = { seedJson },
        )
        catalog.ensureSeeded()

        repository = WorkoutRepository(
            dao = database.workoutDao(),
            routinesDao = database.routinesDao(),
            now = { 1_700_000_000_000 },
        )
        val exercises = catalog.loadCatalog().exercises
        exerciseA = exercises.first { it.slug == "press-banca-barra" }.id
        exerciseB = exercises.first { it.slug == "remo-barra" }.id
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `expone el descanso objetivo de la rutina de la sesion`() = runTest {
        val routines = com.fitlog.app.data.RoutinesRepository(
            dao = database.routinesDao(),
            now = { 1_700_000_000_000 },
        )
        val routine = routines.createRoutine("Dia de empuje", null)
        routines.addExercise(
            routine.id,
            com.fitlog.app.data.RoutineExerciseInput(
                exerciseId = exerciseA,
                targetSets = 4,
                targetReps = 8,
                targetWeightKg = null,
                restSeconds = 120,
                notes = null,
            ),
        )
        routines.addExercise(
            routine.id,
            com.fitlog.app.data.RoutineExerciseInput(
                exerciseId = exerciseB,
                targetSets = 3,
                targetReps = 10,
                targetWeightKg = null,
                restSeconds = null,
                notes = null,
            ),
        )

        val session = repository.startSession(routine.id)
        assertEquals(mapOf(exerciseA to 120), repository.restTargets(session.routineId))
        assertEquals(emptyMap<String, Int>(), repository.restTargets(null))
    }

    @Test
    fun `la serie guarda su marca de tiempo`() = runTest {
        val session = repository.startSession()
        repository.addSet(
            AddSetInput(
                sessionId = session.id,
                exerciseId = exerciseA,
                weightKg = 60.0,
                reps = 10,
                rir = 2,
                notes = null,
                isWarmup = false,
            ),
        )

        val sets = repository.sessionDetail(session.id).sets
        assertEquals(1, sets.size)
        assertEquals(1_700_000_000_000, sets.first().createdAtMs)
    }

    @Test
    fun `importa sesiones salteando las que ya existen`() = runTest {
        val draft = WorkoutRepository.ImportedSession(
            startedAtMs = 1_690_000_000_000,
            finishedAtMs = 1_690_000_600_000,
            notes = "Huawei Health · Running · 5 km",
        )

        val first = repository.importSessions(listOf(draft))
        assertEquals(1, first.imported)
        assertEquals(0, first.skipped)

        // Repetir la importacion no duplica: la fecha de inicio ya esta.
        val second = repository.importSessions(listOf(draft))
        assertEquals(0, second.imported)
        assertEquals(1, second.skipped)

        val sessions = repository.sessions()
        assertEquals(1, sessions.size)
        assertEquals("Huawei Health · Running · 5 km", sessions.first().notes)
        assertEquals(1_690_000_600_000, sessions.first().finishedAt)
    }

    @Test
    fun `importa solo lo nuevo de una exportacion posterior`() = runTest {
        val viejo = WorkoutRepository.ImportedSession(1_690_000_000_000, 1_690_000_600_000, "viejo")
        repository.importSessions(listOf(viejo))

        val nuevo = WorkoutRepository.ImportedSession(1_700_000_000_000, 1_700_000_600_000, "nuevo")
        val result = repository.importSessions(listOf(viejo, nuevo))

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertEquals(2, repository.sessions().size)
    }

    @Test
    fun `inicia y finaliza una sesion`() = runTest {
        val session = repository.startSession()
        assertEquals(session.startedAt, 1_700_000_000_000)
        assertNull(session.finishedAt)
        assertEquals(session.id, repository.activeSession()?.id)

        repository.finishSession(session.id)
        assertNull(repository.activeSession())

        val history = repository.sessions()
        assertEquals(1, history.size)
        assertEquals(1_700_000_000_000, history.first().finishedAt)
    }

    @Test
    fun `rechaza una segunda sesion activa`() = runTest {
        repository.startSession()
        val error = runCatching { repository.startSession() }.exceptionOrNull()
        assertEquals(WorkoutErrorCode.SESSION_ALREADY_ACTIVE, (error as WorkoutException).code)
    }

    @Test
    fun `registra series con indice automatico por ejercicio`() = runTest {
        val session = repository.startSession()

        val first = repository.addSet(set(session.id, exerciseA, weight = 100.0, reps = 8))
        val second = repository.addSet(set(session.id, exerciseA, weight = 100.0, reps = 6))
        val other = repository.addSet(set(session.id, exerciseB, weight = 60.0, reps = 10))

        assertEquals(1, first.setIndex)
        assertEquals(2, second.setIndex)
        assertEquals(1, other.setIndex)
    }

    @Test
    fun `valida peso repeticiones rir y ejercicio`() = runTest {
        val session = repository.startSession()

        assertEquals(
            WorkoutErrorCode.INVALID_INPUT,
            runCatching { repository.addSet(set(session.id, exerciseA, weight = -5.0, reps = 8)) }
                .exceptionOrNull()
                .let { (it as WorkoutException).code },
        )
        assertEquals(
            WorkoutErrorCode.INVALID_INPUT,
            runCatching { repository.addSet(set(session.id, exerciseA, weight = 100.0, reps = -1)) }
                .exceptionOrNull()
                .let { (it as WorkoutException).code },
        )
        assertEquals(
            WorkoutErrorCode.INVALID_INPUT,
            runCatching {
                repository.addSet(
                    set(session.id, exerciseA, weight = 100.0, reps = 8).copy(rir = 15)
                )
            }.exceptionOrNull().let { (it as WorkoutException).code },
        )
        assertEquals(
            WorkoutErrorCode.EXERCISE_NOT_FOUND,
            runCatching {
                repository.addSet(set(session.id, "01ARYZ6S41TSV4RRFFQ69G5FAV", weight = 100.0, reps = 8))
            }.exceptionOrNull().let { (it as WorkoutException).code },
        )
    }

    @Test
    fun `rechaza series en una sesion finalizada`() = runTest {
        val session = repository.startSession()
        repository.finishSession(session.id)

        val error = runCatching {
            repository.addSet(set(session.id, exerciseA, weight = 100.0, reps = 8))
        }.exceptionOrNull()

        assertEquals(WorkoutErrorCode.SESSION_NOT_ACTIVE, (error as WorkoutException).code)
    }

    @Test
    fun `edita y elimina series con borrado logico`() = runTest {
        val session = repository.startSession()
        val created = repository.addSet(set(session.id, exerciseA, weight = 100.0, reps = 8))

        repository.updateSet(created.id, UpdateSetInput(weightKg = 102.5, reps = 8, rir = 1, notes = "subida"))

        var detail = repository.sessionDetail(session.id)
        assertEquals(102.5, detail.sets.first().weightKg ?: 0.0, 1e-9)
        assertEquals(1, detail.sets.first().rir)

        repository.deleteSet(created.id)
        detail = repository.sessionDetail(session.id)
        assertTrue(detail.sets.isEmpty())
        assertEquals(0.0, detail.session.summary.totalVolumeKg, 1e-9)
    }

    @Test
    fun `el borrado no cambia los indices de las demas series`() = runTest {
        val session = repository.startSession()
        val indices = mutableListOf<Int>()
        repeat(3) { indices.add(repository.addSet(set(session.id, exerciseA, weight = 80.0, reps = 10)).setIndex) }

        val all = repository.sessionDetail(session.id).sets
        repository.deleteSet(all[1].id)

        val remaining = repository.sessionDetail(session.id).sets.map { it.setIndex }
        assertEquals(listOf(1, 3), remaining)
    }

    @Test
    fun `el historial incluye resumen por sesion`() = runTest {
        val older = repository.startSession()
        repository.addSet(set(older.id, exerciseA, weight = 100.0, reps = 8, isWarmup = false))
        repository.finishSession(older.id)

        val newer = repository.startSession()
        repository.addSet(set(newer.id, exerciseB, weight = 60.0, reps = 10, isWarmup = false))
        repository.addSet(set(newer.id, exerciseB, weight = 40.0, reps = 10, isWarmup = true))

        val history = repository.sessions()
        assertEquals(2, history.size)
        val latest = history.first()
        assertEquals(newer.id, latest.id)
        assertEquals(1, latest.summary.workingSets)
        assertEquals(600.0, latest.summary.totalVolumeKg, 1e-9)
        assertNull(latest.finishedAt)

        assertEquals(800.0, history[1].summary.totalVolumeKg, 1e-9)
    }

    private fun set(
        sessionId: String,
        exerciseId: String,
        weight: Double?,
        reps: Int?,
        rir: Int? = null,
        isWarmup: Boolean = false,
    ) = AddSetInput(
        sessionId = sessionId,
        exerciseId = exerciseId,
        weightKg = weight,
        reps = reps,
        rir = rir,
        notes = null,
        isWarmup = isWarmup,
    )
}
