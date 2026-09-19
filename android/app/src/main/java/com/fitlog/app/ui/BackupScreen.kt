package com.fitlog.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Respaldo: exportar las secciones elegidas a un archivo e importar fusionando con lo local.
 *
 * El encabezado lo pone el shell; acá van las dos acciones, la confirmación previa a elegir el
 * archivo y el resumen de lo que hizo la fusión, con las primitivas del sistema de diseño.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BackupScreen(
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingImport by remember { mutableStateOf(false) }

    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val json = state.exportedJson
        if (uri != null && json != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
        }
        viewModel.onExported()
    }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
            if (json != null) {
                viewModel.import(json)
            }
        }
    }

    LaunchedEffect(state.exportedJson) {
        if (state.exportedJson != null) {
            val name = "fitlog-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json"
            createDocument.launch(name)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "Exportá o fusioná tus datos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionHeader(title = "Exportar")

        FitLogCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                state.groups.forEach { group ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = group.id in state.selectedGroups,
                            onCheckedChange = { viewModel.toggleGroup(group.id) },
                        )
                        Text(
                            text = group.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            PrimaryAction(
                label = "Descargar respaldo",
                icon = FitLogIcons.ArrowDown,
                enabled = !state.busy,
                onClick = viewModel::export,
            )
            Text(
                text = "Los ejercicios propios que usan tus rutinas y entrenamientos se incluyen automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionHeader(title = "Importar y fusionar")

        FitLogCard {
            Text(
                text = "Se fusiona por identificador: gana la fila más nueva, los borrados se propagan " +
                    "y las filas locales que no están en el archivo no se tocan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pendingImport) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PrimaryAction(
                        label = "Elegir archivo",
                        onClick = {
                            pendingImport = false
                            openDocument.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryAction(
                        label = "Cancelar",
                        onClick = { pendingImport = false },
                    )
                }
            } else {
                PrimaryAction(
                    label = "Importar respaldo",
                    enabled = !state.busy,
                    onClick = { pendingImport = true },
                )
            }
        }

        state.error?.let { message ->
            ErrorState(message = message)
        }

        state.summary?.let { summary ->
            val fitLog = MaterialTheme.fitLogColors

            SectionHeader(title = "Resumen")

            FitLogCard {
                val relevant = summary.filterValues {
                    it.inserted + it.updated + it.ignored + it.remapped > 0
                }
                if (relevant.isEmpty()) {
                    Text(
                        text = "No había filas nuevas ni más recientes: no se cambió nada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                relevant.forEach { (table, counts) ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(text = table, style = MaterialTheme.typography.titleMedium)
                        MergeRow(
                            label = "nuevas",
                            value = "+${Format.integer(counts.inserted)}",
                            tint = fitLog.success,
                        )
                        MergeRow(
                            label = "actualizadas",
                            value = "~${Format.integer(counts.updated)}",
                            tint = fitLog.success,
                        )
                        MergeRow(
                            label = "ignoradas",
                            value = "=${Format.integer(counts.ignored)}",
                            tint = fitLog.danger,
                        )
                        if (counts.remapped > 0) {
                            MergeRow(
                                label = "remapeadas",
                                value = "↻${Format.integer(counts.remapped)}",
                                tint = fitLog.data,
                            )
                        }
                    }
                }
                TextButton(onClick = viewModel::clearSummary) { Text("Cerrar resumen") }
            }
        }
    }
}

/**
 * Fila del resumen de la fusión: etiqueta a la izquierda y el número a la derecha con el tono
 * semántico de lo aplicado o de lo rechazado.
 */
@Composable
private fun MergeRow(
    label: String,
    value: String,
    tint: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
        )
    }
}
