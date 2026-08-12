package com.hexis.bi.data.health.sync

data class HealthRangeCoverage(
    val syncedDays: Int = 0,
    val totalDays: Int = 0,
) {
    val isComplete: Boolean get() = syncedDays >= totalDays

    val isPartial: Boolean get() = !isComplete

    companion object {
        val SETTLED = HealthRangeCoverage(0, 0)
    }
}
