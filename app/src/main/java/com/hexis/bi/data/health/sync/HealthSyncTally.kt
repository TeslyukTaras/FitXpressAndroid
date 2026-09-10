package com.hexis.bi.data.health.sync

data class HealthSyncTally(val rows: Int = 0, val windows: Int = 0) {

    val fetchedNothing: Boolean get() = windows > 0 && rows == 0

    operator fun plus(other: HealthSyncTally): HealthSyncTally =
        HealthSyncTally(rows + other.rows, windows + other.windows)

    companion object {
        val NOTHING_FETCHED = HealthSyncTally()
    }
}
