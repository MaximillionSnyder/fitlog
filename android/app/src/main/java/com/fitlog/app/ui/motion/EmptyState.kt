package com.fitlog.app.ui.motion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.theme.LocalFitLogColors
import com.fitlog.app.ui.theme.Spacing

/**
 * Estado vacio compartido: blob de marca, mensaje y una accion cuando existe un paso siguiente.
 *
 * Un estado vacio sin salida deja al usuario en un callejon: si hay una accion posible, se muestra.
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        } else {
            MorphingBlob(
                modifier = Modifier.size(88.dp),
                seed = message.hashCode(),
                durationMillis = 11000,
            )
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionLabel != null && onAction != null) {
            SecondaryAction(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}

/** Estado de carga compartido. */
@Composable
fun LoadingState(
    message: String = "Cargando…",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Estado de error compartido: mensaje con el token de peligro y reintento opcional. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val fitLog = LocalFitLogColors.current
    FitLogCardToned(
        modifier = modifier,
        container = fitLog.dangerSoft,
        content = fitLog.danger,
    ) {
        Icon(
            imageVector = FitLogIcons.Shield,
            contentDescription = null,
            tint = fitLog.danger,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = "Algo salió mal",
            style = MaterialTheme.typography.titleMedium,
            color = fitLog.danger,
        )
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) {
            SecondaryAction(label = "Reintentar", onClick = onRetry)
        }
    }
}

@Composable
private fun FitLogCardToned(
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    body: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            content = body,
        )
    }
}
