package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.BodyMetricException
import com.fitlog.app.data.BodyMetricInput
import com.fitlog.app.data.BodyMetricUpdate
import com.fitlog.app.data.BodyMetricsRepository
import com.fitlog.app.domain.Body
import com.fitlog.app.domain.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BodyMetricsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val formError: String? = null,
    val kind: Body.Kind = Body.Kind.BODY_WEIGHT,
    val preset: Progress.RangePreset = Progress.RangePreset.LAST_90_DAYS,
    val metrics: List<Body.Point> = emptyList(),
    val series: List<Body.Point> = emptyList(),
    val stats: Body.Stats = Body.stats(emptyList()),
)

@HiltViewModel
class BodyMetricsViewModel @Inject constructor(
    private val repository: BodyMetricsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BodyMetricsUiState())
    val state: StateFlow<BodyMetricsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val metrics = repository.metrics()
                _state.update { it.copy(loading = false, metrics = metrics) }
                recompute()
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar las medidas")
                }
            }
        }
    }

    fun selectKind(kind: Body.Kind) {
        _state.update { it.copy(kind = kind) }
        recompute()
    }

    fun selectPreset(preset: Progress.RangePreset) {
        _state.update { it.copy(preset = preset) }
        recompute()
    }

    fun add(value: Double, measuredAtMs: Long, notes: String?) {
        viewModelScope.launch {
            try {
                repository.create(
                    BodyMetricInput(
                        kind = _state.value.kind.wire,
                        value = value,
                        measuredAtMs = measuredAtMs,
                        notes = notes,
                    )
                )
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: BodyMetricException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun update(id: String, value: Double, measuredAtMs: Long, notes: String?) {
        viewModelScope.launch {
            try {
                repository.update(id, BodyMetricUpdate(value, measuredAtMs, notes))
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: BodyMetricException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                load()
            } catch (error: BodyMetricException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    private fun recompute() {
        val state = _state.value
        val range = Progress.rangeFor(state.preset, System.currentTimeMillis())
        val series = Body.series(state.metrics, state.kind, range)
        _state.update { it.copy(series = series, stats = Body.stats(series)) }
    }
}
