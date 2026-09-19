package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.domain.Formulas
import com.fitlog.app.domain.Progress
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.sharedNavBounds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val metricLabels = mapOf(
    Progress.Metric.MAX_WEIGHT to "Peso máximo",
    Progress.Metric.VOLUME to "Volumen",
    Progress.Metric.BEST_ONE_REP_MAX to "1RM estimado",
)

private val presetLabels = mapOf(
    Progress.RangePreset.LAST_30_DAYS to "30 días",
    Progress.RangePreset.LAST_90_DAYS to "90 días",
    Progress.RangePreset.ALL to "Todo",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    val selectedExercise = state.exercises.firstOrNull { it.id == state.selectedExerciseId }
    val values = state.points.map { Progress.metricValue(it, state.metric) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Progreso",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.sharedNavBounds("home-progress"),
                )
                Text(
                    text = "Evolución por ejercicio",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onBack) { Text("Volver") }
        }

        state.error?.let { message -> Text(text = message, color = MaterialTheme.colorScheme.error) }

        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedExercise?.name ?: "Elegir ejercicio")
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metricLabels.forEach { (metric, label) ->
                FilterChip(
                    selected = state.metric == metric,
                    onClick = { viewModel.selectMetric(metric) },
                    label = { Text(label) },
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presetLabels.forEach { (preset, label) ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { viewModel.selectPreset(preset) },
                    label = { Text(label) },
                )
            }
        }

        if (state.loading || state.refreshing) {
            Text(text = "Calculando progreso…", style = MaterialTheme.typography.bodySmall)
        }

        if (!state.loading && state.points.isEmpty()) {
            EmptyState(
                message = if (selectedExercise != null) {
                    "No hay series registradas de \"${selectedExercise.name}\" en el rango elegido."
                } else {
                    "Elegí un ejercicio para ver su progreso."
                },
            )
        }

        if (state.points.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${metricLabels[state.metric]} · ${state.points.size} sesiones",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = formatMetric(values.last(), state.metric),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    FitLogLineChart(
                        values = values,
                        labels = state.points.map { formatDay(it.startedAtMs) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        valueFormatter = { formatMetric(it, state.metric) },
                    )
                }
            }

            state.points.reversed().forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = formatDay(point.startedAtMs), style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${point.workingSets} series · " +
                            formatMetric(Progress.metricValue(point, state.metric), state.metric),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showPicker) {
        ProgressExercisePickerDialog(
            exercises = state.exercises,
            onDismiss = { showPicker = false },
            onSelect = { exercise ->
                viewModel.selectExercise(exercise.id)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressExercisePickerDialog(
    exercises: List<CatalogExercise>,
    onDismiss: () -> Unit,
    onSelect: (CatalogExercise) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val normalized = CatalogText.normalize(query)
    val visible = exercises.filter { exercise ->
        normalized.isEmpty() || CatalogText.normalize(exercise.name).contains(normalized)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elegir ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    visible.forEach { exercise ->
                        TextButton(
                            onClick = { onSelect(exercise) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = exercise.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun formatMetric(value: Double, metric: Progress.Metric): String =
    if (metric == Progress.Metric.VOLUME) {
        "${value.roundToInt()} kg"
    } else {
        "${Formulas.roundToTenth(value)} kg"
    }

private fun formatDay(timestampMs: Long): String =
    SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestampMs))
