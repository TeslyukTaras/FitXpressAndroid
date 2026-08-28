package com.hexis.bi.data.sleep

import com.hexis.bi.data.health.model.detachSamples
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimeInBedCheck {

    private val bedtime = LocalDateTime.of(2026, 8, 19, 22, 30)
    private val wakeTime = LocalDateTime.of(2026, 8, 20, 7, 8)

    private fun session(
        durationMinutes: Int = 443,
        stageSeconds: Map<SleepStage, Long> = mapOf(
            SleepStage.Deep to 3_600L,
            SleepStage.Light to 18_000L,
            SleepStage.REM to 3_900L,
            SleepStage.Awake to 5_580L,
        ),
    ) = SleepSession(
        bedtime = bedtime,
        wakeTime = wakeTime,
        durationMinutes = durationMinutes,
        efficiencyPercent = 82f,
        restingHeartRateBpm = 54,
        hrvMs = 61,
        sdnnMs = 70,
        stages = emptyList(),
        summaryStageSeconds = stageSeconds,
    )

    @Test
    fun `time in bed counts awake, so it exceeds both asleep and reported duration`() {
        val session = session()
        val totals = session.stageTotals
        val asleepMinutes = totals.deepMinutes + totals.lightMinutes + totals.remMinutes

        assertEquals(425, asleepMinutes)
        assertEquals(443, session.durationMinutes)
        assertEquals(518, session.timeInBedMinutes)
        assertTrue(session.timeInBedMinutes > session.durationMinutes)
    }

    @Test
    fun `stage totals sum to the header value`() {
        val session = session()

        assertEquals(session.timeInBedMinutes, session.stageTotals.totalMinutes)
        assertEquals(
            session.timeInBedMinutes,
            SleepStage.entries.sumOf { session.stageTotals.minutesFor(it) },
        )
    }

    @Test
    fun `a session with no stage data falls back to reported duration`() {
        val session = session(stageSeconds = emptyMap())

        assertEquals(0, session.stageTotals.totalMinutes)
        assertEquals(443, session.timeInBedMinutes)
    }

    @Test
    fun `time in bed survives a samples-less cache read`() {
        val slim = session().toCanonicalAggregate().detachSamples().first
        val restored = slim.toSleepSession()

        assertTrue(restored.stages.isEmpty())
        assertEquals(518, restored.timeInBedMinutes)
    }
}
