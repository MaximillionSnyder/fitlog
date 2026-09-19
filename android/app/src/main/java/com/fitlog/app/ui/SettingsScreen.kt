package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.AppSettings
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.LabeledValue
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.ThemeMode
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Ajustes: modo de tema, colores dinámicos y estado de la base de datos.
 *
 * El estado de la base de datos vivía en Inicio ocupando el lugar del contenido útil; acá es
 * información de diagnóstico y no compite con el panel de entrenamiento.
 */
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    modifier: Modifier = Modifier,
    viewModel: DbStatusViewModel = hiltViewModel(),
) {
    val status by viewModel.state.collectAsStateWithLifecycle()
    val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by appSettings.dynamicColor.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(title = "Apariencia")

        FitLogCard {
            Text(text = "Tema", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Elegí cómo se ve FitLog en este teléfono.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { appSettings.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                        label = { Text(text = shortLabel(mode)) },
                    )
                }
            }
        }

        FitLogCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FitLogIcons.Sun,
                    contentDescription = null,
                    tint = MaterialTheme.fitLogColors.warning,
                    modifier = Modifier.size(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Colores dinámicos", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Usa la paleta del sistema en Android 12 o superior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = { appSettings.setDynamicColor(it) },
                )
            }
        }

        SectionHeader(title = "Datos")

        when (val current = status) {
            DbStatusUi.Cargando -> FitLogCard {
                Text(text = "Iniciando base de datos…", style = MaterialTheme.typography.bodyMedium)
            }

            is DbStatusUi.Error -> ErrorState(
                message = current.message,
                onRetry = { viewModel.refresh() },
            )

            is DbStatusUi.Listo -> FitLogCard {
                Text(text = "Base de datos", style = MaterialTheme.typography.titleMedium)
                LabeledValue("Estado", "Lista")
                LabeledValue("Esquema", "v${current.schemaVersion}")
                LabeledValue("SQLite", current.sqliteVersion)
                LabeledValue("Archivo", current.databaseName)
                LabeledValue("Grupos musculares", current.muscleGroupCount.toString())
            }
        }
    }
}

private fun shortLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Auto"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
}
