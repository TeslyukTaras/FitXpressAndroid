package com.hexis.bi.domain.body

import com.hexis.bi.data.scan.ScanRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PhysiqueScoreCheck {

    private val heightCm = 165f

    private fun scan(shouldersCm: Float?): ScanRecord = ScanRecord(
        measurements = buildMap {
            put(BodyMeasurementKeys.Waist, 84.6f)
            shouldersCm?.let { put(BodyMeasurementKeys.Shoulders, it) }
        },
        frontLinearParams = mapOf(BodyMeasurementKeys.Waist to 30.0f),
        weightKg = 60.5f,
        fatPercentage = 28.9f,
        leanBodyMassKg = 43.0f,
    )

    private fun engineScore(): Float {
        val bodyFat = 4f + ((28.9f - 24f) / (35f - 24f)) * (2f - 4f)
        val leanPct = 43.0f / 60.5f * 100f
        val lean = 3f + ((leanPct - 60f) / (90f - 60f)) * (10f - 3f)
        val waist = 2f + ((84.6f / heightCm - 0.62f) / (0.43f - 0.62f)) * (10f - 2f)
        return (0.5f * bodyFat + 0.25f * lean + 0.15f * waist) / 0.9f
    }

    @Test
    fun `score matches the engine composite over body fat lean mass and waist`() {
        val actual = scan(shouldersCm = 110f).physiqueScore(heightCm)
        assertNotNull(actual)
        assertEquals(engineScore().toDouble(), actual!!.toDouble(), 0.001)
    }

    @Test
    fun `proportion does not move the score`() {
        val narrow = scan(shouldersCm = 100f).physiqueScore(heightCm)
        val broad = scan(shouldersCm = 130f).physiqueScore(heightCm)
        val absent = scan(shouldersCm = null).physiqueScore(heightCm)
        assertEquals(narrow!!.toDouble(), broad!!.toDouble(), 0.0001)
        assertEquals(narrow.toDouble(), absent!!.toDouble(), 0.0001)
    }

    @Test
    fun `proportion is still reported for display`() {
        val breakdown = scan(shouldersCm = 130f).physiqueScoreBreakdown(heightCm)
        assertNotNull(breakdown?.proportionScore)
        assertNotNull(breakdown?.shoulderToWaistRatio)
    }
}
