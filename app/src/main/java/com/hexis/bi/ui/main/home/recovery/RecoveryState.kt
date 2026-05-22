package com.hexis.bi.ui.main.home.recovery

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.ui.theme.Yellow
import com.hexis.bi.utils.constants.RecoveryConstants

enum class RecoveryTab(@StringRes val labelRes: Int) {
    Day(R.string.recovery_tab_day),
    Summary(R.string.recovery_tab_summary),
}

enum class RecoveryLoadState { Loading, Ready, Error }

enum class RecoveryStatus(
    @StringRes val labelRes: Int,
    val color: Color,
) {
    Ready(R.string.recovery_status_ready, Blue300),
    Recovering(R.string.recovery_status_recovering, Yellow),
    Low(R.string.recovery_status_low, Red100);

    companion object {
        fun fromScore(score: Int): RecoveryStatus = when {
            score >= RecoveryConstants.STATUS_READY_MIN -> Ready
            score >= RecoveryConstants.STATUS_RECOVERING_MIN -> Recovering
            else -> Low
        }
    }
}

enum class RecoveryTrend(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val color: Color,
) {
    Stable(R.string.recovery_trend_flat, R.string.recovery_trend_flat_description, Blue300),
    Improving(R.string.recovery_trend_up, R.string.recovery_trend_up_description, Green),
    Decreasing(R.string.recovery_trend_down, R.string.recovery_trend_down_description, Red100),
}

data class RecoveryMetric(
    @StringRes val labelRes: Int,
    val value: String,
    /** Optional unit rendered after [value] in a muted color (e.g. "bpm"). */
    val unit: String? = null,
)

data class DailyRecoveryEntry(
    val dayLabel: String,
    val score: Int,
    val isHighlighted: Boolean = false,
    /** Fuller label shown in the press tooltip (e.g. "Dec 24"); falls back to [dayLabel]. */
    val tooltipLabel: String = "",
)

data class RecoveryState(
    val selectedTab: RecoveryTab = RecoveryTab.Day,

    // Day tab — load status
    val dayLoadState: RecoveryLoadState = RecoveryLoadState.Loading,
    val dayErrorMessage: String? = null,

    // Summary tab — load status
    val summaryLoadState: RecoveryLoadState = RecoveryLoadState.Loading,
    val summaryErrorMessage: String? = null,

    // Day tab
    val dateLabel: String = "",
    val score: Int = 0,
    val metrics: List<RecoveryMetric> = emptyList(),
    val rmssdMs: Int = 0,
    val sdnnMs: Int = 0,
    val canGoNextDay: Boolean = false,

    // Summary tab
    val weekLabel: String = "",
    val weeklyEntries: List<DailyRecoveryEntry> = emptyList(),
    val avgScore: Int = 0,
    val trend: RecoveryTrend = RecoveryTrend.Stable,
    val canGoNextWeek: Boolean = false,

    // Info bottom sheet
    val showInfoSheet: Boolean = false,
) {
    val status: RecoveryStatus get() = RecoveryStatus.fromScore(score)
}
