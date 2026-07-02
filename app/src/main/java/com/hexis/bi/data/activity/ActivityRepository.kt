package com.hexis.bi.data.activity

import com.hexis.bi.data.terra.TerraDetail
import java.time.LocalDate

interface ActivityRepository {
    suspend fun getSummaryForDate(date: LocalDate): Result<ActivitySummary?>

    suspend fun getSummariesForRange(
        start: LocalDate,
        end: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<List<ActivitySummary>>
}
