package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.LabeledValue
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.motion.sharedNavBounds
import com.fitlog.app.ui.motion.sharedNavElement
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Detalle de un ejercicio: encabezado con la marca de propio y ficha con sus datos.
 *
 * La tarjeta y el nombre conservan las claves de transicion compartida con el catalogo, asi el
 * elemento viaja entre las dos pantallas.
 */
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
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.loading) {
            LoadingState(message = "Cargando ejercicio…")
        }

        if (!state.loading && exercise == null) {
            ErrorState(message = "El ejercicio no existe.")
        }

        exercise?.let { current ->
            val groupName = groups[current.muscleGroupId]?.name ?: "Sin grupo"
            val secondaryGroupName = current.secondaryMuscleGroupId?.let { id -> groups[id]?.name }

            FitLogCard(
                modifier = Modifier.sharedNavBounds("exercise-card-${current.id}"),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.sharedNavElement("exercise-name-${current.id}"),
                    )
                    if (current.isCustom) {
                        CustomBadge()
                    }
                }
                Text(
                    text = "$groupName · ${current.equipment} · ${kindLabel(current.kind)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FitLogCard {
                SectionHeader(title = "Ficha")
                LabeledValue(label = "Tipo", value = kindLabel(current.kind))
                LabeledValue(label = "Grupo muscular", value = groupName)
                secondaryGroupName?.let { name ->
                    LabeledValue(label = "Grupo secundario", value = name)
                }
                LabeledValue(label = "Equipamiento", value = current.equipment)
            }
        }
    }
}

/** Marca de ejercicio propio, con el token de aviso. */
@Composable
private fun CustomBadge() {
    val fitLog = MaterialTheme.fitLogColors
    Surface(
        shape = MaterialTheme.shapes.small,
        color = fitLog.warningSoft,
        contentColor = fitLog.warning,
    ) {
        Text(
            text = "PROPIO",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
        )
    }
}
