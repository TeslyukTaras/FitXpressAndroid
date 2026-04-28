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

private object TerraActivityJsonKeys {
    object Common {
        const val METADATA = "metadata"
        const val SUMMARY = "summary"
    }

    object DateTime {
        const val DATE = "date"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val START_TIME_LOCAL = "start_time_local"
        const val END_TIME_LOCAL = "end_time_local"
        const val TIMESTAMP = "timestamp"
        const val TIMESTAMP_LOCAL = "timestamp_local"
        val CANDIDATES = listOf(
            DATE,
            START_TIME,
            END_TIME,
            START_TIME_LOCAL,
            END_TIME_LOCAL,
            TIMESTAMP,
            TIMESTAMP_LOCAL,
        )
    }

    object Nodes {
        const val DISTANCE_DATA = "distance_data"
        const val ACTIVITY_DATA = "activity_data"
        const val CALORIES_DATA = "calories_data"
    }

    object Steps {
        const val STEPS = "steps"
        const val STEP_COUNT = "step_count"
    }

    object Distance {
        const val DISTANCE_METERS = "distance_meters"
        const val DISTANCE_METRES = "distance_metres"
    }

    object Calories {
        const val ACTIVE_CALORIES = "active_calories"
        const val NET_ACTIVITY_CALORIES = "net_activity_calories"
        const val ACTIVE_ENERGY_BURNED_CALORIES = "active_energy_burned_calories"
    }
}

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
    val metadata = this[TerraActivityJsonKeys.Common.METADATA]?.jsonObject
    val fromDateField = TerraActivityJsonKeys.DateTime.CANDIDATES
        .firstNotNullOfOrNull { key -> metadata?.get(key)?.toString()?.trim('"')?.toLocalDateOrNull() }
    if (fromDateField != null) return fromDateField
    return TerraActivityJsonKeys.DateTime.CANDIDATES
        .firstNotNullOfOrNull { key -> this[key]?.toString()?.trim('"')?.toLocalDateOrNull() }
}

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching {
        if (length == 10) LocalDate.parse(this)
        else OffsetDateTime.parse(this).toLocalDate()
    }.getOrNull()

private fun JsonObject.extractSteps(): Int {
    val distanceData = this[TerraActivityJsonKeys.Nodes.DISTANCE_DATA]?.jsonObject
    val activityData = this[TerraActivityJsonKeys.Nodes.ACTIVITY_DATA]?.jsonObject
    val metadata = this[TerraActivityJsonKeys.Common.METADATA]?.jsonObject
    return int(TerraActivityJsonKeys.Steps.STEPS)
        ?: distanceData?.int(TerraActivityJsonKeys.Steps.STEPS)
        ?: distanceData?.get(TerraActivityJsonKeys.Common.SUMMARY)?.jsonObject?.int(TerraActivityJsonKeys.Steps.STEPS)
        ?: activityData?.int(TerraActivityJsonKeys.Steps.STEPS)
        ?: activityData?.get(TerraActivityJsonKeys.Common.SUMMARY)?.jsonObject?.int(TerraActivityJsonKeys.Steps.STEPS)
        ?: int(TerraActivityJsonKeys.Steps.STEP_COUNT)
        ?: metadata?.int(TerraActivityJsonKeys.Steps.STEP_COUNT)
        ?: metadata?.int(TerraActivityJsonKeys.Steps.STEPS)
        ?: 0
}

private fun JsonObject.extractDistanceKm(): Float {
    val distanceData = this[TerraActivityJsonKeys.Nodes.DISTANCE_DATA]?.jsonObject
    val meters = distanceData?.float(TerraActivityJsonKeys.Distance.DISTANCE_METERS)
        ?: distanceData?.get(TerraActivityJsonKeys.Common.SUMMARY)?.jsonObject?.float(TerraActivityJsonKeys.Distance.DISTANCE_METERS)
        ?: distanceData?.float(TerraActivityJsonKeys.Distance.DISTANCE_METRES)
        ?: distanceData?.get(TerraActivityJsonKeys.Common.SUMMARY)?.jsonObject?.float(TerraActivityJsonKeys.Distance.DISTANCE_METRES)
    return if (meters != null) meters / 1000f else 0f
}

private fun JsonObject.extractActiveCalories(): Int {
    val caloriesData = this[TerraActivityJsonKeys.Nodes.CALORIES_DATA]?.jsonObject
    val activityData = this[TerraActivityJsonKeys.Nodes.ACTIVITY_DATA]?.jsonObject
    return caloriesData?.int(TerraActivityJsonKeys.Calories.ACTIVE_CALORIES)
        ?: caloriesData?.int(TerraActivityJsonKeys.Calories.NET_ACTIVITY_CALORIES)
        ?: caloriesData?.get(TerraActivityJsonKeys.Common.SUMMARY)?.jsonObject?.int(TerraActivityJsonKeys.Calories.ACTIVE_CALORIES)
        ?: activityData?.int(TerraActivityJsonKeys.Calories.ACTIVE_CALORIES)
        ?: activityData?.int(TerraActivityJsonKeys.Calories.ACTIVE_ENERGY_BURNED_CALORIES)
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
        TerraActivityJsonKeys.DateTime.TIMESTAMP,
        TerraActivityJsonKeys.DateTime.TIMESTAMP_LOCAL,
        TerraActivityJsonKeys.DateTime.START_TIME,
        TerraActivityJsonKeys.DateTime.START_TIME_LOCAL,
    )
        .firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.contentOrNull()?.toLocalDateTimeOrNull()?.hour
        }

private fun JsonObject.sampleStepsOrNull(): Int? =
    listOf(
        TerraActivityJsonKeys.Steps.STEPS,
        TerraActivityJsonKeys.Steps.STEP_COUNT,
    )
        .firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitive?.intOrNull
                ?: this[key]?.jsonPrimitive?.floatOrNull?.toInt()
        }

private fun JsonObject.sampleTime(): LocalDateTime? =
    listOf(
        TerraActivityJsonKeys.DateTime.TIMESTAMP,
        TerraActivityJsonKeys.DateTime.TIMESTAMP_LOCAL,
        TerraActivityJsonKeys.DateTime.START_TIME,
        TerraActivityJsonKeys.DateTime.START_TIME_LOCAL,
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
