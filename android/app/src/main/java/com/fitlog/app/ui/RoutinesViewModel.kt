package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.Routine
import com.fitlog.app.data.RoutineException
import com.fitlog.app.data.RoutineExerciseInput
import com.fitlog.app.data.RoutinesRepository
import com.fitlog.app.domain.CatalogExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val formError: String? = null,
    val routines: List<Routine> = emptyList(),
    val exercises: List<CatalogExercise> = emptyList(),
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repository: RoutinesRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RoutinesUiState())
    val state: StateFlow<RoutinesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val catalog = catalogRepository.loadCatalog()
                _state.update {
                    it.copy(
                        loading = false,
                        exercises = catalog.exercises,
                        routines = repository.routines(),
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar rutinas")
                }
            }
        }
    }

    fun createRoutine(name: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.createRoutine(name, description)
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: RoutineException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun updateRoutine(routineId: String, name: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.updateRoutine(routineId, name, description)
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: RoutineException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(routineId)
                load()
            } catch (error: RoutineException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun addExercise(routineId: String, input: RoutineExerciseInput) {
        viewModelScope.launch {
            try {
                repository.addExercise(routineId, input)
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: RoutineException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun removeExercise(routineExerciseId: String) {
        viewModelScope.launch {
            try {
                repository.removeExercise(routineExerciseId)
                load()
            } catch (error: RoutineException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun moveExercise(routineExerciseId: String, direction: RoutinesRepository.MoveDirection) {
        viewModelScope.launch {
            try {
                repository.moveExercise(routineExerciseId, direction)
                load()
            } catch (error: RoutineException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }

    fun clearFormError() {
        _state.update { it.copy(formError = null) }
    }
}
