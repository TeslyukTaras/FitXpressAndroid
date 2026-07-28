package com.hexis.bi.ui.main.home.longevity

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.utils.constants.LongevityConstants
import java.time.LocalDate

/** The [LongevityConstants.SCORE_WINDOW_DAYS]-day window ending today, oldest → newest. */
fun longevityScoreWindow(today: LocalDate): List<LocalDate> =
    (0 until LongevityConstants.SCORE_WINDOW_DAYS)
        .map { today.minusDays((LongevityConstants.SCORE_WINDOW_DAYS - 1 - it).toLong()) }

/** Waist-to-height from the scan's waist circumference and [heightCm], or null if either is missing. */
fun waistToHeightRatio(scan: ScanRecord?, heightCm: Float?): Float? {
    val height = heightCm?.takeIf { it > 0f } ?: return null
    val waist = scan?.let {
        BodyMeasurementKeys.valueFor(it.measurements, BodyMeasurementRegion.Waist)
    } ?: return null
    return waist / height
}
