package com.hexis.bi.ui.main.scan.history

import com.hexis.bi.data.scan.TopChangeVsPrevious

data class ScanHistoryState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDateRangePicker: Boolean = false,
    val selectedStartDateMillis: Long? = null,
    val selectedEndDateMillis: Long? = null,
    val isMetric: Boolean = true,
    val items: List<ScanHistoryListItem> = emptyList(),
)

data class ScanHistoryListItem(
    val scanId: String,
    val dateLabel: String,
    val timeLabel: String,
    val topChange: TopChangeVsPrevious?,
)
