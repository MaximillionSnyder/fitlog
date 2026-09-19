package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.RoutineErrorCode
import com.fitlog.app.data.RoutineException
import com.fitlog.app.data.RoutineExerciseInput
import com.fitlog.app.data.RoutinesRepository
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
class RoutinesRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var repository: RoutinesRepository
    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var exerciseA: String
    private lateinit var exerciseB: String
    private lateinit var exerciseC: String

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

        val catalog = CatalogRepository(dao = database.catalogDao(), seedJsonProvider = { seedJson })
        catalog.ensureSeeded()

        repository = RoutinesRepository(dao = database.routinesDao(), now = { 1_700_000_000_000 })
        workoutRepository = WorkoutRepository(
            dao = database.workoutDao(),
            routinesDao = database.routinesDao(),
            now = { 1_700_000_000_000 },
        )

        val exercises = catalog.loadCatalog().exercises
        exerciseA = exercises.first { it.slug == "press-banca-barra" }.id
        exerciseB = exercises.first { it.slug == "remo-barra" }.id
        exerciseC = exercises.first { it.slug == "sentadilla-barra" }.id
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun targets(exerciseId: String, sets: Int? = 4, reps: Int? = 8) = RoutineExerciseInput(
        exerciseId = exerciseId,
        targetSets = sets,
        targetReps = reps,
        targetWeightKg = null,
        restSeconds = null,
        notes = null,
    )

    @Test
    fun `crea edita y lista rutinas`() = runTest {
        val created = repository.createRoutine("  Día de empuje ", "Pecho, hombros y tríceps")
        assertEquals("Día de empuje", created.name)
        assertEquals("Pecho, hombros y tríceps", created.description)

        repository.updateRoutine(created.id, "Empuje A", null)

        val routines = repository.routines()
        assertEquals(1, routines.size)
        assertEquals("Empuje A", routines.first().name)
        assertNull(routines.first().description)
    }

    @Test
    fun `rechaza nombres vacios`() = runTest {
        val blank = runCatching { repository.createRoutine("   ", null) }.exceptionOrNull()
        assertEquals(RoutineErrorCode.INVALID_INPUT, (blank as RoutineException).code)

        val routine = repository.createRoutine("Valida", null)
        val emptyUpdate = runCatching { repository.updateRoutine(routine.id, "", null) }.exceptionOrNull()
        assertEquals(RoutineErrorCode.INVALID_INPUT, (emptyUpdate as RoutineException).code)
    }

    @Test
    fun `agrega ejercicios con posiciones consecutivas y objetivos`() = runTest {
        val routine = repository.createRoutine("Empuje", null)

        val first = repository.addExercise(routine.id, targets(exerciseA, 4, 8))
        val second = repository.addExercise(routine.id, targets(exerciseB, 3, 10))

        assertEquals(1, first.position)
        assertEquals(2, second.position)
        assertEquals(4, first.targetSets)
        assertEquals(8, first.targetReps)

        val loaded = repository.routine(routine.id)
        assertEquals(
            listOf("Press banca con barra", "Remo con barra"),
            loaded.exercises.map { it.exerciseName },
        )
    }

    @Test
    fun `rechaza ejercicios duplicados inexistentes y rutinas inexistentes`() = runTest {
        val routine = repository.createRoutine("Empuje", null)
        repository.addExercise(routine.id, targets(exerciseA))

        val duplicate = runCatching { repository.addExercise(routine.id, targets(exerciseA)) }.exceptionOrNull()
        assertEquals(RoutineErrorCode.EXERCISE_ALREADY_IN_ROUTINE, (duplicate as RoutineException).code)

        val missingExercise = runCatching {
            repository.addExercise(routine.id, targets("01ARYZ6S41TSV4RRFFQ69G5FAV"))
        }.exceptionOrNull()
        assertEquals(RoutineErrorCode.EXERCISE_NOT_FOUND, (missingExercise as RoutineException).code)

        val missingRoutine = runCatching {
            repository.addExercise("01ARYZ6S41TSV4RRFFQ69G5FAV", targets(exerciseB))
        }.exceptionOrNull()
        assertEquals(RoutineErrorCode.ROUTINE_NOT_FOUND, (missingRoutine as RoutineException).code)
    }

    @Test
    fun `reordena ejercicios y renumera posiciones`() = runTest {
        val routine = repository.createRoutine("Full body", null)
        val a = repository.addExercise(routine.id, targets(exerciseA))
        val b = repository.addExercise(routine.id, targets(exerciseB))
        val c = repository.addExercise(routine.id, targets(exerciseC))

        repository.moveExercise(c.id, RoutinesRepository.MoveDirection.UP)
        var loaded = repository.routine(routine.id)
        assertEquals(listOf(a.id, c.id, b.id), loaded.exercises.map { it.id })
        assertEquals(listOf(1, 2, 3), loaded.exercises.map { it.position })

        repository.moveExercise(c.id, RoutinesRepository.MoveDirection.UP)
        loaded = repository.routine(routine.id)
        assertEquals(listOf(c.id, a.id, b.id), loaded.exercises.map { it.id })

        repository.moveExercise(a.id, RoutinesRepository.MoveDirection.DOWN)
        loaded = repository.routine(routine.id)
        assertEquals(listOf(c.id, b.id, a.id), loaded.exercises.map { it.id })
        assertEquals(listOf(1, 2, 3), loaded.exercises.map { it.position })
    }

    @Test
    fun `rechaza movimientos fuera de rango`() = runTest {
        val routine = repository.createRoutine("Full body", null)
        val a = repository.addExercise(routine.id, targets(exerciseA))
        repository.addExercise(routine.id, targets(exerciseB))

        val error = runCatching {
            repository.moveExercise(a.id, RoutinesRepository.MoveDirection.UP)
        }.exceptionOrNull()
        assertEquals(RoutineErrorCode.INVALID_MOVE, (error as RoutineException).code)

        val loaded = repository.routine(routine.id)
        assertEquals(listOf(1, 2), loaded.exercises.map { it.position })
    }

    @Test
    fun `quita un ejercicio y renumera los restantes`() = runTest {
        val routine = repository.createRoutine("Full body", null)
        val a = repository.addExercise(routine.id, targets(exerciseA))
        val b = repository.addExercise(routine.id, targets(exerciseB))
        val c = repository.addExercise(routine.id, targets(exerciseC))

        repository.removeExercise(b.id)

        val loaded = repository.routine(routine.id)
        assertEquals(listOf(a.id, c.id), loaded.exercises.map { it.id })
        assertEquals(listOf(1, 2), loaded.exercises.map { it.position })
    }

    @Test
    fun `elimina la rutina con borrado logico sin tocar el historial`() = runTest {
        val routine = repository.createRoutine("Pierna", null)
        repository.addExercise(routine.id, targets(exerciseC))

        val session = workoutRepository.startSession(routine.id)
        repository.deleteRoutine(routine.id)

        assertTrue(repository.routines().isEmpty())

        val history = workoutRepository.sessions()
        assertEquals(1, history.size)
        assertEquals(session.id, history.first().id)

        val detail = workoutRepository.sessionDetail(session.id)
        assertEquals(routine.id, detail.session.routineId)
        assertNull(detail.session.routineName)
    }

    @Test
    fun `inicia una sesion desde la rutina y muestra el nombre en el detalle`() = runTest {
        val routine = repository.createRoutine("Día de empuje", null)
        repository.addExercise(routine.id, targets(exerciseA))

        val session = workoutRepository.startSession(routine.id)
        assertEquals(routine.id, session.routineId)
        assertEquals("Día de empuje", session.routineName)

        val detail = workoutRepository.sessionDetail(session.id)
        assertEquals("Día de empuje", detail.session.routineName)
    }

    @Test
    fun `rechaza iniciar una sesion con rutina inexistente`() = runTest {
        val error = runCatching {
            workoutRepository.startSession("01ARYZ6S41TSV4RRFFQ69G5FAV")
        }.exceptionOrNull()

        assertEquals(
            com.fitlog.app.data.WorkoutErrorCode.ROUTINE_NOT_FOUND,
            (error as com.fitlog.app.data.WorkoutException).code,
        )
        assertTrue(workoutRepository.sessions().isEmpty())
    }
}
