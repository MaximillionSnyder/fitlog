package com.fitlog.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fitlog.app.domain.Route
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Trazado del recorrido.
 *
 * No es un mapa: un GPX trae las coordenadas, no las imagenes (esas vienen de un servicio de
 * teselas y necesitan conexion). El dibujo se escala solo para entrar en la tarjeta, con el inicio
 * y el fin marcados.
 */
@Composable
fun RouteSketch(route: List<Route.Point>, modifier: Modifier = Modifier) {
    if (route.size < 2) return

    val fitLog = MaterialTheme.fitLogColors
    val latitudes = route.map { it.latitude }
    val longitudes = route.map { it.longitude }
    val minLatitude = latitudes.min()
    val maxLatitude = latitudes.max()
    val minLongitude = longitudes.min()
    val maxLongitude = longitudes.max()

    FitLogCard(modifier = modifier) {
        SectionHeader(title = "Recorrido", trailing = "${route.size} puntos")
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = Spacing.sm),
        ) {
            // Se mantiene la proporcion real para que el trazado no salga deformado.
            val spanLatitude = (maxLatitude - minLatitude).takeIf { it > 0 } ?: 1e-6
            val spanLongitude = (maxLongitude - minLongitude).takeIf { it > 0 } ?: 1e-6
            val scale = minOf(size.width / spanLongitude, size.height / spanLatitude)
            val offsetX = (size.width - spanLongitude * scale) / 2
            val offsetY = (size.height - spanLatitude * scale) / 2

            fun point(index: Int): Offset {
                val latitude = route[index].latitude
                val longitude = route[index].longitude
                return Offset(
                    x = (offsetX + (longitude - minLongitude) * scale).toFloat(),
                    // La latitud crece hacia el norte y el lienzo hacia abajo.
                    y = (offsetY + (maxLatitude - latitude) * scale).toFloat(),
                )
            }

            val path = Path().apply {
                val first = point(0)
                moveTo(first.x, first.y)
                for (index in 1 until route.size) {
                    val next = point(index)
                    lineTo(next.x, next.y)
                }
            }
            drawPath(
                path = path,
                color = fitLog.data,
                style = Stroke(width = 4f, cap = StrokeCap.Round),
            )

            val start = point(0)
            val end = point(route.size - 1)
            drawCircle(color = fitLog.success, radius = 9f, center = start)
            drawCircle(color = fitLog.danger, radius = 9f, center = end)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Inicio y fin marcados · el mapa con imágenes necesita conexión",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
