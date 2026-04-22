package com.hexis.bi.data.sleep

import java.time.LocalDateTime

data class TerraSleepStageInterval(
    val stage: TerraSleepStage,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    val durationMinutes: Int
        get() = java.time.Duration.between(start, end).toMinutes().toInt()
}

enum class TerraSleepStage { Deep, REM, Light, Awake }

data class TerraSleepSession(
    val bedtime: LocalDateTime,
    val wakeTime: LocalDateTime,
    val durationMinutes: Int,
    val efficiencyPercent: Float,
    val restingHeartRateBpm: Int,
    val hrvMs: Int,
    val stages: List<TerraSleepStageInterval>,
)
