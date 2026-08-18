package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.MetricSeries
import com.hexis.bi.intelligence.model.QualityVerdict
import com.hexis.bi.intelligence.model.Trend
import kotlin.math.abs

object QualityStatus {
    const val OK = "ok"
    const val LOW_COVERAGE = "low_coverage"
    const val STALE = "stale"
    const val INSUFFICIENT_HISTORY = "insufficient_history"
    const val IMPLAUSIBLE = "implausible"
}

private const val COVERAGE_REASON_DIGITS = 2
private const val PER_WEEK_REASON_DIGITS = 1
private const val DAYS_PER_WEEK = 7.0

internal fun assess(
    series: MetricSeries,
    trend: Trend?,
    runDate: String,
    config: EngineConfig,
): QualityVerdict {
    val quality = config.quality
    val domain = series.domain
    val reasons = mutableListOf<String>()
    var status = QualityStatus.OK
    val lastDate = series.points.lastOrNull()?.date.orEmpty()

    if (trend == null || trend.direction == Directions.INSUFFICIENT_DATA) {
        reasons += QualityStatus.INSUFFICIENT_HISTORY
        status = QualityStatus.INSUFFICIENT_HISTORY
    }

    val effectiveCoverage =
        if (trend != null) minOf(series.coverage, trend.coverage) else series.coverage

    if (domain == Domains.BODY) {
        val scans = series.points.size
        if (scans < quality.minScansForBody) {
            reasons += "only_${scans}_scans(min ${quality.minScansForBody})"
            if (status == QualityStatus.OK) status = QualityStatus.LOW_COVERAGE
        }
    } else {
        val floor = quality.minCoverageFor(domain)
        if (effectiveCoverage < floor) {
            reasons += "coverage_${formatFixed(effectiveCoverage, COVERAGE_REASON_DIGITS)}<$floor"
            if (status == QualityStatus.OK) status = QualityStatus.LOW_COVERAGE
        }
    }

    if (lastDate.isNotEmpty() && runDate.isNotEmpty()) {
        val gap = EngineDates.daysBetween(lastDate, runDate)
        val limit = quality.freshnessDaysFor(series.metric, domain)
        if (gap > limit) {
            reasons += "stale_${gap}d(>$limit)"
            if (status == QualityStatus.OK) status = QualityStatus.STALE
        }
    }

    val bound = quality.plausibilityPerWeek[series.metric]
    if (bound != null && trend != null && series.points.size >= quality.minPointsForPlausibility) {
        val spanDays = EngineDates.daysBetween(series.points.first().date, series.points.last().date)
        if (spanDays > 0) {
            val perWeek = abs(trend.absChange) / (spanDays / DAYS_PER_WEEK)
            if (perWeek > bound) {
                reasons += "implausible_${formatFixed(perWeek, PER_WEEK_REASON_DIGITS)}/wk(>$bound)"
                status = QualityStatus.IMPLAUSIBLE
            }
        }
    }

    return QualityVerdict(
        metric = series.metric,
        domain = domain,
        ok = status == QualityStatus.OK,
        status = status,
        reasons = reasons,
        coverage = effectiveCoverage,
        lastDate = lastDate,
    )
}

internal fun stillLearning(verdicts: Map<String, QualityVerdict>, config: EngineConfig): Boolean {
    val floor = config.quality.stillLearningCoverage
    val tracked = verdicts.values.filter { it.domain != Domains.BODY }
    if (tracked.isEmpty()) return true
    val weak = tracked.count { it.coverage < floor }
    return weak.toDouble() / tracked.size > config.quality.stillLearningWeakFraction
}
