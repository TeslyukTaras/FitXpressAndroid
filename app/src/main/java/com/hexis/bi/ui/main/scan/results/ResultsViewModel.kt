package com.hexis.bi.ui.main.scan.results

import android.app.Application
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.millisToShortMonthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
            _state.update { it.copy(isMetric = profile.unitSystem.isMetricUnitSystem()) }
        }
    }

    private fun loadMeasurements() = launch(showLoading = false) {
        val selectedScanId = scanResultRepository.selectedScanId
        if (selectedScanId != null) {
            val scans = scanHistoryRepository.getRecentScans(limit = 100).getOrElse { return@launch }
            val selectedIndex = scans.indexOfFirst { it.id == selectedScanId }
            if (selectedIndex >= 0) {
                val current = scans[selectedIndex]
                val previous = scans.getOrNull(selectedIndex + 1)?.takeIf { it.measurements.isNotEmpty() }
                val beforePrevious = scans.getOrNull(selectedIndex + 2)?.takeIf { it.measurements.isNotEmpty() }
                val rows = MeasurementMapper.mapFromRecords(
                    current = current,
                    previous = previous,
                    beforePrevious = beforePrevious,
                )
                _state.update {
                    it.copy(
                        measurements = rows,
                        model3dUrl = current.model3dUrl,
                        todayDate = current.timestamp.millisToShortMonthDay(),
                        previousDate = previous?.timestamp?.millisToShortMonthDay(),
                    )
                }
                return@launch
            }
        }

        val result = scanResultRepository.latestResult ?: return@launch
        val (previousScan, beforePreviousScan) = scanHistoryRepository.getPreviousTwoScans()
            .getOrElse { null to null }
            .let { (prev, beforePrev) ->
                prev?.takeIf { it.measurements.isNotEmpty() } to
                    beforePrev?.takeIf { it.measurements.isNotEmpty() }
            }
        val rows = MeasurementMapper.map(
            current = result.response,
            previous = previousScan,
            beforePrevious = beforePreviousScan,
        )
        _state.update {
            it.copy(
                measurements = rows,
                model3dUrl = result.response.model3dUrl,
                todayDate = System.currentTimeMillis().millisToShortMonthDay(),
                previousDate = previousScan?.timestamp?.millisToShortMonthDay(),
            )
        }
    }

    fun selectTab(tab: ResultsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun toggleColorAnalysis() {
        _state.update { it.copy(colorAnalysisEnabled = !it.colorAnalysisEnabled) }
    }

    fun toggleSkinAreas() {
        _state.update { it.copy(showSkinAreas = !it.showSkinAreas) }
    }
}
