package com.fitlog.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Home
import com.fitlog.app.ui.components.FitLogCard
import com.fitlog.app.ui.components.FitLogIcons
import com.fitlog.app.ui.components.Format
import com.fitlog.app.ui.components.NavigationRow
import com.fitlog.app.ui.components.PrimaryAction
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.motion.LoadingState
import com.fitlog.app.ui.motion.MorphingBlob
import com.fitlog.app.ui.theme.Spacing
import com.fitlog.app.ui.theme.fitLogColors

/**
 * Panel de Inicio: hero con la accion de entrenamiento, sesion activa, estadisticas de la semana y
 * accesos directos. Ya no es un indice de botones: el menu vive en la barra inferior y en "Más".
 */
@Composable
fun HomeScreen(
    onStartWorkout: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenComparisons: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            item { LoadingState(message = "Preparando tu panel…") }
        }

        if (!state.loading) {
            item {
                HeroCard(
                    greeting = state.greeting,
                    hasActiveSession = state.activeSession != null,
                    onStartWorkout = onStartWorkout,
                    onContinueWorkout = onOpenWorkout,
                )
            }

            state.activeSession?.let { session ->
                item {
                    ActiveSessionCard(
                        name = session.routineName ?: "Entrenamiento libre",
                        workingSets = session.summary.workingSets,
                        volumeKg = session.summary.totalVolumeKg,
                        onContinue = onOpenWorkout,
                    )
                }
            }

            if (state.summary.today.sessions > 0) {
                item { TodayCard(today = state.summary.today, onOpenWorkout = onOpenWorkout) }
            }

            item { SectionHeader(title = "Últimos 7 días", trailing = "vs. 7 anteriores") }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    StatTile(
                        label = "Sesiones",
                        value = Format.integer(state.summary.current.sessions),
                        deltaPercent = state.summary.sessionsDeltaPercent,
                        icon = FitLogIcons.Calendar,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Volumen",
                        value = Format.volumeKg(state.summary.current.volumeKg),
                        unit = "kg",
                        deltaPercent = state.summary.volumeDeltaPercent,
                        icon = FitLogIcons.Chart,
                        accent = MaterialTheme.fitLogColors.data,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    StatTile(
                        label = "Racha",
                        value = Format.integer(state.summary.streakWeeks),
                        unit = if (state.summary.streakWeeks == 1) "semana" else "semanas",
                        icon = FitLogIcons.Spark,
                        accent = MaterialTheme.fitLogColors.accent,
                        modifier = Modifier.weight(1f),
                    )
                    val weight = state.summary.latestBodyWeightKg
                    StatTile(
                        label = "Peso corporal",
                        value = if (weight == null) "—" else Format.kg(weight),
                        unit = if (weight == null) null else "kg",
                        deltaLabel = if (weight == null) "Sin medidas todavía" else "Último registro",
                        icon = FitLogIcons.Scale,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.summary.totalSessions > 0 && state.recentSessions.isNotEmpty()) {
                item { SectionHeader(title = "Recientes") }
                items(state.recentSessions, key = { it.id }) { recent ->
                    RecentSessionRow(recent = recent, onClick = onOpenWorkout)
                }
            }

            if (state.summary.totalSessions == 0) {
                item {
                    StartHintCard(onOpenRoutines = onOpenRoutines)
                }
            }

            item { SectionHeader(title = "Accesos") }

            item {
                NavigationRow(
                    sharedBoundsKey = "home-routines",
                    title = "Rutinas",
                    description = "Tus días de entrenamiento con series y reps objetivo",
                    icon = FitLogIcons.Calendar,
                    onClick = onOpenRoutines,
                )
            }
            item {
                NavigationRow(
                    sharedBoundsKey = "home-progress",
                    title = "Progreso",
                    description = "Evolución por ejercicio: peso máximo, volumen y 1RM",
                    icon = FitLogIcons.Chart,
                    onClick = onOpenProgress,
                )
            }
            item {
                NavigationRow(
                    sharedBoundsKey = "home-comparisons",
                    title = "Comparativas",
                    description = "Récords, mes contra mes y balance muscular",
                    icon = FitLogIcons.Trophy,
                    onClick = onOpenComparisons,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    greeting: String,
    hasActiveSession: Boolean,
    onStartWorkout: () -> Unit,
    onContinueWorkout: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val fitLog = MaterialTheme.fitLogColors
    val transition = rememberInfiniteTransition(label = "hero")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-drift",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 210.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        fitLog.accent.copy(alpha = 0.18f),
                        colors.surfaceContainer,
                        colors.surfaceContainerHigh,
                    )
                )
            ),
    ) {
        MorphingBlob(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .size(190.dp),
            colors = listOf(
                fitLog.accent.copy(alpha = 0.45f),
                Color.Transparent,
            ),
            seed = 5,
            durationMillis = 15000,
        )
        MorphingBlob(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = 28.dp)
                .size(140.dp),
            colors = listOf(
                fitLog.data.copy(alpha = 0.35f),
                Color.Transparent,
            ),
            seed = 11,
            durationMillis = 18000,
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = Spacing.xl, end = Spacing.xl, top = 44.dp, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "$greeting,",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = if (hasActiveSession) "Sesión en curso" else "¿Entrenamos?",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
            )
            Text(
                text = if (hasActiveSession) {
                    "Retomá donde la dejaste."
                } else {
                    "Registrá tus series y mirá cómo progresa tu volumen."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Box(modifier = Modifier.padding(top = Spacing.xs)) {
                PrimaryAction(
                    label = if (hasActiveSession) "Continuar sesión" else "Iniciar entrenamiento",
                    icon = if (hasActiveSession) FitLogIcons.Play else FitLogIcons.Dumbbell,
                    onClick = if (hasActiveSession) onContinueWorkout else onStartWorkout,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Marca de agua con la deriva del gradiente: da vida al hero sin costo de imagenes.
        Text(
            text = "FITLOG",
            style = MaterialTheme.typography.labelSmall,
            color = fitLog.accent.copy(alpha = 0.25f + 0.15f * drift),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = Spacing.xl, top = Spacing.lg),
        )
    }
}

@Composable
private fun TodayCard(today: Home.Window, onOpenWorkout: () -> Unit) {
    FitLogCard(onClick = onOpenWorkout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = FitLogIcons.Spark,
                contentDescription = null,
                tint = MaterialTheme.fitLogColors.accent,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Hoy", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${today.workingSets} series · ${Format.volumeKg(today.volumeKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Ver",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActiveSessionCard(
    name: String,
    workingSets: Int,
    volumeKg: Double,
    onContinue: () -> Unit,
) {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard(
        containerColor = fitLog.accentSoft,
        contentColor = MaterialTheme.colorScheme.onSurface,
        onClick = onContinue,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = FitLogIcons.Dumbbell,
                contentDescription = null,
                tint = fitLog.accent,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$workingSets series · ${Format.volumeKg(volumeKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Continuar",
                style = MaterialTheme.typography.labelLarge,
                color = fitLog.accentText,
            )
        }
    }
}

@Composable
private fun RecentSessionRow(recent: RecentSession, onClick: () -> Unit) {
    FitLogCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = recent.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${recent.durationLabel} · ${recent.workingSets} series",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${Format.volumeKg(recent.volumeKg)} kg",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StartHintCard(onOpenRoutines: () -> Unit) {
    val fitLog = MaterialTheme.fitLogColors
    FitLogCard(containerColor = fitLog.dataSoft) {
        Text(text = "Tu primer entrenamiento", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Armá una rutina o iniciá una sesión libre: después vas a ver acá tu volumen, " +
                "tu racha y tu evolución.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryAction(
            label = "Crear una rutina",
            icon = FitLogIcons.Plus,
            onClick = onOpenRoutines,
        )
    }
}
