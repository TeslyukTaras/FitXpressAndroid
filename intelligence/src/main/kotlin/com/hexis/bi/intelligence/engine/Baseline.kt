package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.model.Baseline
import com.hexis.bi.intelligence.model.MetricSeries

internal fun computeBaseline(
    series: MetricSeries,
    baselineDays: Int,
    beforeDate: String? = null,
): Baseline? {
    val eligible = series.points.filter { beforeDate == null || it.date < beforeDate }
    val values = eligible.takeLast(baselineDays).map { it.value }
    if (values.isEmpty()) return null
    val median = median(values)
    return Baseline(
        metric = series.metric,
        median = median,
        mad = mad(values, median),
        n = values.size,
    )
}
