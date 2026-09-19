package com.fitlog.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.AddSetInput
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.ComparisonsRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.domain.Comparisons
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
class ComparisonsRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var comparisons: ComparisonsRepository
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
        comparisons = ComparisonsRepository(dao = database.comparisonsDao())

        val exercises = catalog.loadCatalog().exercises
        pressBanca = exercises.first { it.slug == "press-banca-barra" }.id
        remoBarra = exercises.first { it.slug == "remo-barra" }.id
    }

    @After
    fun tearDown() {
        database.close()
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

    private suspend fun registerSession(startedAt: Long, sets: List<AddSetInput>): String {
        currentTime = startedAt
        val session = workout.startSession(null)
        for (entry in sets) {
            workout.addSet(entry.copy(sessionId = session.id))
        }
        workout.finishSession(session.id)
        return session.id
    }

    @Test
    fun `calcula marcas personales por ejercicio`() = runTest {
        registerSession(
            startedAt = 1_000,
            sets = listOf(
                set(pressBanca, 100.0, 8),
                set(pressBanca, 40.0, 10, isWarmup = true),
                set(remoBarra, 60.0, 10),
            ),
        )
        registerSession(startedAt = 2_000, sets = listOf(set(pressBanca, 105.0, 5)))

        val records = Comparisons.personalRecords(comparisons.sets())

        assertEquals(listOf(pressBanca, remoBarra), records.map { it.exerciseId })
        val press = records.first()
        assertEquals(105.0, press.bestWeightKg, 1e-9)
        assertEquals(2_000L, press.bestWeightAtMs)
        assertEquals(126.7, press.bestOneRepMaxKg, 1e-9)
        assertEquals(1_000L, press.bestOneRepMaxAtMs)
        assertEquals(800.0, press.bestSessionVolumeKg, 1e-9)
        assertEquals(8, press.bestReps)
    }

    @Test
    fun `excluye series eliminadas de las marcas`() = runTest {
        currentTime = 1_000
        val session = workout.startSession(null)
        workout.addSet(set(pressBanca, 100.0, 8).copy(sessionId = session.id))
        val removed = workout.addSet(set(pressBanca, 200.0, 1).copy(sessionId = session.id))

        var records = Comparisons.personalRecords(comparisons.sets())
        assertEquals(200.0, records.first().bestWeightKg, 1e-9)

        workout.deleteSet(removed.id)

        records = Comparisons.personalRecords(comparisons.sets())
        assertEquals(100.0, records.first().bestWeightKg, 1e-9)
    }

    @Test
    fun `compara periodos contiguos`() = runTest {
        val day = 86_400_000L
        val now = 1_700_000_000_000L
        registerSession(startedAt = now - 40 * day, sets = listOf(set(pressBanca, 100.0, 8)))
        registerSession(startedAt = now - 10 * day, sets = listOf(set(pressBanca, 100.0, 10)))

        val ranges = Comparisons.comparisonRanges(Comparisons.Preset.LAST_30_DAYS, now)
        val comparison = Comparisons.comparePeriods(comparisons.sets(), ranges.current, ranges.previous)

        assertEquals(1000.0, comparison.current.volumeKg, 1e-9)
        assertEquals(1, comparison.current.sessions)
        assertEquals(800.0, comparison.previous.volumeKg, 1e-9)
        assertEquals(25.0, comparison.volumeDeltaPct ?: 0.0, 1e-9)
    }

    @Test
    fun `calcula el balance muscular por grupo principal`() = runTest {
        registerSession(
            startedAt = 1_000,
            sets = listOf(
                set(pressBanca, 100.0, 8),
                set(remoBarra, 60.0, 10),
                set(remoBarra, 20.0, 10, isWarmup = true),
            ),
        )

        val balance = Comparisons.muscleBalance(comparisons.sets(), comparisons.groups())

        assertEquals(listOf("pecho", "espalda"), balance.map { it.muscleGroupSlug })
        assertEquals(800.0, balance[0].volumeKg, 1e-9)
        assertEquals(600.0, balance[1].volumeKg, 1e-9)
        assertEquals(57.1, balance[0].sharePct, 1e-9)
        assertEquals(42.9, balance[1].sharePct, 1e-9)
    }

    @Test
    fun `sin series no hay marcas ni balance`() = runTest {
        val records = Comparisons.personalRecords(comparisons.sets())
        val balance = Comparisons.muscleBalance(comparisons.sets(), comparisons.groups())

        assertTrue(records.isEmpty())
        assertTrue(balance.isEmpty())
    }
}
