package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.AddSetInput
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.UpdateSetInput
import com.fitlog.app.data.WorkoutException
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.data.WorkoutSession
import com.fitlog.app.data.WorkoutSet
import com.fitlog.app.domain.CatalogExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val formError: String? = null,
    val active: WorkoutSession? = null,
    val activeSets: List<WorkoutSet> = emptyList(),
    val history: List<WorkoutSession> = emptyList(),
    val exercises: List<CatalogExercise> = emptyList(),
    val restTargets: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    private var autoStartHandled = false

    init {
        load()
    }

    /**
     * Arranca una sesion apenas se abre la pantalla, una sola vez por instancia.
     *
     * Es lo que usa el panel de Inicio cuando el usuario toca "Iniciar entrenamiento": entra a
     * Entrenar ya con la sesion abierta, sin un segundo toque.
     */
    fun autoStartOnce(routineId: String? = null) {
        if (autoStartHandled) return
        autoStartHandled = true
        viewModelScope.launch {
            if (repository.activeSession() != null) return@launch
            startSession(routineId)
        }
    }

    /** [silent] evita el estado de carga: se usa al volver a la pantalla, no al abrirla. */
    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(loading = if (silent) it.loading else true, error = null) }
            try {
                val catalog = catalogRepository.loadCatalog()
                val active = repository.activeSession()
                _state.update {
                    it.copy(
                        loading = false,
                        exercises = catalog.exercises,
                        active = active,
                        activeSets = activeSetsOf(active?.id),
                        history = repository.sessions(),
                        restTargets = repository.restTargets(active?.routineId),
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar entrenamientos")
                }
            }
        }
    }

    private suspend fun activeSetsOf(sessionId: String?): List<WorkoutSet> =
        if (sessionId == null) emptyList() else repository.sessionDetail(sessionId).sets

    fun startSession(routineId: String? = null) {
        viewModelScope.launch {
            try {
                repository.startSession(routineId)
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: WorkoutException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun finishSession() {
        val active = _state.value.active ?: return
        viewModelScope.launch {
            try {
                repository.finishSession(active.id)
                load()
            } catch (error: WorkoutException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun addSet(
        exerciseId: String,
        weightKg: Double?,
        reps: Int?,
        rir: Int?,
        notes: String?,
        isWarmup: Boolean,
    ) {
        val active = _state.value.active ?: return
        viewModelScope.launch {
            try {
                repository.addSet(
                    AddSetInput(
                        sessionId = active.id,
                        exerciseId = exerciseId,
                        weightKg = weightKg,
                        reps = reps,
                        rir = rir,
                        notes = notes,
                        isWarmup = isWarmup,
                    )
                )
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: WorkoutException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun updateSet(setId: String, weightKg: Double?, reps: Int?, rir: Int?, notes: String?) {
        viewModelScope.launch {
            try {
                repository.updateSet(setId, UpdateSetInput(weightKg, reps, rir, notes))
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: WorkoutException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun deleteSet(setId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSet(setId)
                load()
            } catch (error: WorkoutException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun clearFormError() {
        _state.update { it.copy(formError = null) }
    }
}
