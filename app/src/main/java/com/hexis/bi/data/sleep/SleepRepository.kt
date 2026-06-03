package com.hexis.bi.data.sleep

import java.time.LocalDate

interface SleepRepository {
    suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?>

    suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<SleepSession>>

    /** Drops cached sessions so the next read re-fetches — call after a Terra sync lands new data. */
    suspend fun invalidate()
}
