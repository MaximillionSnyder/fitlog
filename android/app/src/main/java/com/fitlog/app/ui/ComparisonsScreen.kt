package com.fitlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Comparisons
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.LabeledValue
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

/**
 * Comparativas: deltas del periodo, marcas personales por ejercicio y balance muscular.
 *
 * La pantalla no arma su propio encabezado (el shell ya muestra "Comparativas"): empieza con los
 * filtros de periodo y baja en secciones con el mismo sistema visual que el resto de la app.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComparisonsScreen(
    modifier: Modifier = Modifier,
    viewModel: ComparisonsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exerciseNames = state.exercises.associate { it.id to it.name }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "PRs, periodos y balance muscular",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.error?.let { message ->
            ErrorState(message = message, onRetry = { viewModel.load() })
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = state.preset == Comparisons.Preset.LAST_30_DAYS,
                onClick = { viewModel.selectPreset(Comparisons.Preset.LAST_30_DAYS) },
                label = { Text("30 días") },
            )
            FilterChip(
                selected = state.preset == Comparisons.Preset.LAST_90_DAYS,
                onClick = { viewModel.selectPreset(Comparisons.Preset.LAST_90_DAYS) },
                label = { Text("90 días") },
            )
        }

        if (state.loading) {
            LoadingState(message = "Calculando comparativas…")
        }

        state.comparison?.let { comparison ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                StatTile(
                    label = "Volumen",
                    value = Format.volumeKg(comparison.current.volumeKg),
                    unit = "kg",
                    deltaPercent = comparison.volumeDeltaPct,
                    deltaLabel = deltaFallbackLabel(comparison.volumeDeltaPct),
                    accent = MaterialTheme.fitLogColors.data,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Series",
                    value = Format.integer(comparison.current.workingSets),
                    deltaPercent = comparison.setsDeltaPct,
                    deltaLabel = deltaFallbackLabel(comparison.setsDeltaPct),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = "Sesiones",
                    value = Format.integer(comparison.current.sessions),
                    deltaPercent = comparison.sessionsDeltaPct,
                    deltaLabel = deltaFallbackLabel(comparison.sessionsDeltaPct),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionHeader(title = "Marcas personales")

        if (!state.loading && state.records.isEmpty()) {
            EmptyState(message = "Todavía no hay marcas registradas.")
        }

        state.records.forEach { record ->
            FitLogCard {
                Text(
                    text = exerciseNames[record.exerciseId] ?: record.exerciseId,
                    style = MaterialTheme.typography.titleMedium,
                )
                LabeledValue(
                    label = "Peso",
                    value = "${Format.kg(record.bestWeightKg)} kg · ${formatDate(record.bestWeightAtMs)}",
                )
                LabeledValue(
                    label = "1RM",
                    value = "${Format.kg(record.bestOneRepMaxKg)} kg · ${formatDate(record.bestOneRepMaxAtMs)}",
                )
                LabeledValue(
                    label = "Volumen",
                    value = "${Format.kg(record.bestSessionVolumeKg)} kg · " +
                        formatDate(record.bestSessionVolumeAtMs),
                )
                LabeledValue(
                    label = "Reps",
                    value = "${Format.integer(record.bestReps)} · ${formatDate(record.bestRepsAtMs)}",
                )
            }
        }

        SectionHeader(title = "Balance muscular del periodo")

        if (!state.loading && state.balance.isEmpty()) {
            EmptyState(message = "No hay volumen registrado en el periodo elegido.")
        }

        state.balance.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = groupLabel(entry.muscleGroupSlug),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${Format.volumeKg(entry.volumeKg)} kg · ${entry.sharePct}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.small,
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (entry.sharePct / 100.0).toFloat().coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(
                                color = MaterialTheme.fitLogColors.data,
                                shape = MaterialTheme.shapes.small,
                            )
                    )
                }
            }
        }
    }
}

/** Texto de la variacion cuando no hay periodo previo con el que comparar. */
private fun deltaFallbackLabel(percent: Double?): String? =
    if (percent == null) "sin datos" else null

private fun groupLabel(slug: String): String = when (slug) {
    "sin-grupo" -> "Sin grupo"
    else -> slug.replaceFirstChar { it.uppercase() }
}

private fun formatDate(timestampMs: Long?): String {
    if (timestampMs == null) return "—"
    return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestampMs))
}
