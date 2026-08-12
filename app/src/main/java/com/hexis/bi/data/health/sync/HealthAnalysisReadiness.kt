package com.hexis.bi.data.health.sync

import com.hexis.bi.utils.constants.HealthAnalysisConstants

data class HealthAnalysisReadiness(
    val daily: HealthRangeCoverage = HealthRangeCoverage.SETTLED,
    val sleep: HealthRangeCoverage = HealthRangeCoverage.SETTLED,
) {
    val isReady: Boolean get() = daily.isComplete && sleep.isComplete

    val syncedDays: Int get() = minOf(daily.syncedDays, sleep.syncedDays)

    val requiredDays: Int get() = maxOf(daily.totalDays, sleep.totalDays)

    val progress: Float
        get() = if (requiredDays == 0) 0f else syncedDays.toFloat() / requiredDays

    override fun toString(): String =
        "$syncedDays/$requiredDays days (daily ${daily.syncedDays}, sleep ${sleep.syncedDays})"

    companion object {
        val NOT_READY = HealthAnalysisReadiness(
            daily = HealthRangeCoverage(0, HealthAnalysisConstants.REQUIRED_DAYS.toInt()),
            sleep = HealthRangeCoverage(0, HealthAnalysisConstants.REQUIRED_DAYS.toInt()),
        )
    }
}
