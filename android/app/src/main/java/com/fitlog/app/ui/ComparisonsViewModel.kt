package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.ComparisonsRepository
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.Comparisons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComparisonsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val preset: Comparisons.Preset = Comparisons.Preset.LAST_30_DAYS,
    val records: List<Comparisons.PersonalRecord> = emptyList(),
    val comparison: Comparisons.PeriodComparison? = null,
    val balance: List<Comparisons.MuscleBalanceEntry> = emptyList(),
    val exercises: List<CatalogExercise> = emptyList(),
    val groups: Map<String, String> = emptyMap(),
)

@HiltViewModel
class ComparisonsViewModel @Inject constructor(
    private val repository: ComparisonsRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ComparisonsUiState())
    val state: StateFlow<ComparisonsUiState> = _state.asStateFlow()

    private var loadedAtMs: Long = 0L
    private var sets: List<Comparisons.SetInput> = emptyList()
    private var groupSlugs: Map<String, String> = emptyMap()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val catalog = catalogRepository.loadCatalog()
                loadedAtMs = System.currentTimeMillis()
                sets = repository.sets()
                groupSlugs = repository.groups()
                _state.update {
                    it.copy(
                        loading = false,
                        exercises = catalog.exercises,
                        groups = groupSlugs,
                    )
                }
                recompute()
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar comparativas")
                }
            }
        }
    }

    fun selectPreset(preset: Comparisons.Preset) {
        _state.update { it.copy(preset = preset) }
        recompute()
    }

    private fun recompute() {
        val preset = _state.value.preset
        val ranges = Comparisons.comparisonRanges(preset, loadedAtMs)
        _state.update {
            it.copy(
                records = Comparisons.personalRecords(sets),
                comparison = Comparisons.comparePeriods(sets, ranges.current, ranges.previous),
                balance = Comparisons.muscleBalance(sets, groupSlugs, ranges.current),
            )
        }
    }
}
