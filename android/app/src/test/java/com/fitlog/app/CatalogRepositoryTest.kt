package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.CatalogErrorCode
import com.fitlog.app.data.CatalogException
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.CreateCustomExerciseInput
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.domain.ExerciseKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var repository: CatalogRepository

    private val seedJson: String by lazy {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.assets.open("seed/catalog.json").bufferedReader().use { it.readText() }
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = CatalogRepository(
            dao = database.catalogDao(),
            seedJsonProvider = { seedJson },
            now = { 1_700_000_000_000 },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `la primera siembra carga el catalogo base`() = runTest {
        val result = repository.ensureSeeded()

        assertTrue(result.seeded)
        assertEquals(12, result.muscleGroups)
        assertEquals(36, result.exercises)

        val snapshot = repository.loadCatalog()
        assertEquals(36, snapshot.exercises.size)
        assertTrue(snapshot.exercises.none { it.isCustom })
    }

    @Test
    fun `las siembras posteriores no duplican filas`() = runTest {
        repository.ensureSeeded()
        val second = repository.ensureSeeded()

        assertFalse(second.seeded)
        assertEquals(36, repository.loadCatalog().exercises.size)
    }

    @Test
    fun `respeta los ejercicios personalizados existentes`() = runTest {
        repository.ensureSeeded()
        repository.createCustomExercise(
            CreateCustomExerciseInput(
                name = "Remo en máquina",
                muscleGroupId = repository.loadCatalog().groups.first().id,
                equipment = "maquina",
                kind = ExerciseKind.STRENGTH,
            )
        )

        val freshRepository = CatalogRepository(
            dao = database.catalogDao(),
            seedJsonProvider = { seedJson },
        )
        freshRepository.ensureSeeded()

        val snapshot = freshRepository.loadCatalog()
        assertEquals(37, snapshot.exercises.size)
        assertEquals(1, snapshot.exercises.count { it.isCustom })
    }

    @Test
    fun `crea un ejercicio propio con slug derivado`() = runTest {
        val groups = repository.loadCatalog().groups
        val created = repository.createCustomExercise(
            CreateCustomExerciseInput(
                name = "  Remo en máquina ",
                muscleGroupId = groups.first().id,
                equipment = "maquina",
                kind = ExerciseKind.STRENGTH,
            )
        )

        assertEquals("remo-en-maquina", created.slug)
        assertEquals("Remo en máquina", created.name)
        assertTrue(created.isCustom)
    }

    @Test
    fun `rechaza nombres vacios y grupos inexistentes`() = runTest {
        val groupId = repository.loadCatalog().groups.first().id

        val blank = runCatching {
            repository.createCustomExercise(
                CreateCustomExerciseInput("   ", groupId, "banda", ExerciseKind.STRENGTH)
            )
        }.exceptionOrNull()
        assertEquals(CatalogErrorCode.INVALID_INPUT, (blank as CatalogException).code)

        val missingGroup = runCatching {
            repository.createCustomExercise(
                CreateCustomExerciseInput("Ejercicio raro", "01ARYZ6S41TSV4RRFFQ69G5FAV", "banda", ExerciseKind.STRENGTH)
            )
        }.exceptionOrNull()
        assertEquals(CatalogErrorCode.MUSCLE_GROUP_NOT_FOUND, (missingGroup as CatalogException).code)
    }

    @Test
    fun `rechaza slugs duplicados`() = runTest {
        val groups = repository.loadCatalog().groups
        val error = runCatching {
            repository.createCustomExercise(
                CreateCustomExerciseInput("Dominadas", groups.first().id, "peso-corporal", ExerciseKind.STRENGTH)
            )
        }.exceptionOrNull()

        assertEquals(CatalogErrorCode.DUPLICATE_SLUG, (error as CatalogException).code)
    }

    @Test
    fun `elimina con borrado logico solo ejercicios propios`() = runTest {
        val snapshot = repository.loadCatalog()
        val base = snapshot.exercises.first { !it.isCustom }

        val protected = runCatching { repository.deleteCustomExercise(base.id) }.exceptionOrNull()
        assertEquals(CatalogErrorCode.BASE_CATALOG_PROTECTED, (protected as CatalogException).code)

        val custom = repository.createCustomExercise(
            CreateCustomExerciseInput("Ejercicio temporal", snapshot.groups.first().id, "banda", ExerciseKind.STRENGTH)
        )
        repository.deleteCustomExercise(custom.id)

        val after = repository.loadCatalog()
        assertNull(after.exercises.firstOrNull { it.id == custom.id })
        assertEquals(36, after.exercises.size)
    }

    @Test
    fun `falla al eliminar un ejercicio inexistente`() = runTest {
        val error = runCatching {
            repository.deleteCustomExercise("01ARYZ6S41TSV4RRFFQ69G5FAV")
        }.exceptionOrNull()

        assertEquals(CatalogErrorCode.EXERCISE_NOT_FOUND, (error as CatalogException).code)
    }
}
