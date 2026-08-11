package com.hexis.bi.intelligence

import com.hexis.bi.intelligence.engine.averageRanks
import com.hexis.bi.intelligence.engine.mad
import com.hexis.bi.intelligence.engine.mean
import com.hexis.bi.intelligence.engine.median
import com.hexis.bi.intelligence.engine.olsChange
import com.hexis.bi.intelligence.engine.roundHalfEven
import com.hexis.bi.intelligence.engine.spearmanAbs
import com.hexis.bi.intelligence.engine.theilSenChange
import com.hexis.bi.intelligence.engine.winsorize
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class MathXParityCheck {

    private val fixture = ParityFixtures.json("mathx-vectors.json")

    @Test
    fun primitives() {
        println("=== PARITY: numeric primitives vs Python mathx-vectors ===")
        val diffs = Diffs("mathx primitives")

        ParityFixtures.cases(fixture, "median").forEachIndexed { i, c ->
            diffs.near("median[$i]", median(c.doubles("input")), c.dbl("expected"), EXACT)
        }
        ParityFixtures.cases(fixture, "mad").forEachIndexed { i, c ->
            diffs.near("mad[$i]", mad(c.doubles("input")), c.dbl("expected"), EXACT)
        }
        ParityFixtures.cases(fixture, "mean").forEachIndexed { i, c ->
            diffs.near("mean[$i]", mean(c.doubles("input")), c.dbl("expected"), EXACT)
        }
        ParityFixtures.cases(fixture, "average_ranks").forEachIndexed { i, c ->
            diffs.nearAll("average_ranks[$i]", averageRanks(c.doubles("input")), c.doubles("expected"), EXACT)
        }
        ParityFixtures.cases(fixture, "spearman_abs").forEachIndexed { i, c ->
            diffs.near("spearman_abs[$i]", spearmanAbs(c.doubles("xs"), c.doubles("ys")), c.dbl("expected"), EXACT)
        }
        ParityFixtures.cases(fixture, "ols_change").forEachIndexed { i, c ->
            val fit = olsChange(c.doubles("days"), c.doubles("values"))
            diffs.near("ols_change[$i].slope", fit.slope, c.dbl("expected_slope"), EXACT)
            diffs.near("ols_change[$i].change", fit.change, c.dbl("expected_change"), EXACT)
        }
        ParityFixtures.cases(fixture, "theil_sen_change").forEachIndexed { i, c ->
            val fit = theilSenChange(c.doubles("days"), c.doubles("values"))
            diffs.near("theil_sen_change[$i].slope", fit.slope, c.dbl("expected_slope"), EXACT)
            diffs.near("theil_sen_change[$i].change", fit.change, c.dbl("expected_change"), EXACT)
        }
        ParityFixtures.cases(fixture, "winsorize").forEachIndexed { i, c ->
            val result = winsorize(c.doubles("input"), c.dbl("pct"))
            diffs.nearAll("winsorize[$i].values", result.values, c.doubles("expected"), EXACT)
            diffs.eq("winsorize[$i].clamped", result.clamped, c.int("expected_clamped"))
        }

        diffs.report()
    }

    @Test
    fun halfEvenRounding() {
        println("=== PARITY: rounding vs Python round() ===")
        val diffs = Diffs("roundHalfEven")
        var traps = 0

        ParityFixtures.cases(fixture, "py_round").forEachIndexed { i, c ->
            val value = c.dbl("value")
            val digits = c.int("digits")
            val got = roundHalfEven(value, digits)
            diffs.near("py_round[$i]($value, $digits)", got, c.dbl("expected"), EXACT)
            if (c.boolOr("differs_from_rint", false)) {
                traps++
                val naive = c.dbl("rint_naive")
                if (got == naive) {
                    diffs.note("py_round[$i]($value, $digits): matched the naive rint value $naive")
                }
            }
        }

        val declared = fixture.obj("notes").getValue("rint_trap_count").jsonPrimitive.int
        diffs.eq("rint trap cases exercised", traps, declared)
        diffs.report()
    }

    private companion object {
        const val EXACT = 0.0
    }
}
