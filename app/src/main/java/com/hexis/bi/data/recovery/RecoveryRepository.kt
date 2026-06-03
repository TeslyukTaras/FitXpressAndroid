package com.hexis.bi.data.recovery

import java.time.LocalDate

interface RecoveryRepository {
    suspend fun getSnapshotForDate(date: LocalDate): Result<RecoverySnapshot?>

    suspend fun getSnapshotsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<RecoverySnapshot>>

    /** Drops cached snapshots so the next read re-fetches — call after a Terra sync lands new data. */
    suspend fun invalidate()
}
