package com.hexis.bi.ui.main.home.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MetricFormatCheck {

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
        assertEquals("82.4 cm", render("cm", 82.4, true))
        assertEquals("56.2 kg", render("kg", 56.2, true))
        assertEquals("28.0 mi", render("m", 45000.0, false))
        assertEquals("32.4 in", render("cm", 82.4, false))
        assertEquals("123.9 lb", render("kg", 56.2, false))
    }
}
