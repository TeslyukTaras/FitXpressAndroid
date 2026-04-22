package com.hexis.bi.data.sleep

import java.time.LocalDate

interface TerraSleepRepository {
    /**
     * Returns null when Terra has no session for the requested date (e.g. wearable not synced).
     * Returns [Result.failure] for transient/network errors.
     */
    suspend fun getSessionForNight(date: LocalDate): Result<TerraSleepSession?>
}
