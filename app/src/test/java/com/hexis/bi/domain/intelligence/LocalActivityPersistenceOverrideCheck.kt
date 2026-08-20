package com.hexis.bi.domain.intelligence

import com.hexis.bi.intelligence.config.EngineConfigParser
import com.hexis.bi.intelligence.engine.Domains
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class LocalActivityPersistenceOverrideCheck {

    @Test
    fun `overrides only activity persistence without mutating loaded config`() {
        val loaded = EngineConfigParser.parse(configJson()).getOrThrow()

        val effective = loaded.withLocalActivityPersistenceOverride()

        assertEquals(14, loaded.trend.minPersistDaysFor(Domains.ACTIVITY))
        assertEquals(7, effective.trend.minPersistDaysFor(Domains.ACTIVITY))
        assertEquals(
            loaded.trend.minPersistDays - Domains.ACTIVITY,
            effective.trend.minPersistDays - Domains.ACTIVITY,
        )
        assertNotSame(loaded, effective)
    }

    private fun configJson(): String = File("src/main/assets/intelligence_config_v1.json").readText()
}
