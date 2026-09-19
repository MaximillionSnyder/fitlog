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
import com.fitlog.app.domain.Insights
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
class InsightsRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var comparisons: ComparisonsRepository
    private lateinit var workout: WorkoutRepository
    private lateinit var pressBanca: String
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

        pressBanca = catalog.loadCatalog().exercises.first { it.slug == "press-banca-barra" }.id
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun registerSession(startedAt: Long, weightKg: Double, reps: Int): String {
        currentTime = startedAt
        val session = workout.startSession(null)
        workout.addSet(
            AddSetInput(
                sessionId = session.id,
                exerciseId = pressBanca,
                weightKg = weightKg,
                reps = reps,
                rir = null,
                notes = null,
                isWarmup = false,
            )
        )
        workout.finishSession(session.id)
        return session.id
    }

    private suspend fun buildTips(nowMs: Long): List<Insights.Tip> {
        val range = Comparisons.comparisonRanges(Comparisons.Preset.LAST_30_DAYS, nowMs).current
        return Insights.build(
            Insights.Input(
                sets = comparisons.sets(),
                groupByExercise = comparisons.groups(),
                range = range,
                periodDays = 30,
            )
        )
    }

    @Test
    fun `detecta progreso con datos reales`() = runTest {
        val day = 86_400_000L
        val now = 1_700_000_000_000L
        registerSession(now - 20 * day, 100.0, 8)
        registerSession(now - 10 * day, 102.5, 8)
        registerSession(now - 2 * day, 105.0, 8)

        val tips = buildTips(now)

        val progress = tips.firstOrNull { it.kind == Insights.Kind.PROGRESS }
        assertEquals(pressBanca, progress?.subject)
        assertEquals(5.0, progress?.value ?: 0.0, 1e-9)
        assertEquals("Tu 1RM estimado subió 5% en 3 sesiones", progress?.message)
        assertTrue(tips.any { it.kind == Insights.Kind.CONSISTENCY })
    }

    @Test
    fun `detecta estancamiento con datos reales`() = runTest {
        val day = 86_400_000L
        val now = 1_700_000_000_000L
        registerSession(now - 25 * day, 100.0, 8)
        registerSession(now - 18 * day, 100.0, 8)
        registerSession(now - 10 * day, 100.0, 8)
        registerSession(now - 3 * day, 100.0, 8)

        val tips = buildTips(now)

        val stagnation = tips.firstOrNull { it.kind == Insights.Kind.STAGNATION }
        assertEquals(pressBanca, stagnation?.subject)
        assertEquals("Sin progreso de 1RM en las últimas 4 sesiones", stagnation?.message)
    }

    @Test
    fun `sin series no hay consejos`() = runTest {
        assertTrue(buildTips(1_700_000_000_000L).isEmpty())
    }
}
