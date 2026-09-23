package com.fitlog.app

import com.fitlog.app.domain.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest {

    private fun point(latitude: Double, longitude: Double, elevation: Double? = null, hr: Double? = null) =
        Route.Point(latitude = latitude, longitude = longitude, elevation = elevation, heartRate = hr)

    @Test
    fun `el texto de la ruta va y vuelve igual`() {
        val route = listOf(
            point(-34.6037, -58.3816, 25.0, 120.0),
            point(-34.6047, -58.3826, 32.5, 170.0),
        )

        val decoded = Route.decode(Route.encode(route))

        assertEquals(2, decoded.size)
        assertEquals(-34.6037, decoded[0].latitude, 0.00001)
        assertEquals(-58.3816, decoded[0].longitude, 0.00001)
        assertEquals(25.0, decoded[0].elevation ?: 0.0, 0.001)
        assertEquals(120.0, decoded[0].heartRate ?: 0.0, 0.001)
        assertEquals(32.5, decoded[1].elevation ?: 0.0, 0.001)
    }

    @Test
    fun `los campos que faltan quedan vacios`() {
        val decoded = Route.decode(Route.encode(listOf(point(-34.6, -58.3))))

        assertEquals(1, decoded.size)
        assertEquals(null, decoded[0].elevation)
        assertEquals(null, decoded[0].heartRate)
    }

    @Test
    fun `una ruta vacia o rota no rompe`() {
        assertTrue(Route.decode(null).isEmpty())
        assertTrue(Route.decode("").isEmpty())
        assertTrue(Route.decode("basura;otra cosa").isEmpty())
    }

    @Test
    fun `recorta la ruta sin perder el principio ni el final`() {
        val many = (0 until 5_000).map { index -> point(-34.6 + index * 0.0001, -58.3) }

        val simplified = Route.simplify(many)

        assertEquals(Route.MAX_POINTS, simplified.size)
        assertEquals(many.first().latitude, simplified.first().latitude, 0.00001)
        assertEquals(many.last().latitude, simplified.last().latitude, 0.00001)
    }

    @Test
    fun `una ruta corta no se toca`() {
        val few = (0 until 20).map { index -> point(-34.6 + index * 0.0001, -58.3) }

        assertEquals(20, Route.simplify(few).size)
    }

    @Test
    fun `descarta los puntos sin coordenadas`() {
        val mixed = listOf(point(-34.6, -58.3), Route.Point(Double.NaN, Double.NaN))

        assertEquals(1, Route.simplify(mixed).size)
    }

    @Test
    fun `la distancia de la ruta suma los tramos`() {
        val route = listOf(point(-34.6037, -58.3816), point(-34.6047, -58.3826))

        // Dos puntos a unos 144 m.
        assertTrue(Route.distanceM(route) in 130.0..160.0)
    }
}
