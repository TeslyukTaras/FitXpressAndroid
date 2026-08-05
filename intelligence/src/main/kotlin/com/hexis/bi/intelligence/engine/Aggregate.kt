package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.MetricPoint
import com.hexis.bi.intelligence.model.MetricSeries

private const val REDUCER_LAST = "last"
private const val REDUCER_MEAN = "mean"
private const val COVERAGE_DIGITS = 3

private val DAILY_REDUCERS = mapOf(
    Metrics.WEIGHT to REDUCER_LAST,
    Metrics.BODY_FAT_PCT to REDUCER_LAST,
    Metrics.LEAN_MASS to REDUCER_LAST,
    Metrics.WAIST to REDUCER_LAST,
    Metrics.RESTING_HR to REDUCER_MEAN,
    Metrics.HRV_RMSSD to REDUCER_MEAN,
    Metrics.VO2MAX to REDUCER_MEAN,
)

internal fun aggregate(
    points: List<MetricPoint>,
    config: EngineConfig,
    windowDays: Int,
): List<MetricSeries> {
    val byMetric = LinkedHashMap<String, LinkedHashMap<String, MutableList<Double>>>()
    for (point in points) {
        byMetric.getOrPut(point.metric) { LinkedHashMap() }
            .getOrPut(point.date) { mutableListOf() }
            .add(point.value)
    }

    return byMetric.map { (metric, dayValues) ->
        val reducer = DAILY_REDUCERS[metric] ?: REDUCER_MEAN
        val source = Metrics.sourceOf(metric)
        val reduced = dayValues.entries.sortedBy { it.key }.map { (day, values) ->
            MetricPoint(day, metric, reduce(values, reducer), source)
        }
        MetricSeries(
            metric = metric,
            domain = config.domainOf(metric),
            unit = Metrics.unitOf(metric),
            points = reduced,
            coverage = coverageOf(reduced.size, windowDays),
        )
    }
}

internal fun coverageOf(days: Int, windowDays: Int): Double =
    if (windowDays == 0) 0.0 else minOf(1.0, roundHalfEven(days.toDouble() / windowDays, COVERAGE_DIGITS))

private fun reduce(values: List<Double>, reducer: String): Double = when (reducer) {
    REDUCER_LAST -> values.last()
    else -> mean(values)
}
