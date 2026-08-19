package com.hexis.bi.ui.main.home

import com.hexis.bi.ui.main.home.intelligence.InsightCard

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.longevity.LongevityDirection
import com.hexis.bi.ui.main.buysuit.orderdetails.OrderDetailsUi
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.IntelligenceConstants
import com.hexis.bi.utils.constants.SleepConstants
import java.util.Locale

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

sealed interface SuitSection {
    data object None : SuitSection

    data class Order(val data: SuitOrderOverview) : SuitSection

    data object Promo : SuitSection
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

enum class IntelligenceScoreKey { PHYSIQUE_DRIFT, PACE_OF_AGING, LONGEVITY, RECOMPOSITION }

sealed interface IntelligenceTileValue {
    data class Gauge(
        val value: String,
        val fraction: Float,
    ) : IntelligenceTileValue

    data class Direction(val direction: LongevityDirection) : IntelligenceTileValue

    data class Amount(val value: String, @StringRes val unitRes: Int) : IntelligenceTileValue
}

data class IntelligenceScoreData(
    val key: IntelligenceScoreKey,
    @StringRes val titleRes: Int,
    val value: IntelligenceTileValue,
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
    val insights: List<InsightCard> = emptyList(),
    val insightsLoaded: Boolean = false,
    val insightsUpdating: Boolean = false,
    val recompositionValue: String = "0",
    val physiqueScore: Float? = null,
    val longevityDirection: LongevityDirection = LongevityDirection.BuildingYourTrend,
    val paceOfAgingValue: String? = null,
    val paceOfAgingScore: Int? = null,
    val isActivitySyncing: Boolean = false,
    val isSleepSyncing: Boolean = false,
) {
    val isAnyHealthDataSyncing: Boolean get() = isActivitySyncing || isSleepSyncing

    val suitSection: SuitSection
        get() = when {
            suitOrder != null -> SuitSection.Order(suitOrder)
            suitSectionResolved && !isSuitConnected -> SuitSection.Promo
            else -> SuitSection.None
        }

    /**
     * The four "Body Intelligence" tiles. Physique Drift (a 0-10 score) and Pace of Aging (the pace
     * multiplier with a derived 0-100 fill) read as gauges; Longevity reports the trend word its own
     * screen leads with, and Recomposition the kilograms recomposed.
     */
    val intelligenceScores: List<IntelligenceScoreData>
        get() = listOf(
            physiqueTile(),
            paceTile(),
            longevityTile(),
            recompositionTile(),
        )

    private fun physiqueTile(): IntelligenceScoreData {
        val clamped = (physiqueScore ?: 0f).coerceIn(PHYSIQUE_MIN_SCORE, PHYSIQUE_MAX_SCORE)
        return IntelligenceScoreData(
            key = IntelligenceScoreKey.PHYSIQUE_DRIFT,
            titleRes = R.string.intelligence_physique_drift,
            value = IntelligenceTileValue.Gauge(
                value = if (physiqueScore == null) "0"
                else String.format(Locale.US, PHYSIQUE_SCORE_FORMAT, clamped),
                fraction = clamped / PHYSIQUE_MAX_SCORE,
            ),
        )
    }

    private fun paceTile(): IntelligenceScoreData {
        val clamped = (paceOfAgingScore ?: 0).coerceIn(0, IntelligenceConstants.MAX_SCORE_INT)
        return IntelligenceScoreData(
            key = IntelligenceScoreKey.PACE_OF_AGING,
            titleRes = R.string.intelligence_pace_of_aging,
            value = IntelligenceTileValue.Gauge(
                value = paceOfAgingValue ?: 0.toString(),
                fraction = clamped / IntelligenceConstants.MAX_SCORE,
            ),
        )
    }

    private fun longevityTile(): IntelligenceScoreData =
        IntelligenceScoreData(
            key = IntelligenceScoreKey.LONGEVITY,
            titleRes = R.string.intelligence_longevity,
            value = IntelligenceTileValue.Direction(longevityDirection),
        )

    private fun recompositionTile(): IntelligenceScoreData =
        IntelligenceScoreData(
            key = IntelligenceScoreKey.RECOMPOSITION,
            titleRes = R.string.intelligence_recomposition,
            value = IntelligenceTileValue.Amount(recompositionValue, R.string.unit_kg),
        )

    private companion object {
        const val PHYSIQUE_MIN_SCORE = 0f
        const val PHYSIQUE_MAX_SCORE = 10f
        const val PHYSIQUE_SCORE_FORMAT = "%.1f"
    }
}
