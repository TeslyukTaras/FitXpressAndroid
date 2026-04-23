package com.hexis.bi.data.sleep

import java.time.LocalDate

interface TerraSleepRepository {
    suspend fun getSessionForNight(date: LocalDate): Result<TerraSleepSession?>

    suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<TerraSleepSession>>
}
