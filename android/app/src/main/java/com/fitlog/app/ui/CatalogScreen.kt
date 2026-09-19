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
import androidx.compose.material3.Button
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
import com.fitlog.app.domain.ExerciseKind
import com.fitlog.app.domain.MuscleGroup

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var detailExercise by remember { mutableStateOf<CatalogExercise?>(null) }

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
                Text(text = "Catálogo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "${state.visible.size} de ${state.exercises.size} ejercicios",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("Volver") }
                Button(onClick = {
                    viewModel.clearFormError()
                    showForm = true
                }) { Text("Nuevo") }
            }
        }

        OutlinedTextField(
            value = state.filters.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Buscar ejercicio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filters.muscleGroupSlug == null,
                onClick = {
                    state.filters.muscleGroupSlug?.let(viewModel::toggleMuscleGroup)
                },
                label = { Text("Todos") },
            )
            state.groups.forEach { group ->
                FilterChip(
                    selected = state.filters.muscleGroupSlug == group.slug,
                    onClick = { viewModel.toggleMuscleGroup(group.slug) },
                    label = { Text(group.name) },
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.equipments.forEach { equipment ->
                FilterChip(
                    selected = state.filters.equipment == equipment,
                    onClick = { viewModel.toggleEquipment(equipment) },
                    label = { Text(equipment) },
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.filters.kind == kind,
                    onClick = { viewModel.toggleKind(kind) },
                    label = { Text(kindLabel(kind)) },
                )
            }
        }

        state.error?.let { message ->
            Text(text = message, color = MaterialTheme.colorScheme.error)
        }

        if (state.loading) {
            Text(text = "Cargando catálogo…", style = MaterialTheme.typography.bodySmall)
        }

        if (!state.loading && state.visible.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No hay ejercicios que coincidan con la búsqueda.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        state.visible.forEach { exercise ->
            Card(
                onClick = { detailExercise = exercise },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = exercise.name, fontWeight = FontWeight.SemiBold)
                            if (exercise.isCustom) {
                                Text(
                                    text = "PROPIO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        val groupName = state.groups.firstOrNull { it.id == exercise.muscleGroupId }?.name
                            ?: "Sin grupo"
                        Text(
                            text = "$groupName · ${exercise.equipment} · ${kindLabel(exercise.kind)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (exercise.isCustom) {
                        TextButton(onClick = { pendingDelete = exercise.id }) {
                            Text(text = "Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        NewExerciseDialog(
            groups = state.groups,
            formError = state.formError,
            onDismiss = { showForm = false },
            onConfirm = { name, groupId, equipment, kind ->
                viewModel.createCustomExercise(name, groupId, equipment, kind)
            },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar ejercicio") },
            text = { Text("El ejercicio dejará de aparecer en el catálogo.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomExercise(id)
                    pendingDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }

    detailExercise?.let { exercise ->
        ExerciseDetailDialog(
            exercise = exercise,
            groups = state.groups,
            onDismiss = { detailExercise = null },
        )
    }
}

@Composable
private fun ExerciseDetailDialog(
    exercise: CatalogExercise,
    groups: List<MuscleGroup>,
    onDismiss: () -> Unit,
) {
    val groupName = groups.firstOrNull { it.id == exercise.muscleGroupId }?.name ?: "Sin grupo"
    val secondaryGroupName = exercise.secondaryMuscleGroupId?.let { id ->
        groups.firstOrNull { it.id == id }?.name
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = exercise.name)
                if (exercise.isCustom) {
                    Text(
                        text = "PROPIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailField(label = "Tipo", value = kindLabel(exercise.kind))
                DetailField(label = "Grupo muscular", value = groupName)
                secondaryGroupName?.let { name ->
                    DetailField(label = "Grupo secundario", value = name)
                }
                DetailField(label = "Equipamiento", value = exercise.equipment)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewExerciseDialog(
    groups: List<MuscleGroup>,
    formError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, ExerciseKind) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    var equipment by remember { mutableStateOf("mancuernas") }
    var kind by remember { mutableStateOf(ExerciseKind.STRENGTH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo ejercicio propio") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                )
                Text(
                    text = "Grupo muscular: ${groups.firstOrNull { it.id == selectedGroupId }?.name ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = selectedGroupId == group.id,
                            onClick = { selectedGroupId = group.id },
                            label = { Text(group.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Equipamiento") },
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(kindLabel(option)) },
                        )
                    }
                }
                formError?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, selectedGroupId, equipment, kind) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun kindLabel(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> "Fuerza"
    ExerciseKind.CARDIO -> "Cardio"
    ExerciseKind.MOBILITY -> "Movilidad"
}
