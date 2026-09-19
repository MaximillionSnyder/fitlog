package com.fitlog.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitlog.app.domain.Body
import com.fitlog.app.domain.Ulid

enum class BodyMetricErrorCode {
    INVALID_INPUT,
    UNKNOWN_KIND,
    NOT_FOUND,
}

class BodyMetricException(
    val code: BodyMetricErrorCode,
    message: String,
) : Exception(message)

data class BodyMetricInput(
    val kind: String,
    val value: Double,
    val measuredAtMs: Long,
    val notes: String?,
)

data class BodyMetricUpdate(
    val value: Double,
    val measuredAtMs: Long,
    val notes: String?,
)

@Dao
abstract class BodyMetricsDao {

    @Query("SELECT * FROM body_metric WHERE deleted_at IS NULL ORDER BY measured_at DESC")
    abstract suspend fun listMetrics(): List<BodyMetricEntity>

    @Query("SELECT * FROM body_metric WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    abstract suspend fun findMetricById(id: String): BodyMetricEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertMetric(metric: BodyMetricEntity)

    @Query("UPDATE body_metric SET value = :value, measured_at = :measuredAt, notes = :notes, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun updateMetric(
        id: String,
        value: Double,
        measuredAt: Long,
        notes: String?,
        updatedAt: Long,
    )

    @Query("UPDATE body_metric SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    abstract suspend fun softDeleteMetric(id: String, deletedAt: Long, updatedAt: Long)
}

class BodyMetricsRepository(
    private val dao: BodyMetricsDao,
    private val idGenerator: () -> String = { Ulid.generate() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun metrics(): List<Body.Point> = dao.listMetrics().map { it.toDomain() }

    suspend fun create(input: BodyMetricInput): Body.Point {
        val kind = validateKind(input.kind)
        validate(kind.wire, input.value)
        val timestamp = now()
        val entity = BodyMetricEntity(
            id = idGenerator(),
            measuredAt = input.measuredAtMs,
            kind = kind.wire,
            value = input.value,
            unit = Body.unitForKind(kind).wire,
            notes = input.notes?.trim()?.ifEmpty { null },
            createdAt = timestamp,
            updatedAt = timestamp,
            deletedAt = null,
        )
        dao.insertMetric(entity)
        return entity.toDomain()
    }

    suspend fun update(id: String, input: BodyMetricUpdate) {
        val existing = dao.findMetricById(id)
            ?: throw BodyMetricException(BodyMetricErrorCode.NOT_FOUND, "La medida no existe")
        validate(existing.kind, input.value)

        dao.updateMetric(
            id = id,
            value = input.value,
            measuredAt = input.measuredAtMs,
            notes = input.notes?.trim()?.ifEmpty { null },
            updatedAt = now(),
        )
    }

    suspend fun delete(id: String) {
        if (dao.findMetricById(id) == null) {
            throw BodyMetricException(BodyMetricErrorCode.NOT_FOUND, "La medida no existe")
        }
        val timestamp = now()
        dao.softDeleteMetric(id = id, deletedAt = timestamp, updatedAt = timestamp)
    }

    private fun validateKind(kindWire: String): Body.Kind {
        val kind = Body.Kind.fromWire(kindWire)
            ?: throw BodyMetricException(
                BodyMetricErrorCode.UNKNOWN_KIND,
                "Tipo de medida desconocido: $kindWire",
            )
        return kind
    }

    private fun validate(kindWire: String, value: Double) {
        val error = Body.validate(kindWire, value) ?: return
        throw BodyMetricException(BodyMetricErrorCode.INVALID_INPUT, error)
    }

    private fun BodyMetricEntity.toDomain(): Body.Point = Body.Point(
        id = id,
        kind = Body.Kind.fromWire(kind)
            ?: throw IllegalStateException("Tipo de medida desconocido en la base: $kind"),
        measuredAtMs = measuredAt,
        value = value,
        unit = Body.Unit.fromWire(unit)
            ?: throw IllegalStateException("Unidad desconocida en la base: $unit"),
        notes = notes,
    )
}
