package com.hexis.bi.data.recovery

import java.time.LocalDate

enum class StressLevel { Low, Medium, High }

enum class ActivityLoadLevel { Light, Moderate, Heavy }

data class RecoverySnapshot(
    val date: LocalDate,
    val score: Int,
    val sleepScore: Int,
    val hrvMs: Int,
    val restingHeartRateBpm: Int,
    val activeCalories: Int,
    /** Null when HRV is missing — derived solely from HRV. */
    val stressLevel: StressLevel?,
    /** Null when no activity data is available. */
    val activityLoad: ActivityLoadLevel?,
)
