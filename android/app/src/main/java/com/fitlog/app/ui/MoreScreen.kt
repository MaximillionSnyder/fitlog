package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.IconBadge
import com.fitlog.app.ui.components.NavigationRow
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.destinations.moreDestinations
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Pantalla "Más": agrupa los destinos que no son de primer nivel.
 *
 * La barra inferior se queda con lo cotidiano (Inicio, Entrenar, Progreso, Rutinas) y el resto vive
 * acá con una descripción, para que la navegación principal no tenga nueve destinos.
 */
@Composable
fun MoreScreen(
    onOpenRoute: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.sm,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            MoreIntro()
        }

        item {
            SectionHeader(title = "Explorar")
        }

        items(moreDestinations()) { destination ->
            NavigationRow(
                title = destination.title,
                description = destination.description,
                icon = destination.icon,
                onClick = { onOpenRoute(destination.route) },
            )
        }

        item {
            SectionHeader(title = "App")
        }

        item {
            NavigationRow(
                title = "Ajustes",
                description = "Tema, colores y estado de la base de datos",
                icon = FitLogIcons.Gear,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun MoreIntro() {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard(containerColor = fitLog.dataSoft) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = FitLogIcons.Spark,
                tint = fitLog.data,
                background = MaterialTheme.colorScheme.surfaceContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = "Todo en un solo lugar", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Catálogo, medidas, comparativas, tips y respaldo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
