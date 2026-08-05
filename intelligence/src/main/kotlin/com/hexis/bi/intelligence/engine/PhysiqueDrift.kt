package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.MetricSeries
import kotlin.math.abs

object DriftStatus {
    const val OK = "ok"
    const val INSUFFICIENT_SCANS = "insufficient_scans"
    const val NOT_COMPARABLE = "not_comparable"
}

object DriftDirection {
    const val POSITIVE = "positive"
    const val NEGATIVE = "negative"
    const val HOLDING = "holding"
}

data class PhysiqueDrift(
    val status: String,
    val scans: Int,
    val drift: Double? = null,
    val direction: String? = null,
    val driver: String? = null,
    val componentDeltas: Map<String, Double> = emptyMap(),
    val firstScan: String? = null,
    val lastScan: String? = null,
)

private const val DRIFT_DIGITS = 2
private const val AGREEMENT_TARGET_COMPONENTS = 3.0

internal fun evaluatePhysiqueDrift(series: List<MetricSeries>, config: EngineConfig): PhysiqueDrift {
    val rows = scanDays(series)
    val dates = rows.keys.sorted()
    if (dates.size < config.windows.minScansForDrift) {
        return PhysiqueDrift(DriftStatus.INSUFFICIENT_SCANS, dates.size)
    }

    val physique = config.composites.physique
    val firstParts = physiqueParts(rows.getValue(dates.first()), config)
    val lastParts = physiqueParts(rows.getValue(dates.last()), config)
    val shared = firstParts.keys.filter { it in lastParts.keys }
    if (shared.isEmpty()) return PhysiqueDrift(DriftStatus.NOT_COMPARABLE, dates.size)

    val firstScore = scoreFromParts(firstParts.filterKeys { it in shared }, physique)
    val lastScore = scoreFromParts(lastParts.filterKeys { it in shared }, physique)
    if (firstScore == null || lastScore == null) {
        return PhysiqueDrift(DriftStatus.NOT_COMPARABLE, dates.size)
    }

    val drift = roundHalfEven(lastScore - firstScore, DRIFT_DIGITS)
    val deltas = shared.associateWith {
        roundHalfEven(lastParts.getValue(it).score - firstParts.getValue(it).score, DRIFT_DIGITS)
    }
    val driver = deltas.maxByOrNull { abs(it.value) }?.key
    val threshold = physique.driftMeaningfulDelta
    val direction = when {
        drift >= threshold -> DriftDirection.POSITIVE
        drift <= -threshold -> DriftDirection.NEGATIVE
        else -> DriftDirection.HOLDING
    }

    return PhysiqueDrift(
        status = DriftStatus.OK,
        scans = dates.size,
        drift = drift,
        direction = direction,
        driver = driver,
        componentDeltas = deltas,
        firstScan = dates.first(),
        lastScan = dates.last(),
    )
}

internal fun driftConfidenceFactors(
    drift: PhysiqueDrift,
    config: EngineConfig,
    runDate: String,
): Map<String, Double> {
    val physique = config.composites.physique
    val coverageReference = config.windows.driftCoverageScans
    val deltas = drift.componentDeltas
    val driftValue = requireNotNull(drift.drift)
    val agreeing = deltas.values.count { (it > 0.0) == (driftValue > 0.0) }
    val against = deltas.size - agreeing

    var recencyFactor = 1.0
    val lastScan = drift.lastScan
    if (runDate.isNotEmpty() && lastScan != null) {
        val fresh = config.quality.freshnessDaysFor(Metrics.WEIGHT, Domains.BODY)
        val gap = EngineDates.daysBetween(lastScan, runDate)
        recencyFactor = if (gap <= fresh) 1.0 else maxOf(0.0, 1.0 - (gap - fresh).toDouble() / fresh)
    }

    return mapOf(
        ConfidenceFactors.SIGNAL_MAGNITUDE to minOf(abs(driftValue) / physique.driftSignalReference, 1.0),
        ConfidenceFactors.PERSISTENCE to if (deltas.isEmpty()) 0.0 else agreeing.toDouble() / deltas.size,
        ConfidenceFactors.AGREEMENT to minOf(agreeing / AGREEMENT_TARGET_COMPONENTS, 1.0),
        ConfidenceFactors.COVERAGE to minOf(drift.scans.toDouble() / coverageReference, 1.0),
        ConfidenceFactors.RECENCY to recencyFactor,
        ConfidenceFactors.SOURCE_QUALITY to 1.0,
        ConfidenceFactors.CONTRADICTION to if (deltas.isEmpty()) 0.0 else against.toDouble() / deltas.size,
    )
}

private fun scanDays(series: List<MetricSeries>): Map<String, Map<String, Double>> {
    val rows = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    for (metricSeries in series) {
        if (metricSeries.metric !in Metrics.SCAN) continue
        for (point in metricSeries.points) {
            rows.getOrPut(point.date) { LinkedHashMap() }[metricSeries.metric] = point.value
        }
    }
    return rows
}
