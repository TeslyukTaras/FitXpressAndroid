package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.Finding
import com.hexis.bi.intelligence.model.QualityVerdict
import com.hexis.bi.intelligence.model.Trend

object SuppressionReason {
    const val NO_SUPPORTING_TREND = "no_supporting_trend"
    const val STRESS_UNCORROBORATED = "stress_uncorroborated"
    const val DATA_QUALITY = "data_quality"
}

data class SuppressedFinding(
    val insightId: String,
    val interpretation: String,
    val reason: String,
    val message: String? = null,
    val detail: Map<String, List<String>> = emptyMap(),
    val confidence: String? = null,
    val confidenceScore: Double? = null,
)

internal data class FindingsResult(
    val findings: List<Finding>,
    val suppressed: List<SuppressedFinding>,
)

private const val Z_VALUE_DIGITS = 1
private const val DAYS_PER_WEEK = 7
private const val STRESS_GATE_DETAIL = "Spec §9"

private val EVIDENCE_STAGE = mapOf(
    ConfidenceBuckets.HIGH to "clear_trend",
    ConfidenceBuckets.MODERATE to "pattern_forming",
    ConfidenceBuckets.LOW to "early_signal",
)

private val CONFIDENCE_RANK = mapOf(
    ConfidenceBuckets.HIGH to 3,
    ConfidenceBuckets.MODERATE to 2,
    ConfidenceBuckets.LOW to 1,
)

private val METRIC_LABELS = mapOf(
    Metrics.PHYSIQUE_SCORE to "body-scan trend",
    Metrics.WAIST to "waist measurement",
    Metrics.BODY_FAT_PCT to "body-fat measurement",
    Metrics.LEAN_MASS to "lean-mass measurement",
)

internal fun buildFindings(
    candidates: List<Candidate>,
    trendsByMetric: Map<String, Trend>,
    config: EngineConfig,
    runDate: String = "",
    quality: Map<String, QualityVerdict> = emptyMap(),
): FindingsResult {
    val findings = mutableListOf<Finding>()
    val suppressed = mutableListOf<SuppressedFinding>()

    for (candidate in candidates) {
        val supporting = candidate.metrics.mapNotNull { trendsByMetric[it] }
        val confidence: com.hexis.bi.intelligence.model.ConfidenceBreakdown
        val period: String
        val values: Map<String, String>

        when {
            candidate.confidenceFactors != null -> {
                confidence = scoreFactors(candidate.confidenceFactors, config)
                period = if (supporting.isEmpty()) {
                    "since first scan"
                } else {
                    "${supporting.maxOf { it.windowDays } / DAYS_PER_WEEK} weeks"
                }
                values = candidate.supportingValues
            }

            supporting.isNotEmpty() -> {
                confidence = computeConfidence(
                    trends = supporting,
                    agreementCount = candidate.agreementCount,
                    agreementExpected = candidate.agreementExpected,
                    contradiction = candidate.contradiction,
                    config = config,
                    runDate = runDate,
                )
                period = "${supporting.maxOf { it.windowDays } / DAYS_PER_WEEK} weeks"
                values = supporting.associate { it.metric to "${formatSigned(it.zNow, Z_VALUE_DIGITS)} MAD" }
            }

            else -> {
                suppressed += SuppressedFinding(
                    insightId = candidate.id,
                    interpretation = candidate.interpretation,
                    reason = SuppressionReason.NO_SUPPORTING_TREND,
                )
                continue
            }
        }

        if (candidate.requiresCorroboration &&
            candidate.corroboration.distinct().size < config.findings.minimumStressCorroboratingDomains
        ) {
            suppressed += SuppressedFinding(
                insightId = candidate.id,
                interpretation = candidate.interpretation,
                reason = SuppressionReason.STRESS_UNCORROBORATED,
                message = STRESS_GATE_DETAIL,
                confidence = confidence.bucket,
                confidenceScore = confidence.score,
            )
            continue
        }

        val failed = candidate.metrics.mapNotNull { quality[it] }.filterNot { it.ok }
        if (failed.isNotEmpty()) {
            suppressed += SuppressedFinding(
                insightId = candidate.id,
                interpretation = candidate.interpretation,
                reason = SuppressionReason.DATA_QUALITY,
                message = qualityMessage(failed),
                detail = failed.associate { it.metric to it.reasons },
                confidence = confidence.bucket,
                confidenceScore = confidence.score,
            )
            continue
        }

        findings += Finding(
            insightId = candidate.id,
            area = candidate.area,
            direction = candidate.direction,
            period = period,
            interpretation = candidate.interpretation,
            facts = candidate.facts,
            supportingValues = values,
            evidenceStage = EVIDENCE_STAGE.getValue(confidence.bucket),
            confidence = confidence.bucket,
            confidenceScore = confidence.score,
            rulesetVersion = config.rulesetVersion,
            source = candidate.source,
            informational = candidate.informational,
        )
    }

    return FindingsResult(rankAndFeature(findings, config), suppressed)
}

private fun rankAndFeature(findings: List<Finding>, config: EngineConfig): List<Finding> {
    val comparator = compareBy<Finding>(
        { config.priorityOf(it.area) },
        { CONFIDENCE_RANK.getValue(it.confidence) },
        { it.confidenceScore },
    )
    val ranked = findings.sortedWith(comparator.reversed())
    var featured = 0
    return ranked.mapIndexed { index, finding ->
        val isFeatured = !finding.informational && featured < config.featuredLimit
        if (isFeatured) featured++
        finding.copy(priorityRank = index + 1, featured = isFeatured)
    }
}

private fun qualityMessage(failed: List<QualityVerdict>): String = failed.mapNotNull { verdict ->
    val label = METRIC_LABELS[verdict.metric] ?: verdict.metric.replace("_", " ")
    val stale = verdict.reasons.firstOrNull { it.startsWith("stale_") }
    when {
        stale != null -> {
            val age = stale.substringBefore("d").removePrefix("stale_")
            val limit = if (">" in stale) stale.substringAfter(">").trimEnd(')') else "the freshness limit"
            "$label was last measured $age days ago; this insight requires data within $limit days"
        }

        verdict.reasons.isNotEmpty() -> "$label: ${verdict.reasons.joinToString(", ")}"
        else -> null
    }
}.joinToString("; ")
