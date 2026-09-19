package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.CatalogException
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.CreateCustomExerciseInput
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.CatalogFilter
import com.fitlog.app.domain.CatalogFilters
import com.fitlog.app.domain.ExerciseKind
import com.fitlog.app.domain.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val groups: List<MuscleGroup> = emptyList(),
    val exercises: List<CatalogExercise> = emptyList(),
    val filters: CatalogFilters = CatalogFilters(),
    val formError: String? = null,
) {
    val visible: List<CatalogExercise>
        get() = CatalogFilter.apply(exercises, groups, filters)

    val equipments: List<String>
        get() = exercises.map { it.equipment }.distinct().sorted()
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = repository.loadCatalog()
                _state.update {
                    it.copy(
                        loading = false,
                        groups = snapshot.groups,
                        exercises = snapshot.exercises,
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar el catálogo")
                }
            }
        }
    }

    /** Vuelve a mostrar todo el catalogo: es la salida del estado "sin resultados". */
    fun clearFilters() {
        _state.update { it.copy(filters = CatalogFilters()) }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(filters = it.filters.copy(query = value)) }
    }

    fun toggleMuscleGroup(slug: String) {
        _state.update {
            val current = it.filters.muscleGroupSlug
            it.copy(filters = it.filters.copy(muscleGroupSlug = if (current == slug) null else slug))
        }
    }

    fun toggleEquipment(equipment: String) {
        _state.update {
            val current = it.filters.equipment
            it.copy(filters = it.filters.copy(equipment = if (current == equipment) null else equipment))
        }
    }

    fun toggleKind(kind: ExerciseKind) {
        _state.update {
            val current = it.filters.kind
            it.copy(filters = it.filters.copy(kind = if (current == kind) null else kind))
        }
    }

    fun createCustomExercise(name: String, muscleGroupId: String, equipment: String, kind: ExerciseKind) {
        viewModelScope.launch {
            try {
                repository.createCustomExercise(
                    CreateCustomExerciseInput(
                        name = name,
                        muscleGroupId = muscleGroupId,
                        equipment = equipment,
                        kind = kind,
                    )
                )
                _state.update { it.copy(formError = null) }
                load()
            } catch (error: CatalogException) {
                _state.update { it.copy(formError = error.message) }
            }
        }
    }

    fun clearFormError() {
        _state.update { it.copy(formError = null) }
    }

    fun deleteCustomExercise(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomExercise(id)
                _state.update { it.copy(error = null) }
                load()
            } catch (error: CatalogException) {
                _state.update { it.copy(error = error.message) }
            }
        }
    }
}
