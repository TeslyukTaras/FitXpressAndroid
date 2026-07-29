package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?>

    suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<List<SleepSession>>

    suspend fun cachedSessions(start: LocalDate, end: LocalDate): List<SleepSession>

    suspend fun sync(start: LocalDate, end: LocalDate): Result<Unit>

    val updates: Flow<Unit>
}
