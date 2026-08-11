package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.config.EngineConfigParser
import com.hexis.bi.intelligence.model.MetricPoint
import com.hexis.bi.intelligence.model.MetricSeries
import com.hexis.bi.intelligence.model.QualityVerdict
import com.hexis.bi.intelligence.model.Trend
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue

internal object ParityFixtures {

    const val FIXTURE_SOURCE = "fixture"

    fun text(name: String): String =
        checkNotNull(ParityFixtures::class.java.getResourceAsStream("/$name")) {
            "missing fixture resource $name"
        }.bufferedReader().use { it.readText() }

    fun json(name: String): JsonObject = Json.parseToJsonElement(text(name)).jsonObject

    fun config(name: String = "engine-config-v1.json"): EngineConfig =
        EngineConfigParser.parse(text(name)).getOrThrow()

    fun cases(fixture: JsonObject, section: String): List<JsonObject> =
        fixture.getValue(section).jsonArray.map { it.jsonObject }
}

internal fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

internal fun JsonObject.strOrNull(key: String): String? =
    this[key]?.jsonPrimitive?.takeIf { it.content != "null" }?.content

internal fun JsonObject.dbl(key: String): Double = getValue(key).jsonPrimitive.double

internal fun JsonObject.dblOr(key: String, fallback: Double): Double =
    this[key]?.jsonPrimitive?.doubleOrNullSafe() ?: fallback

internal fun JsonObject.dblOrNull(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNullSafe()

internal fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int

internal fun JsonObject.intOr(key: String, fallback: Int): Int =
    this[key]?.jsonPrimitive?.content?.toIntOrNull() ?: fallback

internal fun JsonObject.bool(key: String): Boolean = getValue(key).jsonPrimitive.boolean

internal fun JsonObject.boolOr(key: String, fallback: Boolean): Boolean =
    this[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: fallback

internal fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

internal fun JsonObject.present(key: String): JsonElement? = this[key]?.takeIf { it !is JsonNull }

internal fun JsonObject.arr(key: String): JsonArray = getValue(key).jsonArray

internal fun JsonObject.strings(key: String): List<String> =
    this[key]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()

internal fun JsonObject.doubles(key: String): List<Double> =
    getValue(key).jsonArray.map { it.jsonPrimitive.double }

internal fun JsonObject.doubleMap(key: String): Map<String, Double> =
    this[key]?.jsonObject?.mapValues { it.value.jsonPrimitive.double }.orEmpty()

internal fun JsonElement.jsonArrayStrings(): List<String> =
    jsonArray.map { it.jsonPrimitive.content }

internal fun JsonObject.asDoubleMap(): Map<String, Double> =
    mapValues { it.value.jsonPrimitive.double }

internal fun JsonObject.stringMap(key: String): Map<String, String> =
    this[key]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()

private fun kotlinx.serialization.json.JsonPrimitive.doubleOrNullSafe(): Double? =
    if (content == "null") null else content.toDoubleOrNull()

internal fun JsonElement.asSeries(fallbackUnit: String = "u"): MetricSeries {
    val o = jsonObject
    val metric = o.str("metric")
    return MetricSeries(
        metric = metric,
        domain = o.str("domain"),
        unit = o.strOrNull("unit") ?: fallbackUnit,
        coverage = o.dblOr("coverage", 0.0),
        points = o.arr("points").map { p ->
            MetricPoint(
                date = p.jsonObject.str("date"),
                metric = metric,
                value = p.jsonObject.dbl("value"),
                source = ParityFixtures.FIXTURE_SOURCE,
            )
        },
    )
}

internal fun JsonElement.asTrend(
    metric: String? = null,
    domain: String? = null,
    direction: String? = null,
): Trend {
    val o = jsonObject
    return Trend(
        metric = metric ?: o.str("metric"),
        domain = domain ?: o.str("domain"),
        windowDays = o.intOr("window_days", 0),
        direction = o.strOrNull("direction") ?: direction.orEmpty(),
        slope = o.dblOr("slope", 0.0),
        velocity = o.dblOr("velocity", 0.0),
        persistenceDays = o.intOr("persistence_days", 0),
        absChange = o.dblOr("abs_change", 0.0),
        relChange = o.dblOr("rel_change", 0.0),
        trendStrength = o.dblOr("trend_strength", 0.0),
        coverage = o.dblOr("coverage", 0.0),
        zNow = o.dblOr("z_now", 0.0),
        lastDate = o.strOrNull("last_date").orEmpty(),
        priorPeriodChange = o.dblOrNull("prior_period_change"),
        changeVsPrior = o.dblOrNull("change_vs_prior"),
    )
}

internal fun JsonElement.asQualityVerdict(): QualityVerdict {
    val o = jsonObject
    return QualityVerdict(
        metric = o.str("metric"),
        domain = o.str("domain"),
        ok = o.boolOr("ok", true),
        status = o.strOrNull("status").orEmpty(),
        reasons = o.strings("reasons"),
        coverage = o.dblOr("coverage", 0.0),
        lastDate = o.strOrNull("last_date").orEmpty(),
    )
}

internal class Diffs(private val label: String) {

    private val problems = mutableListOf<String>()
    private var checks = 0

    fun eq(where: String, got: Any?, want: Any?) {
        checks++
        if (got != want) problems += "$where: $got != $want"
    }

    fun near(where: String, got: Double, want: Double, tolerance: Double) {
        checks++
        if (!closeEnough(got, want, tolerance)) problems += "$where: $got != $want (tol $tolerance)"
    }

    fun nearAll(where: String, got: List<Double>, want: List<Double>, tolerance: Double) {
        checks++
        if (got.size != want.size) {
            problems += "$where: size ${got.size} != ${want.size}"
            return
        }
        got.indices.filterNot { closeEnough(got[it], want[it], tolerance) }
            .forEach { problems += "$where[$it]: ${got[it]} != ${want[it]} (tol $tolerance)" }
    }

    fun note(problem: String) {
        problems += problem
    }

    fun report() {
        val verdict = if (problems.isEmpty()) "MATCHES ($checks checks)" else "${problems.size} difference(s)"
        println("--- $label: $verdict")
        problems.take(MAX_REPORTED).forEach { println("    $it") }
        if (problems.size > MAX_REPORTED) println("    ... and ${problems.size - MAX_REPORTED} more")
        assertTrue(
            "$label diverged from the Python reference:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    private fun closeEnough(got: Double, want: Double, tolerance: Double): Boolean =
        got == want || (got.isNaN() && want.isNaN()) || kotlin.math.abs(got - want) <= tolerance

    private companion object {
        const val MAX_REPORTED = 40
    }
}
