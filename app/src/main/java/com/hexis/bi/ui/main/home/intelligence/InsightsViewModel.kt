package com.hexis.bi.ui.main.home.intelligence

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.domain.intelligence.IntelligenceCoordinator
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.millisToShortMonthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class InsightsState(
    val cards: List<InsightCard> = emptyList(),
    val updating: Boolean = false,
    val latestScanDate: String? = null,
    val showInfoSheet: Boolean = false,
)

class InsightsViewModel internal constructor(
    application: Application,
    private val intelligenceCoordinator: IntelligenceCoordinator,
    private val scanHistoryRepository: ScanHistoryRepository,
) : BaseViewModel(application, initialLoading = false) {

    private val _state = MutableStateFlow(InsightsState())
    val state = _state.asStateFlow()
    private val scanDateJob = LatestJobController()

    init {
        intelligenceCoordinator.refreshIfStale()
        loadScanDate()
        intelligenceCoordinator.state
            .onEach { reportState ->
                val cards = reportState.run?.let { run ->
                    EngineFindingsMapper.simpleFindings(
                        report = run.report,
                        copy = run.copy,
                        windowDays = run.config.windows.analysisDays,
                        isMetric = run.isMetric,
                    )
                }
                _state.update {
                    it.copy(
                        cards = cards ?: it.cards,
                        updating = reportState.updating,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    fun refresh() {
        if (_state.value.cards.isEmpty()) return
        _state.update { it.copy(updating = true) }
        intelligenceCoordinator.refreshNow()
    }

    private fun loadScanDate() {
        scanDateJob.replace(viewModelScope) {
            val date = scanHistoryRepository.getLatestScan().getOrNull()
                ?.timestamp?.millisToShortMonthDay()
            _state.update { it.copy(latestScanDate = date) }
        }
    }

}
