package com.fitlog.app

import android.database.Cursor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fitlog.app.data.FitLogDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchemaParityTest {

    private data class Column(
        val type: String,
        val notNull: Boolean,
        val primaryKey: Boolean,
    )

    @Test
    fun `las tablas creadas por Room coinciden con el esquema canonico`() {
        val database = openDatabase()
        try {
            val room = roomTables(database)
            val canonical = canonicalTables(TestVectors.loadText("schema.sql"))

            assertEquals(canonical.keys.sorted(), room.keys.sorted())
            for ((table, canonicalColumns) in canonical) {
                val roomColumns = room[table] ?: error("Room no creo la tabla $table")
                assertEquals("columnas de $table", canonicalColumns.keys.sorted(), roomColumns.keys.sorted())
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `tipo, nulabilidad y clave primaria coinciden con el esquema canonico`() {
        val database = openDatabase()
        try {
            val room = roomTables(database)
            val canonical = canonicalTables(TestVectors.loadText("schema.sql"))

            for ((table, canonicalColumns) in canonical) {
                val roomColumns = room[table] ?: error("Room no creo la tabla $table")
                for ((column, expected) in canonicalColumns) {
                    val actual = roomColumns[column] ?: error("Room no creo $table.$column")
                    assertEquals("tipo de $table.$column", expected.type, actual.type)
                    assertEquals("nulabilidad de $table.$column", expected.notNull, actual.notNull)
                    assertEquals("PK de $table.$column", expected.primaryKey, actual.primaryKey)
                }
            }
        } finally {
            database.close()
        }
    }

    private fun openDatabase(): FitLogDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FitLogDatabase::class.java,
        ).build()

    private fun roomTables(database: FitLogDatabase): Map<String, Map<String, Column>> {
        val sqlite = database.openHelper.readableDatabase
        val tableNames = mutableListOf<String>()

        sqlite.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' AND name <> 'room_master_table' ORDER BY name"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
        }

        return tableNames.associateWith { tableName ->
            val columns = mutableMapOf<String, Column>()
            sqlite.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.columnIndexOrThrow("name")
                val typeIndex = cursor.columnIndexOrThrow("type")
                val notNullIndex = cursor.columnIndexOrThrow("notnull")
                val pkIndex = cursor.columnIndexOrThrow("pk")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    columns[name] = Column(
                        type = cursor.getString(typeIndex).uppercase(),
                        notNull = cursor.getInt(notNullIndex) == 1,
                        primaryKey = cursor.getInt(pkIndex) > 0,
                    )
                }
            }
            columns
        }
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

    private fun Cursor.columnIndexOrThrow(name: String): Int {
        val index = getColumnIndex(name)
        check(index >= 0) { "columna $name no encontrada en el cursor" }
        return index
    }

    private companion object {
        val TABLE_REGEX = Regex("""CREATE TABLE (?:IF NOT EXISTS )?(\w+) \((.*?)\n\);""", RegexOption.DOT_MATCHES_ALL)
        val COLUMN_REGEX = Regex("""^(\w+)\s+(TEXT|INTEGER|REAL)\b(.*)$""", RegexOption.IGNORE_CASE)
    }
}
