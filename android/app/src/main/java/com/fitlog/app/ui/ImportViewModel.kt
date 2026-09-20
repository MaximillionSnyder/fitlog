package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.WorkoutRepository
import com.fitlog.app.domain.Gpx
import com.fitlog.app.domain.HuaweiHealth
import com.fitlog.app.domain.ImportedWorkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Tipo de deporte con su cantidad, para la vista previa. */
data class ImportSportSummary(val name: String, val count: Int)

data class ImportUiState(
    val step: Step = Step.EMPTY,
    val reading: Boolean = false,
    val importing: Boolean = false,
    val error: String? = null,
    val filesRead: Int = 0,
    val workouts: List<ImportedWorkout> = emptyList(),
    val alreadyImported: Int = 0,
    val result: WorkoutRepository.ImportResult? = null,
) {
    enum class Step { EMPTY, PREVIEW, DONE }

    val pending: Int get() = workouts.size - alreadyImported
    val canImport: Boolean get() = step == Step.PREVIEW && pending > 0 && !importing

    /** Tipos de deporte encontrados, de mayor a menor cantidad. */
    val sports: List<ImportSportSummary>
        get() = workouts
            .groupingBy { it.sportName }
            .eachCount()
            .map { (name, count) -> ImportSportSummary(name, count) }
            .sortedByDescending { it.count }

    val firstAtMs: Long? get() = workouts.minOfOrNull { it.startedAtMs }
    val lastAtMs: Long? get() = workouts.maxOfOrNull { it.startedAtMs }
}

/**
 * Importacion de entrenamientos desde una exportacion de Huawei Health.
 *
 * La pantalla no escribe nada hasta que el usuario confirma: primero lee, muestra la vista previa y
 * recien despues importa.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    /** Lee los archivos elegidos y arma la vista previa. */
    fun readFiles(contents: List<Pair<String, String>>) {
        if (contents.isEmpty()) {
            _state.update {
                it.copy(step = ImportUiState.Step.EMPTY, error = "No se eligió ningún archivo")
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(reading = true, error = null, result = null) }
            try {
                val parsed = withContext(Dispatchers.IO) { read(contents) }
                val existing = withContext(Dispatchers.IO) {
                    repository.sessions().map { it.startedAt }.toHashSet()
                }
                _state.update {
                    it.copy(
                        reading = false,
                        step = ImportUiState.Step.PREVIEW,
                        filesRead = parsed.filesRead,
                        workouts = parsed.workouts,
                        alreadyImported = parsed.workouts.count { workout ->
                            existing.contains(workout.startedAtMs)
                        },
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        reading = false,
                        step = ImportUiState.Step.EMPTY,
                        error = error.message ?: "No se pudieron leer los archivos",
                    )
                }
            }
        }
    }

    fun import() {
        val current = _state.value
        if (!current.canImport) return

        viewModelScope.launch {
            _state.update { it.copy(importing = true, error = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.importSessions(
                        current.workouts.map { workout ->
                            WorkoutRepository.ImportedSession(
                                startedAtMs = workout.startedAtMs,
                                finishedAtMs = workout.finishedAtMs,
                                notes = HuaweiHealth.noteFor(workout),
                            )
                        }
                    )
                }
                _state.update {
                    it.copy(importing = false, step = ImportUiState.Step.DONE, result = result)
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(importing = false, error = error.message ?: "No se pudo importar")
                }
            }
        }
    }

    fun reset() {
        _state.value = ImportUiState()
    }

    /**
     * Lee los archivos elegidos, sean de la exportacion de Huawei Health o GPX.
     *
     * Se decide por archivo: un GPX es XML y la exportacion es JSON, asi que una misma carpeta puede
     * traer los dos formatos.
     */
    private fun read(contents: List<Pair<String, String>>): HuaweiHealth.ParseResult {
        val huawei = mutableListOf<String>()
        val gpx = mutableListOf<Pair<String, String>>()
        for ((name, content) in contents) {
            if (Gpx.looksLikeGpx(content)) gpx += content to name else huawei += content
        }

        val parsed = HuaweiHealth.parse(huawei)
        val fromGpx = gpx.flatMap { (content, name) -> Gpx.parse(content, name) }
        val workouts = (parsed.workouts + fromGpx)
            .distinctBy { it.key }
            .sortedByDescending { it.startedAtMs }

        return HuaweiHealth.ParseResult(
            workouts = workouts,
            filesRead = parsed.filesRead + gpx.size,
            filesSkipped = parsed.filesSkipped,
        )
    }
}
