package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.fitlog.app.ui.motion.sharedNavBounds
import com.fitlog.app.ui.motion.sharedNavElement

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    catalogEntry: NavBackStackEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = hiltViewModel(catalogEntry),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercises.firstOrNull { it.id == exerciseId }
    val groups = state.groups.associateBy { it.id }

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
                text = "Detalle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(onClick = onBack) { Text("Volver") }
        }

        if (state.loading) {
            Text(text = "Cargando ejercicio…", style = MaterialTheme.typography.bodySmall)
        }

        if (!state.loading && exercise == null) {
            Text(
                text = "El ejercicio no existe.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        exercise?.let { current ->
            val groupName = groups[current.muscleGroupId]?.name ?: "Sin grupo"
            val secondaryGroupName = current.secondaryMuscleGroupId?.let { id -> groups[id]?.name }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedNavBounds("exercise-card-${current.id}"),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = current.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.sharedNavElement("exercise-name-${current.id}"),
                        )
                        if (current.isCustom) {
                            Text(
                                text = "PROPIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                    Text(
                        text = "$groupName · ${current.equipment} · ${kindLabel(current.kind)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DetailField(label = "Tipo", value = kindLabel(current.kind))
                    DetailField(label = "Grupo muscular", value = groupName)
                    secondaryGroupName?.let { name ->
                        DetailField(label = "Grupo secundario", value = name)
                    }
                    DetailField(label = "Equipamiento", value = current.equipment)
                }
            }
        }
    }
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
