package com.hexis.bi.ui.main.buysuit.suitsize

data class SuitSizeResultsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val isMetric: Boolean = true,
    val heightCm: Float = 179f,
    val weightKg: Float = 80f,
    val suitSize: String = "Medium",
    /** Scan the recommendation was derived from; carried into the order for provenance. */
    val scanId: String? = null,
)
