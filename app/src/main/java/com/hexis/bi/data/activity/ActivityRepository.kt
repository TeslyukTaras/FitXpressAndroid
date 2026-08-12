package com.hexis.bi.data.activity

import com.hexis.bi.data.health.sync.HealthRangeCoverage
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    suspend fun getSummaryForDate(date: LocalDate): Result<ActivitySummary?>

    suspend fun getSummariesForRange(start: LocalDate, end: LocalDate): Result<List<ActivitySummary>>

    suspend fun coverage(start: LocalDate, end: LocalDate): HealthRangeCoverage

    suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit>

    val updates: Flow<Unit>
}
