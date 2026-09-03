package com.hexis.bi.intelligence.prediction

import com.hexis.bi.intelligence.engine.EngineDates
import com.hexis.bi.intelligence.engine.olsChange
import com.hexis.bi.intelligence.prediction.PredictionConstants.APPLY_GAINS
import com.hexis.bi.intelligence.prediction.PredictionConstants.DEFAULT_GAIN
import com.hexis.bi.intelligence.prediction.PredictionConstants.HORIZON_DAYS
import com.hexis.bi.intelligence.prediction.PredictionConstants.MAX_GAIN
import com.hexis.bi.intelligence.prediction.PredictionConstants.MAX_WEEKLY_DELTA
import com.hexis.bi.intelligence.prediction.PredictionConstants.MIN_BUCKETS
import com.hexis.bi.intelligence.prediction.PredictionConstants.MIN_GAIN
import com.hexis.bi.intelligence.prediction.PredictionConstants.WINDOW_BUCKETS
import java.time.LocalDate

data class ScanDay(
    val date: String,
    val scanId: String,
    val values: Map<PredictionSeries, Double>,
)

data class WeeklyPrediction(
    val sourceScanId: String,
    val sourceDate: String,
    val targetDate: String,
    val bucketsUsed: Int,
    val gainsUsed: Map<PredictionSeries, Double>,
    val slopePerDay: Map<PredictionSeries, Double>,
    val sourceValue: Map<PredictionSeries, Double>,
    val predicted: Map<PredictionSeries, Double>,
)

fun predictWeekly(
    days: List<ScanDay>,
    gains: Map<PredictionSeries, Double> = emptyMap(),
): WeeklyPrediction? {
    val ordered = days.sortedBy { it.date }
    if (ordered.size < MIN_BUCKETS) return null

    val window = ordered.takeLast(WINDOW_BUCKETS)
    val source = ordered.last()

    val gainsUsed = LinkedHashMap<PredictionSeries, Double>()
    val slopePerDay = LinkedHashMap<PredictionSeries, Double>()
    val sourceValue = LinkedHashMap<PredictionSeries, Double>()
    val predicted = LinkedHashMap<PredictionSeries, Double>()

    for (series in PredictionSeries.entries) {
        val start = source.values[series] ?: continue
        val fitted = window.mapNotNull { day -> day.values[series]?.let { EngineDates.ordinal(day.date) to it } }
        if (fitted.size < MIN_BUCKETS) continue

        val slope = olsChange(fitted.map { it.first }, fitted.map { it.second }).slope
        val gain = effectiveGain(gains[series])
        val drift = (slope * HORIZON_DAYS * gain).coerceIn(-MAX_WEEKLY_DELTA, MAX_WEEKLY_DELTA)

        gainsUsed[series] = gain
        slopePerDay[series] = slope
        sourceValue[series] = start
        predicted[series] = series.clamp(start + drift)
    }

    if (predicted.isEmpty()) return null

    return WeeklyPrediction(
        sourceScanId = source.scanId,
        sourceDate = source.date,
        targetDate = LocalDate.parse(source.date).plusDays(HORIZON_DAYS.toLong()).toString(),
        bucketsUsed = window.size,
        gainsUsed = gainsUsed,
        slopePerDay = slopePerDay,
        sourceValue = sourceValue,
        predicted = predicted,
    )
}

internal fun effectiveGain(stored: Double?): Double {
    if (!APPLY_GAINS) return DEFAULT_GAIN
    return (stored?.takeIf(Double::isFinite) ?: DEFAULT_GAIN).coerceIn(MIN_GAIN, MAX_GAIN)
}
