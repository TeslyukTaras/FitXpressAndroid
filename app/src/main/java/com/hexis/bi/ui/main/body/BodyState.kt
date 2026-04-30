package com.hexis.bi.ui.main.body

data class BodyScanItem(
    val id: String,
    val timestamp: Long,
    val hasModel3d: Boolean,
    val measurementsCount: Int,
)

data class BodyState(
    val isLoading: Boolean = true,
    val scans: List<BodyScanItem> = emptyList(),
    val errorMessage: String? = null,
)
