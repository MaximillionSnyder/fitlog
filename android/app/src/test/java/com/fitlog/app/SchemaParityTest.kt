package com.fitlog.app

import java.io.File
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchemaParityTest {

    private data class Column(
        val type: String,
        val notNull: Boolean,
        val primaryKey: Boolean,
    )

    @Test
    fun `las entidades Room declaran las mismas tablas que el esquema canonico`() {
        val canonical = canonicalTables(TestVectors.loadText("schema.sql"))
        val room = roomTables()

        assertEquals(canonical.keys.sorted(), room.keys.sorted())
    }

    @Test
    fun `las entidades Room declaran las mismas columnas que el esquema canonico`() {
        val canonical = canonicalTables(TestVectors.loadText("schema.sql"))
        val room = roomTables()

        for ((table, canonicalColumns) in canonical) {
            val roomColumns = room[table] ?: error("Room no declara la tabla $table")
            assertEquals("columnas de $table", canonicalColumns.keys.sorted(), roomColumns.keys.sorted())
        }
    }

    @Test
    fun `tipo, nulabilidad y clave primaria coinciden con el esquema canonico`() {
        val canonical = canonicalTables(TestVectors.loadText("schema.sql"))
        val room = roomTables()

        for ((table, canonicalColumns) in canonical) {
            val roomColumns = room[table] ?: error("Room no declara la tabla $table")
            for ((column, expected) in canonicalColumns) {
                val actual = roomColumns[column] ?: error("Room no declara $table.$column")
                assertEquals("tipo de $table.$column", expected.type, actual.type)
                assertEquals("nulabilidad de $table.$column", expected.notNull, actual.notNull)
                assertEquals("PK de $table.$column", expected.primaryKey, actual.primaryKey)
            }
        }
    }

    private fun roomTables(): Map<String, Map<String, Column>> {
        val schemaFile = listOf(
            File("schemas/com.fitlog.app.data.FitLogDatabase/1.json"),
            File("app/schemas/com.fitlog.app.data.FitLogDatabase/1.json"),
        ).firstOrNull { it.exists() }
            ?: error("No se encontro el JSON de esquema exportado por Room; revisa room.schemaLocation")

        val root = kotlinx.serialization.json.Json.parseToJsonElement(schemaFile.readText()).jsonObject
        val entities = root.getValue("database").jsonObject.getValue("entities").jsonArray

        val result = mutableMapOf<String, Map<String, Column>>()
        for (entity in entities) {
            val entityObject = entity.jsonObject
            val tableName = entityObject.getValue("tableName").jsonPrimitive.content
            val primaryKeys = entityObject.getValue("primaryKey").jsonObject
                .getValue("columnNames").jsonArray
                .map { it.jsonPrimitive.content }
                .toSet()

            val columns = entityObject.getValue("fields").jsonArray.associate { field ->
                val fieldObject = field.jsonObject
                val columnName = fieldObject.getValue("columnName").jsonPrimitive.content
                columnName to Column(
                    type = fieldObject.getValue("affinity").jsonPrimitive.content.uppercase(),
                    notNull = fieldObject["notNull"]?.jsonPrimitive?.booleanOrNull ?: false,
                    primaryKey = columnName in primaryKeys,
                )
            }
            result[tableName] = columns
        }

        assertTrue("Room no exporto entidades", result.isNotEmpty())
        return result
    }

    private fun canonicalTables(sql: String): Map<String, Map<String, Column>> {
        val result = mutableMapOf<String, Map<String, Column>>()
        for (match in TABLE_REGEX.findAll(sql)) {
            val tableName = match.groupValues[1]
            val body = match.groupValues[2]
            val columns = mutableMapOf<String, Column>()
            for (definition in splitTopLevel(body)) {
                val columnMatch = COLUMN_REGEX.find(definition.trim()) ?: continue
                val name = columnMatch.groupValues[1]
                val rest = columnMatch.groupValues[3].uppercase()
                columns[name] = Column(
                    type = columnMatch.groupValues[2].uppercase(),
                    notNull = "NOT NULL" in rest,
                    primaryKey = "PRIMARY KEY" in rest,
                )
            }
            result[tableName] = columns
        }
        return result
    }

    private fun splitTopLevel(body: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (char in body) {
            when (char) {
                '(' -> {
                    depth += 1
                    current.append(char)
                }
                ')' -> {
                    depth -= 1
                    current.append(char)
                }
                ',' -> if (depth == 0) {
                    parts.add(current.toString())
                    current.clear()
                } else {
                    current.append(char)
                }
                else -> current.append(char)
            }
        }
        if (current.isNotBlank()) parts.add(current.toString())
        return parts
    }

    private companion object {
        val TABLE_REGEX = Regex("""CREATE TABLE (?:IF NOT EXISTS )?(\w+) \((.*?)\n\);""", RegexOption.DOT_MATCHES_ALL)
        val COLUMN_REGEX = Regex("""^(\w+)\s+(TEXT|INTEGER|REAL)\b(.*)$""", RegexOption.IGNORE_CASE)
    }
}
