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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.motion.MorphActionButton
import com.fitlog.app.ui.motion.MorphingBlob
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors
import kotlinx.coroutines.delay

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
    // La duracion de la sesion activa se refresca sola cada 30 s mientras haya una en curso.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val activeId = state.active?.id
    LaunchedEffect(activeId) {
        if (activeId == null) return@LaunchedEffect
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(30_000)
        }
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
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
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
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
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

        state.error?.let { message -> ErrorState(message = message, onRetry = { viewModel.load() }) }

        if (state.loading) {
            LoadingState(message = "Cargando entrenamientos…")
        }

        state.active?.let { active ->
            SessionSummaryCard(active = active, nowMs = nowMs)

            FitLogCard {
                SectionHeader(title = "Registrar serie")
                SecondaryAction(
                    label = exerciseName,
                    onClick = { showExercisePicker = true },
                    icon = FitLogIcons.Dumbbell,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                    Text(
                        text = message,
                        color = MaterialTheme.fitLogColors.danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                PrimaryAction(
                    label = "Registrar serie",
                    icon = FitLogIcons.Plus,
                    onClick = {
                        val exercise = selectedExercise ?: state.exercises.firstOrNull()
                        if (exercise != null) {
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
                        }
                    },
                )
            }

            if (state.activeSets.isEmpty()) {
                EmptyState(message = "Todavía no hay series en esta sesión.")
            }

            state.activeSets.forEach { set ->
                SetRow(
                    set = set,
                    onEdit = { editingSet = set },
                    onDelete = { viewModel.deleteSet(set.id) },
                )
            }
        }

        SectionHeader(
            title = "Historial",
            trailing = if (state.history.isEmpty()) null else Format.integer(state.history.size) + " sesiones",
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

/**
 * Resumen de la sesion en curso: duracion, series y volumen, con el fondo de marca animado.
 */
@Composable
private fun SessionSummaryCard(active: WorkoutSession, nowMs: Long) {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard(containerColor = fitLog.accentSoft) {
        Box {
            MorphingBlob(
                modifier = Modifier.matchParentSize(),
                colors = listOf(
                    fitLog.accent.copy(alpha = 0.16f),
                    fitLog.accent.copy(alpha = 0.02f),
                ),
                seed = 21,
                durationMillis = 18000,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Sesión activa")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SummaryCell(
                        label = "Duración",
                        value = Format.duration((nowMs - active.startedAt).coerceAtLeast(0L)),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCell(
                        label = "Series",
                        value = Format.integer(active.summary.workingSets),
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCell(
                        label = "Volumen",
                        value = "${Format.volumeKg(active.summary.totalVolumeKg)} kg",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCell(
                        label = "Totales",
                        value = Format.integer(active.summary.totalSets),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

/** Fila de una serie registrada: datos, edicion y borrado. */
@Composable
private fun SetRow(set: WorkoutSet, onEdit: () -> Unit, onDelete: () -> Unit) {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "#${set.setIndex} ${set.exerciseName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (set.isWarmup) {
                        WarmupBadge()
                    }
                }
                Text(
                    text = "${Format.kg(set.weightKg)} kg × ${set.reps ?: "—"}" +
                        (set.rir?.let { " · RIR $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) {
                    Text(text = "Borrar", color = fitLog.danger)
                }
            }
        }
    }
}

/** Marca de serie de calentamiento. */
@Composable
private fun WarmupBadge() {
    val fitLog = MaterialTheme.fitLogColors
    Surface(
        shape = MaterialTheme.shapes.small,
        color = fitLog.warningSoft,
        contentColor = fitLog.warning,
    ) {
        Text(
            text = "CALENTAMIENTO",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
        )
    }
}

@Composable
private fun HistoryCard(session: WorkoutSession, onOpenDetail: () -> Unit) {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTimestamp(session.startedAt),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (session.finishedAt == null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = fitLog.accentSoft,
                            contentColor = fitLog.accentText,
                        ) {
                            Text(
                                text = "EN CURSO",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = (WorkoutSummary.formatDuration(session.startedAt, session.finishedAt) ?: "—") +
                        " · ${Format.integer(session.summary.workingSets)} series · " +
                        "${Format.volumeKg(session.summary.totalVolumeKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
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
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
