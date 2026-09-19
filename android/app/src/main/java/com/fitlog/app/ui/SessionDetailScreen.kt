package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.WorkoutSet
import com.fitlog.app.domain.PaceSetInput
import com.fitlog.app.domain.WorkoutSummary
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Detalle de un entrenamiento: resumen de la sesion y sus series agrupadas por ejercicio.
 */
@Composable
fun SessionDetailScreen(
    sessionId: String,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }

    val detail = state.detail

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.sm,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (state.loading) {
            item { LoadingState(message = "Cargando entrenamiento…") }
        }

        state.error?.let { message ->
            item { ErrorState(message = message, onRetry = { viewModel.refresh(sessionId) }) }
        }

        if (detail != null) {
            val session = detail.session
            val pace = WorkoutSummary.pace(
                detail.sets.map { PaceSetInput(createdAtMs = it.createdAtMs, isWarmup = it.isWarmup) }
            )
            item {
                Text(
                    text = formatSessionTimestamp(session.startedAt),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    StatTile(
                        label = "Duración",
                        value = WorkoutSummary.formatDuration(session.startedAt, session.finishedAt) ?: "En curso",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Series",
                        value = Format.integer(session.summary.workingSets),
                        deltaLabel = "${Format.integer(session.summary.totalSets)} en total",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                StatTile(
                    label = "Volumen",
                    value = Format.volumeKg(session.summary.totalVolumeKg),
                    unit = "kg",
                    accent = MaterialTheme.fitLogColors.data,
                )
            }

            if (pace.workingSets > 0) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        StatTile(
                            label = "Duración real",
                            value = Format.duration(pace.spanMs),
                            deltaLabel = "de la primera a la última serie",
                            modifier = Modifier.weight(1f),
                        )
                        pace.setsPerHour?.let { rate ->
                            StatTile(
                                label = "Ritmo",
                                value = Format.decimal(rate, 1),
                                unit = "series/h",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            pace.averageRestMs?.let { rest ->
                item {
                    StatTile(
                        label = "Descanso promedio",
                        value = Format.duration(rest),
                        deltaLabel = "entre series efectivas",
                    )
                }
            }

            if (session.routineName != null) {
                item {
                    FitLogCard {
                        SectionHeader(title = "Rutina")
                        Text(text = session.routineName, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            session.notes?.let { notes ->
                item {
                    FitLogCard {
                        SectionHeader(title = "Notas")
                        Text(text = notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Series",
                    trailing = Format.integer(detail.sets.size) + " registradas",
                )
            }

            if (detail.sets.isEmpty()) {
                item {
                    EmptyState(
                        title = "Sin series",
                        message = "Esta sesión no tiene series registradas.",
                    )
                }
            }

            val byExercise = detail.sets.groupBy { it.exerciseName }
            byExercise.forEach { (exerciseName, sets) ->
                item(key = "exercise-$exerciseName") {
                    FitLogCard {
                        Text(text = exerciseName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${Format.integer(sets.count { !it.isWarmup })} series efectivas" +
                                (sets.count { it.isWarmup }.let { if (it > 0) " · ${Format.integer(it)} de calentamiento" else "" }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(sets, key = { it.id }) { set ->
                    SetDetailRow(set = set)
                }
            }
        }
    }
}

@Composable
private fun SetDetailRow(set: WorkoutSet) {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#${set.setIndex}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (set.isWarmup) {
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
            }
            Text(
                text = "${Format.kg(set.weightKg)} kg × ${set.reps ?: "—"}" +
                    (set.rir?.let { " · RIR $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        set.notes?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatSessionTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp)).replaceFirstChar { it.uppercase() }
}
