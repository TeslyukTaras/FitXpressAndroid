package com.hexis.bi.ui.main.home.intelligence

import com.hexis.bi.BuildConfig
import com.hexis.bi.domain.intelligence.RunIntelligenceUseCase
import com.hexis.bi.intelligence.config.CopyConfig
import com.hexis.bi.intelligence.engine.ConfidenceBuckets
import com.hexis.bi.intelligence.engine.EngineReport
import com.hexis.bi.intelligence.model.Finding
import com.hexis.bi.intelligence.narrate.InsightNarrator
import com.hexis.bi.utils.constants.FindingMetricAliases
import com.hexis.bi.utils.constants.FindingValues
import com.hexis.bi.utils.constants.InsightSubjects

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
)

data class InsightCard(
    val area: String,
    val explanation: String,
    val confidence: FindingConfidence,
    val values: List<FindingValue>,
)

sealed interface EngineFindingsState {
    data object Hidden : EngineFindingsState

    data object Empty : EngineFindingsState

    data class Ready(
        val rows: List<EngineFindingRow>,
        val confidence: FindingConfidence,
        val values: List<FindingValue>,
    ) : EngineFindingsState
}

data class AreaFindings(
    private val byWindow: Map<Int, EngineFindingsState> = emptyMap(),
    val primaryWindowDays: Int = 0,
    private val engineRan: Boolean = false,
) {
    fun forWindow(windowDays: Int): EngineFindingsState = byWindow[windowDays]
        ?: if (engineRan) EngineFindingsState.Empty else EngineFindingsState.Hidden

    val primary: EngineFindingsState get() = forWindow(primaryWindowDays)
}

object EngineFindingsMapper {

    fun forAreas(
        report: EngineReport,
        copy: CopyConfig,
        areas: Set<String>,
        isMetric: Boolean,
    ): AreaFindings = AreaFindings(
        byWindow = report.availableWindows.associateWith { window ->
            rowsFor(report, copy, areas, window, isMetric)
        },
        primaryWindowDays = report.primaryWindowDays,
        engineRan = true,
    )

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
            patterns.forEach { (_, narrated) ->
                add(EngineFindingRow(narrated.heading, narrated.explanation))
            }
            if (supporting.isNotEmpty()) {
                val sentence = InsightNarrator.sentence(
                    facts = supporting.flatMap { it.facts },
                    period = supporting.first().period,
                    copy = copy,
                    bare = patterns.isNotEmpty(),
                )
                if (sentence.isNotBlank()) add(EngineFindingRow(null, sentence))
            }
        }.distinctBy { it.explanation }

        if (rows.isEmpty()) return EngineFindingsState.Empty

        val leading = mostConfidentChange(patterns.map { it.first } + ranked)
        return EngineFindingsState.Ready(
            rows = rows,
            confidence = leading.confidenceLevel,
            values = valuesFor(leading, report, copy, windowDays, isMetric),
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
                    values = if (finding.informational) {
                        emptyList()
                    } else {
                        valuesFor(finding, report, copy, windowDays, isMetric)
                    },
                )
            }
            .sortedWith(compareBy({ it.confidence.ordinal }, { it.area }))

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
        finding: Finding,
        report: EngineReport,
        copy: CopyConfig,
        windowDays: Int,
        isMetric: Boolean,
    ): List<FindingValue> {
        if (windowDays != report.primaryWindowDays) return emptyList()
        return finding.supportingValues.keys.map(::resolveMetric).mapNotNull { metric ->
            val row = report.metricTrends.firstOrNull { it.metric == metric } ?: return@mapNotNull null
            val baseline = row.baseline ?: return@mapNotNull null
            val change = row.trendsByWindow[windowDays]?.absChange ?: return@mapNotNull null
            val format = MetricFormat.of(row.unit, isMetric) ?: return@mapNotNull null
            FindingValue(
                label = copy.labels[metric].orEmpty(),
                from = format.render(baseline.median),
                to = format.render(baseline.median + change),
                unit = format.unit(copy.labels[metric].orEmpty()),
            )
        }.take(FindingValues.MAX_VALUES)
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
    if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) return AreaFindings()
    val scope = areas.toSet()
    return invoke().fold(
        onSuccess = { EngineFindingsMapper.forAreas(it.report, it.copy, scope, it.isMetric) },
        onFailure = { AreaFindings() },
    )
}
