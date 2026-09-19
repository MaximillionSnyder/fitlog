package com.fitlog.app.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

@Composable
fun MorphActionButton(
    started: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "session-morph",
    )

    val containerMorph = remember { Morph(IdleSessionPolygon, ActiveSessionPolygon) }
    val iconMorph = remember { Morph(PlayPolygon, StopPolygon) }
    val shape = MorphPolygonShape(containerMorph, progress)

    val containerColor =
        if (started) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val contentColor =
        if (started) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MorphIcon(
            morph = iconMorph,
            progress = progress,
            color = contentColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = if (started) "Finalizar" else "Iniciar",
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun MorphIcon(
    morph: Morph,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        path.transform(matrix)
        drawPath(path, color = color)
    }
}
