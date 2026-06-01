package com.hexis.bi.ui.main.home.longevity

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Drives the Longevity screen. There is no longevity data source yet, so the screen renders
 * representative static values matching the design; swap [buildState] for a repository call when
 * real data is available.
 */
class LongevityViewModel(application: Application) : BaseViewModel(application) {

    private val _state = MutableStateFlow(buildState())
    val state: StateFlow<LongevityState> = _state.asStateFlow()

    fun selectTab(tab: LongevityTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun showInfoSheet() {
        _state.update { it.copy(showInfoSheet = true) }
    }

    fun dismissInfoSheet() {
        _state.update { it.copy(showInfoSheet = false) }
    }

    private fun buildState(): LongevityState = LongevityState(
        selectedTab = LongevityTab.Daily,
        score = 82,
        syncedDate = "Dec 24, 2026",
        daily = LongevityTrendData(
            points = listOf(25f, 42f, 33f, 46f, 22f, 30f, 75f),
            axisLabels = listOf("12 am", "11:59 pm"),
            dateLabel = "Dec 24",
            trend = LongevityTrend.Improving,
        ),
        weekly = LongevityTrendData(
            points = listOf(25f, 40f, 26f, 52f, 26f, 34f, 75f),
            axisLabels = listOf("18", "19", "20", "21", "22", "23", "24"),
            currentLabelIndex = 6,
            dateLabel = "Dec 18 - Dec 24",
            trend = LongevityTrend.Improving,
        ),
        signals = buildSignals(),
        statusSignals = buildStatusSignals(),
    )

    private fun buildSignals(): List<LongevitySignal> = listOf(
        LongevitySignal(R.string.longevity_signal_hrv, "54", R.string.longevity_unit_ms),
        LongevitySignal(R.string.longevity_signal_rhr, "68", R.string.unit_bpm),
        LongevitySignal(R.string.longevity_signal_sleep, "7 h 30 m"),
        LongevitySignal(R.string.longevity_signal_recovery, "87 %"),
        LongevitySignal(R.string.longevity_signal_activity, "7,100", R.string.longevity_unit_steps),
        LongevitySignal(R.string.longevity_signal_vo2_max, "42"),
    )

    private fun buildStatusSignals(): List<LongevitySignal> = listOf(
        LongevitySignal(
            R.string.longevity_signal_waist_profile,
            appContext.getString(R.string.longevity_trend_improving),
        ),
        LongevitySignal(
            R.string.longevity_signal_physique_trend,
            appContext.getString(R.string.longevity_trend_stable),
        ),
        LongevitySignal(
            R.string.longevity_signal_stress_load,
            appContext.getString(R.string.longevity_trend_improving),
        ),
    )
}
