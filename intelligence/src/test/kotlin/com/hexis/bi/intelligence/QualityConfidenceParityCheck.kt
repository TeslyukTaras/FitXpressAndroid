package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.engine.assess
import com.hexis.bi.intelligence.engine.computeConfidence
import com.hexis.bi.intelligence.engine.scoreFactors
import com.hexis.bi.intelligence.engine.stillLearning
import com.hexis.bi.intelligence.model.QualityVerdict
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class QualityConfidenceParityCheck {

    private val fixture = ParityFixtures.json("quality-confidence-vectors.json")
    private val config = ParityFixtures.config()

    @Test
    fun qualityGate() {
        println("=== PARITY: quality gate vs Python ===")
        val diffs = Diffs("quality gate")

        ParityFixtures.cases(fixture, "quality").forEach { case ->
            val name = case.str("name")
            val series = case.getValue("series").asSeries()
            val trend = case.present("trend")?.asTrend(metric = series.metric, domain = series.domain)
            val verdict = assess(series, trend, case.str("run_date"), config)
            val expected = case.obj("expected")

            diffs.eq("$name.ok", verdict.ok, expected.bool("ok"))
            diffs.eq("$name.status", verdict.status, expected.str("status"))
            diffs.eq("$name.reasons", verdict.reasons, expected.strings("reasons"))
            diffs.near("$name.coverage", verdict.coverage, expected.dbl("coverage"), EXACT)
            diffs.eq("$name.last_date", verdict.lastDate, expected.strOrNull("last_date").orEmpty())
        }

        diffs.report()
    }

    @Test
    fun stillLearningFlag() {
        println("=== PARITY: still-learning flag vs Python ===")
        val diffs = Diffs("still_learning")

        ParityFixtures.cases(fixture, "still_learning").forEach { case ->
            val verdicts = case.getValue("coverages").jsonArray.mapIndexed { i, pair ->
                val row = pair.jsonArray
                val domain = row[0].jsonPrimitive.content
                "metric_$i" to QualityVerdict(
                    metric = "metric_$i",
                    domain = domain,
                    ok = true,
                    status = "ok",
                    reasons = emptyList(),
                    coverage = row[1].jsonPrimitive.content.toDouble(),
                    lastDate = "",
                )
            }.toMap()
            diffs.eq(case.str("name"), stillLearning(verdicts, config), case.bool("expected"))
        }

        diffs.report()
    }

    @Test
    fun confidenceFactorScoring() {
        println("=== PARITY: confidence factor scoring vs Python ===")
        val diffs = Diffs("confidence factors")

        ParityFixtures.cases(fixture, "confidence_factors").forEach { case ->
            val name = case.str("name")
            val breakdown = scoreFactors(case.doubleMap("factors"), config)
            val expected = case.obj("expected")
            expected.doubleMap("factors").forEach { (key, want) ->
                diffs.near("$name.factors.$key", breakdown.factors[key] ?: Double.NaN, want, EXACT)
            }
            diffs.near("$name.score", breakdown.score, expected.dbl("score"), EXACT)
            diffs.eq("$name.bucket", breakdown.bucket, expected.str("bucket"))
        }

        diffs.report()
    }

    @Test
    fun confidenceFromTrends() {
        println("=== PARITY: confidence from trends vs Python ===")
        val diffs = Diffs("confidence trends")

        ParityFixtures.cases(fixture, "confidence_trends").forEach { case ->
            val name = case.str("name")
            val trends = case.arr("trends").map { it.asTrend() }
            val breakdown = computeConfidence(
                trends = trends,
                agreementCount = case.int("agreement_count"),
                agreementExpected = case.int("agreement_expected"),
                contradiction = case.dbl("contradiction"),
                config = config,
                runDate = case.strOrNull("run_date").orEmpty(),
            )
            val expected = case.obj("expected")
            expected.doubleMap("factors").forEach { (key, want) ->
                diffs.near("$name.factors.$key", breakdown.factors[key] ?: Double.NaN, want, EXACT)
            }
            diffs.near("$name.score", breakdown.score, expected.dbl("score"), EXACT)
            diffs.eq("$name.bucket", breakdown.bucket, expected.str("bucket"))
        }

        diffs.report()
    }

    private companion object {
        const val EXACT = 0.0
    }
}
