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
    val enabled: Boolean,
    val windowDays: Int,
    val direction: String,
    val statuses: Map<String, String>,
)

data class WindowFindings(
    val windowDays: Int,
    val findings: List<Finding>,
    val findingsByArea: Map<String, List<Finding>>,
    val suppressed: List<SuppressedFinding>,
    val verdicts: List<QualityVerdict>,
) {
    val metricsOk: Int get() = verdicts.count { it.ok }

    val metricsTotal: Int get() = verdicts.size
}

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
    val findingsByWindow: Map<Int, WindowFindings>,
    val areaWeights: Map<String, Double>,
    val baselinesByWindow: Map<Int, Map<String, Baseline>> = emptyMap(),
) {
    fun forWindow(windowDays: Int): WindowFindings? = findingsByWindow[windowDays]

    fun baselineFor(windowDays: Int, metric: String): Baseline? =
        baselinesByWindow[windowDays]?.get(metric)

    val availableWindows: List<Int> get() = findingsByWindow.keys.sorted()

    fun weightOf(finding: Finding): Double = areaWeights[finding.area] ?: DEFAULT_AREA_WEIGHT

    private companion object {
        const val DEFAULT_AREA_WEIGHT = 1.0
    }
}

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
        val widestSupported = windows.filter { it <= available }.maxOrNull() ?: windows.min()

        val requestedFoundationWindow = config.composites.foundations.windowDays
        val foundationWindows = windows.filter { it <= available && trendsByWindow.getValue(it).isNotEmpty() }
        val foundationWindow = if (requestedFoundationWindow in foundationWindows) {
            requestedFoundationWindow
        } else {
            foundationWindows.maxOrNull() ?: widestSupported
        }
        val foundations = if (config.features.foundations) {
            evaluateFoundations(trendsByWindow.getValue(foundationWindow), config)
        } else {
            FoundationsResult(FoundationDirection.DISABLED, emptyMap())
        }
        val drift = if (config.features.physiqueDrift) {
            evaluatePhysiqueDrift(series, config)
        } else {
            PhysiqueDrift(DriftStatus.INSUFFICIENT_SCANS, scans = 0)
        }

        val narratable = windows.filter { trendsByWindow.getValue(it).isNotEmpty() }
            .ifEmpty { listOf(widestSupported) }
        val primary = narratable.max()
        val baselines = baselinesByWindow.getValue(primary)
        val rows = metricTrends.map { it.copy(baseline = baselines[it.metric]) }
        val findingsByWindow = narratable.associateWith { window ->
            val trends = trendsByWindow.getValue(window)
            val windowVerdicts = series.associate {
                it.metric to assess(it, trends[it.metric], input.runDate, config)
            }
            val candidates = buildList {
                addAll(enabledRuleCandidates(evaluateRules(trends), config))
                if (config.features.metricFindings) addAll(singleMetricCandidates(trends, config))
                if (config.features.foundations) addAll(foundationCandidates(foundations))
                if (config.features.physiqueDrift) addAll(driftCandidates(drift, config, input.runDate))
            }
            val built = buildFindings(candidates, trends, config, input.runDate, windowVerdicts)
            WindowFindings(
                windowDays = window,
                findings = built.findings,
                findingsByArea = groupByArea(built.findings),
                suppressed = built.suppressed,
                verdicts = windowVerdicts.values.toList(),
            )
        }

        val primaryFindings = findingsByWindow.getValue(primary)

        return EngineReport(
            runDate = input.runDate,
            primaryWindowDays = primary,
            stillLearning = stillLearning(primaryFindings.verdicts.associateBy { it.metric }, config),
            metricsOk = primaryFindings.metricsOk,
            metricsTotal = primaryFindings.metricsTotal,
            verdicts = primaryFindings.verdicts,
            foundations = FoundationsReport(
                enabled = config.features.foundations,
                windowDays = foundationWindow,
                direction = foundations.direction,
                statuses = foundations.statuses,
            ),
            physiqueDrift = drift,
            metricTrends = rows,
            findings = primaryFindings.findings,
            findingsByArea = primaryFindings.findingsByArea,
            suppressed = primaryFindings.suppressed,
            findingsByWindow = findingsByWindow,
            baselinesByWindow = baselinesByWindow.filterKeys { it in findingsByWindow },
            areaWeights = findingsByWindow.values
                .flatMap { it.findings }
                .map { it.area }
                .distinct()
                .associateWith { config.priorityOf(it) },
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
