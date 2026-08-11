package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.engine.CanonicalDayRecord
import com.hexis.bi.intelligence.engine.CanonicalScanRecord
import com.hexis.bi.intelligence.engine.EngineReport
import com.hexis.bi.intelligence.engine.IntelligenceEngine
import com.hexis.bi.intelligence.engine.normalizeCanonical
import com.hexis.bi.intelligence.model.EngineInput
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class GoldenParityCheck {

    private val golden = ParityFixtures.json("engine-full-golden-v1.json")

    private fun days(node: JsonObject, key: String): List<CanonicalDayRecord> {
        val holder = node.getValue(key)
        val rows = if (holder is JsonArray) holder else holder.jsonObject.arr("data")
        return rows.map { row ->
            CanonicalDayRecord(
                day = row.jsonObject.str("day"),
                metrics = row.jsonObject.doubleMap("metrics"),
            )
        }
    }

    private fun scans(node: JsonObject): List<CanonicalScanRecord> =
        node.arr("scans").map { row ->
            val o = row.jsonObject
            CanonicalScanRecord(
                documentId = o.str("document_id"),
                completedAt = o.str("completed_at"),
                savedAt = o.str("saved_at"),
                weightKg = o.dblOrNull("weight"),
                estimatedWeightKg = o.dblOrNull("estimated_weight"),
                fatPercentage = o.dblOrNull("fat_percentage"),
                leanBodyMassKg = o.dblOrNull("lean_body_mass"),
                estimatedLeanBodyMassKg = o.dblOrNull("estimated_lean_body_mass"),
                circumferenceParamsCm = o.doubleMap("circumference_params"),
            )
        }

    private fun runWith(configResource: String): EngineReport {
        val input = golden.obj("input")
        return IntelligenceEngine.run(
            EngineInput(
                runDate = input.str("run_date"),
                pullDays = input.int("pull_days"),
                points = normalizeCanonical(days(input, "daily"), days(input, "sleep"), scans(input)),
            ),
            ParityFixtures.config(configResource),
        )
    }

    private fun compare(label: String, configResource: String) {
        val diffs = Diffs(label)
        val report = try {
            runWith(configResource)
        } catch (e: Exception) {
            diffs.note("$label: engine threw ${e::class.simpleName}: ${e.message}")
            diffs.report()
            return
        }
        val expected = golden.obj("expected")
        val want = expected.arr("findings").map { it.jsonObject }

        diffs.eq("finding ids in order", report.findings.map { it.insightId }, want.map { it.str("insight_id") })

        val byId = report.findings.associateBy { it.insightId }
        want.forEach { w ->
            val id = w.str("insight_id")
            val got = byId[id]
            if (got == null) {
                diffs.note("missing finding $id")
                return@forEach
            }
            diffs.eq("$id.interpretation", got.interpretation, w.str("interpretation"))
            diffs.eq("$id.direction", got.direction, w.str("direction"))
            diffs.eq("$id.area", got.area, w.str("area"))
            diffs.eq("$id.source", got.source, w.str("source"))
            diffs.eq("$id.featured", got.featured, w.bool("featured"))
            diffs.eq("$id.informational", got.informational, w.bool("informational"))
            diffs.eq("$id.evidence_stage", got.evidenceStage, w.str("evidence_stage"))
            diffs.eq("$id.confidence", got.confidence, w.str("confidence"))
            diffs.near("$id.confidence_score", got.confidenceScore, w.dbl("confidence_score"), EXACT)
            diffs.eq("$id.rank", got.priorityRank, w.int("rank"))
        }
        (byId.keys - want.map { it.str("insight_id") }.toSet())
            .forEach { diffs.note("unexpected finding $it") }

        diffs.eq("still_learning", report.stillLearning, expected.bool("still_learning"))

        val foundations = expected.obj("foundations")
        diffs.eq("foundations.window_days", report.foundations.windowDays, foundations.int("window_days"))
        diffs.eq("foundations.direction", report.foundations.direction, foundations.str("direction"))
        diffs.eq("foundations.statuses", report.foundations.statuses, foundations.stringMap("statuses"))

        val drift = expected.obj("physique_drift")
        diffs.eq("drift.direction", report.physiqueDrift.direction, drift.str("direction"))
        diffs.eq("drift.scans", report.physiqueDrift.scans, drift.int("scans"))
        diffs.eq("drift.drift", report.physiqueDrift.drift, drift.dblOrNull("drift"))

        val wantQuality = expected.stringMap("quality")
        val gotQuality = report.verdicts.associate { it.metric to it.status }
        diffs.eq("quality metrics", gotQuality.keys.sorted(), wantQuality.keys.sorted())
        wantQuality.forEach { (metric, status) ->
            gotQuality[metric]?.let { diffs.eq("quality[$metric]", it, status) }
        }

        val wantSuppressed = expected.arr("suppressed").map { it.jsonObject }
        diffs.eq(
            "suppressed ids",
            report.suppressed.map { it.insightId },
            wantSuppressed.map { it.str("insight_id") },
        )
        report.suppressed.zip(wantSuppressed).forEach { (got, w) ->
            diffs.eq("suppressed.${got.insightId}.reason", got.reason, w.str("reason"))
        }

        diffs.report()
    }

    @Test
    fun contractsConfig() {
        println("=== PARITY: full engine vs Python golden (contracts config) ===")
        compare("full engine / contracts config", "engine-config-v1.json")
    }

    @Test
    fun shippedAssetConfig() {
        println("=== PARITY: full engine vs Python golden (shipped asset) ===")
        compare("full engine / shipped asset", "intelligence_config_v1.json")
    }

    private companion object {
        const val EXACT = 0.0
    }
}
