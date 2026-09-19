package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.BackupRepository
import com.fitlog.app.data.BackupSectionGroup
import com.fitlog.app.domain.Backup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val groups: List<BackupSectionGroup> = BackupRepository.GROUPS,
    val selectedGroups: Set<String> = setOf("custom_exercises", "routines", "workouts", "body_metrics"),
    val busy: Boolean = false,
    val error: String? = null,
    val summary: Map<String, Backup.SectionSummary>? = null,
    val exportedJson: String? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun toggleGroup(groupId: String) {
        _state.update { current ->
            val selected = current.selectedGroups
            current.copy(
                selectedGroups = if (groupId in selected) selected - groupId else selected + groupId,
            )
        }
    }

    fun export() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val tables = BackupRepository.tablesFor(_state.value.selectedGroups.toList())
                if (tables.isEmpty()) {
                    _state.update { it.copy(busy = false, error = "Elegí al menos una sección para exportar") }
                    return@launch
                }
                val json = repository.export(tables, APP_VERSION)
                _state.update { it.copy(busy = false, exportedJson = json) }
            } catch (error: Exception) {
                _state.update { it.copy(busy = false, error = error.message ?: "Error al exportar") }
            }
        }
    }

    fun onExported() {
        _state.update { it.copy(exportedJson = null) }
    }

    fun import(json: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, summary = null) }
            try {
                val summary = repository.import(json)
                _state.update { it.copy(busy = false, summary = summary) }
            } catch (error: Backup.BackupException) {
                _state.update { it.copy(busy = false, error = error.detail) }
            } catch (error: Exception) {
                _state.update { it.copy(busy = false, error = error.message ?: "Error al importar") }
            }
        }
    }

    fun clearSummary() {
        _state.update { it.copy(summary = null) }
    }

    private companion object {
        const val APP_VERSION = "0.1.7"
    }
}
