package com.hexis.bi.ui.main.home.recovery

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.ui.theme.Yellow

enum class RecoveryTab { Day, Summary }

enum class RecoveryStatus(
    @StringRes val labelRes: Int,
    val color: Color,
) {
    Ready(R.string.recovery_status_ready, Blue300),
    Recovering(R.string.recovery_status_recovering, Yellow),
    Low(R.string.recovery_status_low, Red100);

    companion object {
        fun fromScore(score: Int): RecoveryStatus = when {
            score >= 70 -> Ready
            score >= 40 -> Recovering
            else -> Low
        }
    }
}

data class RecoveryMetric(
    @StringRes val labelRes: Int,
    val value: String,
)

data class DailyRecoveryEntry(
    val dayLabel: String,
    val score: Int,
    val isHighlighted: Boolean = false,
)

data class RecoveryState(
    val selectedTab: RecoveryTab = RecoveryTab.Day,

    // Day tab
    val dateLabel: String = "",
    val score: Int = 0,
    val metrics: List<RecoveryMetric> = emptyList(),
    val canGoNextDay: Boolean = false,

    // Summary tab
    val weekLabel: String = "",
    val weeklyEntries: List<DailyRecoveryEntry> = emptyList(),
    val avgScore: Int = 0,
    val trendLabel: String = "Stable",
    val trendDescription: String = "Recovery stayed stable this week.",
    val canGoNextWeek: Boolean = false,

    // Info bottom sheet
    val showInfoSheet: Boolean = false,
) {
    val status: RecoveryStatus get() = RecoveryStatus.fromScore(score)
}
