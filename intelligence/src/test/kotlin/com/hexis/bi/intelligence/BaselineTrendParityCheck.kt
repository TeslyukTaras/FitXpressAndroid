package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.engine.EngineDates
import com.hexis.bi.intelligence.engine.computeBaseline
import com.hexis.bi.intelligence.engine.computeTrend
import com.hexis.bi.intelligence.engine.normalizeNegativeZero
import com.hexis.bi.intelligence.model.MetricPoint
import com.hexis.bi.intelligence.model.MetricSeries
import java.time.LocalDate
import org.junit.Test

class BaselineTrendParityCheck {

    @Test
    fun pinnedVector() {
        println("=== PARITY: baseline + trend vs Python engine-golden-v1 ===")
        val diffs = Diffs("baseline/trend vector")
        val fixture = ParityFixtures.json("engine-golden-v1.json")
        val input = fixture.obj("input")
        val expected = fixture.obj("expected")

        val metric = input.str("metric")
        val startDay = LocalDate.parse(input.str("start_day"))
        val windowDays = input.int("window_days")
        val series = MetricSeries(
            metric = metric,
            domain = input.str("domain"),
            unit = input.str("unit"),
            coverage = 1.0,
            points = input.doubles("values").mapIndexed { i, value ->
                MetricPoint(
                    date = startDay.plusDays(i.toLong()).toString(),
                    metric = metric,
                    value = value,
                    source = "daily",
                )
            },
        )

        val cutoff = EngineDates.minusDays(series.points.last().date, (windowDays - 1).toLong())
        val baseline = computeBaseline(series, input.int("baseline_days"), beforeDate = cutoff)
        if (baseline == null) {
            diffs.note("computeBaseline returned null")
            diffs.report()
            return
        }
        val trend = computeTrend(series, baseline, windowDays, ParityFixtures.config())

        diffs.near("baseline_median", baseline.median, expected.dbl("baseline_median"), EXACT)
        diffs.near("baseline_mad", baseline.mad, expected.dbl("baseline_mad"), EXACT)
        diffs.eq("baseline_n", baseline.n, expected.int("baseline_n"))
        diffs.eq("direction", trend.direction, expected.str("direction"))
        diffs.near("slope", normalizeNegativeZero(trend.slope), expected.dbl("slope"), EXACT)
        diffs.near("absolute_change", normalizeNegativeZero(trend.absChange), expected.dbl("absolute_change"), EXACT)
        diffs.near("coverage", trend.coverage, expected.dbl("coverage"), EXACT)
        diffs.near("z_now", normalizeNegativeZero(trend.zNow), expected.dbl("z_now"), EXACT)
        diffs.near("trend_strength", trend.trendStrength, expected.dbl("trend_strength"), EXACT)
        diffs.eq("persistence_days", trend.persistenceDays, expected.int("persistence_days"))
        diffs.eq(
            "prior_period_change",
            trend.priorPeriodChange?.let { normalizeNegativeZero(it) },
            expected.dblOrNull("prior_period_change"),
        )
        diffs.eq(
            "change_vs_prior",
            trend.changeVsPrior?.let { normalizeNegativeZero(it) },
            expected.dblOrNull("change_vs_prior"),
        )

        diffs.report()
    }

    private companion object {
        const val EXACT = 0.0
    }
}
