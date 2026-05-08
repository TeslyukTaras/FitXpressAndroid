package com.hexis.bi.ui.main.scan.results

import android.app.Application
import com.google.firebase.Timestamp
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanResult
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.millisToShortMonthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

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
        val latest = scanResultRepository.latestResult

        when {
            selectedScanId != null -> {
                val current = scanHistoryRepository.getScanRecordById(selectedScanId).getOrElse {
                    _state.update { it.copy(isPreviewSectionLoading = false) }
                    return@launch
                }
                if (current == null) {
                    _state.update { it.copy(isPreviewSectionLoading = false) }
                    return@launch
                }
                applyHistoryImmediate(current)
                launch(showLoading = false) { enrichHistoryNeighbors(current) }
            }
            latest != null -> {
                applyFreshScanImmediate(latest)
                launch(showLoading = false) { enrichFreshScanWithNeighbors() }
            }
            else -> {
                _state.update { it.copy(isPreviewSectionLoading = false) }
            }
        }
    }

    private fun applyFreshScanImmediate(result: ScanResult) {
        val rows = MeasurementMapper.map(
            current = result.response,
            previous = null,
            beforePrevious = null,
        )
        _state.update {
            it.copy(
                measurements = rows,
                model3dUrl = result.response.model3dUrl,
                previousModel3dUrl = null,
                isPreviewSectionLoading = false,
                todayDate = System.currentTimeMillis().millisToShortMonthDay(),
                previousDate = null,
            )
        }
    }

    private suspend fun enrichFreshScanWithNeighbors() {
        val latest = scanResultRepository.latestResult ?: return
        val (previousScan, beforePreviousScan) = scanHistoryRepository.getPreviousTwoScans()
            .getOrElse { return }
            .let { (prev, beforePrev) ->
                prev?.takeIf { it.measurements.isNotEmpty() } to
                    beforePrev?.takeIf { it.measurements.isNotEmpty() }
            }
        val rows = MeasurementMapper.map(
            current = latest.response,
            previous = previousScan,
            beforePrevious = beforePreviousScan,
        )
        _state.update {
            it.copy(
                measurements = rows,
                previousModel3dUrl = previousScan?.model3dUrl?.takeUnless { url -> url.isNullOrBlank() },
                previousDate = previousScan?.timestamp?.millisToShortMonthDay(),
            )
        }
    }

    private fun applyHistoryImmediate(current: ScanRecord) {
        val rows = MeasurementMapper.mapFromRecords(
            current = current,
            previous = null,
            beforePrevious = null,
        )
        _state.update {
            it.copy(
                measurements = rows,
                model3dUrl = current.model3dUrl,
                previousModel3dUrl = null,
                isPreviewSectionLoading = false,
                todayDate = current.timestamp.millisToShortMonthDay(),
                previousDate = null,
            )
        }
    }

    private suspend fun enrichHistoryNeighbors(current: ScanRecord) {
        val cutoff = Timestamp(Date(current.timestamp))
        val older = scanHistoryRepository.getOlderScanRecordsBefore(cutoff, limit = 2).getOrElse { return }
        val previous = older.getOrNull(0)?.takeIf { it.measurements.isNotEmpty() }
        val beforePrevious = older.getOrNull(1)?.takeIf { it.measurements.isNotEmpty() }
        val rows = MeasurementMapper.mapFromRecords(current, previous, beforePrevious)
        _state.update {
            it.copy(
                measurements = rows,
                previousModel3dUrl = previous?.model3dUrl?.takeUnless { url -> url.isNullOrBlank() },
                previousDate = previous?.timestamp?.millisToShortMonthDay(),
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

    override fun onCleared() {
        scanResultRepository.selectedScanId = null
        super.onCleared()
    }
}
