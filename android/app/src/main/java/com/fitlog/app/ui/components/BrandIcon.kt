package com.fitlog.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Set de iconos propio de FitLog.
 *
 * Se dibujan a mano en un lienzo de 24x24 con un estilo geometrico comun (trazo de 1.8dp y uniones
 * redondeadas). Evita la dependencia de `material-icons-extended`, que suma varios megabytes de
 * recursos por un punado de iconos.
 *
 * Solo se usan primitivas de [PathBuilder] (`moveTo`, `lineTo`, `arcToRelative`, `curveTo`): las
 * helpers de conveniencia (`addRoundRect`, `addCircle`) no forman parte de la API publica estable.
 */
object FitLogIcons {

    val Home: ImageVector = icon("Home") {
        stroke { moveTo(4f, 11.2f); lineTo(12f, 4.2f); lineTo(20f, 11.2f) }
        stroke { moveTo(6.4f, 10.6f); lineTo(6.4f, 19.4f); lineTo(17.6f, 19.4f); lineTo(17.6f, 10.6f) }
        stroke { moveTo(10.2f, 19.4f); lineTo(10.2f, 14.2f); lineTo(13.8f, 14.2f); lineTo(13.8f, 19.4f) }
    }

    val Dumbbell: ImageVector = icon("Dumbbell") {
        fill { addRoundRect(3.2f, 10.4f, 6f, 13.6f, 0.8f) }
        fill { addRoundRect(17.2f, 10.4f, 20f, 13.6f, 0.8f) }
        fill { addRoundRect(6.6f, 8.4f, 9.4f, 15.6f, 0.9f) }
        fill { addRoundRect(13.8f, 8.4f, 16.6f, 15.6f, 0.9f) }
        fill { addRoundRect(9.6f, 11.3f, 13.6f, 12.7f, 0.6f) }
    }

    val Chart: ImageVector = icon("Chart") {
        fill { addRoundRect(3.4f, 13f, 5.6f, 20f, 0.8f) }
        fill { addRoundRect(8.6f, 9f, 11f, 20f, 0.8f) }
        fill { addRoundRect(14f, 4.6f, 16.4f, 20f, 0.8f) }
        fill { addRoundRect(18.8f, 11f, 21.2f, 20f, 0.8f) }
    }

    val Calendar: ImageVector = icon("Calendar") {
        fill(fillAlpha = 0.18f) { addRoundRect(3.2f, 5f, 20.8f, 20.4f, 2.6f) }
        stroke { moveTo(5.6f, 9.4f); lineTo(18.4f, 9.4f) }
        stroke { moveTo(8.4f, 3.6f); lineTo(8.4f, 6.6f) }
        stroke { moveTo(15.6f, 3.6f); lineTo(15.6f, 6.6f) }
        stroke {
            moveTo(4f, 19.2f)
            verticalLineTo(6.8f)
            arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.4f, dy1 = -2.4f)
            horizontalLineTo(17.6f)
            arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 2.4f, dy1 = 2.4f)
            verticalLineTo(19.2f)
            arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.4f, dy1 = 2.4f)
            horizontalLineTo(6.4f)
            arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.4f, dy1 = -2.4f)
            close()
        }
    }

    val Grid: ImageVector = icon("Grid") {
        fill { addRoundRect(4f, 4f, 10.6f, 10.6f, 2.2f) }
        fill(fillAlpha = 0.7f) { addRoundRect(13.4f, 4f, 20f, 10.6f, 2.2f) }
        fill(fillAlpha = 0.7f) { addRoundRect(4f, 13.4f, 10.6f, 20f, 2.2f) }
        fill { addRoundRect(13.4f, 13.4f, 20f, 20f, 2.2f) }
    }

    val Plus: ImageVector = icon("Plus") {
        stroke { moveTo(12f, 5.4f); lineTo(12f, 18.6f) }
        stroke { moveTo(5.4f, 12f); lineTo(18.6f, 12f) }
    }

    val Search: ImageVector = icon("Search") {
        stroke { addCircle(10.8f, 10.8f, 5.4f) }
        stroke { moveTo(15.2f, 15.2f); lineTo(20f, 20f) }
    }

    val Scale: ImageVector = icon("Scale") {
        fill(fillAlpha = 0.18f) { addRoundRect(4f, 4f, 20f, 20f, 5f) }
        stroke {
            moveTo(5.6f, 16.4f)
            arcToRelative(7.4f, 7.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 12.8f, dy1 = 0f)
        }
        stroke { moveTo(12f, 16.2f); lineTo(15.4f, 10.4f) }
        fill { addCircle(12f, 16.2f, 1.2f) }
    }

    val Trophy: ImageVector = icon("Trophy") {
        stroke {
            moveTo(7.6f, 4.6f)
            horizontalLineTo(16.4f)
            verticalLineTo(11f)
            arcToRelative(4.4f, 4.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -8.8f, dy1 = 0f)
            close()
        }
        stroke {
            moveTo(7.8f, 6.6f)
            horizontalLineTo(5.4f)
            verticalLineTo(8.6f)
            arcToRelative(2.6f, 2.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 2.6f, dy1 = 2.6f)
        }
        stroke {
            moveTo(16.2f, 6.6f)
            horizontalLineTo(18.6f)
            verticalLineTo(8.6f)
            arcToRelative(2.6f, 2.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -2.6f, dy1 = 2.6f)
        }
        stroke { moveTo(12f, 15.4f); lineTo(12f, 18.4f) }
        stroke { moveTo(8.6f, 20.4f); horizontalLineTo(15.4f); verticalLineTo(18.4f); horizontalLineTo(8.6f); close() }
    }

    val Spark: ImageVector = icon("Spark") {
        stroke {
            moveTo(12f, 3.6f)
            curveTo(12.9f, 8.2f, 14.4f, 9.6f, 19f, 10.5f)
            curveTo(14.4f, 11.4f, 12.9f, 12.9f, 12f, 17.5f)
            curveTo(11.1f, 12.9f, 9.6f, 11.4f, 5f, 10.5f)
            curveTo(9.6f, 9.6f, 11.1f, 8.2f, 12f, 3.6f)
            close()
        }
        fill { addCircle(18.4f, 18.4f, 1.5f) }
    }

    val Shield: ImageVector = icon("Shield") {
        stroke {
            moveTo(12f, 3.6f)
            lineTo(19f, 6.2f)
            verticalLineTo(11.6f)
            curveTo(19f, 16.2f, 16.2f, 19.4f, 12f, 20.8f)
            curveTo(7.8f, 19.4f, 5f, 16.2f, 5f, 11.6f)
            verticalLineTo(6.2f)
            close()
        }
        stroke { moveTo(8.8f, 12.2f); lineTo(11.2f, 14.6f); lineTo(15.4f, 9.8f) }
    }

    val Gear: ImageVector = icon("Gear") {
        stroke { addCircle(12f, 12f, 2.4f) }
        stroke { moveTo(12f, 3.6f); lineTo(12f, 6f) }
        stroke { moveTo(12f, 18f); lineTo(12f, 20.4f) }
        stroke { moveTo(3.6f, 12f); lineTo(6f, 12f) }
        stroke { moveTo(18f, 12f); lineTo(20.4f, 12f) }
        stroke { moveTo(6.1f, 6.1f); lineTo(7.8f, 7.8f) }
        stroke { moveTo(16.2f, 16.2f); lineTo(17.9f, 17.9f) }
        stroke { moveTo(17.9f, 6.1f); lineTo(16.2f, 7.8f) }
        stroke { moveTo(7.8f, 16.2f); lineTo(6.1f, 17.9f) }
    }

    val Sun: ImageVector = icon("Sun") {
        stroke { addCircle(12f, 12f, 3.4f) }
        stroke { moveTo(12f, 2.8f); lineTo(12f, 5f) }
        stroke { moveTo(12f, 19f); lineTo(12f, 21.2f) }
        stroke { moveTo(2.8f, 12f); lineTo(5f, 12f) }
        stroke { moveTo(19f, 12f); lineTo(21.2f, 12f) }
        stroke { moveTo(5.5f, 5.5f); lineTo(7.1f, 7.1f) }
        stroke { moveTo(16.9f, 16.9f); lineTo(18.5f, 18.5f) }
        stroke { moveTo(18.5f, 5.5f); lineTo(16.9f, 7.1f) }
        stroke { moveTo(7.1f, 16.9f); lineTo(5.5f, 18.5f) }
    }

    val Moon: ImageVector = icon("Moon") {
        stroke {
            moveTo(20f, 14.4f)
            arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -10.4f, dy1 = -10.4f)
            arcToRelative(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = 10.4f, dy1 = 10.4f)
            close()
        }
    }

    val Play: ImageVector = icon("Play") {
        fill { moveTo(8.4f, 5.6f); lineTo(18.4f, 12f); lineTo(8.4f, 18.4f); close() }
    }

    val Stop: ImageVector = icon("Stop") {
        fill { addRoundRect(7f, 7f, 17f, 17f, 2.4f) }
    }

    val ArrowUp: ImageVector = icon("ArrowUp") {
        stroke { moveTo(12f, 19f); lineTo(12f, 5.6f) }
        stroke { moveTo(6.4f, 11.2f); lineTo(12f, 5.6f); lineTo(17.6f, 11.2f) }
    }

    val ArrowDown: ImageVector = icon("ArrowDown") {
        stroke { moveTo(12f, 5f); lineTo(12f, 18.4f) }
        stroke { moveTo(6.4f, 12.8f); lineTo(12f, 18.4f); lineTo(17.6f, 12.8f) }
    }

    val ChevronRight: ImageVector = icon("ChevronRight") {
        stroke { moveTo(9.6f, 5.6f); lineTo(16f, 12f); lineTo(9.6f, 18.4f) }
    }

    val Back: ImageVector = icon("Back") {
        stroke { moveTo(19f, 12f); lineTo(5.4f, 12f) }
        stroke { moveTo(11.4f, 5.8f); lineTo(5.2f, 12f); lineTo(11.4f, 18.2f) }
    }

    val More: ImageVector = icon("More") {
        fill { addCircle(5.6f, 12f, 1.9f) }
        fill { addCircle(12f, 12f, 1.9f) }
        fill { addCircle(18.4f, 12f, 1.9f) }
    }
}

private const val ICON_STROKE_WIDTH = 1.8f

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        tintColor = Color.White,
    ).apply(block).build()

private fun ImageVector.Builder.fill(
    fillAlpha: Float = 1f,
    block: PathBuilder.() -> Unit,
) {
    path(
        fill = SolidColor(Color.White),
        fillAlpha = fillAlpha,
        stroke = null,
        pathBuilder = block,
    )
}

private fun ImageVector.Builder.stroke(block: PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = ICON_STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

/** Rectangulo con esquinas redondeadas dibujado con arcos relativos. */
private fun PathBuilder.addRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    val r = minOf(radius, (right - left) / 2f, (bottom - top) / 2f)
    moveTo(left + r, top)
    horizontalLineTo(right - r)
    arcToRelative(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = r, dy1 = r)
    verticalLineTo(bottom - r)
    arcToRelative(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -r, dy1 = r)
    horizontalLineTo(left + r)
    arcToRelative(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -r, dy1 = -r)
    verticalLineTo(top + r)
    arcToRelative(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = r, dy1 = -r)
    close()
}

/** Circulo dibujado con dos semicircunferencias. */
private fun PathBuilder.addCircle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = radius * 2f, dy1 = 0f)
    arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -radius * 2f, dy1 = 0f)
    close()
}
