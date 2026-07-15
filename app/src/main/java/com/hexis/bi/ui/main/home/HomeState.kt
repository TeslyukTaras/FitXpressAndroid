package com.hexis.bi.ui.main.home

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.ui.main.buysuit.orderdetails.OrderDetailsUi
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

/**
 * Home "Suit order" card; replaces the buy-a-suit promo banner while an order is in flight.
 * All fields arrive display-ready from the ViewModel.
 */
data class SuitOrderOverview(
    val status: String,
    /** "Tracking:" once a tracking number exists, "Order:" before that. */
    val referenceLabel: String,
    /** Masked tracking / order number, e.g. "1Z***893". */
    val referenceValue: String,
    /** Short date like "Dec 29"; null until an ETA is known. */
    val eta: String?,
)

/** Scan overview card: the headline key change plus a trend series for the sparkline. */
data class ScanOverview(
    val value: String = "",
    val unit: String? = null,
    val valueLabel: String? = null,
    val subtitle: String = "",
    val changePositive: Boolean? = null,
    val trend: List<Float> = emptyList(),
)

enum class IntelligenceScoreKey { RECOMPOSITION, PHYSIQUE_DRIFT, LONGEVITY, PACE_OF_AGING }

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
    val suitOrder: SuitOrderOverview? = null,
    val orderDetails: OrderDetailsUi? = null,
    val suitSectionResolved: Boolean = false,
    val showOrderDetails: Boolean = false,
    val hasUnreadNotifications: Boolean = false,
    val sleepGoalHours: Int = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
    val activityGoalSteps: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val activity: ActivityOverview = ActivityOverview(),
    val sleep: SleepOverview = SleepOverview(),
    val scan: ScanOverview = ScanOverview(),
    val recompositionValue: String = "0",
    val recompositionFraction: Float = 0f,
    val recoveryScore: Int? = null,
    val physiqueScore: Float? = null,
    val longevityScore: Int? = null,
    val paceOfAgingValue: String? = null,
    val paceOfAgingScore: Int? = null,
) {
    /**
     * The four "Body Intelligence" gauges. Longevity uses a 0-100 score; Physique Drift uses a
     * 1-10 score; Pace of Aging displays the pace multiplier with a derived 0-100 fill.
     */
    val intelligenceScores: List<IntelligenceScoreData>
        get() = listOf(
            recompositionGauge(),
            physiqueGauge(),
            scoreGauge(IntelligenceScoreKey.LONGEVITY, R.string.intelligence_longevity, longevityScore),
            paceGauge(),
        )

    private fun recompositionGauge(): IntelligenceScoreData =
        IntelligenceScoreData(
            key = IntelligenceScoreKey.RECOMPOSITION,
            titleRes = R.string.intelligence_recomposition,
            value = recompositionValue,
            fraction = recompositionFraction.coerceIn(0f, 1f),
        )

    private fun scoreGauge(key: IntelligenceScoreKey, @StringRes titleRes: Int, score: Int?): IntelligenceScoreData {
        val clamped = (score ?: 0).coerceIn(0, IntelligenceConstants.MAX_SCORE_INT)
        return IntelligenceScoreData(
            key = key,
            titleRes = titleRes,
            value = clamped.toString(),
            fraction = clamped / IntelligenceConstants.MAX_SCORE,
        )
    }

    private fun paceGauge(): IntelligenceScoreData {
        val clamped = (paceOfAgingScore ?: 0).coerceIn(0, IntelligenceConstants.MAX_SCORE_INT)
        return IntelligenceScoreData(
            key = IntelligenceScoreKey.PACE_OF_AGING,
            titleRes = R.string.intelligence_pace_of_aging,
            value = paceOfAgingValue ?: 0.toString(),
            fraction = clamped / IntelligenceConstants.MAX_SCORE,
        )
    }

    private fun physiqueGauge(): IntelligenceScoreData {
        val clamped = (physiqueScore ?: 0f).coerceIn(PHYSIQUE_MIN_SCORE, PHYSIQUE_MAX_SCORE)
        return IntelligenceScoreData(
            key = IntelligenceScoreKey.PHYSIQUE_DRIFT,
            titleRes = R.string.intelligence_physique_drift,
            value = if (physiqueScore == null) "0" else String.format(java.util.Locale.US, "%.1f", clamped),
            fraction = clamped / PHYSIQUE_MAX_SCORE,
        )
    }

    private companion object {
        const val PHYSIQUE_MIN_SCORE = 0f
        const val PHYSIQUE_MAX_SCORE = 10f
    }
}
