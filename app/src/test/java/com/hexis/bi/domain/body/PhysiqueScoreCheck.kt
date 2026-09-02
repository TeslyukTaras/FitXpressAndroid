package com.hexis.bi.domain.body

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.config.EngineConfigParser
import com.hexis.bi.intelligence.engine.Metrics
import com.hexis.bi.intelligence.engine.physiqueScore
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class PhysiqueScoreCheck {

    private val heightCm = 165f

    private val config: EngineConfig =
        EngineConfigParser.parse(File("src/main/assets/intelligence_config_v1.json").readText())
            .getOrThrow()

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

    private fun engineScore(shouldersCm: Float?): Double? {
        val scoped = config.copy(
            composites = config.composites.copy(heightCm = heightCm.toDouble()),
        )
        val day = buildMap {
            put(Metrics.WEIGHT, 60.5)
            put(Metrics.BODY_FAT_PCT, 28.9)
            put(Metrics.LEAN_MASS, 43.0)
            put(Metrics.WAIST, 84.6)
            shouldersCm?.let { put(Metrics.SHOULDER_TO_WAIST, (it / 30.0f).toDouble()) }
        }
        return physiqueScore(day, scoped)
    }

    @Test
    fun `the screens and the engine run the same formula`() {
        val shoulders = 110f
        val fromScreens = scan(shoulders).physiqueScore(config, heightCm)
        assertNotNull(fromScreens)
        assertEquals(engineScore(shoulders)!!, fromScreens!!.toDouble(), 0.0001)
    }

    @Test
    fun `proportion stays out of the score while the config disables it`() {
        assertFalse(config.composites.physique.enableProportion)
        val narrow = scan(shouldersCm = 100f).physiqueScore(config, heightCm)
        val broad = scan(shouldersCm = 130f).physiqueScore(config, heightCm)
        val absent = scan(shouldersCm = null).physiqueScore(config, heightCm)
        assertEquals(narrow!!.toDouble(), broad!!.toDouble(), 0.0001)
        assertEquals(narrow.toDouble(), absent!!.toDouble(), 0.0001)
    }

    @Test
    fun `enabling proportion in the config moves the score`() {
        val enabled = config.copy(
            composites = config.composites.copy(
                physique = config.composites.physique.copy(enableProportion = true),
            ),
        )
        val gated = scan(shouldersCm = 130f).physiqueScore(config, heightCm)!!
        val weighted = scan(shouldersCm = 130f).physiqueScore(enabled, heightCm)!!
        assertNotNull(gated)
        assertFalse(gated.toDouble() == weighted.toDouble())
    }

    @Test
    fun `proportion is still reported for display`() {
        val breakdown = scan(shouldersCm = 130f).physiqueScoreBreakdown(config, heightCm)
        assertNotNull(breakdown?.proportionScore)
        assertNotNull(breakdown?.shoulderToWaistRatio)
    }

    @Test
    fun `lean percentage is derived from the averaged day bucket`() {
        val timestamp = Instant.parse("2026-01-01T10:00:00Z").toEpochMilli()
        val scans = listOf(
            ScanRecord(
                id = "first",
                timestamp = timestamp,
                weightKg = 100f,
                fatPercentage = 20f,
                leanBodyMassKg = 50f,
            ),
            ScanRecord(
                id = "second",
                timestamp = timestamp + 60_000,
                weightKg = 50f,
                fatPercentage = 20f,
                leanBodyMassKg = 40f,
            ),
        )

        val day = scans.predictionDays(config, heightCm, ZoneOffset.UTC).single()
        assertEquals(45.0, day.values.getValue(com.hexis.bi.intelligence.prediction.PredictionSeries.LEAN_KG), 1e-9)
        assertEquals(60.0, day.values.getValue(com.hexis.bi.intelligence.prediction.PredictionSeries.LEAN_PCT), 1e-9)
        assertEquals("second", day.scanId)
    }
}
