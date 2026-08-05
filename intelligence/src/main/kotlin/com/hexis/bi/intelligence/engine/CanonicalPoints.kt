package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.model.MetricPoint

data class CanonicalDayRecord(
    val day: String,
    val metrics: Map<String, Double>,
)

data class CanonicalScanRecord(
    val documentId: String,
    val completedAt: String,
    val savedAt: String,
    val weightKg: Double? = null,
    val estimatedWeightKg: Double? = null,
    val fatPercentage: Double? = null,
    val leanBodyMassKg: Double? = null,
    val estimatedLeanBodyMassKg: Double? = null,
    val circumferenceParamsCm: Map<String, Double> = emptyMap(),
)

private const val SECONDS_PER_MINUTE = 1.0 / 60.0
private const val SECONDS_PER_HOUR = 1.0 / 3600.0
private const val DAY_LENGTH = 10

private val DAILY_MAPPING = mapOf(
    "steps" to (Metrics.STEPS to 1.0),
    "distance_meters" to (Metrics.DISTANCE to 1.0),
    "active_duration_seconds" to (Metrics.ACTIVE_MINUTES to SECONDS_PER_MINUTE),
    "active_calories_kcal" to (Metrics.ACTIVE_CALORIES to 1.0),
    "vo2max_ml_per_min_per_kg" to (Metrics.VO2MAX to 1.0),
)

private val SLEEP_MAPPING = mapOf(
    "sleep_duration_seconds" to (Metrics.SLEEP_DURATION to SECONDS_PER_HOUR),
    "sleep_efficiency_ratio" to (Metrics.SLEEP_EFFICIENCY to 1.0),
    "deep_sleep_seconds" to (Metrics.DEEP_SLEEP to SECONDS_PER_HOUR),
    "rem_sleep_seconds" to (Metrics.REM_SLEEP to SECONDS_PER_HOUR),
    "resting_hr_bpm" to (Metrics.RESTING_HR to 1.0),
    "hrv_rmssd_ms" to (Metrics.HRV_RMSSD to 1.0),
)

fun normalizeCanonical(
    daily: List<CanonicalDayRecord>,
    sleep: List<CanonicalDayRecord>,
    scans: List<CanonicalScanRecord>,
): List<MetricPoint> =
    normalizeDays(daily, DAILY_MAPPING, Metrics.SOURCE_DAILY) +
        normalizeDays(sleep, SLEEP_MAPPING, Metrics.SOURCE_SLEEP) +
        normalizeScans(scans)

private fun normalizeDays(
    records: List<CanonicalDayRecord>,
    mapping: Map<String, Pair<String, Double>>,
    source: String,
): List<MetricPoint> = records.flatMap { record ->
    record.metrics.map { (key, value) ->
        val (metric, scale) = mapping[key] ?: (key to 1.0)
        MetricPoint(record.day, metric, value * scale, source)
    }
}

private fun normalizeScans(scans: List<CanonicalScanRecord>): List<MetricPoint> =
    scans.sortedBy { "${it.savedAt.ifEmpty { it.completedAt }}|${it.documentId}" }
        .flatMap { scan ->
            val day = scan.completedAt.take(DAY_LENGTH)
            if (day.isEmpty()) return@flatMap emptyList()
            buildList {
                point(day, Metrics.WEIGHT, scan.weightKg.orIfBlank(scan.estimatedWeightKg))
                point(day, Metrics.BODY_FAT_PCT, scan.fatPercentage)
                point(day, Metrics.LEAN_MASS, scan.leanBodyMassKg.orIfBlank(scan.estimatedLeanBodyMassKg))
                point(day, Metrics.WAIST, scan.circumferenceParamsCm[Metrics.WAIST])
            }
        }

private fun MutableList<MetricPoint>.point(day: String, metric: String, value: Double?) {
    if (value != null) add(MetricPoint(day, metric, value, Metrics.SOURCE_SCAN))
}

private fun Double?.orIfBlank(fallback: Double?): Double? =
    if (this == null || this == 0.0) fallback else this
