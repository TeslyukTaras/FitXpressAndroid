package com.hexis.bi.ui.main.home.intelligence

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.BuildConfig
import com.hexis.bi.data.health.sync.HealthSyncScheduler
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.domain.intelligence.RunIntelligenceUseCase
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.millisToShortMonthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val healthSyncScheduler: HealthSyncScheduler,
) : BaseViewModel(application, initialLoading = false) {

    private val _state = MutableStateFlow(InsightsState())
    val state = _state.asStateFlow()
    private var backfillInFlight = false
    private val insightsJob = LatestJobController()
    private val scanDateJob = LatestJobController()

    init {
        load(clearUpdatingOnComplete = false)
        loadScanDate()
        healthSyncScheduler.backfillInFlight()
            .onEach { inFlight ->
                val transition = backfillTransition(backfillInFlight, inFlight)
                backfillInFlight = inFlight
                when (transition) {
                    BackfillTransition.ACTIVE -> {
                        insightsJob.cancel()
                        _state.update { it.copy(updating = true) }
                    }
                    BackfillTransition.SETTLED -> load(clearUpdatingOnComplete = true)
                    BackfillTransition.IDLE -> _state.update { it.copy(updating = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    fun refresh() {
        if (_state.value.cards.isEmpty()) return
        _state.update { it.copy(updating = true) }
        load(clearUpdatingOnComplete = true)
    }

    private fun loadScanDate() {
        scanDateJob.replace(viewModelScope) {
            val date = scanHistoryRepository.getLatestScan().getOrNull()
                ?.timestamp?.millisToShortMonthDay()
            _state.update { it.copy(latestScanDate = date) }
        }
    }

    private fun load(clearUpdatingOnComplete: Boolean) {
        if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) {
            _state.update {
                it.copy(
                    loaded = true,
                    updating = if (clearUpdatingOnComplete) false else it.updating,
                )
            }
            return
        }
        insightsJob.replace(viewModelScope) {
            val cards = runIntelligence().map { run ->
                EngineFindingsMapper.simpleFindings(
                        report = run.report,
                        copy = run.copy,
                        windowDays = run.config.windows.analysisDays,
                        isMetric = run.isMetric,
                    )
            }.getOrNull()
            _state.update {
                it.copy(
                    cards = cards ?: it.cards,
                    loaded = true,
                    updating = if (clearUpdatingOnComplete) false else it.updating,
                )
            }
        }
    }
}
