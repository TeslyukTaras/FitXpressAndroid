package com.hexis.bi.data.sleep

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Dev-only [SleepRepository] that fabricates realistic, Terra-shaped sleep JSON and runs it through
 * the production [TerraSleepJsonMapper]. This means the data travels the exact same parse path as a
 * real Terra `/sleep` payload, so the UI sees genuine [SleepSession] objects — handy for previewing
 * the screen without a connected wearable.
 *
 * Wired in via `BuildConfig.FAKE_SLEEP_DATA` (true only on the `dev` flavor). Data is seeded by the
 * wake date, so each night is varied but stable across reloads. No sessions are generated for future
 * dates.
 */
class FakeTerraSleepRepository : SleepRepository {

    override suspend fun getSessionForNight(date: LocalDate): Result<SleepSession?> =
        Result.success(buildNightJson(date)?.let(TerraSleepJsonMapper::sessionOrNull))

    override suspend fun getSessionsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<SleepSession>> {
        val sessions = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .mapNotNull { date -> buildNightJson(date)?.let(TerraSleepJsonMapper::sessionOrNull) }
            .toList()
        return Result.success(sessions)
    }

    /** Builds a Terra `/sleep` row for the night whose wake-up falls on [wakeDate], or null if future. */
    private fun buildNightJson(wakeDate: LocalDate): JsonElement? {
        if (wakeDate.isAfter(LocalDate.now())) return null

        val rnd = Random(wakeDate.toEpochDay())
        val bedtime = wakeDate.minusDays(1).atTime(22 + rnd.nextInt(2), rnd.nextInt(60))
        val inBedMinutes = (MIN_IN_BED_MIN + rnd.nextInt(MAX_IN_BED_MIN - MIN_IN_BED_MIN)).toLong()
        val wakeTime = bedtime.plusMinutes(inBedMinutes)

        val intervals = buildHypnogram(bedtime, wakeTime, rnd)
        val awakeMinutes = intervals
            .filter { it.stage == SleepStage.Awake }
            .sumOf { java.time.Duration.between(it.start, it.end).toMinutes() }
        val asleepSeconds = (inBedMinutes - awakeMinutes).coerceAtLeast(1) * 60f
        val inBedSeconds = inBedMinutes * 60f

        return buildJsonObject {
            put("metadata", buildJsonObject {
                put("start_time", bedtime.toString())
                put("end_time", wakeTime.toString())
            })
            put("sleep_durations_data", buildJsonObject {
                put("sleep_efficiency", asleepSeconds / inBedSeconds * 100f)
                put("in_bed", buildJsonObject {
                    put("duration_in_bed_seconds", inBedSeconds)
                })
                put("asleep", buildJsonObject {
                    put("duration_asleep_state_seconds", asleepSeconds)
                })
            })
            put("heart_rate_data", buildJsonObject {
                put("summary", buildJsonObject {
                    put("resting_hr_bpm", 50 + rnd.nextInt(13))
                    put("avg_hr_bpm", 55 + rnd.nextInt(13))
                    put("avg_hrv_rmssd", 40 + rnd.nextInt(31))
                    put("avg_hrv_sdnn", 50 + rnd.nextInt(31))
                })
                put("detailed", buildJsonObject {
                    put("hr_samples", sampleSeries(bedtime, wakeTime, rnd, "bpm", from = 62, to = 52, jitter = 4))
                    put("hrv_samples_rmssd", sampleSeries(bedtime, wakeTime, rnd, "hrv_rmssd", from = 42, to = 58, jitter = 6))
                })
            })
            put("sleep_stage_data", buildJsonObject {
                put("light_periods", periodsArray(intervals, SleepStage.Light))
                put("deep_periods", periodsArray(intervals, SleepStage.Deep))
                put("rem_periods", periodsArray(intervals, SleepStage.REM))
                put("wake_periods", periodsArray(intervals, SleepStage.Awake))
            })
        }
    }

    /** A Terra-shaped detailed sample array drifting [from] → [to] across the night with jitter. */
    private fun sampleSeries(
        bedtime: LocalDateTime,
        wakeTime: LocalDateTime,
        rnd: Random,
        valueField: String,
        from: Int,
        to: Int,
        jitter: Int,
    ) = buildJsonArray {
        val totalMinutes = java.time.Duration.between(bedtime, wakeTime).toMinutes()
        val steps = (totalMinutes / SAMPLE_INTERVAL_MIN).toInt().coerceAtLeast(1)
        for (i in 0..steps) {
            val progress = i / steps.toFloat()
            val base = from + (to - from) * progress
            val value = (base + rnd.nextInt(-jitter, jitter + 1)).toInt().coerceAtLeast(1)
            add(buildJsonObject {
                put("timestamp", bedtime.plusMinutes((i * SAMPLE_INTERVAL_MIN)).toString())
                put(valueField, value)
            })
        }
    }

    private fun periodsArray(intervals: List<StageSpan>, stage: SleepStage) = buildJsonArray {
        intervals.filter { it.stage == stage }.forEach { span ->
            add(buildJsonObject {
                put("start", span.start.toString())
                put("end", span.end.toString())
            })
        }
    }

    private data class StageSpan(val stage: SleepStage, val start: LocalDateTime, val end: LocalDateTime)

    /**
     * Generates a plausible hypnogram: deep sleep concentrated early, REM later, light sleep
     * dominant throughout, with the occasional brief awakening — mirroring a typical night cycle.
     */
    private fun buildHypnogram(
        bedtime: LocalDateTime,
        wakeTime: LocalDateTime,
        rnd: Random,
    ): List<StageSpan> {
        val totalMinutes = java.time.Duration.between(bedtime, wakeTime).toMinutes().toFloat()
        val spans = mutableListOf<StageSpan>()
        var cursor = bedtime

        fun add(stage: SleepStage, minutes: Int) {
            if (!cursor.isBefore(wakeTime)) return
            val end = minOf(cursor.plusMinutes(minutes.toLong()), wakeTime)
            spans.add(StageSpan(stage, cursor, end))
            cursor = end
        }

        add(SleepStage.Awake, 3 + rnd.nextInt(5))
        while (cursor.isBefore(wakeTime.minusMinutes(10))) {
            val progress = java.time.Duration.between(bedtime, cursor).toMinutes() / totalMinutes
            add(SleepStage.Light, 15 + rnd.nextInt(25))
            if (progress < 0.55f) add(SleepStage.Deep, 15 + rnd.nextInt(30)) else add(SleepStage.Light, 10 + rnd.nextInt(15))
            add(SleepStage.Light, 8 + rnd.nextInt(15))
            if (progress > 0.35f) add(SleepStage.REM, 10 + rnd.nextInt(30))
            if (rnd.nextFloat() < 0.25f) add(SleepStage.Awake, 2 + rnd.nextInt(6))
        }
        // Fill any remainder up to wake, finishing with a brief awakening.
        add(SleepStage.Light, Int.MAX_VALUE)
        if (spans.lastOrNull()?.stage != SleepStage.Awake) {
            spans[spans.lastIndex] = spans.last().let { it.copy(end = it.end.minusMinutes(2)) }
            spans.add(StageSpan(SleepStage.Awake, spans.last().end, wakeTime))
        }
        return spans.filter { it.start.isBefore(it.end) }.sortedBy { it.start }
    }

    private companion object {
        const val MIN_IN_BED_MIN = 420 // 7h
        const val MAX_IN_BED_MIN = 510 // 8.5h
        const val SAMPLE_INTERVAL_MIN = 5L
    }
}
