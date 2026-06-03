package com.hexis.bi.ui.main.home

import androidx.annotation.DrawableRes
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

enum class IntelligenceScoreKey { VBI, VLI, POA, LONGEVITY }

data class IntelligenceScoreData(
    val key: IntelligenceScoreKey,
    val title: String,
    val value: String,
    val level: ScoreLevel,
)

internal const val OVERVIEW_SLEEP_INDEX = 0
internal const val OVERVIEW_ACTIVITY_INDEX = 1
internal const val OVERVIEW_RECOVERY_INDEX = 2
internal const val OVERVIEW_SCAN_INDEX = 3

internal fun buildOverviewCards(
    sleepCard: OverviewCardData,
    activityCard: OverviewCardData,
    recoveryCard: OverviewCardData,
    scanCard: OverviewCardData,
): List<OverviewCardData> = listOf(sleepCard, activityCard, recoveryCard, scanCard)

private val defaultIntelligenceScores = listOf(
    IntelligenceScoreData(IntelligenceScoreKey.VBI, title = "VBI Score", value = "20", level = ScoreLevel.Low),
    IntelligenceScoreData(IntelligenceScoreKey.VLI, title = "VLI Score", value = "60", level = ScoreLevel.Medium),
    IntelligenceScoreData(IntelligenceScoreKey.POA, title = "POA Score", value = "100", level = ScoreLevel.High),
    IntelligenceScoreData(IntelligenceScoreKey.LONGEVITY, title = "Longevity", value = "82", level = ScoreLevel.High),
)

data class HomeState(
    val userName: String = "",
    val imageUrl: String? = null,
    val weight: String? = null,
    val height: String? = null,
    val age: String? = null,
    val isSuitConnected: Boolean = false,
    val hasUnreadNotifications: Boolean = false,
    val sleepGoalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
    val activityGoalSteps: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val overviewCards: List<OverviewCardData> = emptyList(),
    val intelligenceScores: List<IntelligenceScoreData> = defaultIntelligenceScores,
)
