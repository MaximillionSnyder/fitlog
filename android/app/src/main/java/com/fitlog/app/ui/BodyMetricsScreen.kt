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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Body
import com.fitlog.app.domain.Progress
import com.fitlog.app.ui.motion.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Peso corporal y medidas", style = MaterialTheme.typography.bodySmall)

        state.error?.let { message -> Text(text = message, color = MaterialTheme.colorScheme.error) }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Body.Kind.entries.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { viewModel.selectKind(kind) },
                    label = { Text(Body.label(kind)) },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        val parsed = value.trim().replace(',', '.').toDoubleOrNull()
                        if (parsed == null) {
                            return@Button
                        }
                        viewModel.add(parsed, System.currentTimeMillis(), notes.ifBlank { null })
                        value = ""
                        notes = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Registrar ${Body.label(state.kind).lowercase()}") }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(text = "Cargando medidas…", style = MaterialTheme.typography.bodySmall)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell("Última", state.stats.latest?.let { Body.formatValue(it, unit) } ?: "—")
                StatCell("Mín", state.stats.min?.let { Body.formatValue(it, unit) } ?: "—")
                StatCell("Máx", state.stats.max?.let { Body.formatValue(it, unit) } ?: "—")
                StatCell(
                    "Variación",
                    state.stats.deltaAbs?.let { delta ->
                        val sign = if (delta > 0) "+" else ""
                        "$sign$delta ${unit.wire}"
                    } ?: "—",
                )
            }
        }

        if (!state.loading && state.series.isEmpty()) {
            EmptyState(
                message = "No hay medidas de ${Body.label(state.kind).lowercase()} en el periodo elegido.",
            )
        }

        if (state.series.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${Body.label(state.kind)} · ${state.series.size} mediciones",
                        style = MaterialTheme.typography.bodySmall,
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
            }

            state.series.reversed().forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = formatDay(point.measuredAtMs), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = Body.formatValue(point.value, point.unit),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        TextButton(onClick = { editing = point }) { Text("Editar") }
                        TextButton(onClick = { viewModel.delete(point.id) }) {
                            Text(text = "Borrar", color = MaterialTheme.colorScheme.error)
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
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
