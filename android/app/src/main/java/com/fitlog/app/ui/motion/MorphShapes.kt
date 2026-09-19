package com.fitlog.app.ui.motion

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

val IdleSessionPolygon: RoundedPolygon = RoundedPolygon(
    numVertices = 6,
    rounding = CornerRounding(radius = 0.35f, smoothing = 0.5f),
)

val ActiveSessionPolygon: RoundedPolygon = RoundedPolygon.star(
    numVerticesPerRadius = 6,
    radius = 1f,
    innerRadius = 0.72f,
    rounding = CornerRounding(radius = 0.3f, smoothing = 0.5f),
)

val PlayPolygon: RoundedPolygon = RoundedPolygon(
    vertices = floatArrayOf(0f, -0.9f, 0.85f, 0f, 0f, 0.9f),
    rounding = CornerRounding(radius = 0.18f, smoothing = 0.3f),
)

val StopPolygon: RoundedPolygon = RoundedPolygon(
    vertices = floatArrayOf(-0.78f, -0.78f, 0.78f, -0.78f, 0.78f, 0.78f, -0.78f, 0.78f),
    rounding = CornerRounding(radius = 0.22f, smoothing = 0.3f),
)

class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {

    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        matrix.reset()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        val path = morph.toPath(progress).asComposePath()
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
