package com.hexis.bi.data.intelligence

import com.hexis.bi.data.health.model.CanonicalBodyScanAggregate
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.model.CanonicalMetrics
import com.hexis.bi.data.health.model.asInstant
import com.hexis.bi.intelligence.engine.CanonicalDayRecord
import com.hexis.bi.intelligence.engine.CanonicalScanRecord
import com.hexis.bi.intelligence.engine.normalizeCanonical
import com.hexis.bi.intelligence.model.EngineInput
import java.time.LocalDate

internal object CanonicalEngineInputAdapter {

    fun toEngineInput(
        daily: List<CanonicalDailyAggregate>,
        sleep: List<CanonicalDailyAggregate>,
        scans: List<CanonicalBodyScanAggregate>,
        window: ClosedRange<LocalDate>,
        runDate: LocalDate,
        analysisDays: Int,
        heightCm: Double?,
    ): EngineInput = EngineInput(
        runDate = runDate.toString(),
        pullDays = analysisDays,
        points = normalizeCanonical(
            daily = daily.map { it.toDayRecord() },
            sleep = sleep.map { it.toDayRecord() },
            scans = scans.inWindow(window).mapNotNull { it.toScanRecordOrNull() },
        ),
        heightCm = heightCm,
    )

    fun mergeByDay(perIdentity: List<List<CanonicalDailyAggregate>>): List<CanonicalDailyAggregate> {
        val byDay = LinkedHashMap<String, CanonicalDailyAggregate>()
        for (rows in perIdentity) {
            for (row in rows) byDay.putIfAbsent(row.day, row)
        }
        return byDay.values.sortedBy { it.day }
    }
}

/**
 * The scan snapshot holds a user's whole history, but the engine picks its primary window from the
 * longest calendar span across all series. Feeding it years of scans alongside a two-month wearable
 * window selects a window no wearable series can satisfy, and every daily metric comes back
 * `insufficient_history`. The reference filters scans the same way — `cache.load_scans(uid, start,
 * end)` — so every series covers the same span.
 */
private fun List<CanonicalBodyScanAggregate>.inWindow(
    window: ClosedRange<LocalDate>,
): List<CanonicalBodyScanAggregate> {
    val start = window.start.toString()
    val end = window.endInclusive.toString()
    return filter { it.completedAt.take(ISO_DAY_LENGTH) in start..end }
}

private fun CanonicalDailyAggregate.toDayRecord() = CanonicalDayRecord(day, metrics.asOrderedMap())

private fun CanonicalMetrics.asOrderedMap(): Map<String, Double> = buildMap {
    put("steps", steps)
    put("distance_meters", distanceMeters)
    put("active_duration_seconds", activeDurationSeconds)
    put("active_calories_kcal", activeCaloriesKcal)
    put("vo2max_ml_per_min_per_kg", vo2MaxMlPerMinPerKg)
    put("sleep_duration_seconds", sleepDurationSeconds)
    put("sleep_efficiency_ratio", sleepEfficiencyRatio)
    put("deep_sleep_seconds", deepSleepSeconds)
    put("rem_sleep_seconds", remSleepSeconds)
    put("resting_hr_bpm", restingHeartRateBpm)
    put("hrv_rmssd_ms", hrvRmssdMs)
}

private fun MutableMap<String, Double>.put(key: String, value: Double?) {
    if (value != null && value.isFinite()) put(key, value)
}

private fun CanonicalBodyScanAggregate.toScanRecordOrNull(): CanonicalScanRecord? {
    val savedAtMillis = runCatching { savedAt.asInstant().toEpochMilli() }.getOrNull() ?: return null
    if (savedAtMillis <= 0L) return null
    if (completedAt.length < ISO_DAY_LENGTH) return null
    return CanonicalScanRecord(
        documentId = documentId,
        completedAt = completedAt,
        savedAt = savedAt,
        weightKg = weightKg.finiteOrNull(),
        estimatedWeightKg = estimatedWeightKg.finiteOrNull(),
        fatPercentage = fatPercentage.finiteOrNull(),
        leanBodyMassKg = leanBodyMassKg.finiteOrNull(),
        estimatedLeanBodyMassKg = null,
        circumferenceParamsCm = circumferenceParamsCm.filterValues { it.isFinite() },
    )
}

private fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }

private const val ISO_DAY_LENGTH = 10
