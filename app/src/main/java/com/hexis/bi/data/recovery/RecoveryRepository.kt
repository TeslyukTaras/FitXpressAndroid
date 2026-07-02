package com.hexis.bi.data.recovery

import java.time.LocalDate

interface RecoveryRepository {
    suspend fun getSnapshotForDate(date: LocalDate): Result<RecoverySnapshot?>

    suspend fun getSnapshotsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<RecoverySnapshot>>
}
