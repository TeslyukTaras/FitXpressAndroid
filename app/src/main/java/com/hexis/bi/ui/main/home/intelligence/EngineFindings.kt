package com.hexis.bi.ui.main.home.intelligence

import com.hexis.bi.BuildConfig
import com.hexis.bi.domain.intelligence.RunIntelligenceUseCase
import com.hexis.bi.intelligence.config.CopyConfig
import com.hexis.bi.intelligence.engine.ConfidenceBuckets
import com.hexis.bi.intelligence.engine.Domains
import com.hexis.bi.intelligence.engine.EngineReport
import com.hexis.bi.intelligence.engine.PhysiqueDrift
import com.hexis.bi.intelligence.engine.SuppressedFinding
import com.hexis.bi.intelligence.model.Finding
import com.hexis.bi.intelligence.narrate.InsightNarrator
import com.hexis.bi.utils.constants.FindingMetricAliases
import com.hexis.bi.utils.constants.FindingValues
import com.hexis.bi.utils.constants.InsightSubjects
import kotlin.math.abs

enum class FindingConfidence { HIGH, MEDIUM, LOW }

data class FindingValue(
    val label: String,
    val from: List<ValuePart>,
    val to: List<ValuePart>,
    val unit: String,
)

data class EngineFindingRow(
    val heading: String?,
    val explanation: String,
    val values: List<FindingValue> = emptyList(),
)

data class InsightCard(
    val area: String,
    val explanation: String,
    val confidence: FindingConfidence,
    val values: List<FindingValue>,
)

data class NightComparison(
    val title: String,
    val template: String,
    val label: String,
    val value: List<ValuePart>,
    val usual: List<ValuePart>,
    val unit: String,
)

data class DebugFinding(
    val insightId: String,
    val priorityRank: Int,
    val confidence: String,
    val confidenceScore: Double,
    val factors: Map<String, Double> = emptyMap(),
)

data class EngineDebugInfo(
    val windowDays: Int,
    val areas: Set<String>,
    val findings: List<DebugFinding>,
    val trendCount: Int,
    val suppressed: List<SuppressedFinding>,
    val qualityFailureCount: Int,
    val drift: PhysiqueDrift? = null,
)

sealed interface EngineFindingsState {
    data object Loading : EngineFindingsState

    data object Empty : EngineFindingsState

    data class Ready(
        val rows: List<EngineFindingRow>,
        val confidence: FindingConfidence,
    ) : EngineFindingsState
}

data class AreaFindings(
    private val byWindow: Map<Int, EngineFindingsState> = emptyMap(),
    private val debugByWindow: Map<Int, EngineDebugInfo> = emptyMap(),
    val primaryWindowDays: Int = 0,
    private val engineRan: Boolean = false,
) {
    companion object {
        fun completed(): AreaFindings = AreaFindings(engineRan = true)
    }

    fun forWindow(windowDays: Int): EngineFindingsState = byWindow[windowDays]
        ?: if (engineRan) EngineFindingsState.Empty else EngineFindingsState.Loading

    val primary: EngineFindingsState get() = forWindow(primaryWindowDays)

    fun debugForWindow(windowDays: Int): EngineDebugInfo? = debugByWindow[windowDays]

    val primaryDebug: EngineDebugInfo? get() = debugForWindow(primaryWindowDays)
}

internal const val DAY_TITLE = "day_title"
private const val DAY_ABOVE = "day_above"
private const val DAY_BELOW = "day_below"
internal const val METRIC_PLACEHOLDER = "{metric}"
internal const val USUAL_PLACEHOLDER = "{usual}"

object EngineFindingsMapper {

    fun forAreas(
        report: EngineReport,
        copy: CopyConfig,
        areas: Set<String>,
        isMetric: Boolean,
        analysisWindowDays: Int = report.primaryWindowDays,
    ): AreaFindings = AreaFindings(
        byWindow = report.availableWindows.associateWith { window ->
            rowsFor(report, copy, areas, window, isMetric)
        },
        debugByWindow = if (BuildConfig.DEBUG) {
            report.availableWindows.associateWith { window -> debugFor(report, areas, window) }
        } else {
            emptyMap()
        },
        primaryWindowDays = analysisWindowDays,
        engineRan = true,
    )

    private fun debugFor(report: EngineReport, areas: Set<String>, windowDays: Int): EngineDebugInfo {
        val window = report.forWindow(windowDays)
        return EngineDebugInfo(
            windowDays = windowDays,
            areas = areas,
            findings = window?.findings.orEmpty()
                .filter { it.area in areas }
                .map {
                    DebugFinding(
                        it.insightId, it.priorityRank, it.confidence, it.confidenceScore,
                        it.confidenceFactors,
                    )
                },
            trendCount = report.metricTrends.count { it.domain in areas && windowDays in it.trendsByWindow },
            suppressed = window?.suppressed.orEmpty().filter { it.area in areas },
            qualityFailureCount = window?.verdicts.orEmpty()
                .count { !it.ok && report.metricTrends.any { row -> row.metric == it.metric && row.domain in areas } },
            drift = report.physiqueDrift.takeIf { Domains.BODY in areas },
        )
    }

    private fun rowsFor(
        report: EngineReport,
        copy: CopyConfig,
        areas: Set<String>,
        windowDays: Int,
        isMetric: Boolean,
    ): EngineFindingsState {
        val findings = report.forWindow(windowDays)?.findings.orEmpty().filter { it.area in areas }

        val patterns = findings
            .mapNotNull { finding ->
                val narrated = InsightNarrator.narrate(finding, copy) ?: return@mapNotNull null
                if (narrated.heading == null) null else finding to narrated
            }
            .sortedWith(compareBy({ it.first.confidenceRank }, { it.first.priorityRank }))
        val covered = patterns.flatMap { it.first.facts }.toSet()

        val ranked = findings
            .filter { copy.headings[it.interpretation] == null && !covered.containsAll(it.facts) }
            .sortedWith(compareBy({ it.informational }, { it.confidenceRank }))
            .take(FindingValues.MAX_SUPPORTING_METRICS)
        val supporting = ranked.sortedBy { it.priorityRank }

        val rows = buildList {
            patterns.forEach { (finding, narrated) ->
                add(
                    EngineFindingRow(
                        heading = narrated.heading,
                        explanation = narrated.explanation,
                        values = valuesFor(listOf(finding), report, copy, windowDays, isMetric),
                    ),
                )
            }
            if (supporting.isNotEmpty()) {
                val sentence = InsightNarrator.sentence(
                    facts = supporting.flatMap { it.facts },
                    period = supporting.first().period,
                    copy = copy,
                    bare = patterns.isNotEmpty(),
                )
                if (sentence.isNotBlank()) {
                    add(
                        EngineFindingRow(
                            heading = null,
                            explanation = sentence,
                            values = valuesFor(supporting, report, copy, windowDays, isMetric),
                        ),
                    )
                }
            }
        }.distinctBy { it.heading to it.explanation }

        if (rows.isEmpty()) return EngineFindingsState.Empty

        val section = patterns.map { it.first } + ranked
        return EngineFindingsState.Ready(
            rows = rows,
            confidence = mostConfidentChange(section).confidenceLevel,
        )
    }

    fun simpleFindings(
        report: EngineReport,
        copy: CopyConfig,
        windowDays: Int,
        isMetric: Boolean,
    ): List<InsightCard> =
        report.forWindow(windowDays)?.findings.orEmpty()
            .filter { InsightNarrator.narrate(it, copy) != null }
            .groupBy { it.subject }
            .values
            .map(::mostConfidentChange)
            .mapNotNull { finding ->
                val confident = finding.confidenceLevel == FindingConfidence.HIGH
                val narrated = InsightNarrator.narrate(finding, copy) ?: return@mapNotNull null
                val sentence = InsightNarrator.standalone(finding, copy, confident)
                    ?: narrated.heading
                    ?: narrated.explanation
                if (sentence.isBlank()) return@mapNotNull null
                InsightCard(
                    area = finding.subject,
                    explanation = sentence,
                    confidence = finding.confidenceLevel,
                    values = valuesFor(listOf(finding), report, copy, windowDays, isMetric),
                )
            }
            .sortedWith(compareBy({ it.confidence.ordinal }, { it.area }))

    fun dayComparisons(
        report: EngineReport,
        copy: CopyConfig,
        windowDays: Int,
        isMetric: Boolean,
        dayValues: Map<String, Double>,
    ): List<NightComparison> {
        val title = copy.templates[DAY_TITLE] ?: return emptyList()
        return dayValues
            .mapNotNull { (metric, value) ->
                val baseline = report.baselineFor(windowDays, metric) ?: return@mapNotNull null
                val z = baseline.z(value)
                if (abs(z) < FindingValues.DAY_Z_THRESHOLD) return@mapNotNull null
                val template = copy.templates[if (z > 0) DAY_ABOVE else DAY_BELOW]
                    ?: return@mapNotNull null
                val row = report.metricTrends.firstOrNull { it.metric == metric }
                    ?: return@mapNotNull null
                val format = MetricFormat.of(row.unit, isMetric, metric) ?: return@mapNotNull null
                val label = copy.labels[metric] ?: return@mapNotNull null
                val display = format.distinguishing(value, baseline.median)
                abs(z) to NightComparison(
                    title = title,
                    template = template,
                    label = label,
                    value = display.render(value),
                    usual = display.render(baseline.median),
                    unit = display.unit(label),
                )
            }
            .sortedByDescending { it.first }
            .map { it.second }
            .take(FindingValues.MAX_VALUES)
    }

    private fun resolveMetric(supportingKey: String): String =
        if (supportingKey == FindingMetricAliases.PHYSIQUE_DRIFT_KEY) {
            FindingMetricAliases.PHYSIQUE_SCORE_METRIC
        } else {
            supportingKey
        }

    private fun mostConfidentChange(findings: List<Finding>): Finding {
        val changes = findings.filterNot { it.informational }
        return (changes.ifEmpty { findings }).maxBy { it.confidenceScore }
    }

    private fun valuesFor(
        findings: List<Finding>,
        report: EngineReport,
        copy: CopyConfig,
        windowDays: Int,
        isMetric: Boolean,
    ): List<FindingValue> {
        return findings.filterNot { it.informational }
            .sortedBy { it.priorityRank }
            .flatMap { it.supportingValues.keys }
            .map(::resolveMetric)
            .distinct()
            .mapNotNull { metric ->
                val row = report.metricTrends.firstOrNull { it.metric == metric }
                    ?: return@mapNotNull null
                val baseline = report.baselineFor(windowDays, metric) ?: return@mapNotNull null
                val change = row.trendsByWindow[windowDays]?.absChange ?: return@mapNotNull null
                val format = MetricFormat.of(row.unit, isMetric, metric) ?: return@mapNotNull null
                val latest = if (metric == FindingMetricAliases.PHYSIQUE_SCORE_METRIC) {
                    report.latestValues[metric] ?: baseline.median + change
                } else {
                    baseline.median + change
                }
                val display = format.distinguishing(baseline.median, latest)
                FindingValue(
                    label = copy.labels[metric].orEmpty(),
                    from = display.render(baseline.median),
                    to = display.render(latest),
                    unit = display.unit(copy.labels[metric].orEmpty()),
                )
            }
            .take(FindingValues.MAX_VALUES)
    }
}

private val Finding.confidenceLevel: FindingConfidence
    get() = when (confidence) {
        ConfidenceBuckets.HIGH -> FindingConfidence.HIGH
        ConfidenceBuckets.MODERATE -> FindingConfidence.MEDIUM
        else -> FindingConfidence.LOW
    }

private val Finding.confidenceRank: Int get() = confidenceLevel.ordinal

private val Finding.subject: String
    get() = InsightSubjects.BY_INTERPRETATION[interpretation] ?: area

internal suspend fun RunIntelligenceUseCase.findingsFor(vararg areas: String): AreaFindings {
    if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) return AreaFindings.completed()
    val scope = areas.toSet()
    return invoke().fold(
        onSuccess = {
            EngineFindingsMapper.forAreas(
                report = it.report,
                copy = it.copy,
                areas = scope,
                isMetric = it.isMetric,
                analysisWindowDays = it.config.windows.analysisDays,
            )
        },
        onFailure = { AreaFindings.completed() },
    )
}
