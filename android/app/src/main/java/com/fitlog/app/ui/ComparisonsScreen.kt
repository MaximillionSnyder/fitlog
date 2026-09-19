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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Comparisons
import com.fitlog.app.ui.motion.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "PRs, periodos y balance muscular", style = MaterialTheme.typography.bodySmall)

        state.error?.let { message -> Text(text = message, color = MaterialTheme.colorScheme.error) }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(text = "Calculando comparativas…", style = MaterialTheme.typography.bodySmall)
        }

        state.comparison?.let { comparison ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DeltaCell("Volumen", formatKg(comparison.current.volumeKg), comparison.volumeDeltaPct)
                    DeltaCell("Series", comparison.current.workingSets.toString(), comparison.setsDeltaPct)
                    DeltaCell("Sesiones", comparison.current.sessions.toString(), comparison.sessionsDeltaPct)
                }
            }
        }

        Text(
            text = "MARCAS PERSONALES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        if (!state.loading && state.records.isEmpty()) {
            EmptyState(message = "Todavía no hay marcas registradas.")
        }

        state.records.forEach { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = exerciseNames[record.exerciseId] ?: record.exerciseId,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Peso: ${formatKg(record.bestWeightKg)} · ${formatDate(record.bestWeightAtMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "1RM: ${formatKg(record.bestOneRepMaxKg)} · ${formatDate(record.bestOneRepMaxAtMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Volumen: ${formatKg(record.bestSessionVolumeKg)} · " +
                            formatDate(record.bestSessionVolumeAtMs),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Reps: ${record.bestReps} · ${formatDate(record.bestRepsAtMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Text(
            text = "BALANCE MUSCULAR DEL PERIODO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        if (!state.loading && state.balance.isEmpty()) {
            EmptyState(message = "No hay volumen registrado en el periodo elegido.")
        }

        state.balance.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = groupLabel(entry.muscleGroupSlug),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${formatKg(entry.volumeKg)} · ${entry.sharePct}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (entry.sharePct / 100.0).toFloat().coerceIn(0f, 1f))
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun DeltaCell(label: String, value: String, deltaPct: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = Comparisons.formatDelta(deltaPct),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                deltaPct == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                deltaPct > 0 -> Color(0xFF6EE7B7)
                deltaPct < 0 -> Color(0xFFFDA4AF)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private fun groupLabel(slug: String): String = when (slug) {
    "sin-grupo" -> "Sin grupo"
    else -> slug.replaceFirstChar { it.uppercase() }
}

private fun formatKg(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()} kg" else "$rounded kg"
}

private fun formatDate(timestampMs: Long?): String {
    if (timestampMs == null) return "—"
    return SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestampMs))
}
