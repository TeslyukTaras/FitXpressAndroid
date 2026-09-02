package com.hexis.bi.domain.body

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.prediction.PredictionSeries
import com.hexis.bi.intelligence.prediction.ScanDay
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal fun List<ScanRecord>.predictionDays(
    config: EngineConfig,
    heightCm: Float?,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ScanDay> {
    val buckets = asSequence()
        .mapNotNull { scan -> scan.predictionInput(config, heightCm)?.let { scan to it } }
        .groupBy { (scan, _) -> LocalDate.ofInstant(Date(scan.timestamp).toInstant(), zone) }
        .map { (date, sameDay) ->
            val inputs = sameDay.map { (_, input) -> input }
            val values = averageSeries(inputs.map { it.values }).toMutableMap()
            val averageLeanKg = values[PredictionSeries.LEAN_KG]
            val averageWeightKg = inputs.map { it.weightKg }.average()
            if (averageLeanKg != null && averageWeightKg > 0.0) {
                values[PredictionSeries.LEAN_PCT] = averageLeanKg / averageWeightKg * 100.0
            }
            val representative = sameDay.maxBy { (scan, _) -> scan.timestamp }.first
            PredictionBucket(
                day = ScanDay(
                    date = date.toString(),
                    scanId = representative.id,
                    values = values,
                ),
                representative = representative,
            )
        }
        .sortedBy { it.day.date }

    val steps = DoubleArray(buckets.size)
    for (index in 1 until buckets.size) {
        val delta = comparablePhysiqueScoreDelta(
            latest = buckets[index].representative,
            previous = buckets[index - 1].representative,
            config = config,
            heightCm = buckets[index].representative.heightCm ?: heightCm,
        )?.toDouble() ?: 0.0
        steps[index] = steps[index - 1] + delta
    }

    // Anchored on the newest scan so the latest comparable score is the score the user is shown.
    val anchor = buckets.lastOrNull()?.day?.values?.get(PredictionSeries.SCORE)
        ?: return buckets.map { it.day }
    val base = anchor - steps.last()
    return buckets.mapIndexed { index, bucket ->
        bucket.day.copy(
            values = bucket.day.values +
                    (PredictionSeries.COMPARABLE_SCORE to (base + steps[index]).coerceIn(1.0, 10.0)),
        )
    }
}

private data class PredictionBucket(
    val day: ScanDay,
    val representative: ScanRecord,
)

private data class PredictionInput(
    val weightKg: Double,
    val values: Map<PredictionSeries, Double>,
)

private fun ScanRecord.predictionInput(
    config: EngineConfig,
    heightCm: Float?,
): PredictionInput? {
    val weight = weightKg?.toDouble()?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val fatPct = fatPercentage?.toDouble()
        ?.takeIf { it.isFinite() && it in 0.0..100.0 }
        ?: return null
    // Calibration score is isolated to the scan document. The profile height is only a
    // compatibility fallback for older scan documents that did not persist their own height.
    val breakdown = physiqueScoreBreakdown(config, this.heightCm ?: heightCm)

    return PredictionInput(weightKg = weight, values = buildMap {
        put(PredictionSeries.FAT_PCT, fatPct)
        leanBodyMassKg?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            put(PredictionSeries.LEAN_KG, it)
            put(PredictionSeries.LEAN_PCT, it / weight * 100.0)
        }
        fatBodyMassKg?.toDouble()?.takeIf { it.isFinite() && it >= 0.0 }?.let {
            put(PredictionSeries.FAT_KG, it)
        }
        breakdown?.let { parts ->
            parts.score.toDouble().takeIf(Double::isFinite)?.let {
                put(PredictionSeries.SCORE, it)
            }
        }
    })
}

private fun averageSeries(
    values: List<Map<PredictionSeries, Double>>,
): Map<PredictionSeries, Double> = PredictionSeries.entries
    .mapNotNull { series ->
        val present = values.mapNotNull { it[series] }
        if (present.isEmpty()) null else series to present.sum() / present.size
    }
    .toMap()
