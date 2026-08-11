package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.engine.agingScore
import com.hexis.bi.intelligence.engine.buildCompositeSeries
import com.hexis.bi.intelligence.engine.driftConfidenceFactors
import com.hexis.bi.intelligence.engine.evaluateFoundations
import com.hexis.bi.intelligence.engine.evaluatePhysiqueDrift
import com.hexis.bi.intelligence.engine.longevityScore
import com.hexis.bi.intelligence.engine.physiqueParts
import com.hexis.bi.intelligence.engine.physiqueScore
import com.hexis.bi.intelligence.engine.stressScore
import com.hexis.bi.intelligence.model.MetricPoint
import com.hexis.bi.intelligence.model.MetricSeries
import com.hexis.bi.intelligence.model.Trend
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class CompositeParityCheck {

    private val fixture = ParityFixtures.json("composite-vectors.json")
    private val config = ParityFixtures.config()

    private fun withHeight(heightCm: Double?): EngineConfig =
        config.copy(composites = config.composites.copy(heightCm = heightCm))

    @Test
    fun compositeDayScores() {
        println("=== PARITY: composite day scores vs Python ===")
        val diffs = Diffs("composite days")

        ParityFixtures.cases(fixture, "composite_days").forEach { case ->
            val name = case.str("name")
            val cfg = withHeight(case.dblOrNull("height_cm"))
            val day = case.doubleMap("day")
            val expected = case.obj("expected")

            diffs.eq("$name.longevity_score", longevityScore(day, cfg), expected.dblOrNull("longevity_score"))
            diffs.eq("$name.aging_score", agingScore(day, cfg), expected.dblOrNull("aging_score"))
            diffs.eq("$name.stress_score", stressScore(day, cfg), expected.dblOrNull("stress_score"))
            diffs.eq("$name.physique_score", physiqueScore(day, cfg), expected.dblOrNull("physique_score"))

            val parts = physiqueParts(day, cfg)
            val wantParts = expected.obj("physique_parts")
            diffs.eq("$name.physique_parts.keys", parts.keys.sorted(), wantParts.keys.sorted())
            wantParts.forEach { (component, node) ->
                val part = parts[component]
                if (part == null) {
                    diffs.note("$name.physique_parts.$component: missing")
                } else {
                    diffs.near("$name.physique_parts.$component.weight", part.weight, node.jsonObject.dbl("weight"), EXACT)
                    diffs.near("$name.physique_parts.$component.score", part.score, node.jsonObject.dbl("score"), EXACT)
                }
            }
        }

        diffs.report()
    }

    @Test
    fun compositeSeriesBuild() {
        println("=== PARITY: composite series vs Python ===")
        val diffs = Diffs("composite series")

        val case = fixture.obj("composite_series")
        val cfg = withHeight(case.dblOrNull("height_cm"))
        val source = case.arr("input").map { it.asSeries() }
        val built = buildCompositeSeries(source, case.int("window_days"), cfg)
        val expected = case.arr("expected").map { it.jsonObject }

        diffs.eq("series metrics", built.map { it.metric }, expected.map { it.str("metric") })
        expected.forEach { want ->
            val metric = want.str("metric")
            val got = built.firstOrNull { it.metric == metric }
            if (got == null) {
                diffs.note("composite series: missing $metric")
                return@forEach
            }
            diffs.eq("$metric.domain", got.domain, want.str("domain"))
            diffs.eq("$metric.unit", got.unit, want.str("unit"))
            diffs.near("$metric.coverage", got.coverage, want.dbl("coverage"), EXACT)
            val wantPoints = want.arr("points").map { it.jsonObject }
            diffs.eq("$metric.points.size", got.points.size, wantPoints.size)
            got.points.zip(wantPoints).forEachIndexed { i, (gp, wp) ->
                diffs.eq("$metric.points[$i].date", gp.date, wp.str("date"))
                diffs.near("$metric.points[$i].value", gp.value, wp.dbl("value"), EXACT)
            }
        }

        diffs.report()
    }

    @Test
    fun foundationRollUp() {
        println("=== PARITY: foundation roll-up vs Python ===")
        val diffs = Diffs("foundations")

        ParityFixtures.cases(fixture, "foundations").forEach { case ->
            val name = case.str("name")
            val leanChange = case.dbl("lean_abs_change")
            val trends = case.stringMap("directions").mapValues { (metric, direction) ->
                Trend(
                    metric = metric,
                    domain = config.domainOf(metric),
                    windowDays = 30,
                    direction = direction,
                    slope = 0.0,
                    velocity = 0.0,
                    persistenceDays = 0,
                    absChange = if (metric == LEAN_MASS) leanChange else 0.0,
                    relChange = 0.0,
                    trendStrength = 0.0,
                    coverage = 1.0,
                    zNow = 0.0,
                )
            }
            val result = evaluateFoundations(trends, config)
            val expected = case.obj("expected")
            diffs.eq("$name.direction", result.direction, expected.str("direction"))
            diffs.eq("$name.statuses", result.statuses, expected.stringMap("statuses"))
        }

        diffs.report()
    }

    @Test
    fun physiqueDrift() {
        println("=== PARITY: physique drift vs Python ===")
        val diffs = Diffs("physique drift")

        ParityFixtures.cases(fixture, "physique_drift").forEach { case ->
            val name = case.str("name")
            val cfg = withHeight(case.dblOrNull("height_cm"))
            val byMetric = LinkedHashMap<String, MutableList<MetricPoint>>()
            case.arr("scans").map { it.jsonObject }.forEach { scan ->
                val date = scan.str("date")
                scan.doubleMap("values").forEach { (metric, value) ->
                    byMetric.getOrPut(metric) { mutableListOf() } +=
                        MetricPoint(date = date, metric = metric, value = value, source = "scan")
                }
            }
            val series = byMetric.map { (metric, points) ->
                MetricSeries(metric = metric, domain = BODY, unit = "u", points = points, coverage = 1.0)
            }

            val drift = evaluatePhysiqueDrift(series, cfg)
            val expected = case.obj("expected")
            diffs.eq("$name.status", drift.status, expected.str("status"))
            diffs.eq("$name.scans", drift.scans, expected.int("scans"))
            diffs.eq("$name.drift", drift.drift, expected.dblOrNull("drift"))
            diffs.eq("$name.direction", drift.direction, expected.strOrNull("direction"))
            diffs.eq("$name.driver", drift.driver, expected.strOrNull("driver"))
            diffs.eq("$name.first_scan", drift.firstScan, expected.strOrNull("first_scan"))
            diffs.eq("$name.last_scan", drift.lastScan, expected.strOrNull("last_scan"))
            expected.doubleMap("component_deltas").forEach { (component, want) ->
                diffs.near(
                    "$name.component_deltas.$component",
                    drift.componentDeltas[component] ?: Double.NaN,
                    want,
                    EXACT,
                )
            }

            case.present("expected_confidence_factors")?.let { node ->
                val want = node.jsonObject
                val got = driftConfidenceFactors(drift, cfg, case.str("run_date"))
                want.asDoubleMap().forEach { (key, value) ->
                    diffs.near("$name.drift_factors.$key", got[key] ?: Double.NaN, value, EXACT)
                }
            }
        }

        diffs.report()
    }

    private companion object {
        const val EXACT = 0.0
        const val LEAN_MASS = "lean_mass"
        const val BODY = "body"
    }
}
