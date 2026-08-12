package com.hexis.bi.data.sleep

import com.hexis.bi.data.health.model.AggregatePeriod
import com.hexis.bi.data.health.model.AggregateProvenance
import com.hexis.bi.data.health.model.AggregateQuality
import com.hexis.bi.data.health.model.CANONICAL_HEALTH_AGGREGATE_VERSION
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.model.CanonicalDailyUi
import com.hexis.bi.data.health.model.CanonicalMetrics
import com.hexis.bi.data.health.model.CanonicalSample
import com.hexis.bi.data.health.model.CanonicalStageInterval
import com.hexis.bi.data.health.model.CanonicalStageMinutes
import com.hexis.bi.data.health.model.MAX_CANONICAL_SLEEP_SAMPLES
import com.hexis.bi.data.health.model.asLocalDateTime
import com.hexis.bi.data.health.model.toCanonicalTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

internal fun CanonicalDailyAggregate.toSleepSession(): SleepSession {
    require(recordType == "daily_sleep") { "Expected daily_sleep, got $recordType" }
    val bedtime = requireNotNull(ui.bedtime ?: period.start) { "Sleep aggregate has no bedtime" }.asLocalDateTime()
    val wakeTime = requireNotNull(ui.wakeTime ?: period.end) { "Sleep aggregate has no wake time" }.asLocalDateTime()
    return SleepSession(
        bedtime = bedtime,
        wakeTime = wakeTime,
        durationMinutes = ui.durationMinutes ?: ((metrics.sleepDurationSeconds ?: 0.0) / 60.0).roundToInt(),
        efficiencyPercent = (ui.efficiencyPercent ?: (metrics.sleepEfficiencyRatio ?: 0.0) * 100.0).toFloat(),
        restingHeartRateBpm = metrics.restingHeartRateBpm?.roundToInt() ?: 0,
        hrvMs = metrics.hrvRmssdMs?.roundToInt() ?: 0,
        sdnnMs = ui.sdnnMs?.roundToInt() ?: 0,
        stages = ui.timelineIntervals.mapNotNull { it.toDomainOrNull() },
        isNap = ui.isNap,
        heartRateSamples = ui.heartRateSamples.map { SleepSample(it.epochMillis, it.value.roundToInt()) },
        hrvSamples = ui.hrvSamples.map { SleepSample(it.epochMillis, it.value.roundToInt()) },
        sessionCount = quality.sessionCount.coerceAtLeast(1),
        aggregateStageTotals = com.hexis.bi.data.sleep.SleepStageTotals(
            deepMinutes = ui.stageMinutes.deep, lightMinutes = ui.stageMinutes.light,
            remMinutes = ui.stageMinutes.rem, awakeMinutes = ui.stageMinutes.awake,
        ),
    )
}

internal fun SleepSession.toCanonicalAggregate(day: LocalDate = wakeTime.toLocalDate()): CanonicalDailyAggregate =
    aggregateStageTotals.let { totals -> CanonicalDailyAggregate(
        schemaVersion = CANONICAL_HEALTH_AGGREGATE_VERSION,
        recordType = "daily_sleep",
        source = "sleep",
        day = day.toString(),
        period = AggregatePeriod(bedtime.toCanonicalTimestamp(), wakeTime.toCanonicalTimestamp()),
        provenance = AggregateProvenance(provider = "merged"),
        metrics = CanonicalMetrics(
            sleepDurationSeconds = durationMinutes * 60.0,
            sleepEfficiencyRatio = efficiencyPercent / 100.0,
            deepSleepSeconds = (totals?.deepMinutes ?: stages.stageSeconds(SleepStage.Deep).wholeMinutes()) * 60.0,
            remSleepSeconds = (totals?.remMinutes ?: stages.stageSeconds(SleepStage.REM).wholeMinutes()) * 60.0,
            restingHeartRateBpm = restingHeartRateBpm.toDouble(),
            hrvRmssdMs = hrvMs.toDouble(),
        ),
        ui = CanonicalDailyUi(
            bedtime = bedtime.toCanonicalTimestamp(), wakeTime = wakeTime.toCanonicalTimestamp(), durationMinutes = durationMinutes,
            efficiencyPercent = efficiencyPercent.toDouble(), sdnnMs = sdnnMs.toDouble(), isNap = isNap,
            stageMinutes = CanonicalStageMinutes(
                deep = totals?.deepMinutes ?: stages.stageSeconds(SleepStage.Deep).wholeMinutes(),
                light = totals?.lightMinutes ?: stages.stageSeconds(SleepStage.Light).wholeMinutes(),
                rem = totals?.remMinutes ?: stages.stageSeconds(SleepStage.REM).wholeMinutes(),
                awake = totals?.awakeMinutes ?: stages.stageSeconds(SleepStage.Awake).wholeMinutes(),
            ),
            timelineIntervals = stages.map {
                CanonicalStageInterval(
                    it.stage.name.lowercase(),
                    it.start.toCanonicalTimestamp(),
                    it.end.toCanonicalTimestamp(),
                )
            },
            heartRateSamples = heartRateSamples.toCanonicalSamples(bedtime, wakeTime),
            hrvSamples = hrvSamples.toCanonicalSamples(bedtime, wakeTime),
        ),
        quality = AggregateQuality(
            sessionCount = sessionCount, hasTimeline = stages.isNotEmpty(),
            hasHeartRateSamples = heartRateSamples.isNotEmpty(), hasHrvSamples = hrvSamples.isNotEmpty(),
        ),
    ) }

private fun List<SleepSample>.toCanonicalSamples(
    bedtime: LocalDateTime,
    wakeTime: LocalDateTime,
): List<CanonicalSample> {
    if (size <= MAX_CANONICAL_SLEEP_SAMPLES) {
        return map { CanonicalSample(it.epochMillis, it.value.toDouble()) }
    }
    val bedtimeMillis = bedtime.toSampleMillis()
    val spanMillis = (wakeTime.toSampleMillis() - bedtimeMillis).coerceAtLeast(1)
    return groupBy { sample ->
        val fraction = (sample.epochMillis - bedtimeMillis).toDouble() / spanMillis
        (fraction * MAX_CANONICAL_SLEEP_SAMPLES).toInt().coerceIn(0, MAX_CANONICAL_SLEEP_SAMPLES - 1)
    }.toSortedMap().values.map { bucket ->
        val sorted = bucket.sortedBy { it.epochMillis }
        CanonicalSample(
            epochMillis = sorted[sorted.size / 2].epochMillis,
            value = kotlin.math.round(sorted.map { it.value }.average() * 100.0) / 100.0,
        )
    }
}

private fun CanonicalStageInterval.toDomainOrNull(): SleepStageInterval? {
    val parsed = when (stage.lowercase()) {
        "deep" -> SleepStage.Deep
        "rem" -> SleepStage.REM
        "light" -> SleepStage.Light
        "awake", "wake" -> SleepStage.Awake
        else -> null
    } ?: return null
    return SleepStageInterval(parsed, start.asLocalDateTime(), end.asLocalDateTime())
}
