package com.hexis.bi.data.sleep

import org.junit.Assert.assertEquals
import org.junit.Test

class TerraNumericFieldCheck {

    private fun row(restingHr: Any?, avgHr: Any?, level: Any?): Map<String, Any?> = mapOf(
        "metadata" to mapOf(
            "start_time" to "2026-08-11T21:21:59+00:00",
            "end_time" to "2026-08-12T06:06:16+00:00",
        ),
        "sleep_durations_data" to mapOf(
            "asleep" to mapOf("duration_asleep_state_seconds" to 25500.0),
            "hypnogram_samples" to listOf(
                mapOf("timestamp" to "2026-08-11T21:21:59+00:00", "level" to level),
                mapOf("timestamp" to "2026-08-12T00:00:00+00:00", "level" to level),
            ),
        ),
        "heart_rate_data" to mapOf(
            "summary" to mapOf("resting_hr_bpm" to restingHr, "avg_hr_bpm" to avgHr),
        ),
    )

    private fun rhrOf(restingHr: Any?, avgHr: Any? = 62) =
        TerraSleepJsonMapper.sessionOrNull(row(restingHr, avgHr, 4))!!.restingHeartRateBpm

    private fun stagesOf(level: Any?) =
        TerraSleepJsonMapper.sessionOrNull(row(53, 62, level))!!.stages

    @Test
    fun `whole number resting hr parses`() {
        assertEquals(53, rhrOf(53))
    }

    @Test
    fun `resting hr written with a trailing zero parses`() {
        assertEquals(53, rhrOf(53.0))
    }

    @Test
    fun `fractional resting hr truncates instead of falling back to avg hr`() {
        assertEquals(53, rhrOf(53.6))
        assertEquals(53, rhrOf(53.6, 62.4))
    }

    @Test
    fun `a zero resting hr falls back to avg hr`() {
        assertEquals(62, rhrOf(0))
        assertEquals(62, rhrOf(0.0, 62.4))
    }

    @Test
    fun `no measured heart rate stays zero`() {
        assertEquals(0, rhrOf(0, 0.0))
        assertEquals(0, rhrOf(null, null))
    }

    @Test
    fun `hypnogram levels parse whether whole or float`() {
        assertEquals(2, stagesOf(4).size)
        assertEquals(2, stagesOf(4.0).size)
        assertEquals(stagesOf(4).map { it.stage }, stagesOf(4.0).map { it.stage })
    }

    @Test
    fun `an unmapped hypnogram level still drops the sample`() {
        assertEquals(0, stagesOf(2).size)
        assertEquals(0, stagesOf(2.0).size)
    }
}
