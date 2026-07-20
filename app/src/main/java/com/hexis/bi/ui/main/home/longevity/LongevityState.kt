package com.hexis.bi.ui.main.home.longevity

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.longevity.FoundationStatus
import com.hexis.bi.domain.longevity.LongevityDirection

enum class LongevityTrend(@StringRes val labelRes: Int) {
    Improving(R.string.longevity_trend_improving),
    Stable(R.string.longevity_trend_stable),
    Decreasing(R.string.longevity_trend_decreasing),
}

enum class LongevityWindow(@StringRes val labelRes: Int) {
    FourWeeks(R.string.longevity_window_4w),
    SixMonths(R.string.longevity_window_6m),
    OneYear(R.string.longevity_window_1y),
}

data class LongevityEvidenceUi(
    val label: String,
    val value: String,
    val unit: String = "",
)

data class LongevityFoundationUi(
    @StringRes val titleRes: Int,
    val status: FoundationStatus,
    val evidence: List<LongevityEvidenceUi> = emptyList(),
)

data class LongevityState(
    val selectedWindow: LongevityWindow = LongevityWindow.FourWeeks,
    val direction: LongevityDirection = LongevityDirection.BuildingYourTrend,
    val foundations: List<LongevityFoundationUi> = emptyList(),
    val showInfoSheet: Boolean = false,
)
