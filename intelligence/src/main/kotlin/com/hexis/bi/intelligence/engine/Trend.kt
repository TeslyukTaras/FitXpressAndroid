package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.config.ThresholdsConfig
import com.hexis.bi.intelligence.config.TrendConfig
import com.hexis.bi.intelligence.model.Baseline
import com.hexis.bi.intelligence.model.MetricPoint
import com.hexis.bi.intelligence.model.MetricSeries
import com.hexis.bi.intelligence.model.Trend
import kotlin.math.abs

private const val SLOPE_DIGITS = 5
private const val CHANGE_DIGITS = 3
private const val REL_CHANGE_DIGITS = 4
private const val STRENGTH_DIGITS = 3
private const val COVERAGE_DIGITS = 3
private const val Z_DIGITS = 3
private const val RECENT_DAYS_FOR_Z = 3
private const val MIN_POINTS_FOR_TREND = 2
private const val MIN_POINTS_FOR_PERSISTENCE = 3

internal fun computeTrend(
    series: MetricSeries,
    baseline: Baseline,
    windowDays: Int,
    config: EngineConfig,
): Trend {
    val trendConfig = config.trend
    val windowed = windowOf(series, windowDays)
    val lastDate = windowed.points.lastOrNull()?.date.orEmpty()
    val minPersistDays = trendConfig.minPersistDaysFor(series.domain)

    val tooFewPoints = windowed.points.size < MIN_POINTS_FOR_TREND ||
        (series.domain != Domains.BODY && windowed.points.size < minPersistDays)
    if (tooFewPoints) return insufficient(series, windowDays, lastDate)

    val days = windowed.points.map { EngineDates.ordinal(it.date) }
    val values = winsorize(windowed.points.map { it.value }, trendConfig.winsorizePct).values

    val fit = fitOf(days, values, trendConfig)
    val strength = spearmanAbs(days, values)
    val persistence = stepPersistence(values, fit.change)
    val zNow = zNowOf(values, baseline)
    val threshold = thresholdOf(series.metric, baseline, config)
    val direction = classify(fit.change, threshold, persistence, trendConfig)

    val relChange = if (baseline.median == 0.0) 0.0 else fit.change / baseline.median
    val priorChange = if (config.windows.priorPeriod) periodChange(windowed.priorPoints, trendConfig) else null

    return Trend(
        metric = series.metric,
        domain = series.domain,
        windowDays = windowDays,
        direction = direction,
        slope = roundHalfEven(fit.slope, SLOPE_DIGITS),
        velocity = roundHalfEven(abs(fit.slope), SLOPE_DIGITS),
        persistenceDays = sideRun(values, baseline.median),
        absChange = roundHalfEven(fit.change, CHANGE_DIGITS),
        relChange = roundHalfEven(relChange, REL_CHANGE_DIGITS),
        trendStrength = roundHalfEven(strength, STRENGTH_DIGITS),
        coverage = roundHalfEven(
            windowed.points.map { it.date }.toSet().size.toDouble() / windowDays,
            COVERAGE_DIGITS,
        ),
        zNow = roundHalfEven(zNow, Z_DIGITS),
        lastDate = lastDate,
        priorPeriodChange = priorChange?.let { roundHalfEven(it, CHANGE_DIGITS) },
        changeVsPrior = priorChange?.let { roundHalfEven(fit.change - it, CHANGE_DIGITS) },
    )
}

private class WindowedPoints(val points: List<MetricPoint>, val priorPoints: List<MetricPoint>)

private fun windowOf(series: MetricSeries, windowDays: Int): WindowedPoints {
    val end = series.points.lastOrNull()?.date ?: return WindowedPoints(emptyList(), emptyList())
    val start = EngineDates.minusDays(end, windowDays - 1L)
    val priorStart = EngineDates.minusDays(start, windowDays.toLong())
    return WindowedPoints(
        points = series.points.filter { it.date >= start },
        priorPoints = series.points.filter { it.date >= priorStart && it.date < start },
    )
}

private fun fitOf(days: List<Double>, values: List<Double>, trendConfig: TrendConfig): LinearFit =
    if (trendConfig.estimator == TrendConfig.ESTIMATOR_THEIL_SEN) {
        theilSenChange(days, values)
    } else {
        olsChange(days, values)
    }

private fun periodChange(points: List<MetricPoint>, trendConfig: TrendConfig): Double? {
    if (points.size < MIN_POINTS_FOR_TREND) return null
    val values = winsorize(points.map { it.value }, trendConfig.winsorizePct).values
    val days = points.map { EngineDates.ordinal(it.date) }
    return fitOf(days, values, trendConfig).change
}

private fun stepPersistence(values: List<Double>, change: Double): Double {
    if (values.size < MIN_POINTS_FOR_PERSISTENCE || change == 0.0) return 1.0
    val steps = values.zipWithNext { previous, next -> next - previous }.filter { it != 0.0 }
    if (steps.isEmpty()) return 1.0
    return steps.count { (it > 0.0) == (change > 0.0) }.toDouble() / steps.size
}

private fun sideRun(values: List<Double>, median: Double): Int {
    var best = 0
    var run = 0
    var sign = 0
    for (value in values) {
        val side = if (value >= median) 1 else -1
        run = if (side == sign) run + 1 else 1
        sign = side
        best = maxOf(best, run)
    }
    return best
}

private fun zNowOf(values: List<Double>, baseline: Baseline): Double {
    val scale = Baseline.MAD_TO_STD_DEV * baseline.mad
    if (scale == 0.0) return 0.0
    return (mean(values.takeLast(RECENT_DAYS_FOR_Z)) - baseline.median) / scale
}

private fun thresholdOf(metric: String, baseline: Baseline, config: EngineConfig): Double {
    val absolute = config.thresholds.absoluteMetrics[metric]
    if (absolute != null && config.thresholds.defaultMode != ThresholdsConfig.MODE_FORCE_RELATIVE) {
        return absolute
    }
    return config.trend.stableDeadbandFraction * baseline.mad
}

private fun classify(
    change: Double,
    threshold: Double,
    persistence: Double,
    trendConfig: TrendConfig,
): String {
    val magnitude = abs(change)
    if (magnitude < threshold) return Directions.STABLE
    if (magnitude >= threshold * trendConfig.decisiveChangeMultiple) return directionOf(change)
    if (persistence < trendConfig.minPersistence) return Directions.STABLE
    return directionOf(change)
}

private fun directionOf(change: Double): String =
    if (change > 0.0) Directions.UP else Directions.DOWN

private fun insufficient(series: MetricSeries, windowDays: Int, lastDate: String): Trend = Trend(
    metric = series.metric,
    domain = series.domain,
    windowDays = windowDays,
    direction = Directions.INSUFFICIENT_DATA,
    slope = 0.0,
    velocity = 0.0,
    persistenceDays = 0,
    absChange = 0.0,
    relChange = 0.0,
    trendStrength = 0.0,
    coverage = series.coverage,
    zNow = 0.0,
    lastDate = lastDate,
)
