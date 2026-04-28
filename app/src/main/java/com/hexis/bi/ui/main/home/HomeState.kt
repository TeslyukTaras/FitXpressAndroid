package com.hexis.bi.ui.main.home

import androidx.annotation.DrawableRes
import com.hexis.bi.R
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.SleepConstants

enum class OverviewCardVariant { Default, Accent, Primary }

enum class ScoreLevel { Low, Medium, High }

data class OverviewCardData(
    val title: String,
    @DrawableRes val iconRes: Int,
    val value: String,
    val valueLabel: String? = null,
    val subtitle: String,
    val variant: OverviewCardVariant = OverviewCardVariant.Default,
)

data class IntelligenceScoreData(
    val title: String,
    val value: String,
    val level: ScoreLevel,
)

internal const val OVERVIEW_SLEEP_INDEX = 0
internal const val OVERVIEW_ACTIVITY_INDEX = 1

internal fun buildOverviewCards(
    sleepCard: OverviewCardData,
    activityCard: OverviewCardData,
): List<OverviewCardData> = listOf(sleepCard, activityCard) + defaultOverviewCardsTail

/** Recovery and scan — still mock data until those features are wired. */
private val defaultOverviewCardsTail = listOf(
    OverviewCardData(
        title = "Recovery",
        iconRes = R.drawable.ic_refresh,
        value = "82/100",
        subtitle = "Ready",
    ),
    OverviewCardData(
        title = "Scan",
        iconRes = R.drawable.ic_body,
        value = "0.6 cm",
        valueLabel = "↓ waist",
        subtitle = "Dec 15 • Key change",
        variant = OverviewCardVariant.Primary,
    ),
)

private val defaultIntelligenceScores = listOf(
    IntelligenceScoreData(title = "VBI Score", value = "20", level = ScoreLevel.Low),
    IntelligenceScoreData(title = "VLI Score", value = "60", level = ScoreLevel.Medium),
    IntelligenceScoreData(title = "POA Score", value = "100", level = ScoreLevel.High),
)

data class HomeState(
    val userName: String = "",
    val avatarUrl: String? = null,
    val weight: String? = null,
    val height: String? = null,
    val age: String? = null,
    val isSuitConnected: Boolean = false,
    val sleepGoalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
    val activityGoalSteps: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val overviewCards: List<OverviewCardData> = emptyList(),
    val intelligenceScores: List<IntelligenceScoreData> = defaultIntelligenceScores,
)
