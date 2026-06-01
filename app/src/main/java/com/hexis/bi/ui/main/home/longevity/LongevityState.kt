package com.hexis.bi.ui.main.home.longevity

import androidx.annotation.StringRes
import com.hexis.bi.R

enum class LongevityTab(@StringRes val labelRes: Int) {
    Daily(R.string.longevity_tab_daily),
    Weekly(R.string.longevity_tab_weekly),
}

enum class LongevityTrend(@StringRes val labelRes: Int) {
    Improving(R.string.longevity_trend_improving),
    Stable(R.string.longevity_trend_stable),
    Decreasing(R.string.longevity_trend_decreasing),
}

/**
 * One cell in the Longevity signals grid: a muted [labelRes] above a teal [value] with an optional
 * muted [unitRes] (e.g. "ms", "bpm"). Status signals simply omit the unit.
 */
data class LongevitySignal(
    @StringRes val labelRes: Int,
    val value: String,
    @StringRes val unitRes: Int? = null,
)

/** A longevity trend series: y values in [0, 100] evenly spaced along the x-axis. */
data class LongevityTrendData(
    val points: List<Float> = emptyList(),
    val axisLabels: List<String> = emptyList(),
    /** Index into [axisLabels] of the current date, highlighted on the x-axis (-1 = none). */
    val currentLabelIndex: Int = -1,
    /** Date label shown under the section title (e.g. "Dec 24" or "Dec 18 - Dec 24"). */
    val dateLabel: String = "",
    val trend: LongevityTrend = LongevityTrend.Improving,
)

data class LongevityState(
    val selectedTab: LongevityTab = LongevityTab.Daily,
    val score: Int = 0,
    val syncedDate: String = "",
    val daily: LongevityTrendData = LongevityTrendData(),
    val weekly: LongevityTrendData = LongevityTrendData(),
    /** Numeric signals (HRV, RHR, Sleep, Recovery, Activity, VO2 Max). */
    val signals: List<LongevitySignal> = emptyList(),
    /** Status signals (Waist Profile, Physique Trend, Stress Load). */
    val statusSignals: List<LongevitySignal> = emptyList(),
    val showInfoSheet: Boolean = false,
)
