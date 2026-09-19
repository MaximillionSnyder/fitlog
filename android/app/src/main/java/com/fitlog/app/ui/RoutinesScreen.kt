package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.Routine
import com.fitlog.app.data.RoutineExerciseInput
import com.fitlog.app.data.RoutinesRepository
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogText
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutinesScreen(
    onTrainRoutine: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Routine?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(text = "Plantillas para arrancar a entrenar", style = MaterialTheme.typography.bodySmall)

        state.error?.let { message -> ErrorState(message = message, onRetry = { viewModel.load() }) }

        FitLogCard {
                SectionHeader(title = "Nueva rutina")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la rutina") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.formError?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.fitLogColors.danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                PrimaryAction(
                    label = "Crear rutina",
                    icon = FitLogIcons.Plus,
                    onClick = {
                        viewModel.createRoutine(name, description.ifBlank { null })
                        name = ""
                        description = ""
                    },
                )
        }

        if (state.loading) {
            LoadingState(message = "Cargando rutinas…")
        }

        if (!state.loading && state.routines.isEmpty()) {
            EmptyState(
                title = "Todavía no tenés rutinas",
                message = "Armá tu primera plantilla con el formulario de arriba.",
            )
        }

        if (state.routines.isNotEmpty()) {
            SectionHeader(
                title = "Tus rutinas",
                trailing = Format.integer(state.routines.size) + " en total",
            )
        }

        state.routines.forEach { routine ->
            FitLogCard {
                Text(text = routine.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = (routine.description ?: "Sin descripción") +
                        " · ${Format.integer(routine.exercises.size)} ejercicios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PrimaryAction(
                        label = "Entrenar",
                        icon = FitLogIcons.Play,
                        onClick = { onTrainRoutine(routine.id) },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryAction(label = "Editar", onClick = { editing = routine })
                    SecondaryAction(
                        label = if (expandedId == routine.id) "Ocultar" else "Ejercicios",
                        onClick = {
                            expandedId = if (expandedId == routine.id) null else routine.id
                        },
                    )
                    SecondaryAction(
                        label = "Eliminar",
                        onClick = { viewModel.deleteRoutine(routine.id) },
                    )
                }

                if (expandedId == routine.id) {
                    SectionHeader(title = "Ejercicios de la rutina")
                    RoutineExercisesEditor(
                        routine = routine,
                        exercises = state.exercises,
                        formError = state.formError,
                        onAdd = { input -> viewModel.addExercise(routine.id, input) },
                        onRemove = viewModel::removeExercise,
                        onMoveUp = { id -> viewModel.moveExercise(id, RoutinesRepository.MoveDirection.UP) },
                        onMoveDown = { id -> viewModel.moveExercise(id, RoutinesRepository.MoveDirection.DOWN) },
                    )
                }
            }
        }
    }

    editing?.let { routine ->
        EditRoutineDialog(
            routine = routine,
            onDismiss = { editing = null },
            onSave = { newName, newDescription ->
                viewModel.updateRoutine(routine.id, newName, newDescription)
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineExercisesEditor(
    routine: Routine,
    exercises: List<CatalogExercise>,
    formError: String?,
    onAdd: (RoutineExerciseInput) -> Unit,
    onRemove: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
) {
    var selectedExerciseId by remember { mutableStateOf(exercises.firstOrNull()?.id ?: "") }
    var targetSets by remember { mutableStateOf("4") }
    var targetReps by remember { mutableStateOf("8") }
    var targetWeight by remember { mutableStateOf("") }
    var restSeconds by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    val selectedName = exercises.firstOrNull { it.id == selectedExerciseId }?.name ?: "Elegir ejercicio"

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (routine.exercises.isEmpty()) {
            Text(
                text = "La rutina todavía no tiene ejercicios.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        routine.exercises.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${item.position}. ${item.exerciseName}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${item.targetSets ?: "—"} × ${item.targetReps ?: "—"}" +
                            (item.targetWeightKg?.let { " · $it kg" } ?: "") +
                            (item.restSeconds?.let { " · ${it}s" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    TextButton(onClick = { onMoveUp(item.id) }) { Text("↑") }
                    TextButton(onClick = { onMoveDown(item.id) }) { Text("↓") }
                    TextButton(onClick = { onRemove(item.id) }) {
                        Text(text = "✕", color = MaterialTheme.fitLogColors.danger)
                    }
                }
            }
        }

        SecondaryAction(
            label = selectedName,
            icon = FitLogIcons.Dumbbell,
            onClick = { showPicker = true },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = targetSets,
                onValueChange = { targetSets = it },
                label = { Text("Series") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = targetReps,
                onValueChange = { targetReps = it },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = it },
                label = { Text("Peso") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = restSeconds,
                onValueChange = { restSeconds = it },
                label = { Text("Desc.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        formError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.fitLogColors.danger,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        PrimaryAction(
            label = "Agregar ejercicio",
            icon = FitLogIcons.Plus,
            onClick = {
                if (selectedExerciseId.isNotEmpty()) {
                    onAdd(
                        RoutineExerciseInput(
                            exerciseId = selectedExerciseId,
                            targetSets = targetSets.trim().toIntOrNull(),
                            targetReps = targetReps.trim().toIntOrNull(),
                            targetWeightKg = targetWeight.trim().replace(',', '.').toDoubleOrNull(),
                            restSeconds = restSeconds.trim().toIntOrNull(),
                            notes = null,
                        )
                    )
                    targetWeight = ""
                    restSeconds = ""
                }
            },
        )
    }

    if (showPicker) {
        RoutineExercisePickerDialog(
            exercises = exercises,
            onDismiss = { showPicker = false },
            onSelect = { exercise ->
                selectedExerciseId = exercise.id
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineExercisePickerDialog(
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

@Composable
private fun EditRoutineDialog(
    routine: Routine,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf(routine.name) }
    var description by remember { mutableStateOf(routine.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar rutina") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, description.ifBlank { null }) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
