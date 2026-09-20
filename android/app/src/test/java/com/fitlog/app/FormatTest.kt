package com.fitlog.app

import com.fitlog.app.ui.components.Format
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `el paso de peso suma y resta sin bajar de cero`() {
        assertEquals("62.5", Format.stepDecimal("60", 2.5))
        assertEquals("57.5", Format.stepDecimal("60", -2.5))
        assertEquals("0", Format.stepDecimal("0", -2.5))
        assertEquals("2.5", Format.stepDecimal("", 2.5))
        assertEquals("62.5", Format.stepDecimal("60,0", 2.5))
    }

    @Test
    fun `el paso de reps nunca baja de una`() {
        assertEquals("11", Format.stepInteger("10", 1))
        assertEquals("9", Format.stepInteger("10", -1))
        assertEquals("1", Format.stepInteger("1", -1))
        assertEquals("1", Format.stepInteger("", -1))
    }

    @Test
    fun `los numeros se muestran con separador de miles y sin ceros de relleno`() {
        assertEquals("1 234", Format.integer(1234))
        assertEquals("60", Format.kg(60.0))
        assertEquals("62.5", Format.kg(62.5))
        assertEquals("—", Format.kg(null))
        assertEquals("1 250", Format.volumeKg(1250.0))
        assertEquals("12.5", Format.volumeKg(12.5))
    }

    @Test
    fun `la duracion se muestra compacta`() {
        assertEquals("45 s", Format.duration(45_000))
        assertEquals("12 m", Format.duration(12 * 60_000L))
        assertEquals("1 h 05 m", Format.duration(65 * 60_000L))
    }
}
