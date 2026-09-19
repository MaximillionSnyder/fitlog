package com.fitlog.app

import com.fitlog.app.domain.RoutineOrder
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RoutineOrderTest {

    private val vectors = TestVectors.load("routine-order.json").jsonObject

    @Test
    fun `el movimiento coincide con los vectores compartidos`() {
        vectors.getValue("move_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val name = testCase.getValue("name").jsonPrimitive.content
            val ids = testCase.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            val fromIndex = testCase.getValue("from_index").jsonPrimitive.intOrNull ?: 0
            val toIndex = testCase.getValue("to_index").jsonPrimitive.intOrNull ?: 0
            val expected = testCase.getValue("expected").jsonArray.map { it.jsonPrimitive.content }

            val original = ids.toList()
            val result = RoutineOrder.moveItem(ids, fromIndex, toIndex)

            assertEquals(name, expected, result)
            assertEquals("$name (no muta la entrada)", original, ids)
        }
    }

    @Test
    fun `los movimientos invalidos fallan`() {
        vectors.getValue("invalid_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val ids = testCase.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            val fromIndex = testCase.getValue("from_index").jsonPrimitive.intOrNull ?: 0
            val toIndex = testCase.getValue("to_index").jsonPrimitive.intOrNull ?: 0

            assertThrows(IllegalArgumentException::class.java) {
                RoutineOrder.moveItem(ids, fromIndex, toIndex)
            }
        }
    }

    @Test
    fun `las posiciones coinciden con los vectores compartidos`() {
        vectors.getValue("position_cases").jsonArray.forEach { element ->
            val testCase = element.jsonObject
            val ids = testCase.getValue("ids").jsonArray.map { it.jsonPrimitive.content }
            val expected = testCase.getValue("expected").jsonArray.map { entry ->
                val item = entry.jsonObject
                item.getValue("id").jsonPrimitive.content to
                    (item.getValue("position").jsonPrimitive.intOrNull ?: 0)
            }

            assertEquals(testCase.getValue("name").jsonPrimitive.content, expected, RoutineOrder.assignPositions(ids))
        }
    }
}
