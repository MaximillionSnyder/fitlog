package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de "hay una sesion en curso" para el shell.
 *
 * La barra inferior y el encabezado necesitan saber si el entrenamiento esta abierto para marcar la
 * pestana y ofrecer continuar; el detalle de la sesion sigue viviendo en su ViewModel.
 */
@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    data class State(
        val active: Boolean = false,
        val routineName: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val session = runCatching { repository.activeSession() }.getOrNull()
            _state.update {
                State(active = session != null, routineName = session?.routineName)
            }
        }
    }
}
