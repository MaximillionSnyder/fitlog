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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.domain.Formulas
import com.fitlog.app.domain.Progress
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.LabeledValue
import com.fitlog.app.ui.components.SecondaryAction
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
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(title = "Evolución por ejercicio")

        state.error?.let { message ->
            ErrorState(message = message, onRetry = { viewModel.refreshSeries() })
        }

        SecondaryAction(
            label = selectedExercise?.name ?: "Elegir ejercicio",
            onClick = { showPicker = true },
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            metricLabels.forEach { (metric, label) ->
                FilterChip(
                    selected = state.metric == metric,
                    onClick = { viewModel.selectMetric(metric) },
                    label = { Text(label) },
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            presetLabels.forEach { (preset, label) ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { viewModel.selectPreset(preset) },
                    label = { Text(label) },
                )
            }
        }

        if (state.loading || state.refreshing) {
            LoadingState(message = "Calculando progreso…")
        }

        if (!state.loading && state.points.isEmpty()) {
            EmptyState(
                title = "Sin datos en el rango",
                message = if (selectedExercise != null) {
                    "No hay series registradas de \"${selectedExercise.name}\" en el rango elegido."
                } else {
                    "Elegí un ejercicio para ver su progreso."
                },
                actionLabel = if (state.preset == Progress.RangePreset.ALL) null else "Ver todo",
                onAction = { viewModel.selectPreset(Progress.RangePreset.ALL) },
            )
        }

        if (state.points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                StatTile(
                    label = metricLabels.getValue(state.metric),
                    value = formatMetric(values.last(), state.metric),
                    icon = FitLogIcons.Chart,
                    accent = MaterialTheme.fitLogColors.data,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Sesiones",
                    value = Format.integer(state.points.size),
                    icon = FitLogIcons.Calendar,
                    modifier = Modifier.weight(1f),
                )
            }

            FitLogCard {
                FitLogLineChart(
                    values = values,
                    labels = state.points.map { formatDay(it.startedAtMs) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    valueFormatter = { formatMetric(it, state.metric) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                state.points.reversed().forEach { point ->
                    LabeledValue(
                        label = formatDay(point.startedAtMs),
                        value = "${Format.integer(point.workingSets)} series · " +
                            formatMetric(Progress.metricValue(point, state.metric), state.metric),
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
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
