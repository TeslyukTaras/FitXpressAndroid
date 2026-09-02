package com.hexis.bi.intelligence.prediction

import com.hexis.bi.intelligence.engine.EngineDates
import com.hexis.bi.intelligence.prediction.PredictionConstants.DEFAULT_GAIN
import com.hexis.bi.intelligence.prediction.PredictionConstants.GAIN_STEP
import com.hexis.bi.intelligence.prediction.PredictionConstants.IMPLAUSIBLE_DELTA
import com.hexis.bi.intelligence.prediction.PredictionConstants.MAX_ELAPSED_DAYS
import com.hexis.bi.intelligence.prediction.PredictionConstants.MAX_GAIN
import com.hexis.bi.intelligence.prediction.PredictionConstants.MAX_RATIO
import com.hexis.bi.intelligence.prediction.PredictionConstants.MAX_WEEKLY_DELTA
import com.hexis.bi.intelligence.prediction.PredictionConstants.MIN_ELAPSED_DAYS
import com.hexis.bi.intelligence.prediction.PredictionConstants.MIN_GAIN
import com.hexis.bi.intelligence.prediction.PredictionConstants.MIN_RATIO
import kotlin.math.abs

enum class SkipReason(val key: String) {
    NO_PREDICTION("noPrediction"),
    ELAPSED_OUT_OF_RANGE("elapsedOutOfRange"),
    PREDICTION_TOO_SMALL("predictionTooSmall"),
    IMPLAUSIBLE_SCAN("implausibleScan"),
    MISSING_SERIES("missingSeries"),
    ALREADY_APPLIED("alreadyApplied"),
}

data class SeriesEvaluation(
    val sourceValue: Double,
    val predicted: Double,
    val actualValue: Double,
    val expectedDelta: Double,
    val actualDelta: Double,
    val ratio: Double,
)

data class CalibrationResult(
    val sourceScanId: String,
    val actualScanId: String,
    val sourceDate: String,
    val actualDate: String,
    val elapsedDays: Int,
    val evaluated: Map<PredictionSeries, SeriesEvaluation>,
    val skipped: Map<PredictionSeries, SkipReason>,
    val gainsBefore: Map<PredictionSeries, Double>,
    val gainsAfter: Map<PredictionSeries, Double>,
)

fun calibrate(
    pending: WeeklyPrediction,
    actual: ScanDay,
    gains: Map<PredictionSeries, Double>,
): CalibrationResult {
    val elapsedDays = EngineDates.daysBetween(pending.sourceDate, actual.date)
    val outOfRange = elapsedDays < MIN_ELAPSED_DAYS || elapsedDays > MAX_ELAPSED_DAYS

    val evaluated = LinkedHashMap<PredictionSeries, SeriesEvaluation>()
    val skipped = LinkedHashMap<PredictionSeries, SkipReason>()
    val gainsBefore = LinkedHashMap<PredictionSeries, Double>()
    val gainsAfter = LinkedHashMap<PredictionSeries, Double>()

    for (series in PredictionSeries.PERSISTED) {
        val gain = storedGain(gains[series])
        gainsBefore[series] = gain
        gainsAfter[series] = gain

        if (outOfRange) {
            skipped[series] = SkipReason.ELAPSED_OUT_OF_RANGE
            continue
        }

        val source = pending.sourceValue[series]
        val slope = pending.slopePerDay[series]
        val actualValue = actual.values[series]
        if (source == null || slope == null || actualValue == null) {
            skipped[series] = SkipReason.MISSING_SERIES
            continue
        }

        val gainUsed = storedGain(pending.gainsUsed[series])
        val expectedDelta = (slope * gainUsed * elapsedDays)
            .coerceIn(-MAX_WEEKLY_DELTA, MAX_WEEKLY_DELTA)
        if (abs(expectedDelta) < series.negligibleDelta) {
            skipped[series] = SkipReason.PREDICTION_TOO_SMALL
            continue
        }

        val actualDelta = actualValue - source
        if (abs(actualDelta) > IMPLAUSIBLE_DELTA) {
            skipped[series] = SkipReason.IMPLAUSIBLE_SCAN
            continue
        }

        val ratio = actualDelta / expectedDelta
        val error = ratio.coerceIn(MIN_RATIO, MAX_RATIO) - 1.0

        evaluated[series] = SeriesEvaluation(
            sourceValue = source,
            predicted = pending.predicted[series] ?: source,
            actualValue = actualValue,
            expectedDelta = expectedDelta,
            actualDelta = actualDelta,
            ratio = ratio,
        )
        gainsAfter[series] = (gain + GAIN_STEP * error).coerceIn(MIN_GAIN, MAX_GAIN)
    }

    return CalibrationResult(
        sourceScanId = pending.sourceScanId,
        actualScanId = actual.scanId,
        sourceDate = pending.sourceDate,
        actualDate = actual.date,
        elapsedDays = elapsedDays,
        evaluated = evaluated,
        skipped = skipped,
        gainsBefore = gainsBefore,
        gainsAfter = gainsAfter,
    )
}

internal fun storedGain(value: Double?): Double =
    (value?.takeIf(Double::isFinite) ?: DEFAULT_GAIN).coerceIn(MIN_GAIN, MAX_GAIN)
