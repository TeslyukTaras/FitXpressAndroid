package com.hexis.bi.ui.main.home

import androidx.annotation.DrawableRes
import com.hexis.bi.R

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

private val defaultOverviewCards = listOf(
    OverviewCardData(
        title = "Sleep Score",
        iconRes = R.drawable.ic_moon,
        value = "7.5",
        valueLabel = "h",
        subtitle = "Goal: 8 h",
        variant = OverviewCardVariant.Accent,
    ),
    OverviewCardData(
        title = "Activity Score",
        iconRes = R.drawable.ic_steps,
        value = "9,500",
        valueLabel = "steps",
        subtitle = "Goal: 10,000",
    ),
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
    val overviewCards: List<OverviewCardData> = defaultOverviewCards,
    val intelligenceScores: List<IntelligenceScoreData> = defaultIntelligenceScores,
)
