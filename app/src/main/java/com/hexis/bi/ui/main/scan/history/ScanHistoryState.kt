package com.hexis.bi.ui.main.scan.history

import com.hexis.bi.data.scan.TopChangeVsPrevious

data class ScanHistoryState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val dateRangeText: String? = null,
    val items: List<ScanHistoryListItem> = emptyList(),
)

data class ScanHistoryListItem(
    val scanId: String,
    val dateLabel: String,
    val timeLabel: String,
    val topChange: TopChangeVsPrevious?,
)
