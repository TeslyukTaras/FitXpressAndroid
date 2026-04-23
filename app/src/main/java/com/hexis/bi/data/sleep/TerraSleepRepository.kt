package com.hexis.bi.data.sleep

import java.time.LocalDate

interface TerraSleepRepository {
    /**
     * Returns null when Terra has no session for the requested date (e.g. wearable not synced).
     * Returns [Result.failure] for transient/network errors.
     */
    suspend fun getSessionForNight(date: LocalDate): Result<TerraSleepSession?>

    /**
     * Returns every sleep session whose wake time falls in `[start, end]` (inclusive).
     * Empty list when the user has no connection or Terra has no data — never null.
     */
    suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<TerraSleepSession>>
}
