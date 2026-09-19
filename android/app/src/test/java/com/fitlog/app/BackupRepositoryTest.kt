package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.AddSetInput
import com.fitlog.app.data.BackupRepository
import com.fitlog.app.data.BodyMetricInput
import com.fitlog.app.data.BodyMetricsRepository
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.RoutineExerciseInput
import com.fitlog.app.data.RoutinesRepository
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.domain.Backup
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var backup: BackupRepository
    private lateinit var workout: WorkoutRepository
    private lateinit var routines: RoutinesRepository
    private lateinit var bodyMetrics: BodyMetricsRepository
    private lateinit var pressBanca: String
    private lateinit var groupId: String
    private var currentTime = 1_700_000_000_000L

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

        backup = BackupRepository(database = database, backupDao = database.backupDao(), now = { currentTime })
        workout = WorkoutRepository(
            dao = database.workoutDao(),
            routinesDao = database.routinesDao(),
            now = { currentTime },
        )
        routines = RoutinesRepository(dao = database.routinesDao(), now = { currentTime })
        bodyMetrics = BodyMetricsRepository(dao = database.bodyMetricsDao(), now = { currentTime })

        val loaded = catalog.loadCatalog()
        pressBanca = loaded.exercises.first { it.slug == "press-banca-barra" }.id
        groupId = loaded.groups.first().id
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun registerWorkout(): String {
        val session = workout.startSession(null)
        workout.addSet(
            AddSetInput(
                sessionId = session.id,
                exerciseId = pressBanca,
                weightKg = 100.0,
                reps = 8,
                rir = null,
                notes = null,
                isWarmup = false,
            )
        )
        workout.finishSession(session.id)
        return session.id
    }

    @Test
    fun `exporta e importa un respaldo completo`() = runTest {
        val routine = routines.createRoutine("Empuje", null)
        routines.addExercise(
            routine.id,
            RoutineExerciseInput(pressBanca, 4, 8, null, null, null),
        )
        bodyMetrics.create(BodyMetricInput("body_weight", 80.0, currentTime, null))
        registerWorkout()

        val json = backup.export(Backup.TABLES, "0.1.7")

        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val targetBackup = BackupRepository(
                database = target,
                backupDao = target.backupDao(),
                now = { currentTime },
            )
            val summary = targetBackup.import(json)

            assertEquals(1, summary.getValue("routine").inserted)
            assertEquals(1, summary.getValue("session").inserted)
            assertEquals(1, summary.getValue("set_entry").inserted)
            assertEquals(1, summary.getValue("body_metric").inserted)
        } finally {
            target.close()
        }
    }

    @Test
    fun `reimportar el mismo respaldo no duplica filas`() = runTest {
        routines.createRoutine("Empuje", null)
        registerWorkout()
        val json = backup.export(Backup.TABLES, "0.1.7")

        val summary = backup.import(json)

        assertEquals(0, summary.getValue("routine").inserted)
        assertEquals(1, summary.getValue("routine").ignored)
        assertEquals(1, summary.getValue("session").ignored)
    }

    @Test
    fun `aborta sin efectos cuando falta una referencia`() = runTest {
        routines.createRoutine("Empuje", null)

        val json = """
            {
              "format": "fitlog-backup",
              "format_version": 1,
              "exported_at_ms": 5000,
              "app_version": "0.1.7",
              "sections": {
                "set_entry": [
                  {
                    "id": "set-huerfano",
                    "session_id": "s-fantasma",
                    "exercise_id": "$pressBanca",
                    "set_index": 1,
                    "updated_at": 100,
                    "deleted_at": null
                  }
                ]
              }
            }
        """.trimIndent()

        var code = "sin-error"
        try {
            backup.import(json)
        } catch (error: Backup.BackupException) {
            code = error.code.name.lowercase()
        }

        assertEquals("missing_reference", code)
        assertTrue(backup.readSnapshot(listOf("set_entry")).rows("set_entry").isEmpty())
        assertEquals(1, routines.routines().size)
    }

    @Test
    fun `remapea ejercicios propios con el mismo slug`() = runTest {
        database.backupDao().upsertExercise(
            com.fitlog.app.data.ExerciseEntity(
                id = "ex-local",
                slug = "remo-maquina",
                name = "Remo en máquina",
                muscleGroupId = groupId,
                secondaryMuscleGroupId = null,
                equipment = "maquina",
                kind = "strength",
                isCustom = true,
                createdAt = 1,
                updatedAt = 500,
                deletedAt = null,
            )
        )

        val json = """
            {
              "format": "fitlog-backup",
              "format_version": 1,
              "exported_at_ms": 5000,
              "app_version": "0.1.7",
              "sections": {
                "exercise": [
                  {
                    "id": "ex-remoto",
                    "slug": "remo-maquina",
                    "name": "Remo en máquina",
                    "muscle_group_id": "$groupId",
                    "is_custom": 1,
                    "updated_at": 100,
                    "deleted_at": null
                  }
                ],
                "session": [
                  { "id": "s-remota", "started_at": 1000, "updated_at": 100, "deleted_at": null }
                ],
                "set_entry": [
                  {
                    "id": "set-remoto",
                    "session_id": "s-remota",
                    "exercise_id": "ex-remoto",
                    "set_index": 1,
                    "weight_kg": 50,
                    "reps": 10,
                    "updated_at": 100,
                    "deleted_at": null
                  }
                ]
              }
            }
        """.trimIndent()

        val summary = backup.import(json)

        assertEquals(1, summary.getValue("exercise").remapped)
        assertEquals(0, summary.getValue("exercise").inserted)

        val snapshot = backup.readSnapshot(listOf("exercise", "set_entry"))
        assertEquals(1, snapshot.rows("exercise").size)
        assertEquals("ex-local", snapshot.rows("set_entry").first().values["exercise_id"])
    }

    @Test
    fun `fusiona rutinas con ejercicios sin romper las posiciones`() = runTest {
        val sourceRoutine = routines.createRoutine("Full body", null)
        routines.addExercise(sourceRoutine.id, RoutineExerciseInput(pressBanca, 4, 8, null, null, null))
        val json = backup.export(Backup.TABLES, "0.1.7")

        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).allowMainThreadQueries().build()
        try {
            val targetRoutines = RoutinesRepository(dao = target.routinesDao(), now = { currentTime })
            val localRoutine = targetRoutines.createRoutine("Full body", null)
            targetRoutines.addExercise(localRoutine.id, RoutineExerciseInput(pressBanca, 3, 10, null, null, null))

            val targetBackup = BackupRepository(
                database = target,
                backupDao = target.backupDao(),
                now = { currentTime },
            )
            targetBackup.import(json)

            val loaded = targetRoutines.routines()
            assertEquals(2, loaded.size)
            loaded.forEach { routine ->
                assertEquals(
                    routine.exercises.indices.map { it + 1 },
                    routine.exercises.map { it.position },
                )
            }
        } finally {
            target.close()
        }
    }
}
