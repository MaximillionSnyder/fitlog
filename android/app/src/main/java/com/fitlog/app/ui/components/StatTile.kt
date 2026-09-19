package com.fitlog.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitlog.app.ui.theme.FitLogColors
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Tile de estadistica: etiqueta, valor grande y, opcionalmente, un icono y una variacion.
 *
 * Es la unidad del panel de Inicio y de los resumenes: un dato por tile, con el valor en cifras
 * tabulares y el delta coloreado por su token semantico.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    icon: ImageVector? = null,
    deltaPercent: Double? = null,
    deltaLabel: String? = null,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val fitLog = MaterialTheme.fitLogColors
    val valueColor = accent ?: MaterialTheme.colorScheme.onSurface
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unit != null) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            if (deltaPercent != null) {
                DeltaBadge(percent = deltaPercent, suffix = deltaLabel, colors = fitLog)
            } else if (deltaLabel != null) {
                Text(
                    text = deltaLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    val shape = MaterialTheme.shapes.large
    val container = MaterialTheme.colorScheme.surfaceContainer
    val contentColor = MaterialTheme.colorScheme.onSurface
    val border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border,
        ) {
            content()
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = container,
            contentColor = contentColor,
            border = border,
        ) {
            content()
        }
    }
}

@Composable
private fun DeltaBadge(
    percent: Double,
    suffix: String?,
    colors: FitLogColors,
) {
    val positive = percent > 0.5
    val negative = percent < -0.5
    val tint = when {
        positive -> colors.success
        negative -> colors.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val background = when {
        positive -> colors.successSoft
        negative -> colors.dangerSoft
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val icon = when {
        positive -> FitLogIcons.ArrowUp
        negative -> FitLogIcons.ArrowDown
        else -> null
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = background,
        contentColor = tint,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = if (suffix == null) {
                    Format.deltaText(percent)
                } else {
                    "${Format.deltaText(percent)} $suffix"
                },
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
            )
        }
    }
}

/** Fila de acciones/valores compactos, para resumenes dentro de una tarjeta. */
@Composable
fun MetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
