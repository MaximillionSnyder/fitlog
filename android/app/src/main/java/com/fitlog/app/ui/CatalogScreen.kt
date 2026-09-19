package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.ExerciseKind
import com.fitlog.app.domain.MuscleGroup
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.motion.sharedNavBounds
import com.fitlog.app.ui.motion.sharedNavElement
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Catálogo de ejercicios con las primitivas del sistema: encabezado con el contador y el alta,
 * formulario en tarjeta, filtros en chips y una tarjeta por ejercicio con su transición compartida.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${Format.integer(state.visible.size)} de " +
                    "${Format.integer(state.exercises.size)} ejercicios",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    viewModel.clearFormError()
                    showForm = !showForm
                },
            ) {
                if (!showForm) {
                    Icon(
                        imageVector = FitLogIcons.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.lg),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                }
                Text(
                    text = if (showForm) "Cerrar" else "Nuevo propio",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (showForm) {
            NewExerciseForm(
                groups = state.groups,
                formError = state.formError,
                onDismiss = { showForm = false },
                onConfirm = { name, groupId, equipment, kind ->
                    viewModel.createCustomExercise(name, groupId, equipment, kind)
                },
            )
        }

        OutlinedTextField(
            value = state.filters.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Buscar ejercicio") },
            leadingIcon = {
                Icon(imageVector = FitLogIcons.Search, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            state.equipments.forEach { equipment ->
                FilterChip(
                    selected = state.filters.equipment == equipment,
                    onClick = { viewModel.toggleEquipment(equipment) },
                    label = { Text(equipment) },
                )
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ExerciseKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.filters.kind == kind,
                    onClick = { viewModel.toggleKind(kind) },
                    label = { Text(kindLabel(kind)) },
                )
            }
        }

        state.error?.let { message ->
            ErrorState(message = message, onRetry = { viewModel.load() })
        }

        if (state.loading) {
            LoadingState(message = "Cargando catálogo…")
        }

        if (!state.loading && state.visible.isEmpty()) {
            EmptyState(
                title = "Sin resultados",
                message = "No hay ejercicios que coincidan con la búsqueda.",
                actionLabel = "Limpiar filtros",
                onAction = { viewModel.clearFilters() },
            )
        }

        state.visible.forEach { exercise ->
            val groupName = state.groups.firstOrNull { it.id == exercise.muscleGroupId }?.name
                ?: "Sin grupo"
            FitLogCard(
                modifier = Modifier.sharedNavBounds("exercise-card-${exercise.id}"),
                onClick = { onOpenDetail(exercise.id) },
            ) {
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
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.sharedNavElement("exercise-name-${exercise.id}"),
                            )
                            if (exercise.isCustom) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.fitLogColors.warningSoft,
                                    contentColor = MaterialTheme.fitLogColors.warning,
                                ) {
                                    Text(
                                        text = "PROPIO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.fitLogColors.warning,
                                        modifier = Modifier.padding(
                                            horizontal = Spacing.sm,
                                            vertical = Spacing.xs,
                                        ),
                                    )
                                }
                            }
                        }
                        Text(
                            text = "$groupName · ${exercise.equipment} · ${kindLabel(exercise.kind)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (exercise.isCustom) {
                        TextButton(onClick = { pendingDelete = exercise.id }) {
                            Text(
                                text = "Eliminar",
                                color = MaterialTheme.fitLogColors.danger,
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar ejercicio") },
            text = { Text("El ejercicio dejará de aparecer en el catálogo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomExercise(id)
                        pendingDelete = null
                    },
                ) {
                    Text(text = "Eliminar", color = MaterialTheme.fitLogColors.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewExerciseForm(
    groups: List<MuscleGroup>,
    formError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, ExerciseKind) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    var equipment by remember { mutableStateOf("mancuernas") }
    var kind by remember { mutableStateOf(ExerciseKind.STRENGTH) }

    FitLogCard {
        Text(text = "Nuevo ejercicio propio", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Grupo muscular: ${groups.firstOrNull { it.id == selectedGroupId }?.name ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ExerciseKind.entries.forEach { option ->
                FilterChip(
                    selected = kind == option,
                    onClick = { kind = option },
                    label = { Text(kindLabel(option)) },
                )
            }
        }
        formError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.fitLogColors.danger,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PrimaryAction(
            label = "Guardar",
            icon = FitLogIcons.Plus,
            onClick = { onConfirm(name, selectedGroupId, equipment, kind) },
        )
        SecondaryAction(label = "Cancelar", onClick = onDismiss)
    }
}

internal fun kindLabel(kind: ExerciseKind): String = when (kind) {
    ExerciseKind.STRENGTH -> "Fuerza"
    ExerciseKind.CARDIO -> "Cardio"
    ExerciseKind.MOBILITY -> "Movilidad"
}
