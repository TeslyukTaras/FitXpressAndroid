package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.config.EngineConfigParser
import com.hexis.bi.intelligence.engine.CanonicalDayRecord
import com.hexis.bi.intelligence.engine.CanonicalScanRecord
import com.hexis.bi.intelligence.engine.EngineReport
import com.hexis.bi.intelligence.engine.IntelligenceEngine
import com.hexis.bi.intelligence.engine.normalizeCanonical
import com.hexis.bi.intelligence.model.EngineInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldenParityCheck {

    private fun resource(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/$name")) { "missing resource $name" }
            .bufferedReader().use { it.readText() }

    private val golden = Json.parseToJsonElement(resource("engine-full-golden-v1.json")).jsonObject

    private fun JsonPrimitive.orNullDouble(): Double? =
        if (content == "null") null else content.toDoubleOrNull()

    private fun days(node: JsonObject, key: String): List<CanonicalDayRecord> {
        val holder = node[key]!!
        val rows = if (holder is JsonArray) holder else holder.jsonObject["data"]!!.jsonArray
        return rows.map { row ->
            val o = row.jsonObject
            CanonicalDayRecord(
                day = o["day"]!!.jsonPrimitive.content,
                metrics = o["metrics"]!!.jsonObject.mapValues { it.value.jsonPrimitive.double },
            )
        }
    }

    private fun scans(node: JsonObject): List<CanonicalScanRecord> =
        node["scans"]!!.jsonArray.map { row ->
            val o = row.jsonObject
            fun num(k: String) = o[k]?.jsonPrimitive?.orNullDouble()
            CanonicalScanRecord(
                documentId = o["document_id"]!!.jsonPrimitive.content,
                completedAt = o["completed_at"]!!.jsonPrimitive.content,
                savedAt = o["saved_at"]!!.jsonPrimitive.content,
                weightKg = num("weight"),
                estimatedWeightKg = num("estimated_weight"),
                fatPercentage = num("fat_percentage"),
                leanBodyMassKg = num("lean_body_mass"),
                estimatedLeanBodyMassKg = num("estimated_lean_body_mass"),
                circumferenceParamsCm = o["circumference_params"]?.jsonObject
                    ?.mapValues { it.value.jsonPrimitive.double }.orEmpty(),
            )
        }

    private fun runWith(configResource: String): EngineReport {
        val input = golden["input"]!!.jsonObject
        val config = EngineConfigParser.parse(resource(configResource)).getOrThrow()
        return IntelligenceEngine.run(
            EngineInput(
                runDate = input["run_date"]!!.jsonPrimitive.content,
                pullDays = input["pull_days"]!!.jsonPrimitive.int,
                points = normalizeCanonical(days(input, "daily"), days(input, "sleep"), scans(input)),
            ),
            config,
        )
    }

    private fun compare(label: String, configResource: String): List<String> {
        val problems = mutableListOf<String>()
        val report = try {
            runWith(configResource)
        } catch (e: Exception) {
            return listOf("$label: engine threw ${e::class.simpleName}: ${e.message}")
        }
        val expected = golden["expected"]!!.jsonObject
        val want = expected["findings"]!!.jsonArray.map { it.jsonObject }

        if (want.size != report.findings.size) {
            problems += "$label: finding count ${report.findings.size} != ${want.size}"
        }
        val wantIds = want.map { it["insight_id"]!!.jsonPrimitive.content }
        val gotIds = report.findings.map { it.insightId }
        (wantIds - gotIds.toSet()).forEach { problems += "$label: missing finding $it" }
        (gotIds - wantIds.toSet()).forEach { problems += "$label: unexpected finding $it" }

        want.forEachIndexed { i, w ->
            val id = w["insight_id"]!!.jsonPrimitive.content
            val got = report.findings.getOrNull(i) ?: return@forEachIndexed
            if (got.insightId != id) { problems += "$label: order[$i] ${got.insightId} != $id"; return@forEachIndexed }
            if (got.direction != w["direction"]!!.jsonPrimitive.content)
                problems += "$label: $id direction ${got.direction} != ${w["direction"]!!.jsonPrimitive.content}"
            if (got.confidence != w["confidence"]!!.jsonPrimitive.content)
                problems += "$label: $id confidence ${got.confidence} != ${w["confidence"]!!.jsonPrimitive.content}"
            if (got.evidenceStage != w["evidence_stage"]!!.jsonPrimitive.content)
                problems += "$label: $id stage ${got.evidenceStage} != ${w["evidence_stage"]!!.jsonPrimitive.content}"
            if (got.priorityRank != w["rank"]!!.jsonPrimitive.int)
                problems += "$label: $id rank ${got.priorityRank} != ${w["rank"]!!.jsonPrimitive.int}"
            if (got.featured != w["featured"]!!.jsonPrimitive.boolean)
                problems += "$label: $id featured ${got.featured} != ${w["featured"]!!.jsonPrimitive.boolean}"
            if (got.confidenceScore != w["confidence_score"]!!.jsonPrimitive.double)
                problems += "$label: $id score ${got.confidenceScore} != ${w["confidence_score"]!!.jsonPrimitive.double}"
        }

        if (report.stillLearning != expected["still_learning"]!!.jsonPrimitive.boolean)
            problems += "$label: still_learning ${report.stillLearning}"
        val f = expected["foundations"]!!.jsonObject
        if (report.foundations.windowDays != f["window_days"]!!.jsonPrimitive.int)
            problems += "$label: foundations.window ${report.foundations.windowDays}"
        if (report.foundations.direction != f["direction"]!!.jsonPrimitive.content)
            problems += "$label: foundations.direction ${report.foundations.direction}"
        val wantStatuses = f["statuses"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
        if (report.foundations.statuses != wantStatuses)
            problems += "$label: foundations.statuses ${report.foundations.statuses} != $wantStatuses"
        val d = expected["physique_drift"]!!.jsonObject
        if (report.physiqueDrift.direction != d["direction"]!!.jsonPrimitive.content)
            problems += "$label: drift.direction ${report.physiqueDrift.direction}"
        if (report.physiqueDrift.scans != d["scans"]!!.jsonPrimitive.int)
            problems += "$label: drift.scans ${report.physiqueDrift.scans}"
        if (report.physiqueDrift.drift != d["drift"]!!.jsonPrimitive.double)
            problems += "$label: drift.drift ${report.physiqueDrift.drift} != ${d["drift"]!!.jsonPrimitive.double}"
        val wantQ = expected["quality"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
        val gotQ = report.verdicts.associate { it.metric to it.status }
        (wantQ.keys - gotQ.keys).forEach { problems += "$label: quality missing metric $it" }
        (gotQ.keys - wantQ.keys).forEach { problems += "$label: quality extra metric $it" }
        wantQ.forEach { (m, s) -> gotQ[m]?.let { if (it != s) problems += "$label: quality[$m] $it != $s" } }
        if (report.suppressed.size != expected["suppressed"]!!.jsonArray.size)
            problems += "$label: suppressed ${report.suppressed.size} != ${expected["suppressed"]!!.jsonArray.size}"
        return problems
    }

    @Test
    fun report() {
        val contracts = compare("contracts v1.1.1", "engine-config-v1.json")
        val shipped = compare("shipped asset", "intelligence_config_v1.json")
        println("=== PARITY vs frozen Python golden (22 findings) ===")
        listOf("contracts config" to contracts, "shipped asset config" to shipped).forEach { (name, p) ->
            println("\n--- $name: ${if (p.isEmpty()) "MATCHES" else "${p.size} difference(s)"}")
            p.take(40).forEach { println("    $it") }
        }
        val all = contracts + shipped
        assertTrue("engine diverged from the frozen Python golden:\n" + all.joinToString("\n"), all.isEmpty())
    }
}
