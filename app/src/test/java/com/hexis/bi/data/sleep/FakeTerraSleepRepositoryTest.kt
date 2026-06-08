package com.hexis.bi.data.sleep

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FakeTerraSleepRepositoryTest {

    private val repo = FakeTerraSleepRepository()

    @Test
    fun generatesSaneNightSession() = runBlocking {
        val session = repo.getSessionForNight(LocalDate.now()).getOrThrow()!!
        println("duration=${session.durationMinutes} eff=${session.efficiencyPercent} rhr=${session.restingHeartRateBpm} hrv=${session.hrvMs} stages=${session.stages.size}")
        assertTrue(session.durationMinutes in 420..510)
        assertTrue(session.efficiencyPercent in 60f..100f)
        assertTrue(session.restingHeartRateBpm in 50..62)
        assertTrue(session.hrvMs in 40..70)
        assertTrue(session.stages.size > 5)
        assertTrue(session.stages.any { it.stage == SleepStage.Deep })
        assertTrue(session.stages.any { it.stage == SleepStage.REM })
        assertTrue(session.bedtime.isBefore(session.wakeTime))
    }

    @Test
    fun rangeExcludesFutureAndIsStable() = runBlocking {
        val today = LocalDate.now()
        val sessions = repo.getSessionsForRange(today.minusDays(6), today.plusDays(3)).getOrThrow()
        // 7 days up to and including today, none in the future.
        assertEquals(7, sessions.size)
        assertTrue(sessions.all { !it.wakeTime.toLocalDate().isAfter(today) })

        val again = repo.getSessionsForRange(today.minusDays(6), today.plusDays(3)).getOrThrow()
        assertEquals(sessions.map { it.durationMinutes }, again.map { it.durationMinutes })
    }
}
