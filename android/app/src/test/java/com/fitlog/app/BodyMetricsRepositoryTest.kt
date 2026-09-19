package com.fitlog.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.BodyMetricErrorCode
import com.fitlog.app.data.BodyMetricException
import com.fitlog.app.data.BodyMetricInput
import com.fitlog.app.data.BodyMetricUpdate
import com.fitlog.app.data.BodyMetricsRepository
import com.fitlog.app.data.FitLogDatabase
import com.fitlog.app.domain.Body
import com.fitlog.app.domain.Progress
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
class BodyMetricsRepositoryTest {

    private lateinit var database: FitLogDatabase
    private lateinit var repository: BodyMetricsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).allowMainThreadQueries().build()

        repository = BodyMetricsRepository(dao = database.bodyMetricsDao(), now = { 1_700_000_000_000 })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `registra una medida con unidad derivada`() = runTest {
        val created = repository.create(
            BodyMetricInput(kind = "body_weight", value = 78.5, measuredAtMs = 1_000, notes = "  en ayunas ")
        )

        assertEquals(Body.Unit.KG, created.unit)
        assertEquals(78.5, created.value, 1e-9)
        assertEquals("en ayunas", created.notes)
        assertEquals(1, repository.metrics().size)
    }

    @Test
    fun `rechaza valores invalidos y tipos desconocidos`() = runTest {
        val zero = runCatching {
            repository.create(BodyMetricInput("body_weight", 0.0, 1_000, null))
        }.exceptionOrNull()
        assertEquals(BodyMetricErrorCode.INVALID_INPUT, (zero as BodyMetricException).code)

        val fat = runCatching {
            repository.create(BodyMetricInput("body_fat", 120.0, 1_000, null))
        }.exceptionOrNull()
        assertEquals(BodyMetricErrorCode.INVALID_INPUT, (fat as BodyMetricException).code)

        val unknown = runCatching {
            repository.create(BodyMetricInput("altura", 180.0, 1_000, null))
        }.exceptionOrNull()
        assertEquals(BodyMetricErrorCode.UNKNOWN_KIND, (unknown as BodyMetricException).code)

        assertTrue(repository.metrics().isEmpty())
    }

    @Test
    fun `edita y elimina con borrado logico`() = runTest {
        val created = repository.create(BodyMetricInput("waist", 84.0, 1_000, null))

        repository.update(created.id, BodyMetricUpdate(83.5, 2_000, "mejor"))

        var metrics = repository.metrics()
        assertEquals(83.5, metrics.first().value, 1e-9)
        assertEquals(2_000L, metrics.first().measuredAtMs)
        assertEquals("mejor", metrics.first().notes)

        repository.delete(created.id)
        metrics = repository.metrics()
        assertTrue(metrics.isEmpty())
    }

    @Test
    fun `valida contra el tipo guardado al editar`() = runTest {
        val created = repository.create(BodyMetricInput("body_fat", 18.0, 1_000, null))

        val error = runCatching {
            repository.update(created.id, BodyMetricUpdate(150.0, 1_000, null))
        }.exceptionOrNull()

        assertEquals(BodyMetricErrorCode.INVALID_INPUT, (error as BodyMetricException).code)
    }

    @Test
    fun `calcula series y estadisticas del periodo`() = runTest {
        val day = 86_400_000L
        val now = 1_700_000_000_000L

        repository.create(BodyMetricInput("body_weight", 80.0, now - 40 * day, null))
        repository.create(BodyMetricInput("body_weight", 79.0, now - 20 * day, null))
        repository.create(BodyMetricInput("body_weight", 78.0, now - 2 * day, null))
        repository.create(BodyMetricInput("waist", 84.0, now - 2 * day, null))

        val series = Body.series(
            repository.metrics(),
            Body.Kind.BODY_WEIGHT,
            Progress.Range(fromMs = now - 90 * day, toMs = now),
        )
        val stats = Body.stats(series)

        assertEquals(listOf(80.0, 79.0, 78.0), series.map { it.value })
        assertEquals(3, stats.count)
        assertEquals(-2.0, stats.deltaAbs ?: 0.0, 1e-9)
        assertEquals(-2.5, stats.deltaPct ?: 0.0, 1e-9)
    }

    @Test
    fun `sin medidas las estadisticas son nulas`() = runTest {
        val stats = Body.stats(Body.series(repository.metrics(), Body.Kind.BODY_WEIGHT, null))

        assertEquals(0, stats.count)
        assertNull(stats.latest)
        assertNull(stats.deltaPct)
    }
}
