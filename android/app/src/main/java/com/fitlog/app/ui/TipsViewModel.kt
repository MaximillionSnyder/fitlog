package com.fitlog.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.data.CatalogRepository
import com.fitlog.app.data.ComparisonsRepository
import com.fitlog.app.domain.CatalogExercise
import com.fitlog.app.domain.Comparisons
import com.fitlog.app.domain.Insights
import com.fitlog.app.domain.MuscleGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TipsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val preset: Comparisons.Preset = Comparisons.Preset.LAST_30_DAYS,
    val tips: List<Insights.Tip> = emptyList(),
    val exercises: List<CatalogExercise> = emptyList(),
    val muscleGroups: List<MuscleGroup> = emptyList(),
)

@HiltViewModel
class TipsViewModel @Inject constructor(
    private val comparisonsRepository: ComparisonsRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TipsUiState())
    val state: StateFlow<TipsUiState> = _state.asStateFlow()

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
                sets = comparisonsRepository.sets()
                groupSlugs = comparisonsRepository.groups()
                _state.update {
                    it.copy(
                        loading = false,
                        exercises = catalog.exercises,
                        muscleGroups = catalog.groups,
                    )
                }
                recompute()
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message ?: "Error al cargar los tips")
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
        val periodDays = if (preset == Comparisons.Preset.LAST_30_DAYS) 30 else 90
        val range = Comparisons.comparisonRanges(preset, loadedAtMs).current

        _state.update {
            it.copy(
                tips = Insights.build(
                    Insights.Input(
                        sets = sets,
                        groupByExercise = groupSlugs,
                        range = range,
                        periodDays = periodDays,
                    )
                )
            )
        }
    }
}
