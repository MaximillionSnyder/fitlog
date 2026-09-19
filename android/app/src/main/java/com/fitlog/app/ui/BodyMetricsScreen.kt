package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Body
import com.fitlog.app.domain.Progress
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Medidas corporales: alta, edición, borrado y evolución de la medida elegida.
 *
 * El encabezado lo provee el shell; acá solo se usa el sistema de diseño (`FitLogCard`, `StatTile`,
 * `SectionHeader`, acciones y estados compartidos) manteniendo la lógica del ViewModel intacta.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BodyMetricsScreen(
    modifier: Modifier = Modifier,
    viewModel: BodyMetricsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var value by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Body.Point?>(null) }

    val unit = Body.unitForKind(state.kind)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(title = "Peso corporal y medidas")

        state.error?.let { message ->
            ErrorState(message = message, onRetry = { viewModel.load() })
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Body.Kind.entries.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { viewModel.selectKind(kind) },
                    label = { Text(Body.label(kind)) },
                )
            }
        }

        FitLogCard {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Valor (${unit.wire})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.formError?.let { message ->
                Text(text = message, color = MaterialTheme.fitLogColors.danger)
            }
            PrimaryAction(
                label = "Registrar ${Body.label(state.kind).lowercase()}",
                icon = FitLogIcons.Plus,
                onClick = {
                    val parsed = value.trim().replace(',', '.').toDoubleOrNull()
                    if (parsed == null) {
                        return@PrimaryAction
                    }
                    viewModel.add(parsed, System.currentTimeMillis(), notes.ifBlank { null })
                    value = ""
                    notes = ""
                },
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = state.preset == Progress.RangePreset.LAST_30_DAYS,
                onClick = { viewModel.selectPreset(Progress.RangePreset.LAST_30_DAYS) },
                label = { Text("30 días") },
            )
            FilterChip(
                selected = state.preset == Progress.RangePreset.LAST_90_DAYS,
                onClick = { viewModel.selectPreset(Progress.RangePreset.LAST_90_DAYS) },
                label = { Text("90 días") },
            )
            FilterChip(
                selected = state.preset == Progress.RangePreset.ALL,
                onClick = { viewModel.selectPreset(Progress.RangePreset.ALL) },
                label = { Text("Todo") },
            )
        }

        if (state.loading) {
            LoadingState(message = "Cargando medidas…")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            StatTile(
                label = "Última",
                value = state.stats.latest?.let { Body.formatValue(it, unit) } ?: "—",
                icon = FitLogIcons.Scale,
                accent = MaterialTheme.fitLogColors.data,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Mín",
                value = state.stats.min?.let { Body.formatValue(it, unit) } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            StatTile(
                label = "Máx",
                value = state.stats.max?.let { Body.formatValue(it, unit) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            // La variación actual no tiene semántica de color: el tipo de medida cambia el signo
            // deseable, así que se deja el valor neutro del sistema.
            StatTile(
                label = "Variación",
                value = state.stats.deltaAbs?.let { delta ->
                    val sign = if (delta > 0) "+" else ""
                    "$sign$delta ${unit.wire}"
                } ?: "—",
                modifier = Modifier.weight(1f),
            )
        }

        if (!state.loading && state.series.isEmpty()) {
            EmptyState(
                title = "Sin medidas en el periodo",
                message = "No hay medidas de ${Body.label(state.kind).lowercase()} en el periodo elegido.",
                actionLabel = if (state.preset == Progress.RangePreset.ALL) null else "Ver todo",
                onAction = { viewModel.selectPreset(Progress.RangePreset.ALL) },
            )
        }

        if (state.series.isNotEmpty()) {
            FitLogCard {
                Text(
                    text = "${Body.label(state.kind)} · ${Format.integer(state.series.size)} mediciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FitLogLineChart(
                    values = state.series.map { it.value },
                    labels = state.series.map { formatDay(it.measuredAtMs) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    valueFormatter = { Body.formatValue(it, unit) },
                )
            }

            state.series.reversed().forEach { point ->
                FitLogCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                text = formatDay(point.measuredAtMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = Body.formatValue(point.value, point.unit),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { editing = point }) {
                                Text(text = "Editar", color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = { viewModel.delete(point.id) }) {
                                Text(text = "Borrar", color = MaterialTheme.fitLogColors.danger)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { point ->
        EditMetricDialog(
            point = point,
            onDismiss = { editing = null },
            onSave = { newValue, newNotes ->
                viewModel.update(point.id, newValue, point.measuredAtMs, newNotes)
                editing = null
            },
        )
    }
}

@Composable
private fun EditMetricDialog(
    point: Body.Point,
    onDismiss: () -> Unit,
    onSave: (Double, String?) -> Unit,
) {
    var value by remember { mutableStateOf(point.value.toString()) }
    var notes by remember { mutableStateOf(point.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Body.label(point.kind)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Valor (${point.unit.wire})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = value.trim().replace(',', '.').toDoubleOrNull()
                if (parsed != null) {
                    onSave(parsed, notes.ifBlank { null })
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun formatDay(timestampMs: Long): String =
    SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestampMs))
