package com.hexis.bi.data.health.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CanonicalDailyAggregate(
    @SerialName("_aggregate_version") val schemaVersion: Int,
    @SerialName("record_type") val recordType: String,
    val source: String,
    val day: String,
    val period: AggregatePeriod = AggregatePeriod(),
    val provenance: AggregateProvenance = AggregateProvenance(),
    val metrics: CanonicalMetrics = CanonicalMetrics(),
    val ui: CanonicalDailyUi = CanonicalDailyUi(),
    val quality: AggregateQuality = AggregateQuality(),
)

@Serializable
internal data class CanonicalMetrics(
    val steps: Double? = null,
    @SerialName("distance_meters") val distanceMeters: Double? = null,
    @SerialName("active_duration_seconds") val activeDurationSeconds: Double? = null,
    @SerialName("active_calories_kcal") val activeCaloriesKcal: Double? = null,
    @SerialName("vo2max_ml_per_min_per_kg") val vo2MaxMlPerMinPerKg: Double? = null,
    @SerialName("sleep_duration_seconds") val sleepDurationSeconds: Double? = null,
    @SerialName("sleep_efficiency_ratio") val sleepEfficiencyRatio: Double? = null,
    @SerialName("deep_sleep_seconds") val deepSleepSeconds: Double? = null,
    @SerialName("rem_sleep_seconds") val remSleepSeconds: Double? = null,
    @SerialName("resting_hr_bpm") val restingHeartRateBpm: Double? = null,
    @SerialName("hrv_rmssd_ms") val hrvRmssdMs: Double? = null,
)

@Serializable
internal data class CanonicalDailyUi(
    @SerialName("hourly_steps") val hourlySteps: Map<String, Int> = emptyMap(),
    val bedtime: String? = null,
    @SerialName("wake_time") val wakeTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("efficiency_percent") val efficiencyPercent: Double? = null,
    @SerialName("sdnn_ms") val sdnnMs: Double? = null,
    @SerialName("is_nap") val isNap: Boolean = false,
    @SerialName("stage_minutes") val stageMinutes: CanonicalStageMinutes = CanonicalStageMinutes(),
    @SerialName("timeline_intervals") val timelineIntervals: List<CanonicalStageInterval> = emptyList(),
    @SerialName("heart_rate_samples") val heartRateSamples: List<CanonicalSample> = emptyList(),
    @SerialName("hrv_samples") val hrvSamples: List<CanonicalSample> = emptyList(),
)

@Serializable
internal data class CanonicalDaySamples(
    @SerialName("timeline_intervals") val timelineIntervals: List<CanonicalStageInterval> = emptyList(),
    @SerialName("heart_rate_samples") val heartRateSamples: List<CanonicalSample> = emptyList(),
    @SerialName("hrv_samples") val hrvSamples: List<CanonicalSample> = emptyList(),
) {
    val isEmpty: Boolean
        get() = timelineIntervals.isEmpty() && heartRateSamples.isEmpty() && hrvSamples.isEmpty()
}

internal fun CanonicalDailyAggregate.detachSamples(): Pair<CanonicalDailyAggregate, CanonicalDaySamples> {
    val samples = CanonicalDaySamples(ui.timelineIntervals, ui.heartRateSamples, ui.hrvSamples)
    if (samples.isEmpty) return this to samples
    return copy(
        ui = ui.copy(
            timelineIntervals = emptyList(),
            heartRateSamples = emptyList(),
            hrvSamples = emptyList(),
        ),
    ) to samples
}

internal fun CanonicalDailyAggregate.attachSamples(samples: CanonicalDaySamples): CanonicalDailyAggregate =
    if (samples.isEmpty) this else copy(
        ui = ui.copy(
            timelineIntervals = samples.timelineIntervals,
            heartRateSamples = samples.heartRateSamples,
            hrvSamples = samples.hrvSamples,
        ),
    )

@Serializable
internal data class CanonicalStageMinutes(
    val deep: Int = 0,
    val light: Int = 0,
    val rem: Int = 0,
    val awake: Int = 0,
)

@Serializable
internal data class CanonicalStageInterval(
    val stage: String,
    val start: String,
    val end: String,
)

@Serializable
internal data class CanonicalSample(
    val timestamp: String,
    val value: Double,
)

@Serializable
internal data class AggregatePeriod(val start: String? = null, val end: String? = null)

@Serializable
internal data class AggregateProvenance(
    @SerialName("connection_id") val connectionId: String? = null,
    val provider: String? = null,
    @SerialName("summary_id") val summaryId: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    val manufacturer: String? = null,
    @SerialName("upload_type") val uploadType: Int? = null,
)

@Serializable
internal data class AggregateQuality(
    @SerialName("confirmed_empty") val confirmedEmpty: Boolean = false,
    @SerialName("session_count") val sessionCount: Int = 0,
    @SerialName("has_steps") val hasSteps: Boolean = false,
    @SerialName("has_hourly_steps") val hasHourlySteps: Boolean = false,
    @SerialName("has_timeline") val hasTimeline: Boolean = false,
    @SerialName("has_hr_samples") val hasHeartRateSamples: Boolean = false,
    @SerialName("has_hrv_samples") val hasHrvSamples: Boolean = false,
)

/** Full numeric scan facts are compact, so no measurement component is discarded. */
@Serializable
internal data class CanonicalBodyScanAggregate(
    @SerialName("document_id") val documentId: String,
    @SerialName("measurement_id") val measurementId: String? = null,
    @SerialName("completed_at") val completedAt: String,
    @SerialName("saved_at") val savedAt: String,
    @SerialName("model_3d_url") val model3dUrl: String? = null,
    @SerialName("height") val heightCm: Double? = null,
    @SerialName("weight") val weightKg: Double? = null,
    @SerialName("estimated_weight") val estimatedWeightKg: Double? = null,
    val bmi: Double? = null,
    @SerialName("fat_percentage") val fatPercentage: Double? = null,
    @SerialName("lean_body_mass") val leanBodyMassKg: Double? = null,
    @SerialName("fat_body_mass") val fatBodyMassKg: Double? = null,
    @SerialName("circumference_params") val circumferenceParamsCm: Map<String, Double> = emptyMap(),
    @SerialName("front_linear_params") val frontLinearParamsCm: Map<String, Double> = emptyMap(),
    @SerialName("side_linear_params") val sideLinearParamsCm: Map<String, Double> = emptyMap(),
)

/** Shared by the per-domain mappers; Terra timestamps arrive with or without an offset. */
internal fun String.asInstant(): Instant = runCatching { Instant.parse(this) }
    .getOrElse { OffsetDateTime.parse(this).toInstant() }

internal fun String.asLocalDateTime(): LocalDateTime =
    runCatching { OffsetDateTime.parse(this).toLocalDateTime() }
        .getOrElse { LocalDateTime.parse(this) }

internal fun LocalDateTime.toCanonicalTimestamp(): String =
    format(if (nano == 0) CANONICAL_SECONDS_FORMAT else CANONICAL_MICROS_FORMAT)

private val CANONICAL_SECONDS_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

private val CANONICAL_MICROS_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

internal const val CANONICAL_HEALTH_AGGREGATE_VERSION = 4
internal const val METERS_PER_KILOMETER = 1_000.0
internal const val MAX_CANONICAL_SLEEP_SAMPLES = 96
