package com.fitlog.app

import com.fitlog.app.domain.Ulid
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UlidTest {

    @Test
    fun `acepta los ULID validos de los vectores`() {
        val vectors = TestVectors.load("ulid.json").jsonObject.getValue("valid").jsonArray
        for (case in vectors) {
            val value = case.jsonPrimitive.content
            assertTrue("deberia ser valido: $value", Ulid.isValid(value))
        }
    }

    @Test
    fun `rechaza los ULID invalidos de los vectores`() {
        val vectors = TestVectors.load("ulid.json").jsonObject.getValue("invalid").jsonArray
        for (case in vectors) {
            val value = case.jsonPrimitive.content
            assertFalse("deberia ser invalido: $value", Ulid.isValid(value))
        }
    }

    @Test
    fun `genera el ULID esperado a partir de timestamp y aleatoriedad`() {
        val vectors = TestVectors.load("ulid.json").jsonObject.getValue("from_parts").jsonArray
        for (case in vectors) {
            val entry = case.jsonObject
            val timestamp = entry.getValue("timestamp_ms").jsonPrimitive.longOrNull
                ?: error("timestamp_ms no es un entero valido")
            val randomHex = entry.getValue("random_hex").jsonPrimitive.content
            val expected = entry.getValue("expected").jsonPrimitive.content

            assertEquals("fromParts($timestamp, $randomHex)", expected, Ulid.fromParts(timestamp, randomHex))
        }
    }

    @Test
    fun `rechaza entradas invalidas de los vectores`() {
        val vectors = TestVectors.load("ulid.json")
            .jsonObject.getValue("from_parts_invalid").jsonArray
        for (case in vectors) {
            val entry = case.jsonObject
            val timestamp = entry.getValue("timestamp_ms").jsonPrimitive.longOrNull
            val randomHex = entry.getValue("random_hex").jsonPrimitive.content

            if (timestamp == null) {
                val raw = entry.getValue("timestamp_ms").jsonPrimitive.content
                assertNull("el vector usa un timestamp no entero: $raw", raw.toLongOrNull())
                continue
            }
            assertThrows(IllegalArgumentException::class.java) {
                Ulid.fromParts(timestamp, randomHex)
            }
        }
    }

    @Test
    fun `genera ULID unicos y con el timestamp indicado`() {
        val generated = (1..500).map { Ulid.generate() }
        assertEquals(500, generated.toSet().size)
        assertTrue(generated.all { Ulid.isValid(it) })

        val timestamp = 1_700_000_000_000L
        val fixed = Ulid.generate(timestamp)
        assertEquals(Ulid.fromParts(timestamp, "00000000000000000000").take(10), fixed.take(10))
    }
}
