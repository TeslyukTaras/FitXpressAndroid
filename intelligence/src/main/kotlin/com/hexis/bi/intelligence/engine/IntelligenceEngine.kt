package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.Baseline
import com.hexis.bi.intelligence.model.EngineInput
import com.hexis.bi.intelligence.model.Finding
import com.hexis.bi.intelligence.model.MetricSeries
import com.hexis.bi.intelligence.model.QualityVerdict
import com.hexis.bi.intelligence.model.Trend

data class MetricTrendRow(
    val metric: String,
    val domain: String,
    val unit: String,
    val coverage: Double,
    val trendsByWindow: Map<Int, Trend>,
    val baseline: Baseline?,
)

data class FoundationsReport(
    val windowDays: Int,
    val direction: String,
    val statuses: Map<String, String>,
)

data class EngineReport(
    val runDate: String,
    val primaryWindowDays: Int,
    val stillLearning: Boolean,
    val metricsOk: Int,
    val metricsTotal: Int,
    val verdicts: List<QualityVerdict>,
    val foundations: FoundationsReport,
    val physiqueDrift: PhysiqueDrift,
    val metricTrends: List<MetricTrendRow>,
    val findings: List<Finding>,
    val findingsByArea: Map<String, List<Finding>>,
    val suppressed: List<SuppressedFinding>,
)

private const val DAYS_PER_WEEK = 7

object IntelligenceEngine {

    fun run(input: EngineInput, config: EngineConfig): EngineReport {
        val windows = config.windows.trendDays
        val coverageDays = maxOf(1, input.pullDays)

        val measured = aggregate(input.points, config, coverageDays)
        val series = measured + buildCompositeSeries(measured, coverageDays, config)

        val baselinesByWindow = windows.associateWith { LinkedHashMap<String, Baseline>() }
        val trendsByWindow = windows.associateWith { LinkedHashMap<String, Trend>() }
        val metricTrends = series.map { metricSeries ->
            val perWindow = LinkedHashMap<Int, Trend>()
            for (window in windows) {
                if (metricSeries.points.isEmpty()) continue
                if (calendarSpan(metricSeries) < window) continue
                val cutoff = EngineDates.minusDays(metricSeries.points.last().date, window - 1L)
                val baseline = computeBaseline(metricSeries, config.windows.baselineDays, cutoff)
                    ?: if (metricSeries.domain == Domains.BODY) {
                        computeBaseline(metricSeries, config.windows.baselineDays)
                    } else {
                        null
                    }
                    ?: continue
                baselinesByWindow.getValue(window)[metricSeries.metric] = baseline
                val trend = computeTrend(metricSeries, baseline, window, config)
                trendsByWindow.getValue(window)[metricSeries.metric] = trend
                perWindow[window] = trend
            }
            MetricTrendRow(
                metric = metricSeries.metric,
                domain = metricSeries.domain,
                unit = metricSeries.unit,
                coverage = metricSeries.coverage,
                trendsByWindow = perWindow,
                baseline = null,
            )
        }

        val available = series.maxOfOrNull { calendarSpan(it) } ?: 0
        val primary = windows.filter { it <= available }.maxOrNull() ?: windows.min()
        val baselines = baselinesByWindow.getValue(primary)
        val rows = metricTrends.map { it.copy(baseline = baselines[it.metric]) }

        val primaryTrends = trendsByWindow.getValue(primary)
        val verdicts = series.associate { it.metric to assess(it, primaryTrends[it.metric], input.runDate, config) }

        val requestedFoundationWindow = config.composites.foundations.windowDays
        val foundationWindows = windows.filter { it <= available && trendsByWindow.getValue(it).isNotEmpty() }
        val foundationWindow = if (requestedFoundationWindow in foundationWindows) {
            requestedFoundationWindow
        } else {
            foundationWindows.maxOrNull() ?: primary
        }
        val foundations = evaluateFoundations(trendsByWindow.getValue(foundationWindow), config)
        val drift = evaluatePhysiqueDrift(series, config)

        val candidates = evaluateRules(primaryTrends) +
            singleMetricCandidates(primaryTrends, config) +
            foundationCandidates(foundations) +
            driftCandidates(drift, config, input.runDate)
        val result = buildFindings(candidates, primaryTrends, config, input.runDate, verdicts)

        return EngineReport(
            runDate = input.runDate,
            primaryWindowDays = primary,
            stillLearning = stillLearning(verdicts, config),
            metricsOk = verdicts.values.count { it.ok },
            metricsTotal = verdicts.size,
            verdicts = verdicts.values.toList(),
            foundations = FoundationsReport(foundationWindow, foundations.direction, foundations.statuses),
            physiqueDrift = drift,
            metricTrends = rows,
            findings = result.findings,
            findingsByArea = groupByArea(result.findings),
            suppressed = result.suppressed,
        )
    }

    fun periodLabel(windowDays: Int): String = "${windowDays / DAYS_PER_WEEK} weeks"
}

private fun calendarSpan(series: MetricSeries): Int {
    if (series.points.isEmpty()) return 0
    return EngineDates.daysBetween(series.points.first().date, series.points.last().date) + 1
}

private fun groupByArea(findings: List<Finding>): Map<String, List<Finding>> {
    val byArea = LinkedHashMap<String, MutableList<Finding>>()
    for (finding in findings.sortedBy { it.priorityRank }) {
        byArea.getOrPut(finding.area) { mutableListOf() } += finding
    }
    return byArea
}
