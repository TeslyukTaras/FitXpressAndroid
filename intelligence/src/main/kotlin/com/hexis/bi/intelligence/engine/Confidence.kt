package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.ConfidenceBreakdown
import com.hexis.bi.intelligence.model.Trend
import kotlin.math.abs

internal object ConfidenceFactors {
    const val SIGNAL_MAGNITUDE = "signal_magnitude"
    const val PERSISTENCE = "persistence"
    const val AGREEMENT = "agreement"
    const val COVERAGE = "coverage"
    const val RECENCY = "recency"
    const val SOURCE_QUALITY = "source_quality"
    const val CONTRADICTION = "contradiction"

    val ORDER = listOf(
        SIGNAL_MAGNITUDE, PERSISTENCE, AGREEMENT, COVERAGE, RECENCY, SOURCE_QUALITY, CONTRADICTION,
    )
}

object ConfidenceBuckets {
    const val HIGH = "high"
    const val MODERATE = "moderate"
    const val LOW = "low"
}

private const val FACTOR_DIGITS = 3
private const val SCORE_DIGITS = 3
private const val FULL_SIGNAL_Z = 3.0
private const val SINGLE_SOURCE_QUALITY = 1.0

internal fun scoreFactors(factors: Map<String, Double>, config: EngineConfig): ConfidenceBreakdown {
    val weights = config.confidence.weights
    val buckets = config.confidence.buckets
    val clamped = ConfidenceFactors.ORDER.associateWith { key ->
        roundHalfEven((factors[key] ?: 0.0).coerceIn(0.0, 1.0), FACTOR_DIGITS)
    }
    val positive = compensatedSum(
        ConfidenceFactors.ORDER.filter { it != ConfidenceFactors.CONTRADICTION }
            .map { (weights[it] ?: 0.0) * clamped.getValue(it) },
    )
    val penalty = (weights[ConfidenceFactors.CONTRADICTION] ?: 0.0) *
        clamped.getValue(ConfidenceFactors.CONTRADICTION)
    val score = (positive - penalty).coerceIn(0.0, 1.0)
    val bucket = when {
        score >= (buckets[ConfidenceBuckets.HIGH] ?: 1.0) -> ConfidenceBuckets.HIGH
        score >= (buckets[ConfidenceBuckets.MODERATE] ?: 0.0) -> ConfidenceBuckets.MODERATE
        else -> ConfidenceBuckets.LOW
    }
    return ConfidenceBreakdown(
        factors = clamped,
        score = roundHalfEven(score, SCORE_DIGITS),
        bucket = bucket,
    )
}

internal fun computeConfidence(
    trends: List<Trend>,
    agreementCount: Int,
    agreementExpected: Int,
    contradiction: Double,
    config: EngineConfig,
    runDate: String = "",
): ConfidenceBreakdown {
    val lead = trends.maxByOrNull { abs(it.zNow) } ?: error("confidence needs at least one trend")
    val factors = mapOf(
        ConfidenceFactors.SIGNAL_MAGNITUDE to minOf(abs(lead.zNow) / FULL_SIGNAL_Z, 1.0),
        ConfidenceFactors.PERSISTENCE to
            minOf(lead.persistenceDays.toDouble() / maxOf(1, lead.windowDays), 1.0),
        ConfidenceFactors.AGREEMENT to
            minOf(agreementCount.toDouble() / maxOf(1, agreementExpected), 1.0),
        ConfidenceFactors.COVERAGE to minOf(mean(trends.map { it.coverage }), 1.0),
        ConfidenceFactors.RECENCY to recency(trends, runDate, config.confidence.recencyFreshDays),
        ConfidenceFactors.SOURCE_QUALITY to SINGLE_SOURCE_QUALITY,
        ConfidenceFactors.CONTRADICTION to contradiction,
    )
    return scoreFactors(factors, config)
}

internal fun recency(trends: List<Trend>, runDate: String, freshDays: Int): Double {
    if (runDate.isEmpty()) return 1.0
    val gaps = trends.filter { it.lastDate.isNotEmpty() }
        .map { EngineDates.daysBetween(it.lastDate, runDate) }
    val gap = gaps.maxOrNull() ?: return 1.0
    if (gap <= freshDays) return 1.0
    return maxOf(0.0, 1.0 - (gap - freshDays).toDouble() / freshDays)
}
