package com.fitlog.app.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MorphingBlob(
    modifier: Modifier = Modifier,
    colors: List<Color> = emptyList(),
    seed: Int = 0,
    durationMillis: Int = 9000,
) {
    val startPolygon = remember(seed) { organicPolygon(seed, vertices = 7, radiusJitter = 0.28f) }
    val endPolygon = remember(seed) { organicPolygon(seed + 17, vertices = 8, radiusJitter = 0.34f) }
    val morph = remember(startPolygon, endPolygon) { Morph(startPolygon, endPolygon) }

    val transition = rememberInfiniteTransition(label = "blob")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob-progress",
    )

    val resolvedColors = if (colors.isNotEmpty()) {
        colors
    } else {
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
        )
    }

    Canvas(modifier = modifier) {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        path.transform(matrix)
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = resolvedColors,
                center = Offset(size.width * 0.35f, size.height * 0.3f),
                radius = size.maxDimension * 0.75f,
            ),
        )
    }
}

private fun organicPolygon(seed: Int, vertices: Int, radiusJitter: Float): RoundedPolygon {
    val random = Random(seed)
    val points = FloatArray(vertices * 2)
    for (index in 0 until vertices) {
        val angle = (index.toFloat() / vertices) * 2f * PI.toFloat()
        val radius = 1f - radiusJitter * random.nextFloat()
        points[index * 2] = cos(angle) * radius
        points[index * 2 + 1] = sin(angle) * radius
    }
    return RoundedPolygon(
        vertices = points,
        rounding = CornerRounding(radius = 0.35f, smoothing = 0.6f),
    )
}
