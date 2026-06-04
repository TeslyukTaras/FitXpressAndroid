package com.hexis.bi.ui.main.home.longevity

import com.hexis.bi.data.activity.ActivitySummary
import com.hexis.bi.data.recovery.RecoverySnapshot
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.domain.longevity.LongevityInputs
import com.hexis.bi.domain.longevity.computeLongevityScore
import com.hexis.bi.utils.constants.LongevityConstants
import java.time.LocalDate

/** The [LongevityConstants.SCORE_WINDOW_DAYS]-day window ending today, oldest → newest. */
fun longevityScoreWindow(today: LocalDate): List<LocalDate> =
    (0 until LongevityConstants.SCORE_WINDOW_DAYS)
        .map { today.minusDays((LongevityConstants.SCORE_WINDOW_DAYS - 1 - it).toLong()) }

/**
 * Current healthy-aging score over [days] (oldest → newest): the score of the newest day that has
 * data, else 0. Body composition and VO2 max change slowly, so the latest reading in the window
 * stands in for every day. Shared by the Home gauge and the Longevity screen so the two agree.
 */
fun currentLongevityScore(
    days: List<LocalDate>,
    recovery: List<RecoverySnapshot>,
    activity: List<ActivitySummary>,
    latestScan: ScanRecord?,
    heightCm: Float?,
): Int {
    val recoveryByDate = recovery.associateBy { it.date }
    val activityByDate = activity.associateBy { it.date }
    val bodyFat = latestScan?.fatPercentage
    val waistToHeight = waistToHeightRatio(latestScan, heightCm)
    val vo2Max = activity.filter { (it.vo2MaxMlPerMinPerKg ?: 0f) > 0f }
        .maxByOrNull { it.date }?.vo2MaxMlPerMinPerKg

    return days.reversed().firstNotNullOfOrNull { date ->
        computeLongevityScore(
            LongevityInputs(
                hrvMs = recoveryByDate[date]?.hrvMs,
                restingHeartRateBpm = recoveryByDate[date]?.restingHeartRateBpm,
                sleepScore = recoveryByDate[date]?.sleepScore,
                steps = activityByDate[date]?.steps,
                bodyFatPercent = bodyFat,
                waistToHeightRatio = waistToHeight,
                vo2Max = vo2Max,
            )
        )
    } ?: 0
}

/** Waist-to-height from the scan's waist circumference and [heightCm], or null if either is missing. */
fun waistToHeightRatio(scan: ScanRecord?, heightCm: Float?): Float? {
    val height = heightCm?.takeIf { it > 0f } ?: return null
    val waist = scan?.let {
        BodyMeasurementKeys.valueFor(it.measurements, BodyMeasurementRegion.Waist)
    } ?: return null
    return waist / height
}
