package com.hexis.bi.ui.main.home.intelligence

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.BuildConfig
import com.hexis.bi.domain.intelligence.RunIntelligenceUseCase
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.utils.millisToShortMonthDay
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsState(
    val cards: List<InsightCard> = emptyList(),
    val loaded: Boolean = false,
    val updating: Boolean = false,
    val latestScanDate: String? = null,
    val showInfoSheet: Boolean = false,
)

class InsightsViewModel internal constructor(
    application: Application,
    private val runIntelligence: RunIntelligenceUseCase,
    private val scanHistoryRepository: ScanHistoryRepository,
) : BaseViewModel(application, initialLoading = false) {

    private val _state = MutableStateFlow(InsightsState())
    val state = _state.asStateFlow()

    init {
        load()
        loadScanDate()
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    fun refresh() {
        if (_state.value.cards.isEmpty()) return
        _state.update { it.copy(updating = true) }
        load()
    }

    private fun loadScanDate() {
        viewModelScope.launch {
            val date = scanHistoryRepository.getLatestScan().getOrNull()
                ?.timestamp?.millisToShortMonthDay()
            _state.update { it.copy(latestScanDate = date) }
        }
    }

    private fun load() {
        if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) {
            _state.update { it.copy(loaded = true) }
            return
        }
        viewModelScope.launch {
            val cards = runIntelligence().fold(
                onSuccess = { run ->
                    EngineFindingsMapper.simpleFindings(
                        report = run.report,
                        copy = run.copy,
                        windowDays = run.config.windows.analysisDays,
                        isMetric = run.isMetric,
                    )
                },
                onFailure = { emptyList() },
            )
            _state.update { it.copy(cards = cards, loaded = true, updating = false) }
        }
    }
}
