package com.hexis.bi.data.intelligence

import com.hexis.bi.intelligence.config.EngineConfigParser
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationWindowRetentionCheck {

    private val config = EngineConfigParser.parse(
        File("src/main/assets/intelligence_config_v1.json").readText(),
    ).getOrThrow()

    private val today = LocalDate.of(2026, 8, 18)

    @Test
    fun `cache retains every day the engine asks for`() {
        val historyDays = config.windows.trendDays.max()
        val window = observationWindow(today, historyDays, config.windows.baselineDays)
        val retentionFloor = today.minusDays(CanonicalCacheConstants.DAY_RETENTION_DAYS)

        assertEquals(historyDays + config.windows.baselineDays, window.size)
        assertTrue(
            "retention floor $retentionFloor drops ${window.first()}, which the engine needs",
            !window.first().isBefore(retentionFloor),
        )
    }

    @Test
    fun `widest trend window still leaves a full baseline`() {
        val widest = config.windows.trendDays.max()
        val baselineDays = config.windows.baselineDays
        val window = observationWindow(today, widest, baselineDays)

        val trendStart = today.minusDays(widest - 1L)
        val baselineDaysAvailable = window.count { it.isBefore(trendStart) }

        assertEquals(baselineDays, baselineDaysAvailable)
    }
}
