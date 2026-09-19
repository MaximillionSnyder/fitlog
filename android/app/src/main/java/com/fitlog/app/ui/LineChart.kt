package com.fitlog.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Grafico de lineas del sistema: serie de valores con sus etiquetas.
 *
 * Usa el token de datos para la linea y los tokens de superficie para la grilla y las etiquetas,
 * asi el mismo grafico se ve igual en las tres pantallas que lo usan.
 */
@Composable
fun FitLogLineChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = { it.toString() },
) {
    val lineColor = MaterialTheme.fitLogColors.data
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp)

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val minValue = values.min()
        val maxValue = values.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val leftPad = 12.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        fun xAt(index: Int): Float =
            if (values.size == 1) leftPad + chartWidth / 2
            else leftPad + chartWidth * index / (values.size - 1)

        fun yAt(value: Double): Float =
            topPad + chartHeight * (1 - ((value - minValue) / span)).toFloat()

        for (step in 0..2) {
            val y = topPad + chartHeight * step / 2
            drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + chartWidth, y), strokeWidth = 1f)
        }

        if (values.size > 1) {
            val path = Path()
            values.forEachIndexed { index, value ->
                if (index == 0) path.moveTo(xAt(index), yAt(value)) else path.lineTo(xAt(index), yAt(value))
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        values.forEachIndexed { index, value ->
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(xAt(index), yAt(value)))
        }

        drawText(
            textMeasurer = textMeasurer,
            text = valueFormatter(maxValue),
            topLeft = Offset(leftPad, 0f),
            style = labelStyle,
        )
        drawText(
            textMeasurer = textMeasurer,
            text = valueFormatter(minValue),
            topLeft = Offset(leftPad, topPad + chartHeight - 12.dp.toPx()),
            style = labelStyle,
        )
        labels.firstOrNull()?.let { first ->
            drawText(
                textMeasurer = textMeasurer,
                text = first,
                topLeft = Offset(leftPad, topPad + chartHeight + 4.dp.toPx()),
                style = labelStyle,
            )
        }
        labels.lastOrNull()?.let { last ->
            val measured = textMeasurer.measure(last, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = last,
                topLeft = Offset(
                    leftPad + chartWidth - measured.size.width,
                    topPad + chartHeight + 4.dp.toPx(),
                ),
                style = labelStyle,
            )
        }
    }
}
