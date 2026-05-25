package com.hexis.bi.domain.body

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.utils.constants.BodyConstants

internal fun ScanRecord.muscleMassPercentage(): Float? {
    val lean = leanBodyMassKg ?: return null
    val weight = weightKg ?: return null
    if (weight <= 0f) return null
    return (lean / weight) * 100f
}

internal fun ScanRecord.physiqueScore(heightCm: Float?): Float? =
    scoreFor(physiqueScoreParts(heightCm))

/**
 * Computes drift from components captured in both scans, so missing optional
 * measurements do not register as progress.
 */
internal fun comparablePhysiqueScoreDelta(
    latest: ScanRecord,
    previous: ScanRecord?,
    heightCm: Float?,
): Float? {
    previous ?: return null
    val latestParts = latest.physiqueScoreParts(heightCm)
    val previousParts = previous.physiqueScoreParts(heightCm)
    val sharedParts = latestParts.keys intersect previousParts.keys
    if (sharedParts.isEmpty()) return null
    val latestScore = scoreFor(latestParts.filterKeys { it in sharedParts }) ?: return null
    val previousScore = scoreFor(previousParts.filterKeys { it in sharedParts }) ?: return null
    return latestScore - previousScore
}

private enum class PhysiqueScoreMetric {
    BodyFat,
    LeanMass,
    WaistShape,
    Proportion,
}

private data class ScorePart(val weight: Float, val score: Float)

private fun ScanRecord.physiqueScoreParts(heightCm: Float?): Map<PhysiqueScoreMetric, ScorePart> =
    buildMap {
        fatPercentage?.let {
            put(
                PhysiqueScoreMetric.BodyFat,
                ScorePart(BodyConstants.PHYSIQUE_WEIGHT_BODY_FAT, bodyFatScore(it)),
            )
        }
        muscleMassPercentage()?.let {
            put(
                PhysiqueScoreMetric.LeanMass,
                ScorePart(BodyConstants.PHYSIQUE_WEIGHT_LEAN_MASS, leanMassScore(it)),
            )
        }
        waistToHeightRatio(heightCm)?.let {
            put(
                PhysiqueScoreMetric.WaistShape,
                ScorePart(BodyConstants.PHYSIQUE_WEIGHT_WAIST_SHAPE, waistShapeScore(it)),
            )
        }
        shoulderToWaistRatio()?.let {
            put(
                PhysiqueScoreMetric.Proportion,
                ScorePart(BodyConstants.PHYSIQUE_WEIGHT_PROPORTION, proportionScore(it)),
            )
        }
    }

private fun scoreFor(parts: Map<PhysiqueScoreMetric, ScorePart>): Float? {
    val totalWeight = parts.values.sumOf { it.weight.toDouble() }.toFloat()
    if (totalWeight <= 0f) return null
    return parts.values.sumOf { (it.score * it.weight).toDouble() }.toFloat()
        .div(totalWeight)
        .coerceIn(BodyConstants.PHYSIQUE_SCORE_MIN, BodyConstants.PHYSIQUE_SCORE_MAX)
}

private fun ScanRecord.waistToHeightRatio(heightCm: Float?): Float? {
    val height = heightCm?.takeIf { it > 0f } ?: return null
    val waist = BodyMeasurementKeys.valueFor(measurements, BodyMeasurementRegion.Waist) ?: return null
    return waist / height
}

private fun ScanRecord.shoulderToWaistRatio(): Float? {
    val shoulders = BodyMeasurementKeys.valueFor(measurements, BodyMeasurementRegion.Shoulders) ?: return null
    val waist = BodyMeasurementKeys.valueFor(measurements, BodyMeasurementRegion.Waist) ?: return null
    if (waist <= 0f) return null
    return shoulders / waist
}

private fun bodyFatScore(bodyFatPercent: Float): Float = when {
    bodyFatPercent < 9f -> 10f
    bodyFatPercent <= 11f -> interpolateScore(bodyFatPercent, 11f, 9f, 9f, 10f)
    bodyFatPercent <= 15f -> interpolateScore(bodyFatPercent, 15f, 12f, 8f, 9f)
    bodyFatPercent <= 19f -> interpolateScore(bodyFatPercent, 19f, 16f, 6f, 7f)
    bodyFatPercent <= 24f -> interpolateScore(bodyFatPercent, 24f, 20f, 4f, 5f)
    bodyFatPercent <= 35f -> interpolateScore(bodyFatPercent, 35f, 25f, 2f, 3f)
    else -> 1f
}

private fun leanMassScore(leanMassPercent: Float): Float =
    interpolateScore(
        value = leanMassPercent,
        lowInput = 60f,
        highInput = 90f,
        lowScore = 3f,
        highScore = 10f,
    )

private fun waistShapeScore(waistToHeightRatio: Float): Float =
    interpolateScore(
        value = waistToHeightRatio,
        lowInput = 0.62f,
        highInput = 0.43f,
        lowScore = 2f,
        highScore = 10f,
    )

private fun proportionScore(shoulderToWaistRatio: Float): Float =
    interpolateScore(
        value = shoulderToWaistRatio,
        lowInput = 1.15f,
        highInput = 1.65f,
        lowScore = 4f,
        highScore = 10f,
    )

private fun interpolateScore(
    value: Float,
    lowInput: Float,
    highInput: Float,
    lowScore: Float,
    highScore: Float,
): Float {
    val t = ((value - lowInput) / (highInput - lowInput)).coerceIn(0f, 1f)
    return (lowScore + (highScore - lowScore) * t)
        .coerceIn(BodyConstants.PHYSIQUE_SCORE_MIN, BodyConstants.PHYSIQUE_SCORE_MAX)
}
