package com.hexis.bi.data.sleep

import java.time.Duration
import java.time.LocalDateTime

enum class SleepStage { Deep, REM, Light, Awake }

data class SleepStageInterval(
    val stage: SleepStage,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    val durationMinutes: Int
        get() = Duration.between(start, end).toMinutes().toInt()
}

data class SleepSession(
    val bedtime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val durationMinutes: Int,
    val efficiencyPercent: Float,
    val restingHeartRateBpm: Int,
    /** Average HRV as RMSSD, in milliseconds. */
    val hrvMs: Int,
    /** Average HRV as SDNN, in milliseconds. */
    val sdnnMs: Int,
    val stages: List<SleepStageInterval>,
)
