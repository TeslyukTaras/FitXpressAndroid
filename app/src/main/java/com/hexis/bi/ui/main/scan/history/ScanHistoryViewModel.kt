package com.hexis.bi.ui.main.scan.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.utils.constants.DateFormatConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScanHistoryViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ScanHistoryState())
    val state: StateFlow<ScanHistoryState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            scanHistoryRepository.getRecentScans(MAX_SCANS, ScanFetchProjection.LIST_SUMMARY).fold(
                onSuccess = { scans ->
                    val items = scans.mapIndexed { index, scan ->
                        val prev = scans.getOrNull(index + 1)
                        ScanHistoryListItem(
                            scanId = scan.id,
                            dateLabel = formatDate(scan.timestamp),
                            timeLabel = formatTime(scan.timestamp),
                            topChange = MeasurementMapper.topChangeVsPreviousScan(scan, prev),
                        )
                    }
                    val dateRangeText = when {
                        scans.isEmpty() -> null
                        scans.size == 1 -> formatDate(scans.first().timestamp)
                        else -> formatScanDateRange(scans.last().timestamp, scans.first().timestamp)
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            dateRangeText = dateRangeText,
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
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun formatDate(ms: Long): String =
        SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY_YEAR, Locale.getDefault())
            .format(Date(ms))

    private fun formatTime(ms: Long): String =
        SimpleDateFormat(DateFormatConstants.HOUR_MINUTE_24, Locale.getDefault())
            .format(Date(ms))

    private fun formatScanDateRange(oldestMs: Long, newestMs: Long): String {
        val oldest = Date(oldestMs)
        val newest = Date(newestMs)
        val cal = Calendar.getInstance()
        cal.time = oldest
        val oldestYear = cal.get(Calendar.YEAR)
        cal.time = newest
        val newestYear = cal.get(Calendar.YEAR)
        val monthDay = SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY, Locale.getDefault())
        val monthDayYear =
            SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY_YEAR, Locale.getDefault())
        return if (oldestYear == newestYear) {
            "${monthDay.format(oldest)} – ${monthDay.format(newest)}, $oldestYear"
        } else {
            "${monthDayYear.format(oldest)} – ${monthDayYear.format(newest)}"
        }
    }

    companion object {
        private const val MAX_SCANS = 100L
    }
}
