package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.engine.Candidate
import com.hexis.bi.intelligence.engine.FoundationsResult
import com.hexis.bi.intelligence.engine.buildFindings
import com.hexis.bi.intelligence.engine.evaluateRules
import com.hexis.bi.intelligence.engine.foundationCandidates
import com.hexis.bi.intelligence.engine.singleMetricCandidates
import com.hexis.bi.intelligence.model.Trend
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class FindingsParityCheck {

    private val fixture = ParityFixtures.json("findings-vectors.json")
    private val config = ParityFixtures.config()

    private fun trendsFrom(directions: Map<String, String>, fallbackDomain: String): Map<String, Trend> =
        directions.mapValues { (metric, direction) ->
            Trend(
                metric = metric,
                domain = config.domains[metric] ?: fallbackDomain,
                windowDays = 30,
                direction = direction,
                slope = 0.0,
                velocity = 0.0,
                persistenceDays = 0,
                absChange = 0.0,
                relChange = 0.0,
                trendStrength = 0.0,
                coverage = 1.0,
                zNow = 0.0,
            )
        }

    private fun Diffs.compareCandidates(label: String, got: List<Candidate>, want: List<JsonObject>) {
        eq("$label.ids", got.map { it.id }, want.map { it.str("id") })
        got.zip(want).forEach { (g, w) ->
            val id = w.str("id")
            eq("$label.$id.area", g.area, w.str("area"))
            eq("$label.$id.interpretation", g.interpretation, w.str("interpretation"))
            eq("$label.$id.direction", g.direction, w.str("direction"))
            eq("$label.$id.facts", g.facts, w.strings("facts"))
            eq("$label.$id.metrics", g.metrics, w.strings("metrics"))
            eq("$label.$id.agreement_count", g.agreementCount, w.int("agreement_count"))
            eq("$label.$id.agreement_expected", g.agreementExpected, w.int("agreement_expected"))
            eq("$label.$id.contradiction", g.contradiction, w.dbl("contradiction"))
            eq("$label.$id.source", g.source, w.str("source"))
            eq("$label.$id.informational", g.informational, w.bool("informational"))
            eq("$label.$id.requires_corroboration", g.requiresCorroboration, w.bool("requires_corroboration"))
            eq("$label.$id.corroboration", g.corroboration, w.strings("corroboration"))
        }
    }

    @Test
    fun relationshipRules() {
        println("=== PARITY: relationship rules vs Python ===")
        val diffs = Diffs("relationship rules")

        ParityFixtures.cases(fixture, "rules").forEach { case ->
            val trends = trendsFrom(case.stringMap("directions"), BODY)
            diffs.compareCandidates(
                case.str("name"),
                evaluateRules(trends),
                case.arr("expected").map { it.jsonObject },
            )
        }

        diffs.report()
    }

    @Test
    fun singleMetricRules() {
        println("=== PARITY: single-metric candidates vs Python ===")
        val diffs = Diffs("single-metric candidates")

        ParityFixtures.cases(fixture, "single_metric").forEach { case ->
            val trends = trendsFrom(case.stringMap("directions"), ACTIVITY)
            diffs.compareCandidates(
                case.str("name"),
                singleMetricCandidates(trends, config),
                case.arr("expected").map { it.jsonObject },
            )
        }

        diffs.report()
    }

    @Test
    fun foundationRollUpCandidates() {
        println("=== PARITY: foundation candidates vs Python ===")
        val diffs = Diffs("foundation candidates")

        ParityFixtures.cases(fixture, "foundation_candidates").forEach { case ->
            val direction = case.str("direction")
            val result = FoundationsResult(direction, case.stringMap("statuses"))
            diffs.compareCandidates(
                direction,
                foundationCandidates(result),
                case.arr("expected").map { it.jsonObject },
            )
        }

        diffs.report()
    }

    @Test
    fun findingsAndSuppression() {
        println("=== PARITY: findings, suppression and ranking vs Python ===")
        val diffs = Diffs("findings")

        ParityFixtures.cases(fixture, "findings").forEach { case ->
            val name = case.str("name")
            val candidates = case.arr("candidates").map { it.jsonObject.asCandidate() }
            val trends = case.arr("trends").associate { node ->
                val trend = node.asTrend()
                trend.metric to trend
            }
            val quality = case.arr("quality").associate { node ->
                val verdict = node.asQualityVerdict()
                verdict.metric to verdict
            }

            val result = buildFindings(candidates, trends, config, case.str("run_date"), quality)
            val wantFindings = case.arr("expected_findings").map { it.jsonObject }
            val wantSuppressed = case.arr("expected_suppressed").map { it.jsonObject }

            diffs.eq("$name.finding ids", result.findings.map { it.insightId }, wantFindings.map { it.str("insight_id") })
            result.findings.zip(wantFindings).forEach { (got, want) ->
                val id = want.str("insight_id")
                diffs.eq("$name.$id.area", got.area, want.str("area"))
                diffs.eq("$name.$id.direction", got.direction, want.str("direction"))
                diffs.eq("$name.$id.period", got.period, want.str("period"))
                diffs.eq("$name.$id.interpretation", got.interpretation, want.str("interpretation"))
                diffs.eq("$name.$id.facts", got.facts, want.strings("facts"))
                diffs.eq("$name.$id.supporting_values", got.supportingValues, want.stringMap("supporting_values"))
                diffs.eq("$name.$id.evidence_stage", got.evidenceStage, want.str("evidence_stage"))
                diffs.eq("$name.$id.confidence", got.confidence, want.str("confidence"))
                diffs.near("$name.$id.confidence_score", got.confidenceScore, want.dbl("confidence_score"), EXACT)
                diffs.eq("$name.$id.priority_rank", got.priorityRank, want.int("priority_rank"))
                diffs.eq("$name.$id.source", got.source, want.str("source"))
                diffs.eq("$name.$id.featured", got.featured, want.bool("featured"))
                diffs.eq("$name.$id.informational", got.informational, want.bool("informational"))
                diffs.eq("$name.$id.ruleset_version", got.rulesetVersion, want.str("ruleset_version"))
            }

            diffs.eq(
                "$name.suppressed ids",
                result.suppressed.map { it.insightId },
                wantSuppressed.map { it.str("insight_id") },
            )
            result.suppressed.zip(wantSuppressed).forEach { (got, want) ->
                val id = want.str("insight_id")
                diffs.eq("$name.suppressed.$id.interpretation", got.interpretation, want.str("interpretation"))
                diffs.eq("$name.suppressed.$id.reason", got.reason, want.str("reason"))
                diffs.eq("$name.suppressed.$id.message", got.message, want.strOrNull("message"))
                diffs.eq(
                    "$name.suppressed.$id.detail",
                    got.detail,
                    want.obj("detail").mapValues { entry -> entry.value.jsonArrayStrings() },
                )
            }
        }

        diffs.report()
    }

    private fun JsonObject.asCandidate(): Candidate = Candidate(
        id = str("id"),
        area = str("area"),
        interpretation = str("interpretation"),
        direction = str("direction"),
        facts = strings("facts"),
        metrics = strings("metrics"),
        agreementCount = int("agreement_count"),
        agreementExpected = int("agreement_expected"),
        contradiction = dbl("contradiction"),
        source = str("source"),
        informational = bool("informational"),
        requiresCorroboration = bool("requires_corroboration"),
        corroboration = strings("corroboration"),
        confidenceFactors = present("confidence_factors")?.jsonObject?.asDoubleMap(),
        supportingValues = stringMap("supporting_values"),
    )

    private companion object {
        const val EXACT = 0.0
        const val BODY = "body"
        const val ACTIVITY = "activity"
    }
}
