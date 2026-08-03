package com.hexis.bi.data.sleep

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

enum class SleepStage { Deep, REM, Light, Awake }

data class SleepStageInterval(
    val stage: SleepStage,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    val durationMinutes: Int
        get() = Duration.between(start, end).toMinutes().toInt()
}

/** A single timestamped reading from the night, e.g. a heart-rate or HRV sample. */
data class SleepSample(
    val epochMillis: Long,
    val value: Int,
)

/**
 * Sample times are wall-clock [LocalDateTime] with no zone, so a fixed offset keeps every
 * comparison and delta in this file exact while costing one primitive instead of an object.
 */
internal fun LocalDateTime.toSampleMillis(): Long = toInstant(ZoneOffset.UTC).toEpochMilli()

data class SleepStageTotals(
    val deepMinutes: Int = 0,
    val lightMinutes: Int = 0,
    val remMinutes: Int = 0,
    val awakeMinutes: Int = 0,
) {
    fun minutesFor(stage: SleepStage): Int = when (stage) {
        SleepStage.Deep -> deepMinutes
        SleepStage.REM -> remMinutes
        SleepStage.Light -> lightMinutes
        SleepStage.Awake -> awakeMinutes
    }
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
    val isNap: Boolean = false,
    /** Intra-night heart-rate readings (bpm), if the provider reports detailed samples. */
    val heartRateSamples: List<SleepSample> = emptyList(),
    /** Intra-night HRV readings (RMSSD, ms), if the provider reports detailed samples. */
    val hrvSamples: List<SleepSample> = emptyList(),
    val sessionCount: Int = 1,
    val aggregateStageTotals: SleepStageTotals? = null,
) {
    val stageTotals: SleepStageTotals
        get() = aggregateStageTotals ?: SleepStageTotals(
            deepMinutes = stages.filter { it.stage == SleepStage.Deep }.sumOf { it.durationMinutes },
            lightMinutes = stages.filter { it.stage == SleepStage.Light }.sumOf { it.durationMinutes },
            remMinutes = stages.filter { it.stage == SleepStage.REM }.sumOf { it.durationMinutes },
            awakeMinutes = stages.filter { it.stage == SleepStage.Awake }.sumOf { it.durationMinutes },
        )
}
