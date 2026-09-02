package com.hexis.bi.domain.body

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.engine.Metrics
import com.hexis.bi.intelligence.engine.PhysiqueComponents
import com.hexis.bi.intelligence.engine.physiqueParts
import com.hexis.bi.intelligence.engine.scoreFromParts

internal fun ScanRecord.muscleMassPercentage(): Float? {
    val lean = leanBodyMassKg ?: return null
    val weight = weightKg ?: return null
    if (weight <= 0f) return null
    return (lean / weight) * 100f
}

internal fun ScanRecord.physiqueScore(config: EngineConfig, heightCm: Float?): Float? =
    physiqueScoreBreakdown(config, heightCm)?.score

internal data class PhysiqueScoreBreakdown(
    val score: Float,
    val bodyFatPercent: Float?,
    val leanBodyPercent: Float?,
    val waistToHeightRatio: Float?,
    val shoulderToWaistRatio: Float?,
    val bodyFatScore: Float?,
    val leanMassScore: Float?,
    val waistShapeScore: Float?,
    val proportionScore: Float?,
)

internal fun ScanRecord.physiqueScoreBreakdown(
    config: EngineConfig,
    heightCm: Float?,
): PhysiqueScoreBreakdown? {
    val scoped = config.withHeight(heightCm)
    val parts = physiqueParts(dayMetrics(), scoped)
    val score = scoreFromParts(parts, scoped.composites.physique) ?: return null
    val descriptive = physiqueParts(dayMetrics(), scoped.withProportion())
    return PhysiqueScoreBreakdown(
        score = score.toFloat(),
        bodyFatPercent = fatPercentage,
        leanBodyPercent = muscleMassPercentage(),
        waistToHeightRatio = waistToHeightRatio(heightCm),
        shoulderToWaistRatio = shoulderToWaistRatio(),
        bodyFatScore = parts[PhysiqueComponents.BODY_FAT]?.score?.toFloat(),
        leanMassScore = parts[PhysiqueComponents.LEAN_MASS]?.score?.toFloat(),
        waistShapeScore = parts[PhysiqueComponents.WAIST_SHAPE]?.score?.toFloat(),
        proportionScore = descriptive[PhysiqueComponents.PROPORTION]?.score?.toFloat(),
    )
}

/**
 * Computes drift from components captured in both scans, so missing optional
 * measurements do not register as progress.
 */
internal fun comparablePhysiqueScoreDelta(
    latest: ScanRecord,
    previous: ScanRecord?,
    config: EngineConfig,
    heightCm: Float?,
): Float? {
    previous ?: return null
    val scoped = config.withHeight(heightCm).withProportion()
    val physique = scoped.composites.physique
    val latestParts = physiqueParts(latest.dayMetrics(), scoped)
    val previousParts = physiqueParts(previous.dayMetrics(), scoped)
    val shared = latestParts.keys intersect previousParts.keys
    if (shared.isEmpty()) return null
    val latestScore = scoreFromParts(latestParts.filterKeys { it in shared }, physique) ?: return null
    val previousScore = scoreFromParts(previousParts.filterKeys { it in shared }, physique) ?: return null
    return (latestScore - previousScore).toFloat()
}

private fun ScanRecord.dayMetrics(): Map<String, Double> = buildMap {
    weightKg?.let { put(Metrics.WEIGHT, it.toDouble()) }
    fatPercentage?.let { put(Metrics.BODY_FAT_PCT, it.toDouble()) }
    leanBodyMassKg?.let { put(Metrics.LEAN_MASS, it.toDouble()) }
    BodyMeasurementKeys.valueFor(measurements, BodyMeasurementRegion.Waist)
        ?.let { put(Metrics.WAIST, it.toDouble()) }
    shoulderToWaistRatio()?.let { put(Metrics.SHOULDER_TO_WAIST, it.toDouble()) }
}

private fun EngineConfig.withHeight(heightCm: Float?): EngineConfig {
    val height = heightCm?.takeIf { it > 0f }?.toDouble() ?: return this
    return copy(composites = composites.copy(heightCm = height))
}

private fun EngineConfig.withProportion(): EngineConfig =
    copy(composites = composites.copy(physique = composites.physique.copy(enableProportion = true)))

private fun ScanRecord.waistToHeightRatio(heightCm: Float?): Float? {
    val height = heightCm?.takeIf { it > 0f } ?: return null
    val waist = BodyMeasurementKeys.valueFor(measurements, BodyMeasurementRegion.Waist) ?: return null
    return waist / height
}

private fun ScanRecord.shoulderToWaistRatio(): Float? {
    val shoulders = BodyMeasurementKeys.valueFor(measurements, BodyMeasurementRegion.Shoulders) ?: return null
    val waist = frontLinearParams[BodyMeasurementKeys.Waist] ?: return null
    if (waist <= 0f) return null
    return shoulders / waist
}
