package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.WorkoutSession
import com.fitlog.app.data.WorkoutSet
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.domain.WorkoutSummary
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.MorphActionButton
import com.fitlog.app.ui.motion.MorphingBlob

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
    initialRoutineId: String? = null,
    autoStart: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Un id vacio (autoarranque desde Inicio sin rutina) es una sesion libre, no un id invalido.
    val routineId = initialRoutineId?.takeIf { it.isNotBlank() }

    var routineHandled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(autoStart, routineId) {
        if (autoStart) viewModel.autoStartOnce(routineId)
    }

    if (routineId != null && !routineHandled && !state.loading && state.active == null) {
        routineHandled = true
        viewModel.startSession(initialRoutineId)
    }
    var showExercisePicker by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<CatalogExercise?>(null) }
    var editingSet by remember { mutableStateOf<WorkoutSet?>(null) }

    var weight by remember { mutableStateOf("60") }
    var reps by remember { mutableStateOf("10") }
    var rir by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isWarmup by remember { mutableStateOf(false) }

    val exerciseName = selectedExercise?.name ?: state.exercises.firstOrNull()?.name ?: "Elegir ejercicio"

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
            Text(
                text = state.active?.let { active ->
                    "Sesión en curso" +
                        (active.routineName?.let { " · Rutina: $it" } ?: "")
                } ?: "Sin sesión activa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MorphActionButton(
                    started = state.active != null,
                    onClick = {
                        if (state.active != null) {
                            viewModel.finishSession()
                        } else {
                            viewModel.startSession()
                        }
                    },
                )
            }
        }

        state.error?.let { message -> Text(text = message, color = MaterialTheme.colorScheme.error) }

        if (state.loading) {
            Text(text = "Cargando entrenamientos…", style = MaterialTheme.typography.bodySmall)
        }

        state.active?.let { active ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Box {
                    MorphingBlob(
                        modifier = Modifier.matchParentSize(),
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
                        ),
                        seed = 21,
                        durationMillis = 18000,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SummaryCell("Series efectivas", active.summary.workingSets.toString())
                        SummaryCell("Volumen", "${active.summary.totalVolumeKg.toInt()} kg")
                        SummaryCell("Totales", active.summary.totalSets.toString())
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(exerciseName) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Peso kg") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = reps,
                            onValueChange = { reps = it },
                            label = { Text("Reps") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = rir,
                            onValueChange = { rir = it },
                            label = { Text("RIR") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                        Text(text = "Serie de calentamiento", style = MaterialTheme.typography.bodyMedium)
                    }

                    state.formError?.let { message ->
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            val exercise = selectedExercise ?: state.exercises.firstOrNull()
                            if (exercise == null) {
                                return@Button
                            }
                            viewModel.addSet(
                                exerciseId = exercise.id,
                                weightKg = parseDecimal(weight),
                                reps = parseInt(reps),
                                rir = parseInt(rir),
                                notes = notes.trim().ifEmpty { null },
                                isWarmup = isWarmup,
                            )
                            notes = ""
                            isWarmup = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Registrar serie") }
                }
            }

            if (state.activeSets.isEmpty()) {
                EmptyState(message = "Todavía no hay series en esta sesión.")
            }

            state.activeSets.forEach { set ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${set.setIndex} ${set.exerciseName}" +
                                    if (set.isWarmup) "  ·  CALENTAMIENTO" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${set.weightKg ?: "—"} kg × ${set.reps ?: "—"}" +
                                    (set.rir?.let { " · RIR $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { editingSet = set }) { Text("Editar") }
                            TextButton(onClick = { viewModel.deleteSet(set.id) }) {
                                Text(text = "Borrar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "HISTORIAL",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        if (state.history.isEmpty() && !state.loading) {
            EmptyState(message = "Todavía no registraste entrenamientos.")
        }

        state.history.forEach { session ->
            HistoryCard(session = session, onOpenDetail = { viewModel.openDetail(session.id) })
        }

        state.detail?.let { detail ->
            AlertDialog(
                onDismissRequest = viewModel::closeDetail,
                title = { Text("Detalle de la sesión") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (detail.sets.isEmpty()) {
                            Text("Esta sesión no tiene series registradas.")
                        }
                        detail.sets.forEach { set ->
                            Text(
                                text = "#${set.setIndex} ${set.exerciseName}: " +
                                    "${set.weightKg ?: "—"} kg × ${set.reps ?: "—"}" +
                                    (set.rir?.let { " · RIR $it" } ?: "") +
                                    if (set.isWarmup) " (calentamiento)" else "",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::closeDetail) { Text("Cerrar") }
                },
            )
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = state.exercises,
            onDismiss = { showExercisePicker = false },
            onSelect = { exercise ->
                selectedExercise = exercise
                showExercisePicker = false
            },
        )
    }

    editingSet?.let { set ->
        SetEditDialog(
            set = set,
            onDismiss = { editingSet = null },
            onSave = { newWeight, newReps, newRir ->
                viewModel.updateSet(set.id, newWeight, newReps, newRir, set.notes)
                editingSet = null
            },
        )
    }
}

@Composable
private fun SummaryCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HistoryCard(session: WorkoutSession, onOpenDetail: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(session.startedAt) +
                        if (session.finishedAt == null) "  ·  EN CURSO" else "",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = (WorkoutSummary.formatDuration(session.startedAt, session.finishedAt) ?: "—") +
                        " · ${session.summary.workingSets} series · " +
                        "${session.summary.totalVolumeKg.toInt()} kg",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onOpenDetail) { Text("Detalle") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePickerDialog(
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    visible.forEach { exercise ->
                        TextButton(
                            onClick = { onSelect(exercise) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = exercise.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun SetEditDialog(
    set: WorkoutSet,
    onDismiss: () -> Unit,
    onSave: (Double?, Int?, Int?) -> Unit,
) {
    var weight by remember { mutableStateOf(set.weightKg?.toString() ?: "") }
    var reps by remember { mutableStateOf(set.reps?.toString() ?: "") }
    var rir by remember { mutableStateOf(set.rir?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("#${set.setIndex} ${set.exerciseName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Peso kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = rir,
                    onValueChange = { rir = it },
                    label = { Text("RIR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(parseDecimal(weight), parseInt(reps), parseInt(rir)) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun parseDecimal(raw: String): Double? =
    raw.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

private fun parseInt(raw: String): Int? = raw.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

private fun formatTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}
