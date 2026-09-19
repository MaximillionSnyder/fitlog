package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.SessionDetail
import com.fitlog.app.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: SessionDetail? = null,
)

/**
 * Detalle de un entrenamiento ya registrado: la sesion y sus series.
 *
 * Vive en su propia pantalla (antes era un dialogo dentro de Entrenar), asi el historial se lee
 * como una ficha y no como una ventana emergente.
 */
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionDetailUiState())
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    private var loadedId: String? = null

    fun load(sessionId: String, force: Boolean = false) {
        if (!force && loadedId == sessionId && _state.value.detail != null) return
        loadedId = sessionId
        viewModelScope.launch {
            _state.value = SessionDetailUiState(loading = true)
            _state.value = try {
                SessionDetailUiState(loading = false, detail = repository.sessionDetail(sessionId))
            } catch (error: Exception) {
                SessionDetailUiState(
                    loading = false,
                    error = error.message ?: "No se pudo cargar el entrenamiento",
                )
            }
        }
    }

    fun refresh(sessionId: String) = load(sessionId, force = true)
}
