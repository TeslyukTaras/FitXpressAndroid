package com.hexis.bi.ui.main.home.sleep

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.hexis.bi.R
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.domain.enums.HealthProvider
import com.hexis.bi.utils.constants.SleepConstants

enum class SleepTab { Day, Summary }

enum class SleepQuality {
    Good, Fair, Poor;

    @StringRes
    fun nameRes(): Int = when (this) {
        Good -> R.string.sleep_quality_good
        Fair -> R.string.sleep_quality_fair
        Poor -> R.string.sleep_quality_poor
    }
}

enum class SleepLoadState { Loading, Ready, Error }

enum class StageTrend { Up, Down }

@StringRes
fun SleepStage.nameRes(): Int = when (this) {
    SleepStage.Deep -> R.string.sleep_stage_deep
    SleepStage.REM -> R.string.sleep_stage_rem
    SleepStage.Light -> R.string.sleep_stage_light
    SleepStage.Awake -> R.string.sleep_stage_awake
}

@StringRes
fun HealthProvider.nameRes(): Int = when (this) {
    HealthProvider.GoogleHealth -> R.string.health_provider_google_health
    HealthProvider.AppleHealth -> R.string.health_provider_apple_health
}

data class SleepStageData(
    val stage: SleepStage,
    val durationMinutes: Int,
    val color: Color,
    /** Average HRV (RMSSD, ms) during this stage; falls back to the night average. */
    val hrv: Int = 0,
    /** Average heart rate (bpm) during this stage; falls back to the night resting rate. */
    val rhr: Int = 0,
)

data class TimelineSegment(
    val stage: SleepStage,
    val startFraction: Float,
    val endFraction: Float,
)

/** A single charted point: [fraction] across the night (0..1) and its [value]. */
data class ChartPoint(
    val fraction: Float,
    val value: Int,
)

/** One day's per-stage minutes, used to draw a stacked bar in the weekly Sleep Structure chart. */
data class DailyStructure(
    val dayLabel: String,
    val isHighlighted: Boolean = false,
    val stageMinutes: Map<SleepStage, Int> = emptyMap(),
) {
    val totalMinutes: Int get() = stageMinutes.values.sum()
}

data class WeeklyStageData(
    val stage: SleepStage,
    val durationMinutes: Int,
    val trend: StageTrend? = null,
)

data class SleepState(
    val selectedTab: SleepTab = SleepTab.Day,

    // Day tab — load status
    val dayLoadState: SleepLoadState = SleepLoadState.Loading,
    val errorMessage: String? = null,
    val dayLabel: String = "",
    val canGoNextDay: Boolean = false,

    // Summary tab — load status
    val summaryLoadState: SleepLoadState = SleepLoadState.Loading,
    val summaryErrorMessage: String? = null,

    // Day tab — sleep status
    val totalSleepMinutes: Int = 0,
    val sleepGoalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
    val stages: List<SleepStageData> = emptyList(),

    // Day tab — sleep metrics
    val hrv: Int = 0,
    val restingHeartRate: Int = 0,
    val hrvSeries: List<ChartPoint> = emptyList(),
    val rhrSeries: List<ChartPoint> = emptyList(),

    // Day tab — timeline
    val timelineStartHour: Int = 23,
    val timelineEndHour: Int = 6,
    val timelineSegments: List<TimelineSegment> = emptyList(),

    // Day tab — insight for Sleep & Body Recovery
    @StringRes val insightRes: Int = R.string.sleep_recovery_subtitle,

    // Summary tab
    val weekLabel: String = "",
    val weeklyStructure: List<DailyStructure> = emptyList(),
    val weeklyStages: List<WeeklyStageData> = emptyList(),
    val canGoNextWeek: Boolean = false,

    // Settings dialog
    val showSettingsDialog: Boolean = false,
    val sleepGoalHoursDraft: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
    val dataSource: HealthProvider = HealthProvider.GoogleHealth,

    // Recovery bottom sheet
    val showRecoverySheet: Boolean = false,
)
