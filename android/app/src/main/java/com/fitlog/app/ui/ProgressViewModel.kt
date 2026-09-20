package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.ProgressRepository
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.domain.Activity
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val exercises: List<CatalogExercise> = emptyList(),
    val selectedExerciseId: String? = null,
    val metric: Progress.Metric = Progress.Metric.MAX_WEIGHT,
    val preset: Progress.RangePreset = Progress.RangePreset.LAST_90_DAYS,
    val points: List<Progress.Point> = emptyList(),
    val refreshing: Boolean = false,
    /** Actividad importada (Huawei Health o GPX) del rango elegido. */
    val activityPoints: List<Activity.Point> = emptyList(),
    val activityMetric: Activity.Metric = Activity.Metric.DISTANCE,
) {
    val activityValues: List<Double>
        get() = Activity.values(activityPoints, activityMetric)

    val activityTotals: Activity.Totals
        get() = Activity.totals(activityPoints)
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: ProgressRepository,
    private val catalogRepository: CatalogRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressUiState())
    val state: StateFlow<ProgressUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val catalog = catalogRepository.loadCatalog()
                _state.update {
                    it.copy(
                        loading = false,
                        exercises = catalog.exercises,
                        selectedExerciseId = it.selectedExerciseId ?: catalog.exercises.firstOrNull()?.id,
                    )
                }
                refreshSeries()
                refreshActivity()
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar el progreso")
                }
            }
        }
    }

    fun selectExercise(exerciseId: String) {
        _state.update { it.copy(selectedExerciseId = exerciseId) }
        refreshSeries()
    }

    fun selectMetric(metric: Progress.Metric) {
        _state.update { it.copy(metric = metric) }
    }

    fun selectActivityMetric(metric: Activity.Metric) {
        _state.update { it.copy(activityMetric = metric) }
    }

    fun selectPreset(preset: Progress.RangePreset) {
        _state.update { it.copy(preset = preset) }
        refreshSeries()
        refreshActivity()
    }

    /** Serie de actividad importada del rango elegido. */
    private fun refreshActivity() {
        val preset = _state.value.preset
        viewModelScope.launch {
            try {
                val sessions = workoutRepository.sessions()
                val points = Activity.series(
                    sessions.map { session ->
                        Activity.Input(
                            startedAtMs = session.startedAt,
                            finishedAtMs = session.finishedAt,
                            distanceM = session.activity?.distanceM,
                            averageHeartRate = session.activity?.averageHeartRate,
                        )
                    },
                    Progress.rangeFor(preset, System.currentTimeMillis()),
                )
                _state.update { it.copy(activityPoints = points) }
            } catch (error: Exception) {
                _state.update {
                    it.copy(error = error.message ?: "Error al calcular la actividad")
                }
            }
        }
    }

    fun refreshSeries() {
        val exerciseId = _state.value.selectedExerciseId ?: return
        val preset = _state.value.preset
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            try {
                val points = repository.series(exerciseId, Progress.rangeFor(preset, System.currentTimeMillis()))
                _state.update { it.copy(refreshing = false, points = points, error = null) }
            } catch (error: Exception) {
                _state.update {
                    it.copy(refreshing = false, error = error.message ?: "Error al calcular el progreso")
                }
            }
        }
    }
}
