package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraDetail
import com.hexis.bi.data.health.sync.HealthRangeCoverage
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?>

    suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<List<SleepSession>>

    suspend fun coverage(start: LocalDate, end: LocalDate): HealthRangeCoverage

    suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit>

    val updates: Flow<Unit>
}
