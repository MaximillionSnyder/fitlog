package com.fitlog.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.LabeledValue
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Importar entrenamientos de Huawei Health.
 *
 * El usuario elige la carpeta de la exportacion, la app la lee y muestra que encontro antes de
 * escribir nada: cantidad, rango de fechas, tipos y cuantos ya estan en FitLog.
 */
@Composable
fun ImportScreen(
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // El selector de carpeta del sistema: la exportacion trae muchos JSON y hay que leerlos todos.
    val pickFolder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val contents = ImportFiles.readJsonFiles(context, uri)
        viewModel.readFiles(contents)
    }

    // La exportacion llega como ZIP: se puede elegir el archivo sin descomprimir.
    val pickZip = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val contents = ImportFiles.readZipFile(context, uri)
        viewModel.readFiles(contents)
    }

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
        item { ImportIntro() }

        item {
            PrimaryAction(
                label = if (state.step == ImportUiState.Step.EMPTY) {
                    "Elegir carpeta (Huawei Health o GPX)"
                } else {
                    "Elegir otra carpeta"
                },
                icon = FitLogIcons.Plus,
                onClick = { pickFolder.launch(null) },
            )
        }

        item {
            SecondaryAction(
                label = "Elegir el ZIP de Huawei Health",
                icon = FitLogIcons.ArrowDown,
                onClick = { pickZip.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            )
        }

        state.error?.let { message ->
            item { ErrorState(message = message) }
        }

        if (state.reading) {
            item { LoadingState(message = "Leyendo la exportación…") }
        }

        when (state.step) {
            ImportUiState.Step.PREVIEW -> {
                item { SectionHeader(title = "Qué se encontró") }

                if (state.workouts.isEmpty()) {
                    item {
                        FitLogCard {
                            Text(
                                text = "No se encontraron entrenamientos",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Revisá que la carpeta sea la de la exportación de Huawei " +
                                    "Health (la que trae los archivos de entrenamientos).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            StatTile(
                                label = "Entrenamientos",
                                value = Format.integer(state.workouts.size),
                                icon = FitLogIcons.Dumbbell,
                                modifier = Modifier.weight(1f),
                            )
                            StatTile(
                                label = "Ya en FitLog",
                                value = Format.integer(state.alreadyImported),
                                icon = FitLogIcons.Check,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item {
                        FitLogCard {
                            LabeledValue(
                                label = "Desde",
                                value = state.firstAtMs?.let { formatImportDate(it) } ?: "—",
                            )
                            LabeledValue(
                                label = "Hasta",
                                value = state.lastAtMs?.let { formatImportDate(it) } ?: "—",
                            )
                            LabeledValue(
                                label = "Archivos leídos",
                                value = Format.integer(state.filesRead),
                            )
                            if (state.filesSkipped > 0) {
                                LabeledValue(
                                    label = "Archivos descartados",
                                    value = Format.integer(state.filesSkipped),
                                )
                            }
                        }
                    }

                    item { SectionHeader(title = "Tipos de deporte") }
                    items(state.sports) { sport ->
                        FitLogCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = sport.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = Format.integer(sport.count),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    item {
                        PrimaryAction(
                            label = if (state.pending > 0) {
                                "Importar ${Format.integer(state.pending)} entrenamientos"
                            } else {
                                "No hay nada nuevo para importar"
                            },
                            icon = FitLogIcons.ArrowDown,
                            enabled = state.canImport,
                            onClick = viewModel::import,
                        )
                    }

                    if (state.importing) {
                        item { LoadingState(message = "Importando entrenamientos…") }
                    }
                }
            }

            ImportUiState.Step.DONE -> {
                val result = state.result
                item { SectionHeader(title = "Resultado") }
                item {
                    FitLogCard(containerColor = MaterialTheme.fitLogColors.successSoft) {
                        Text(
                            text = "Importación terminada",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.fitLogColors.success,
                        )
                        LabeledValue(
                            label = "Sesiones agregadas",
                            value = Format.integer(result?.imported ?: 0),
                        )
                        LabeledValue(
                            label = "Ya estaban",
                            value = Format.integer(result?.skipped ?: 0),
                        )
                    }
                }
                item {
                    Text(
                        text = "Las sesiones importadas no tienen series: Huawei Health no exporta " +
                            "peso ni reps. Podés abrirlas desde Entrenar y completarlas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    PrimaryAction(
                        label = "Ver el historial",
                        icon = FitLogIcons.Calendar,
                        onClick = onOpenHistory,
                    )
                }
                item {
                    SecondaryAction(label = "Importar otra exportación", onClick = viewModel::reset)
                }
            }

            ImportUiState.Step.EMPTY -> Unit
        }
    }
}

@Composable
private fun ImportIntro() {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard(containerColor = fitLog.dataSoft) {
        Text(
            text = "Traé tus entrenamientos de Huawei Health o GPX",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Elegí el ZIP o la carpeta de la exportación de Huawei Health, o archivos GPX " +
                "de tu reloj. Se leen los dos formatos, se pueden mezclar y nada se escribe hasta " +
                "que confirmes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatImportDate(timestampMs: Long): String {
    val formatter = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}
