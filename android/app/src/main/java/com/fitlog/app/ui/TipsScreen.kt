package com.fitlog.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Comparisons
import com.fitlog.app.domain.Insights
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.sharedNavBounds

private val severityColors = mapOf(
    Insights.Severity.WARNING to Color(0xFFFBBF24),
    Insights.Severity.INFO to Color(0xFF7DD3FC),
    Insights.Severity.SUCCESS to Color(0xFF6EE7B7),
)

private val severityLabels = mapOf(
    Insights.Severity.WARNING to "Atención",
    Insights.Severity.INFO to "Info",
    Insights.Severity.SUCCESS to "Logro",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TipsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TipsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAll by remember { mutableStateOf(false) }

    val exerciseNames = state.exercises.associate { it.id to it.name }
    val groupNames = state.muscleGroups.associate { it.slug to it.name }

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
                Text(
                    text = "Tips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.sharedNavBounds("home-tips"),
                )
                Text(
                    text = "Observaciones con reglas fijas",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = onBack) { Text("Volver") }
        }

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
            Text(text = "Analizando entrenamientos…", style = MaterialTheme.typography.bodySmall)
        }

        if (!state.loading && state.tips.isEmpty()) {
            EmptyState(
                message = "No hay observaciones para este periodo. Seguí registrando entrenamientos.",
            )
        }

        val visible = if (showAll) state.tips else state.tips.take(Insights.TIP_LIMIT)

        visible.forEach { tip ->
            val color = severityColors[tip.severity] ?: MaterialTheme.colorScheme.primary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = (severityLabels[tip.severity] ?: "").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                        )
                        tip.subject?.let { subject ->
                            Text(
                                text = if (tip.kind == Insights.Kind.IMBALANCE) {
                                    groupNames[subject] ?: subject
                                } else {
                                    exerciseNames[subject] ?: subject
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(text = tip.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (state.tips.size > Insights.TIP_LIMIT) {
            TextButton(onClick = { showAll = !showAll }) {
                Text(
                    text = if (showAll) {
                        "Mostrar menos"
                    } else {
                        "Ver los ${state.tips.size} consejos"
                    }
                )
            }
        }
    }
}
