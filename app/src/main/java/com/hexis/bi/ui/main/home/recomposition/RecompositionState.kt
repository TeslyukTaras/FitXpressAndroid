package com.hexis.bi.ui.main.home.recomposition

import androidx.annotation.StringRes
import com.hexis.bi.R

enum class RecompositionWindow(@StringRes val labelRes: Int) {
    FourWeeks(R.string.recomposition_window_4w),
    SixMonths(R.string.recomposition_window_6m),
    OneYear(R.string.recomposition_window_1y),
}

data class RecompositionMetricUi(
    val valueText: String,
    val favorable: Boolean? = null,
    val markerFraction: Float? = null,
)

data class RecompositionCardUi(
    val window: RecompositionWindow,
    val isRecomposition: Boolean = false,
    val recomposedValue: String = "",
    val weightChangeText: String = "",
    val weightSubtitle: String = "",
    val fat: RecompositionMetricUi = RecompositionMetricUi(""),
    val lean: RecompositionMetricUi = RecompositionMetricUi(""),
)

data class RecompositionState(
    val cards: List<RecompositionCardUi> = emptyList(),
    val showInfoSheet: Boolean = false,
)
