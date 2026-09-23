package com.fitlog.app

import com.fitlog.app.data.DatabaseMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La migracion de Android tiene que decir lo mismo que el esquema canonico.
 *
 * El esquema compartido es la fuente de verdad y la web lo aplica tal cual; si Android se separa,
 * la paridad se rompe en silencio hasta que alguien abre una base vieja.
 */
class DatabaseMigrationsTest {

    @Test
    fun `las sentencias de la migracion coinciden con el esquema canonico`() {
        val sql = TestVectors.loadText("schema.sql")
        val canonico = ALTER_REGEX.findAll(sql)
            .map { match -> match.value.trim().trimEnd(';') }
            .filter { it.contains("session", ignoreCase = true) }
            .toList()

        assertEquals(DatabaseMigrations.STATEMENTS_1_2, canonico)
    }

    @Test
    fun `la migracion agrega las columnas de actividad`() {
        val columnas = DatabaseMigrations.STATEMENTS_1_2.map { statement ->
            statement.substringAfter("ADD COLUMN ").substringBefore(' ')
        }

        assertTrue(columnas.containsAll(listOf("distance_m", "calories", "avg_heart_rate")))
        assertTrue(columnas.containsAll(listOf("max_heart_rate", "steps", "elevation_gain_m")))
        assertTrue(columnas.contains("source"))
        assertEquals(7, columnas.size)
    }

    private companion object {
        val ALTER_REGEX = Regex("ALTER TABLE \\w+ ADD COLUMN [^;]+", RegexOption.IGNORE_CASE)
    }
}
