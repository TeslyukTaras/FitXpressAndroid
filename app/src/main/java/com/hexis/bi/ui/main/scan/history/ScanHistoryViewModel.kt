package com.hexis.bi.ui.main.scan.history

import android.app.Application
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.ScanFirestoreConstants.SCAN_HISTORY_MAX_SCANS
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.millisToHourAmPm
import com.hexis.bi.utils.millisToShortMonthDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
const val SCAN_HISTORY_MAX_RANGE_DAYS = 60

class ScanHistoryViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ScanHistoryState())
    val state: StateFlow<ScanHistoryState> = _state.asStateFlow()
    private var allScans: List<ScanRecord> = emptyList()

    init {
        loadUnitSystem()
        refresh()
    }

    private fun loadUnitSystem() = launch(showLoading = false) {
        userRepository.getUser().onSuccess { profile ->
            _state.update { it.copy(isMetric = profile.unitSystem.isMetricUnitSystem()) }
        }
    }

    fun refresh() = launch(showLoading = false, onError = { e ->
        _state.update { it.copy(isLoading = false, error = e.message ?: e.toString()) }
    }) {
        _state.update { it.copy(isLoading = true, error = null) }
        scanHistoryRepository.getRecentScans(SCAN_HISTORY_MAX_SCANS, ScanFetchProjection.LIST_SUMMARY).fold(
            onSuccess = { scans ->
                allScans = scans
                _state.update {
                    it.copy(
                        isLoading = false,
                        items = mapVisibleItems(
                            scans = scans,
                            startDateMillis = it.selectedStartDateMillis,
                            endDateMillis = it.selectedEndDateMillis,
                        ),
                    )
                }
            },
            onFailure = { e ->
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: e.toString())
                }
            },
        )
    }

    fun showDateRangePicker() {
        _state.update { it.copy(showDateRangePicker = true) }
    }

    fun hideDateRangePicker() {
        _state.update { it.copy(showDateRangePicker = false) }
    }

    fun resetDateRange() {
        _state.update {
            it.copy(
                showDateRangePicker = false,
                selectedStartDateMillis = null,
                selectedEndDateMillis = null,
                items = mapVisibleItems(allScans, null, null),
            )
        }
    }

    fun applyDateRange(startDateMillis: Long?, endDateMillis: Long?) {
        val start = startDateMillis ?: endDateMillis
        val end = endDateMillis ?: startDateMillis
        _state.update {
            it.copy(
                showDateRangePicker = false,
                selectedStartDateMillis = start,
                selectedEndDateMillis = end,
                items = mapVisibleItems(allScans, start, end),
            )
        }
    }

    fun clearStateError() {
        _state.update { it.copy(error = null) }
    }

    private fun mapVisibleItems(
        scans: List<ScanRecord>,
        startDateMillis: Long?,
        endDateMillis: Long?,
    ): List<ScanHistoryListItem> {
        val visibleScans = filterScans(scans, startDateMillis, endDateMillis)
        return visibleScans.map { scan ->
            val previous = scans.firstOrNull { it.timestamp < scan.timestamp }
            ScanHistoryListItem(
                scanId = scan.id,
                dateLabel = formatDate(scan.timestamp),
                timeLabel = formatTime(scan.timestamp),
                topChange = MeasurementMapper.topChangeVsPreviousScan(scan, previous),
            )
        }
    }

    private fun filterScans(
        scans: List<ScanRecord>,
        startDateMillis: Long?,
        endDateMillis: Long?,
    ): List<ScanRecord> {
        if (startDateMillis == null && endDateMillis == null) return scans
        val start = startOfDayMillis(startDateMillis ?: endDateMillis ?: return scans)
        val end = endOfDayMillis(endDateMillis ?: startDateMillis ?: return scans)
        val lower = minOf(start, end)
        val upper = maxOf(start, end)
        return scans.filter { it.timestamp in lower..upper }
    }

    private fun startOfDayMillis(utcMidnightMs: Long): Long =
        pickedLocalDate(utcMidnightMs)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun endOfDayMillis(utcMidnightMs: Long): Long =
        pickedLocalDate(utcMidnightMs)
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1

    private fun pickedLocalDate(utcMidnightMs: Long): LocalDate =
        Instant.ofEpochMilli(utcMidnightMs).atZone(ZoneOffset.UTC).toLocalDate()

    private fun formatDate(ms: Long): String = ms.millisToShortMonthDay()

    private fun formatTime(ms: Long): String = ms.millisToHourAmPm()
}

fun isScanHistoryRangeWithinLimit(startDateMillis: Long?, endDateMillis: Long?): Boolean {
    if (startDateMillis == null || endDateMillis == null) return true
    val days = kotlin.math.abs(endDateMillis - startDateMillis) / MILLIS_PER_DAY + 1
    return days <= SCAN_HISTORY_MAX_RANGE_DAYS
}
