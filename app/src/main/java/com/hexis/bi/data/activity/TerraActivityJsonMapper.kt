package com.hexis.bi.data.activity

import com.hexis.bi.data.terra.float
import com.hexis.bi.data.terra.int
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

object TerraActivityJsonMapper {
    fun summaryOrNull(json: JsonElement, fallbackDate: LocalDate? = null): ActivitySummary? {
        val root = json.jsonObject
        val date = root.dateOrNull() ?: fallbackDate ?: return null
        val steps = root.extractSteps().coerceAtLeast(0)
        val distanceKm = (root.extractDistanceKm()).coerceAtLeast(0f)
        val calories = root.extractActiveCalories().coerceAtLeast(0)
        val hourlySteps = root.extractHourlySteps(date)
        return ActivitySummary(
            date = date,
            steps = steps,
            distanceKm = distanceKm,
            activeCalories = calories,
            hourlySteps = hourlySteps,
        )
    }
}

private fun JsonObject.dateOrNull(): LocalDate? {
    val metadata = this["metadata"]?.jsonObject
    val fromDateField = listOf(
        "date",
        "start_time",
        "end_time",
        "start_time_local",
        "end_time_local",
        "timestamp",
        "timestamp_local",
    )
        .firstNotNullOfOrNull { key -> metadata?.get(key)?.toString()?.trim('"')?.toLocalDateOrNull() }
    if (fromDateField != null) return fromDateField
    return listOf(
        "date",
        "start_time",
        "end_time",
        "start_time_local",
        "end_time_local",
        "timestamp",
        "timestamp_local",
    )
        .firstNotNullOfOrNull { key -> this[key]?.toString()?.trim('"')?.toLocalDateOrNull() }
}

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching {
        if (length == 10) LocalDate.parse(this)
        else OffsetDateTime.parse(this).toLocalDate()
    }.getOrNull()

private fun JsonObject.extractSteps(): Int {
    val distanceData = this["distance_data"]?.jsonObject
    val activityData = this["activity_data"]?.jsonObject
    val metadata = this["metadata"]?.jsonObject
    return int("steps")
        ?: distanceData?.int("steps")
        ?: distanceData?.get("summary")?.jsonObject?.int("steps")
        ?: activityData?.int("steps")
        ?: activityData?.get("summary")?.jsonObject?.int("steps")
        ?: int("step_count")
        ?: metadata?.int("step_count")
        ?: metadata?.int("steps")
        ?: 0
}

private fun JsonObject.extractDistanceKm(): Float {
    val distanceData = this["distance_data"]?.jsonObject
    val meters = distanceData?.float("distance_meters")
        ?: distanceData?.get("summary")?.jsonObject?.float("distance_meters")
        ?: distanceData?.float("distance_metres")
        ?: distanceData?.get("summary")?.jsonObject?.float("distance_metres")
    return if (meters != null) meters / 1000f else 0f
}

private fun JsonObject.extractActiveCalories(): Int {
    val caloriesData = this["calories_data"]?.jsonObject
    val activityData = this["activity_data"]?.jsonObject
    return caloriesData?.int("active_calories")
        ?: caloriesData?.int("net_activity_calories")
        ?: caloriesData?.get("summary")?.jsonObject?.int("active_calories")
        ?: activityData?.int("active_calories")
        ?: activityData?.int("active_energy_burned_calories")
        ?: 0
}

private fun JsonObject.extractHourlySteps(targetDate: LocalDate): Map<Int, Int> {
    val samples = mutableListOf<HourlyStepSample>()
    collectHourlyStepSamplesInto(samples)
    if (samples.isEmpty()) return emptyMap()
    val normalized = samples
        .filter { it.date == null || it.date == targetDate }
        .normalizedToIncrements()
    return normalized
        .groupBy { it.hour }
        .mapValues { (_, rows) -> rows.sumOf { it.steps }.coerceAtLeast(0) }
        .toSortedMap()
}

private fun JsonObject.collectHourlyStepSamplesInto(out: MutableList<HourlyStepSample>) {
    for ((_, value) in this) {
        when (value) {
            is JsonObject -> value.collectHourlyStepSamplesInto(out)
            else -> {
                val arr = value as? kotlinx.serialization.json.JsonArray ?: continue
                arr.forEach { item ->
                    val sample = item as? JsonObject ?: return@forEach
                    val hour = sample.sampleHourOrNull() ?: return@forEach
                    val steps = sample.sampleStepsOrNull() ?: return@forEach
                    val sampleTime = sample.sampleTime()
                    val sortTime = sampleTime?.let { it.toLocalDate().toEpochDay() * 24L + it.hour } ?: hour.toLong()
                    if (steps >= 0) {
                        out += HourlyStepSample(
                            hour = hour,
                            steps = steps,
                            sortTime = sortTime,
                            date = sampleTime?.toLocalDate(),
                        )
                    }
                }
            }
        }
    }
}

private fun JsonObject.sampleHourOrNull(): Int? =
    listOf(
        "timestamp",
        "timestamp_local",
        "start_time",
        "start_time_local",
    )
        .firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull()?.toLocalDateTimeOrNull()?.hour
        }

private fun JsonObject.sampleStepsOrNull(): Int? =
    listOf(
        "steps",
        "step_count",
    )
        .firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.intOrNull
                ?: this[key]?.jsonPrimitive?.floatOrNull?.toInt()
        }

private fun JsonObject.sampleTime(): LocalDateTime? =
    listOf(
        "timestamp",
        "timestamp_local",
        "start_time",
        "start_time_local",
    )
        .firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull()?.toLocalDateTimeOrNull()
        }

private fun JsonPrimitive.contentOrNull(): String? =
    if (isString) content else content.takeIf { it.isNotBlank() }

private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { OffsetDateTime.parse(this).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(this) }
        .getOrNull()

private data class HourlyStepSample(
    val hour: Int,
    val steps: Int,
    val sortTime: Long,
    val date: LocalDate? = null,
)

private fun List<HourlyStepSample>.normalizedToIncrements(): List<HourlyStepSample> {
    if (size < 2) return this
    val ordered = sortedBy { it.sortTime }
    val monotonic = ordered.zipWithNext().all { (a, b) -> b.steps >= a.steps }
    if (!monotonic) return ordered
    val deltas = buildList {
        add(HourlyStepSample(ordered.first().hour, ordered.first().steps.coerceAtLeast(0), ordered.first().sortTime))
        for (i in 1 until ordered.size) {
            val delta = (ordered[i].steps - ordered[i - 1].steps).coerceAtLeast(0)
            add(HourlyStepSample(ordered[i].hour, delta, ordered[i].sortTime))
        }
    }
    val rawTotal = ordered.sumOf { it.steps }
    val deltaTotal = deltas.sumOf { it.steps }
    // Use delta conversion only when raw looks cumulative (raw massively exceeds increment total).
    return if (rawTotal > deltaTotal * 2) deltas else ordered
}
