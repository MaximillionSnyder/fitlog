package com.fitlog.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.domain.Formulas
import com.fitlog.app.domain.Progress
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
                Text(text = "Progreso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (selectedExercise != null) {
                        "No hay series registradas de \"${selectedExercise.name}\" en el rango elegido."
                    } else {
                        "Elegí un ejercicio para ver su progreso."
                    },
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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

                    ProgressLineChart(
                        values = values,
                        labels = state.points.map { formatDay(it.startedAtMs) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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

@Composable
private fun ProgressLineChart(
    values: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = labelColor, fontSize = 10.sp)

    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val minValue = values.min()
        val maxValue = values.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val leftPad = 12.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        fun xAt(index: Int): Float =
            if (values.size == 1) leftPad + chartWidth / 2
            else leftPad + chartWidth * index / (values.size - 1)

        fun yAt(value: Double): Float =
            topPad + chartHeight * (1 - ((value - minValue) / span)).toFloat()

        for (step in 0..2) {
            val y = topPad + chartHeight * step / 2
            drawLine(gridColor, Offset(leftPad, y), Offset(leftPad + chartWidth, y), strokeWidth = 1f)
        }

        if (values.size > 1) {
            val path = Path()
            values.forEachIndexed { index, value ->
                if (index == 0) path.moveTo(xAt(index), yAt(value)) else path.lineTo(xAt(index), yAt(value))
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        values.forEachIndexed { index, value ->
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(xAt(index), yAt(value)))
        }

        drawText(
            textMeasurer = textMeasurer,
            text = formatMetric(maxValue, Progress.Metric.MAX_WEIGHT),
            topLeft = Offset(leftPad, 0f),
            style = labelStyle,
        )
        drawText(
            textMeasurer = textMeasurer,
            text = formatMetric(minValue, Progress.Metric.MAX_WEIGHT),
            topLeft = Offset(leftPad, topPad + chartHeight - 12.dp.toPx()),
            style = labelStyle,
        )
        labels.firstOrNull()?.let { first ->
            drawText(
                textMeasurer = textMeasurer,
                text = first,
                topLeft = Offset(leftPad, topPad + chartHeight + 4.dp.toPx()),
                style = labelStyle,
            )
        }
        labels.lastOrNull()?.let { last ->
            val measured = textMeasurer.measure(last, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = last,
                topLeft = Offset(
                    leftPad + chartWidth - measured.size.width,
                    topPad + chartHeight + 4.dp.toPx(),
                ),
                style = labelStyle,
            )
        }
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
