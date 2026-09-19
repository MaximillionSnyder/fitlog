package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.AddSetInput
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.ProgressRepository
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.domain.Progress
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
class ProgressRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var progress: ProgressRepository
    private lateinit var workout: WorkoutRepository
    private lateinit var pressBanca: String
    private lateinit var remoBarra: String
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

        workout = WorkoutRepository(
            dao = database.workoutDao(),
            routinesDao = database.routinesDao(),
            now = { currentTime },
        )
        progress = ProgressRepository(dao = database.progressDao())

        val exercises = catalog.loadCatalog().exercises
        pressBanca = exercises.first { it.slug == "press-banca-barra" }.id
        remoBarra = exercises.first { it.slug == "remo-barra" }.id
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun registerSession(
        startedAt: Long,
        sets: List<AddSetInput>,
    ): String {
        currentTime = startedAt
        val session = workout.startSession(null)
        for (set in sets) {
            workout.addSet(set.copy(sessionId = session.id))
        }
        workout.finishSession(session.id)
        return session.id
    }

    private fun set(
        exerciseId: String,
        weightKg: Double?,
        reps: Int?,
        isWarmup: Boolean = false,
    ) = AddSetInput(
        sessionId = "",
        exerciseId = exerciseId,
        weightKg = weightKg,
        reps = reps,
        rir = null,
        notes = null,
        isWarmup = isWarmup,
    )

    @Test
    fun `construye un punto por sesion con las metricas del ejercicio`() = runTest {
        val older = registerSession(
            startedAt = 1_000,
            sets = listOf(
                set(pressBanca, 100.0, 8),
                set(pressBanca, 40.0, 10, isWarmup = true),
                set(remoBarra, 60.0, 10),
            ),
        )
        val newer = registerSession(startedAt = 2_000, sets = listOf(set(pressBanca, 105.0, 5)))

        val points = progress.series(pressBanca, null)

        assertEquals(listOf(older, newer), points.map { it.sessionId })
        assertEquals(1, points[0].workingSets)
        assertEquals(100.0, points[0].maxWeightKg, 1e-9)
        assertEquals(800.0, points[0].volumeKg, 1e-9)
        assertEquals(126.7, points[0].bestOneRepMaxKg, 1e-9)
        assertEquals(105.0, points[1].maxWeightKg, 1e-9)
        assertEquals(122.5, points[1].bestOneRepMaxKg, 1e-9)
    }

    @Test
    fun `excluye series eliminadas`() = runTest {
        val session = workout.startSession(null)
        val kept = workout.addSet(set(pressBanca, 100.0, 8).copy(sessionId = session.id))
        val removed = workout.addSet(set(pressBanca, 120.0, 1).copy(sessionId = session.id))

        var points = progress.series(pressBanca, null)
        assertEquals(120.0, points.first().maxWeightKg, 1e-9)

        workout.deleteSet(removed.id)

        points = progress.series(pressBanca, null)
        assertEquals(100.0, points.first().maxWeightKg, 1e-9)
        assertEquals(1, points.first().workingSets)
        assertEquals(1, kept.setIndex)
    }

    @Test
    fun `filtra por rango temporal`() = runTest {
        registerSession(startedAt = 1_000, sets = listOf(set(pressBanca, 100.0, 8)))
        val recent = registerSession(startedAt = 2_000, sets = listOf(set(pressBanca, 105.0, 5)))

        val filtered = progress.series(pressBanca, Progress.Range(fromMs = 1_500, toMs = 3_000))
        assertEquals(listOf(recent), filtered.map { it.sessionId })

        val all = progress.series(pressBanca, Progress.rangeFor(Progress.RangePreset.ALL, 10_000))
        assertEquals(2, all.size)
    }

    @Test
    fun `devuelve lista vacia para un ejercicio sin series`() = runTest {
        registerSession(startedAt = 1_000, sets = listOf(set(pressBanca, 100.0, 8)))

        assertTrue(progress.series(remoBarra, null).isEmpty())
    }
}
