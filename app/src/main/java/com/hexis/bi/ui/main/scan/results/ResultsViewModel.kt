package com.hexis.bi.ui.main.scan.results

import android.app.Application
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.ProfileConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    init {
        loadUnitSystem()
        loadMeasurements()
    }

    private fun loadUnitSystem() = launch(showLoading = false) {
        userRepository.getUser().onSuccess { profile ->
            _state.update {
                it.copy(isMetric = profile.unitSystem != ProfileConstants.UNIT_SYSTEM_IMPERIAL)
            }
        }
    }

    private fun loadMeasurements() = launch(showLoading = false) {
        val result = scanResultRepository.latestResult ?: return@launch

        // The current scan is already persisted, so the "previous" one is the 2nd most recent.
        val previousScan = scanHistoryRepository.getPreviousScan()
            .getOrNull()
            ?.takeIf { it.measurements.isNotEmpty() }

        val rows = MeasurementMapper.map(
            current = result.response,
            previous = previousScan,
        )

        val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
        _state.update {
            it.copy(
                measurements = rows,
                model3dUrl = result.response.model3dUrl,
                todayDate = formatter.format(Date(System.currentTimeMillis())),
                previousDate = previousScan?.timestamp?.let { ts -> formatter.format(Date(ts)) },
            )
        }
    }

    fun selectTab(tab: ResultsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun toggleColorAnalysis() {
        _state.update { it.copy(colorAnalysisEnabled = !it.colorAnalysisEnabled) }
    }
}
