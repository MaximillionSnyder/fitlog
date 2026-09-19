package com.fitlog.app

import com.fitlog.app.domain.Backup
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupDomainTest {

    private val vectors = TestVectors.load("backup-merge.json").jsonObject

    private fun toValue(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonPrimitive -> if (element.isString) element.content else element.doubleOrNull ?: element.content
        else -> error("valor no soportado")
    }

    private fun toRow(element: JsonElement): Backup.Row {
        val obj = element.jsonObject
        return Backup.Row(obj.mapValues { (_, value) -> toValue(value) })
    }

    private fun toSnapshot(element: JsonElement): Backup.Snapshot {
        val snapshot = Backup.emptySnapshot()
        for ((table, rows) in element.jsonObject) {
            snapshot.tables[table] = rows.jsonArray.map { toRow(it) }.toMutableList()
        }
        return snapshot
    }

    private fun toFile(element: JsonElement): Backup.File {
        val obj = element.jsonObject
        val sections = mutableMapOf<String, List<Backup.Row>>()
        (obj["sections"] as? JsonObject)?.forEach { (table, rows) ->
            sections[table] = rows.jsonArray.map { toRow(it) }
        }
        return Backup.File(
            format = obj["format"]!!.jsonPrimitive.content,
            formatVersion = obj["format_version"]!!.jsonPrimitive.doubleOrNull?.toInt() ?: 0,
            exportedAtMs = obj["exported_at_ms"]?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L,
            appVersion = obj["app_version"]?.jsonPrimitive?.content ?: "",
            sections = sections,
        )
    }

    @Test
    fun `la fusion coincide con los vectores compartidos`() {
        for (caseElement in vectors["cases"]!!.jsonArray) {
            val testCase = caseElement.jsonObject
            val name = testCase["name"]!!.jsonPrimitive.content
            val local = toSnapshot(testCase["local"]!!)
            val file = toFile(testCase["backup"]!!)

            val result = Backup.merge(local, file)
            val expected = testCase["expected"]!!.jsonObject

            for ((table, summaryElement) in expected["summary"]!!.jsonObject) {
                val expectedSummary = summaryElement.jsonObject
                val actual = result.summary[table] ?: error("falta el resumen de $table")
                assertEquals(
                    "$name · $table · insertadas",
                    expectedSummary["inserted"]!!.jsonPrimitive.doubleOrNull?.toInt(),
                    actual.inserted,
                )
                assertEquals(
                    "$name · $table · actualizadas",
                    expectedSummary["updated"]!!.jsonPrimitive.doubleOrNull?.toInt(),
                    actual.updated,
                )
                assertEquals(
                    "$name · $table · ignoradas",
                    expectedSummary["ignored"]!!.jsonPrimitive.doubleOrNull?.toInt(),
                    actual.ignored,
                )
                assertEquals(
                    "$name · $table · remapeadas",
                    expectedSummary["remapped"]!!.jsonPrimitive.doubleOrNull?.toInt(),
                    actual.remapped,
                )
            }

            for ((table, rowsElement) in expected["snapshot"]!!.jsonObject) {
                val expectedRows = rowsElement.jsonArray.map { toRow(it) }
                val actualRows = result.snapshot.rows(table)
                assertEquals("$name · $table · filas", expectedRows.size, actualRows.size)

                expectedRows.forEachIndexed { index, expectedRow ->
                    val actualRow = actualRows[index]
                    assertEquals(
                        "$name · $table · columnas de la fila $index",
                        expectedRow.values.keys.sorted(),
                        actualRow.values.keys.sorted(),
                    )
                    for ((column, expectedValue) in expectedRow.values) {
                        val actualValue = actualRow.values[column]
                        when (expectedValue) {
                            null -> assertNull("$name · $table.$column", actualValue)
                            is String -> assertEquals("$name · $table.$column", expectedValue, actualValue)
                            is Double -> assertEquals(
                                "$name · $table.$column",
                                expectedValue,
                                (actualValue as? Double) ?: Double.NaN,
                                1e-9,
                            )
                            else -> error("valor esperado no soportado en $table.$column")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `los archivos invalidos fallan con el codigo esperado`() {
        for (caseElement in vectors["invalid_cases"]!!.jsonArray) {
            val testCase = caseElement.jsonObject
            val name = testCase["name"]!!.jsonPrimitive.content
            val local = toSnapshot(testCase["local"]!!)
            val json = testCase["json"]!!.jsonPrimitive.content
            val expected = testCase["expected_error"]!!.jsonPrimitive.content

            var code = "sin-error"
            try {
                Backup.merge(local, Backup.parse(json))
            } catch (error: Backup.BackupException) {
                code = error.code.name.lowercase()
            }
            assertEquals(name, expected, code)
        }
    }

    @Test
    fun `la exportacion incluye solo las secciones pedidas y los propios referenciados`() {
        val snapshot = Backup.emptySnapshot()
        snapshot.tables["exercise"] = mutableListOf(
            Backup.Row(
                mapOf(
                    "id" to "ex-custom",
                    "slug" to "remo-maquina",
                    "name" to "Remo en máquina",
                    "muscle_group_id" to "mg-espalda",
                    "is_custom" to 1.0,
                    "updated_at" to 10.0,
                    "deleted_at" to null,
                )
            ),
            Backup.Row(
                mapOf(
                    "id" to "ex-base",
                    "slug" to "press-banca-barra",
                    "name" to "Press banca con barra",
                    "muscle_group_id" to "mg-pecho",
                    "is_custom" to 0.0,
                    "updated_at" to 10.0,
                    "deleted_at" to null,
                )
            ),
        )
        snapshot.tables["routine"] = mutableListOf(
            Backup.Row(mapOf("id" to "r1", "name" to "Empuje", "updated_at" to 10.0, "deleted_at" to null))
        )
        snapshot.tables["routine_exercise"] = mutableListOf(
            Backup.Row(
                mapOf(
                    "id" to "re1",
                    "routine_id" to "r1",
                    "exercise_id" to "ex-custom",
                    "position" to 1.0,
                    "updated_at" to 10.0,
                    "deleted_at" to null,
                )
            )
        )

        val json = Backup.serialize(snapshot, listOf("routine", "routine_exercise"), "0.1.7", 5000)
        val file = Backup.parse(json)

        assertNotNull(file.sections["routine"])
        assertNull(file.sections["body_metric"])
        assertEquals(listOf("ex-custom"), file.sections["exercise"]?.map { it.values["id"] })
        assertEquals(1, file.sections["routine_exercise"]?.size)
    }

    @Test
    fun `la serializacion y el parseo son reversibles`() {
        val snapshot = Backup.emptySnapshot()
        snapshot.tables["app_setting"] = mutableListOf(
            Backup.Row(mapOf("key" to "tema", "value" to "oscuro", "updated_at" to 10.0))
        )

        val json = Backup.serialize(snapshot, listOf("app_setting"), "0.1.7", 5000)
        val file = Backup.parse(json)

        assertEquals(Backup.FORMAT, file.format)
        assertEquals(Backup.FORMAT_VERSION, file.formatVersion)
        assertEquals(5000L, file.exportedAtMs)
        assertEquals("oscuro", file.sections["app_setting"]?.first()?.values?.get("value"))
        assertEquals(10.0, file.sections["app_setting"]?.first()?.values?.get("updated_at"))
    }
}
