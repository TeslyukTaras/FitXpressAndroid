package com.hexis.bi.ui.main.home

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.IntelligenceConstants
import com.hexis.bi.utils.constants.SleepConstants

/** Activity overview card: today's step total plus the hourly distribution for the mini bar chart. */
data class ActivityOverview(
    val steps: String = "",
    val hourlySteps: List<Float> = emptyList(),
)

/** Sleep overview card: last night's duration and the goal it is measured against. */
data class SleepOverview(
    val durationMinutes: Int = 0,
    val goalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
) {
    val goalFraction: Float
        get() = if (goalHours > 0) {
            (durationMinutes / 60f / goalHours).coerceIn(0f, 1f)
        } else 0f
}

/** Scan overview card: the headline key change plus a trend series for the sparkline. */
data class ScanOverview(
    val value: String = "",
    val unit: String? = null,
    val valueLabel: String? = null,
    val subtitle: String = "",
    val changePositive: Boolean? = null,
    val trend: List<Float> = emptyList(),
)

enum class IntelligenceScoreKey { RECOVERY, PHYSIQUE_DRIFT, LONGEVITY, PACE_OF_AGING }

data class IntelligenceScoreData(
    val key: IntelligenceScoreKey,
    @StringRes val titleRes: Int,
    val value: String,
    /** Gauge fill in [0, 1]. */
    val fraction: Float,
    /** True when there is no real data yet; the gauge shows a "Coming" placeholder. */
    val comingSoon: Boolean = false,
)

data class HomeState(
    val userName: String = "",
    val imageUrl: String? = null,
    val latestScanDate: String? = null,
    val weight: String? = null,
    val height: String? = null,
    val age: String? = null,
    val isSuitConnected: Boolean = false,
    val hasUnreadNotifications: Boolean = false,
    val sleepGoalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
    val activityGoalSteps: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val activity: ActivityOverview = ActivityOverview(),
    val sleep: SleepOverview = SleepOverview(),
    val scan: ScanOverview = ScanOverview(),
    val recoveryScore: Int? = null,
    val longevityScore: Int? = null,
) {
    /**
     * The four "Body Intelligence" gauges. Recovery reflects the loaded score; the rest show a
     * "Coming" placeholder until their data sources land (no mock values).
     */
    val intelligenceScores: List<IntelligenceScoreData>
        get() {
            val recovery = (recoveryScore ?: 0).coerceIn(0, IntelligenceConstants.MAX_SCORE_INT)
            return listOf(
                IntelligenceScoreData(
                    key = IntelligenceScoreKey.RECOVERY,
                    titleRes = R.string.intelligence_recovery,
                    value = recovery.toString(),
                    fraction = recovery / IntelligenceConstants.MAX_SCORE,
                ),
                comingSoon(IntelligenceScoreKey.PHYSIQUE_DRIFT, R.string.intelligence_physique_drift),
                longevityScore?.let { score ->
                    val clamped = score.coerceIn(0, IntelligenceConstants.MAX_SCORE_INT)
                    IntelligenceScoreData(
                        key = IntelligenceScoreKey.LONGEVITY,
                        titleRes = R.string.intelligence_longevity,
                        value = clamped.toString(),
                        fraction = clamped / IntelligenceConstants.MAX_SCORE,
                    )
                } ?: comingSoon(IntelligenceScoreKey.LONGEVITY, R.string.intelligence_longevity),
                comingSoon(IntelligenceScoreKey.PACE_OF_AGING, R.string.intelligence_pace_of_aging),
            )
        }

    private fun comingSoon(key: IntelligenceScoreKey, @StringRes titleRes: Int) =
        IntelligenceScoreData(
            key = key,
            titleRes = titleRes,
            value = "",
            fraction = 0f,
            comingSoon = true,
        )
}
