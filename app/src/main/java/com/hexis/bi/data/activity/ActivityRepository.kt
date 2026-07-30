package com.hexis.bi.data.activity

import com.hexis.bi.data.terra.TerraDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    suspend fun getSummaryForDate(date: LocalDate): Result<ActivitySummary?>

    suspend fun getSummariesForRange(
        start: LocalDate,
        end: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<List<ActivitySummary>>

    suspend fun cachedSummaries(start: LocalDate, end: LocalDate): List<ActivitySummary>

    suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit>

    val updates: Flow<Unit>
}
