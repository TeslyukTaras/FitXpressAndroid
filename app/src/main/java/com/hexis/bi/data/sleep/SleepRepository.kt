package com.hexis.bi.data.sleep

import com.hexis.bi.data.terra.TerraDetail
import java.time.LocalDate

interface SleepRepository {
    suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?>

    suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<List<SleepSession>>
}
