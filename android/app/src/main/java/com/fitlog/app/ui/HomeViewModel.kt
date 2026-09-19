package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.BodyMetricsRepository
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.data.WorkoutSession
import com.fitlog.app.domain.Body
import com.fitlog.app.domain.Home
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resumen de un entrenamiento terminado, tal como lo muestra la lista de recientes. */
data class RecentSession(
    val id: String,
    val name: String,
    val startedAtMs: Long,
    val durationLabel: String,
    val volumeKg: Double,
    val workingSets: Int,
)

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val greeting: String = "Hola",
    val summary: Home.Summary = Home.build(emptyList(), emptyList(), 0L),
    val activeSession: WorkoutSession? = null,
    val recentSessions: List<RecentSession> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workout: WorkoutRepository,
    private val body: BodyMetricsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = coroutineScope {
                    val active = async { workout.activeSession() }
                    val sessions = async { workout.sessions() }
                    val metrics = async { body.metrics() }
                    Loaded(
                        active = active.await(),
                        sessions = sessions.await(),
                        metrics = metrics.await(),
                    )
                }
                _state.update { current ->
                    val now = System.currentTimeMillis()
                    val sessionInputs = snapshot.sessions.map { session ->
                        Home.SessionInput(
                            id = session.id,
                            startedAtMs = session.startedAt,
                            finishedAtMs = session.finishedAt,
                            workingSets = session.summary.workingSets,
                            volumeKg = session.summary.totalVolumeKg,
                        )
                    }
                    val bodyInputs = snapshot.metrics
                        .filter { it.kind == Body.Kind.BODY_WEIGHT }
                        .map { Home.BodyInput(measuredAtMs = it.measuredAtMs, value = it.value) }

                    current.copy(
                        loading = false,
                        greeting = greetingFor(now),
                        summary = Home.build(sessionInputs, bodyInputs, now),
                        activeSession = snapshot.active,
                        recentSessions = snapshot.sessions
                            .filter { it.finishedAt != null }
                            .take(RECENT_LIMIT)
                            .map { session ->
                                RecentSession(
                                    id = session.id,
                                    name = session.routineName ?: "Entrenamiento libre",
                                    startedAtMs = session.startedAt,
                                    durationLabel = session.finishedAt
                                        ?.let { finish -> formatDuration(session.startedAt, finish) }
                                        ?: "—",
                                    volumeKg = session.summary.totalVolumeKg,
                                    workingSets = session.summary.workingSets,
                                )
                            },
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "No se pudo cargar el panel")
                }
            }
        }
    }

    private data class Loaded(
        val active: WorkoutSession?,
        val sessions: List<WorkoutSession>,
        val metrics: List<Body.Point>,
    )

    companion object {
        const val RECENT_LIMIT = 3

        fun greetingFor(nowMs: Long): String {
            val hour = ((nowMs / 3_600_000L) % 24L).toInt()
            return when {
                hour < 6 -> "Buenas noches"
                hour < 13 -> "Buenos días"
                hour < 20 -> "Buenas tardes"
                else -> "Buenas noches"
            }
        }

        fun formatDuration(startedAtMs: Long, finishedAtMs: Long): String {
            val minutes = ((finishedAtMs - startedAtMs) / 60_000L).coerceAtLeast(0L)
            val hours = minutes / 60
            val remainder = minutes % 60
            return if (hours == 0L) "$remainder min" else "$hours h $remainder min"
        }
    }
}
