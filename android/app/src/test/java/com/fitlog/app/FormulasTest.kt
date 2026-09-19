package com.fitlog.app

import com.fitlog.app.domain.Formulas
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormulasTest {

    @Test
    fun `el volumen coincide con los vectores compartidos`() {
        val vectors = TestVectors.load("formulas.json").jsonObject.getValue("volume").jsonArray
        for (case in vectors) {
            val entry = case.jsonObject
            val weightKg = entry.getValue("weight_kg").jsonPrimitive.doubleOrNull
            val reps = entry.getValue("reps").jsonPrimitive.intOrNull
            val expected = entry.getValue("expected").jsonPrimitive.doubleOrNull

            val actual = Formulas.volumeKg(weightKg, reps)
            if (expected == null) {
                assertNull("volumen($weightKg, $reps) deberia ser null", actual)
            } else {
                assertEquals("volumen($weightKg, $reps)", expected, actual ?: Double.NaN, 1e-9)
            }
        }
    }

    @Test
    fun `el 1RM estimado coincide con los vectores compartidos`() {
        val vectors = TestVectors.load("formulas.json")
            .jsonObject.getValue("one_rep_max_epley").jsonArray
        for (case in vectors) {
            val entry = case.jsonObject
            val weightKg = entry.getValue("weight_kg").jsonPrimitive.doubleOrNull
            val reps = entry.getValue("reps").jsonPrimitive.intOrNull
            val expected = entry.getValue("expected").jsonPrimitive.doubleOrNull

            val actual = Formulas.estimatedOneRepMaxKg(weightKg, reps)
            if (expected == null) {
                assertNull("1RM($weightKg, $reps) deberia ser null", actual)
            } else {
                assertEquals("1RM($weightKg, $reps)", expected, actual ?: Double.NaN, 1e-9)
            }
        }
    }
}
