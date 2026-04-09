package com.hexis.bi.ui.main.home.sleep

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.hexis.bi.R
import com.hexis.bi.domain.enums.HealthProvider
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.BlueFadedIndicator100
import com.hexis.bi.ui.theme.BlueFadedIndicator200
import com.hexis.bi.ui.theme.BlueFadedIndicator300

enum class SleepTab { Day, Summary }

enum class SleepStage {
    Deep, REM, Light, Awake;

    @StringRes
    fun nameRes(): Int = when (this) {
        Deep -> R.string.sleep_stage_deep
        REM -> R.string.sleep_stage_rem
        Light -> R.string.sleep_stage_light
        Awake -> R.string.sleep_stage_awake
    }
}

enum class SleepQuality {
    Good, Fair, Poor;

    @StringRes
    fun nameRes(): Int = when (this) {
        Good -> R.string.sleep_quality_good
        Fair -> R.string.sleep_quality_fair
        Poor -> R.string.sleep_quality_poor
    }
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
)

data class TimelineSegment(
    val stage: SleepStage,
    val startFraction: Float,
    val endFraction: Float,
)

private val defaultStages = listOf(
    SleepStageData(SleepStage.Deep, 65, Blue300),
    SleepStageData(SleepStage.REM, 80, BlueFadedIndicator300),
    SleepStageData(SleepStage.Light, 250, BlueFadedIndicator200),
    SleepStageData(SleepStage.Awake, 25, BlueFadedIndicator100),
)

private val defaultTimelineSegments = listOf(
    TimelineSegment(SleepStage.Deep, 0.00f, 0.06f),
    TimelineSegment(SleepStage.Light, 0.06f, 0.12f),
    TimelineSegment(SleepStage.REM, 0.12f, 0.20f),
    TimelineSegment(SleepStage.Deep, 0.20f, 0.28f),
    TimelineSegment(SleepStage.Light, 0.28f, 0.33f),
    TimelineSegment(SleepStage.Awake, 0.33f, 0.36f),
    TimelineSegment(SleepStage.REM, 0.36f, 0.44f),
    TimelineSegment(SleepStage.Deep, 0.44f, 0.50f),
    TimelineSegment(SleepStage.Light, 0.50f, 0.58f),
    TimelineSegment(SleepStage.Awake, 0.58f, 0.61f),
    TimelineSegment(SleepStage.Light, 0.61f, 0.72f),
    TimelineSegment(SleepStage.REM, 0.72f, 0.80f),
    TimelineSegment(SleepStage.Light, 0.80f, 0.90f),
    TimelineSegment(SleepStage.Awake, 0.90f, 0.94f),
    TimelineSegment(SleepStage.Light, 0.94f, 1.00f),
)

data class SleepState(
    val selectedTab: SleepTab = SleepTab.Day,

    // Sleep status
    val totalSleepMinutes: Int = 450,           // 7h 30m
    val sleepQuality: SleepQuality = SleepQuality.Good,
    val sleepGoalHours: Int = 8,
    val stages: List<SleepStageData> = defaultStages,

    // Sleep metrics
    val restfulness: Int = 78,
    val restfulnessMax: Int = 100,
    val hrv: Int = 52,
    val restingHeartRate: Int = 58,

    // Timeline
    val timelineStartHour: Int = 23,            // 11 pm
    val timelineEndHour: Int = 6,               // 6 am
    val timelineSegments: List<TimelineSegment> = defaultTimelineSegments,

    // Settings dialog
    val showSettingsDialog: Boolean = false,
    val sleepGoalHoursDraft: Int = 8,           // editable copy while dialog is open
    val dataSource: HealthProvider = HealthProvider.GoogleHealth,

    // Recovery bottom sheet
    val showRecoverySheet: Boolean = false,
)
