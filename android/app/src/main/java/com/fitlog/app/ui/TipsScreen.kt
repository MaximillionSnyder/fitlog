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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Comparisons
import com.fitlog.app.domain.Insights
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.SecondaryAction
import com.fitlog.app.ui.motion.EmptyState
import com.fitlog.app.ui.motion.ErrorState
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Observaciones del periodo: un tip por tarjeta, con el contenedor tonal segun la severidad.
 *
 * El shell ya muestra el titulo de la pantalla, asi que aca solo vive el subtitulo, los filtros de
 * periodo y la lista de tips. Toda la logica y los textos se mantienen igual que antes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TipsScreen(
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
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.lg,
                bottom = Spacing.xxl,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = "Observaciones con reglas fijas",
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
            LoadingState(message = "Analizando entrenamientos…")
        }

        if (!state.loading && state.tips.isEmpty()) {
            EmptyState(
                title = "Sin observaciones",
                message = "No hay observaciones para este periodo. Seguí registrando entrenamientos.",
                actionLabel = if (state.preset == Comparisons.Preset.LAST_90_DAYS) null else "Ampliar a 90 días",
                onAction = { viewModel.selectPreset(Comparisons.Preset.LAST_90_DAYS) },
            )
        }

        val visible = if (showAll) state.tips else state.tips.take(Insights.TIP_LIMIT)

        visible.forEach { tip ->
            TipCard(
                tip = tip,
                subjectLabel = tip.subject?.let { subject ->
                    if (tip.kind == Insights.Kind.IMBALANCE) {
                        groupNames[subject] ?: subject
                    } else {
                        exerciseNames[subject] ?: subject
                    }
                },
            )
        }

        if (state.tips.size > Insights.TIP_LIMIT) {
            SecondaryAction(
                label = if (showAll) {
                    "Mostrar menos"
                } else {
                    "Ver los ${state.tips.size} consejos"
                },
                onClick = { showAll = !showAll },
            )
        }
    }
}

/** Etiqueta del chip de severidad, ya en mayusculas como el resto de los rotulos. */
private fun severityLabel(severity: Insights.Severity): String = when (severity) {
    Insights.Severity.WARNING -> "ATENCIÓN"
    Insights.Severity.INFO -> "INFO"
    Insights.Severity.SUCCESS -> "LOGRO"
}

/** Tarjeta de un tip: contenedor tonal, chip de severidad, sujeto y mensaje. */
@Composable
private fun TipCard(
    tip: Insights.Tip,
    subjectLabel: String?,
) {
    val fitLog = MaterialTheme.fitLogColors
    val container = when (tip.severity) {
        Insights.Severity.WARNING -> fitLog.warningSoft
        Insights.Severity.INFO -> fitLog.dataSoft
        Insights.Severity.SUCCESS -> fitLog.successSoft
    }
    val tint = when (tip.severity) {
        Insights.Severity.WARNING -> fitLog.warning
        Insights.Severity.INFO -> fitLog.data
        Insights.Severity.SUCCESS -> fitLog.success
    }

    FitLogCard(containerColor = container) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = severityLabel(tip.severity),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
            )
            if (subjectLabel != null) {
                Text(
                    text = subjectLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = tip.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
