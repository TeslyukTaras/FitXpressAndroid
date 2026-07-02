package com.hexis.bi.data.recovery

import java.time.LocalDate

enum class StressLevel { Low, Medium, High }

enum class ActivityLoadLevel { Light, Moderate, Heavy }

data class RecoverySnapshot(
    val date: LocalDate,
    val score: Int,
    val sleepScore: Int,
    /** Average HRV as RMSSD, in milliseconds. Drives the recovery score and stress level. */
    val hrvMs: Int,
    /** Average HRV as SDNN, in milliseconds. Shown in HRV Details for context. */
    val sdnnMs: Int,
    val restingHeartRateBpm: Int,
    val activeCalories: Int,
    /** Null when HRV is missing — derived solely from HRV. */
    val stressLevel: StressLevel?,
    /** Null when no activity data is available. */
    val activityLoad: ActivityLoadLevel?,
)
