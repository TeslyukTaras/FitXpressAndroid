package com.hexis.bi.data.activity

import java.time.LocalDate

interface ActivityRepository {
    suspend fun getSummaryForDate(date: LocalDate): Result<ActivitySummary?>

    suspend fun getSummariesForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<ActivitySummary>>

    /** Drops cached summaries so the next read re-fetches — call after a Terra sync lands new data. */
    suspend fun invalidate()
}
