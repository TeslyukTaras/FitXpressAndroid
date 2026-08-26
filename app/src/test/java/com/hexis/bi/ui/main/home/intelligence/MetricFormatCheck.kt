package com.hexis.bi.ui.main.home.intelligence

import com.hexis.bi.utils.constants.EngineUnits
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class MetricFormatCheck {

    private val systemLocale = Locale.getDefault()

    @Before
    fun pinLocale() = Locale.setDefault(Locale.US)

    @After
    fun restoreLocale() = Locale.setDefault(systemLocale)

    private fun render(unit: String, value: Double, isMetric: Boolean): String {
        val f = MetricFormat.of(unit, isMetric)
        assertNotNull("no format for $unit", f)
        return f!!.render(value).joinToString("") { it.text } + " " + f.unit("")
    }

    @Test
    fun preferenceDependentUnitsRender() {
        println("=== metric ===")
        println("  distance  " + render("m", 45000.0, true))
        println("  waist     " + render("cm", 82.4, true))
        println("  lean mass " + render("kg", 56.2, true))
        println("=== imperial ===")
        println("  distance  " + render("m", 45000.0, false))
        println("  waist     " + render("cm", 82.4, false))
        println("  lean mass " + render("kg", 56.2, false))

        assertEquals("45.0 km", render("m", 45000.0, true))
        assertEquals("82 cm", render("cm", 82.4, true))
        assertEquals("56.2 kg", render("kg", 56.2, true))
        assertEquals("28.0 mi", render("m", 45000.0, false))
        assertEquals("32 in", render("cm", 82.4, false))
        assertEquals("123.9 lb", render("kg", 56.2, false))
    }
}

class InsightRoundingCheck {

    private fun render(unit: String, raw: Double): String =
        MetricFormat.of(unit, isMetric = true)!!.render(raw).joinToString("") { it.text }

    @Test
    fun `sleep and recovery readings drop their decimals`() {
        assertEquals("26", render(EngineUnits.MINUTES, 25.9))
        assertEquals("51", render(EngineUnits.BPM, 51.0))
        assertEquals("22", render(EngineUnits.MILLISECONDS, 22.4))
        assertEquals("42", render(EngineUnits.VO2MAX, 42.3))
        assertEquals("87", render(EngineUnits.CENTIMETRES, 87.1))
    }

    @Test
    fun `body fat keeps one decimal`() {
        assertEquals("18.4", render(EngineUnits.PERCENT, 18.4))
    }

    @Test
    fun `mass and scores keep one decimal`() {
        assertEquals("78.4", render(EngineUnits.KILOGRAMS, 78.4))
        assertEquals("5.1", render(EngineUnits.SCORE_10, 5.1))
        assertEquals("62.4", render(EngineUnits.SCORE_100, 62.4))
    }
}
