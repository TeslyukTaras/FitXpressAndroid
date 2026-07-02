package com.hexis.bi.ui.main.buysuit.suitsize

import com.hexis.bi.domain.order.SuitSize

data class SuitSizeResultsState(
    val isLoading: Boolean = true,
    val isMetric: Boolean = true,
    val heightCm: Float = 179f,
    val weightKg: Float = 80f,
    val suitSize: SuitSize = SuitSize.MEDIUM,
    /** Scan the recommendation was derived from; carried into the order for provenance. */
    val scanId: String? = null,
) {
    /** Only a recommendation backed by a real scan can be ordered (never the fallback defaults). */
    val canProceedToOrder: Boolean
        get() = scanId != null
}
