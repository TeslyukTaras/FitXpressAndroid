package com.hexis.bi.data.activity

import com.hexis.bi.data.terra.arrayOrNull
import com.hexis.bi.data.terra.objectOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.roundToInt

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
        const val OXYGEN_DATA = "oxygen_data"
        const val ACTIVE_DURATIONS_DATA = "active_durations_data"
        const val MET_DATA = "MET_data"
    }

    object Samples {
        const val ACTIVITY_LEVELS = "activity_levels_samples"
        const val ACTIVITY_LEVEL = "activity_level"
        const val MET = "MET_samples"
        const val MET_LEVEL = "level"
    }

    object Duration {
        const val ACTIVITY_SECONDS = "activity_seconds"
        const val ACTIVE_SECONDS = "active_seconds"
        const val ACTIVE_DURATION_SECONDS = "active_duration_seconds"
        const val DURATION_SECONDS = "duration_seconds"
        val CANDIDATES = listOf(
            ACTIVITY_SECONDS,
            ACTIVE_SECONDS,
            ACTIVE_DURATION_SECONDS,
            DURATION_SECONDS,
        )
    }

    object Vo2 {
        const val VO2MAX_ML_PER_MIN_PER_KG = "vo2max_ml_per_min_per_kg"
        const val VO2_MAX_ML_PER_MIN_PER_KG = "vo2_max_ml_per_min_per_kg"
        const val VO2MAX = "vo2max"
        const val VO2_MAX = "vo2_max"
        val CANDIDATES = listOf(VO2MAX_ML_PER_MIN_PER_KG, VO2_MAX_ML_PER_MIN_PER_KG, VO2MAX, VO2_MAX)
    }

    object Steps {
        const val STEPS = "steps"
        const val STEP_COUNT = "step_count"
    }

    object Distance {
        const val DISTANCE_METERS = "distance_meters"
        const val DISTANCE_METRES = "distance_metres"
        const val DISTANCE_KM = "distance_km"
        const val DISTANCE_KILOMETERS = "distance_kilometers"
        const val DISTANCE_KILOMETRES = "distance_kilometres"
        const val DISTANCE_MILES = "distance_miles"
        val METERS_CANDIDATES = listOf(DISTANCE_METERS, DISTANCE_METRES)
        val KM_CANDIDATES = listOf(DISTANCE_KM, DISTANCE_KILOMETERS, DISTANCE_KILOMETRES)
        val MILES_CANDIDATES = listOf(DISTANCE_MILES)
    }

    object Calories {
        const val ACTIVE_CALORIES = "active_calories"
        const val NET_ACTIVITY_CALORIES = "net_activity_calories"
        const val ACTIVE_ENERGY_BURNED_CALORIES = "active_energy_burned_calories"
        const val ACTIVITY_CALORIES = "activity_calories"
        const val TOTAL_BURNED_CALORIES = "total_burned_calories"
        const val TOTAL_CALORIES = "total_calories"
        const val BMR_CALORIES = "bmr_calories"
        val ACTIVE_CANDIDATES = listOf(
            ACTIVE_CALORIES,
            NET_ACTIVITY_CALORIES,
            ACTIVE_ENERGY_BURNED_CALORIES,
            ACTIVITY_CALORIES,
        )
        val TOTAL_CANDIDATES = listOf(TOTAL_BURNED_CALORIES, TOTAL_CALORIES)
        val BMR_CANDIDATES = listOf(BMR_CALORIES)
    }
}

object TerraActivityJsonMapper {
    fun summaryOrNull(json: JsonElement, fallbackDate: LocalDate? = null): ActivitySummary? {
        val root = json as? JsonObject ?: return null
        val date = root.dateOrNull() ?: fallbackDate ?: return null
        val steps = root.extractSteps().coerceAtLeast(0)
        val distanceKm = (root.extractDistanceKm()).coerceAtLeast(0f)
        val calories = root.extractActiveCalories().coerceAtLeast(0)
        val durationSeconds = root.extractActiveDurationSeconds().coerceAtLeast(0)
        val hourlySteps = root.extractHourlySteps(date)
        return ActivitySummary(
            date = date,
            steps = steps,
            distanceKm = distanceKm,
            activeCalories = calories,
            activeDurationSeconds = durationSeconds,
            hourlySteps = hourlySteps,
            vo2MaxMlPerMinPerKg = root.extractVo2Max(),
        )
    }
}

private fun JsonObject.extractVo2Max(): Float? {
    val oxygenData = objectOrNull(TerraActivityJsonKeys.Nodes.OXYGEN_DATA)
    return (oxygenData.firstFloatByKeysDeep(TerraActivityJsonKeys.Vo2.CANDIDATES)
        ?: this.firstFloatByKeysDeep(TerraActivityJsonKeys.Vo2.CANDIDATES))
        ?.takeIf { it > 0f }
}

private fun JsonObject.dateOrNull(): LocalDate? {
    val metadata = objectOrNull(TerraActivityJsonKeys.Common.METADATA)
    val fromDateField = TerraActivityJsonKeys.DateTime.CANDIDATES
        .firstNotNullOfOrNull { key ->
            metadata?.get(key)?.toString()?.trim('"')?.toLocalDateOrNull()
        }
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
    val distanceData = objectOrNull(TerraActivityJsonKeys.Nodes.DISTANCE_DATA)
    val activityData = objectOrNull(TerraActivityJsonKeys.Nodes.ACTIVITY_DATA)
    val metadata = objectOrNull(TerraActivityJsonKeys.Common.METADATA)
    return stepsValue(TerraActivityJsonKeys.Steps.STEPS)
        ?: distanceData?.stepsValue(TerraActivityJsonKeys.Steps.STEPS)
        ?: distanceData?.objectOrNull(TerraActivityJsonKeys.Common.SUMMARY)?.stepsValue(
            TerraActivityJsonKeys.Steps.STEPS
        )
        ?: activityData?.stepsValue(TerraActivityJsonKeys.Steps.STEPS)
        ?: activityData?.objectOrNull(TerraActivityJsonKeys.Common.SUMMARY)?.stepsValue(
            TerraActivityJsonKeys.Steps.STEPS
        )
        ?: stepsValue(TerraActivityJsonKeys.Steps.STEP_COUNT)
        ?: metadata?.stepsValue(TerraActivityJsonKeys.Steps.STEP_COUNT)
        ?: metadata?.stepsValue(TerraActivityJsonKeys.Steps.STEPS)
        ?: 0
}

/**
 * Lenient step reader: Oura reports `steps` as a JSON integer, but Health Connect / Google
 * normalize their aggregate as a float (e.g. `8421.0`). The strict [int] helper returns null for
 * a float token, which would silently zero out the day, so parse through [numberAsIntOrNull].
 */
private fun JsonObject.stepsValue(key: String): Int? =
    (this[key] as? JsonPrimitive)?.numberAsIntOrNull()

private fun JsonObject.extractDistanceKm(): Float {
    val distanceData = objectOrNull(TerraActivityJsonKeys.Nodes.DISTANCE_DATA)
    val activityData = objectOrNull(TerraActivityJsonKeys.Nodes.ACTIVITY_DATA)
    val meters = distanceData.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.METERS_CANDIDATES)
        ?: activityData.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.METERS_CANDIDATES)
        ?: this.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.METERS_CANDIDATES)
    if (meters != null) return (meters / 1000f).coerceAtLeast(0f)

    val km = distanceData.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.KM_CANDIDATES)
        ?: activityData.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.KM_CANDIDATES)
        ?: this.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.KM_CANDIDATES)
    if (km != null) return km.coerceAtLeast(0f)

    val miles = distanceData.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.MILES_CANDIDATES)
        ?: activityData.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.MILES_CANDIDATES)
        ?: this.firstFloatByKeysDeep(TerraActivityJsonKeys.Distance.MILES_CANDIDATES)
    if (miles != null) return (miles * 1.60934f).coerceAtLeast(0f)

    return 0f
}

private fun JsonObject.extractActiveDurationSeconds(): Int {
    val activeDurations = objectOrNull(TerraActivityJsonKeys.Nodes.ACTIVE_DURATIONS_DATA)
    val activityData = objectOrNull(TerraActivityJsonKeys.Nodes.ACTIVITY_DATA)
    val seconds = activeDurations.firstFloatByKeysDeep(TerraActivityJsonKeys.Duration.CANDIDATES)
        ?: activityData.firstFloatByKeysDeep(TerraActivityJsonKeys.Duration.CANDIDATES)
    return seconds?.coerceAtLeast(0f)?.roundToInt() ?: 0
}

private fun JsonObject.extractActiveCalories(): Int {
    val caloriesData = objectOrNull(TerraActivityJsonKeys.Nodes.CALORIES_DATA)
    val activityData = objectOrNull(TerraActivityJsonKeys.Nodes.ACTIVITY_DATA)
    val directActive =
        caloriesData.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.ACTIVE_CANDIDATES)
            ?: activityData.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.ACTIVE_CANDIDATES)
            ?: this.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.ACTIVE_CANDIDATES)
    if (directActive != null) return directActive.coerceAtLeast(0)

    val totalFloat =
        caloriesData.firstFloatByKeysDeep(TerraActivityJsonKeys.Calories.TOTAL_CANDIDATES)
            ?: activityData.firstFloatByKeysDeep(TerraActivityJsonKeys.Calories.TOTAL_CANDIDATES)
            ?: this.firstFloatByKeysDeep(TerraActivityJsonKeys.Calories.TOTAL_CANDIDATES)
    val bmrFloat = caloriesData.firstFloatByKeysDeep(TerraActivityJsonKeys.Calories.BMR_CANDIDATES)
        ?: activityData.firstFloatByKeysDeep(TerraActivityJsonKeys.Calories.BMR_CANDIDATES)
        ?: this.firstFloatByKeysDeep(TerraActivityJsonKeys.Calories.BMR_CANDIDATES)
    if (totalFloat != null && bmrFloat != null) {
        return (totalFloat - bmrFloat).coerceAtLeast(0f).roundToInt()
    }

    val totalInt = caloriesData.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.TOTAL_CANDIDATES)
        ?: activityData.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.TOTAL_CANDIDATES)
        ?: this.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.TOTAL_CANDIDATES)
    val bmrInt = caloriesData.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.BMR_CANDIDATES)
        ?: activityData.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.BMR_CANDIDATES)
        ?: this.firstIntByKeysDeep(TerraActivityJsonKeys.Calories.BMR_CANDIDATES)
    if (totalInt != null && bmrInt != null) return (totalInt - bmrInt).coerceAtLeast(0)
    return 0
}

private fun JsonObject?.firstIntByKeysDeep(keys: List<String>): Int? {
    this ?: return null
    val keySet = keys.map { it.lowercase() }.toSet()
    return walkDeep()
        .firstNotNullOfOrNull { (key, value) ->
            if (key.lowercase() !in keySet) null else (value as? JsonPrimitive)?.numberAsIntOrNull()
        }
}

private fun JsonObject?.firstFloatByKeysDeep(keys: List<String>): Float? {
    this ?: return null
    val keySet = keys.map { it.lowercase() }.toSet()
    return walkDeep()
        .firstNotNullOfOrNull { (key, value) ->
            if (key.lowercase() !in keySet) null else (value as? JsonPrimitive)?.numberAsFloatOrNull()
        }
}

private fun JsonObject.walkDeep(): Sequence<Pair<String, JsonElement>> = sequence {
    for ((key, value) in this@walkDeep) {
        yield(key to value)
        when (value) {
            is JsonObject -> yieldAll(value.walkDeep())
            is kotlinx.serialization.json.JsonArray -> {
                value.forEach { item ->
                    if (item is JsonObject) yieldAll(item.walkDeep())
                }
            }

            else -> Unit
        }
    }
}

private fun JsonObject.extractHourlySteps(targetDate: LocalDate): Map<Int, Int> {
    val samples = mutableListOf<HourlyStepSample>()
    collectHourlyStepSamplesInto(samples)
    val normalized = samples
        .filter { it.date == null || it.date == targetDate }
        .normalizedToIncrements()
    val fromStepSamples = normalized
        .groupBy { it.hour }
        .mapValues { (_, rows) -> rows.sumOf { it.steps }.coerceAtLeast(0) }
        .toSortedMap()
    if (fromStepSamples.isNotEmpty()) return fromStepSamples

    val totalSteps = extractSteps()
    if (totalSteps <= 0) return emptyMap()
    return estimateHourlyStepsFromActivitySamples(targetDate, totalSteps)
}

private fun JsonObject.collectHourlyStepSamplesInto(out: MutableList<HourlyStepSample>) {
    for ((_, value) in this) {
        when (value) {
            is JsonObject -> value.collectHourlyStepSamplesInto(out)
            else -> {
                val arr = value as? JsonArray ?: continue
                arr.forEach { item ->
                    val sample = item as? JsonObject ?: return@forEach
                    val hour = sample.sampleHourOrNull() ?: return@forEach
                    val steps = sample.sampleStepsOrNull() ?: return@forEach
                    val sampleTime = sample.sampleTime()
                    val sortTime = sampleTime?.let { it.toLocalDate().toEpochDay() * 24L + it.hour }
                        ?: hour.toLong()
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

private fun JsonObject.estimateHourlyStepsFromActivitySamples(
    targetDate: LocalDate,
    totalSteps: Int,
): Map<Int, Int> {
    val metSamples = objectOrNull(TerraActivityJsonKeys.Nodes.MET_DATA)
        ?.arrayOrNull(TerraActivityJsonKeys.Samples.MET)
        .orEmpty()
        .mapNotNull { (it as? JsonObject)?.toHourlyActivityWeight(targetDate, ActivitySampleSource.Met) }
        .filter { it.weight > 0f }

    val activityLevelSamples = objectOrNull(TerraActivityJsonKeys.Nodes.ACTIVE_DURATIONS_DATA)
        ?.arrayOrNull(TerraActivityJsonKeys.Samples.ACTIVITY_LEVELS)
        .orEmpty()
        .mapNotNull { (it as? JsonObject)?.toHourlyActivityWeight(targetDate, ActivitySampleSource.ActivityLevel) }
        .filter { it.weight > 0f }

    val weights = if (metSamples.isNotEmpty()) metSamples else activityLevelSamples
    if (weights.isEmpty()) return emptyMap()

    val weightByHour = weights
        .groupBy { it.hour }
        .mapValues { (_, rows) -> rows.sumOf { it.weight.toDouble() }.toFloat() }
        .filterValues { it > 0f }
    if (weightByHour.isEmpty()) return emptyMap()

    return weightByHour.scaleWeightsToSteps(totalSteps)
}

private fun JsonObject.toHourlyActivityWeight(
    targetDate: LocalDate,
    source: ActivitySampleSource,
): HourlyActivityWeight? {
    val sampleTime = sampleTime() ?: return null
    if (sampleTime.toLocalDate() != targetDate) return null
    val raw = when (source) {
        ActivitySampleSource.Met ->
            (this[TerraActivityJsonKeys.Samples.MET_LEVEL] as? JsonPrimitive)
                ?.numberAsFloatOrNull()
        ActivitySampleSource.ActivityLevel ->
            (this[TerraActivityJsonKeys.Samples.ACTIVITY_LEVEL] as? JsonPrimitive)
                ?.numberAsFloatOrNull()
    } ?: return null
    val weight = when (source) {
        // Oura reports resting/placeholder MET samples around 0.9, including future filler for today.
        ActivitySampleSource.Met -> raw - ActivitySampleConstants.RESTING_MET_BASELINE
        // Terra/Oura activity levels use 0/1 for no activity or rest; 2+ indicates movement.
        ActivitySampleSource.ActivityLevel -> raw - ActivitySampleConstants.RESTING_ACTIVITY_LEVEL
    }.coerceAtLeast(0f)
    return HourlyActivityWeight(hour = sampleTime.hour, weight = weight)
}

private fun Map<Int, Float>.scaleWeightsToSteps(totalSteps: Int): Map<Int, Int> {
    val totalWeight = values.sum()
    if (totalWeight <= 0f || totalSteps <= 0) return emptyMap()
    val scaled = entries.associate { (hour, weight) ->
        hour to ((weight.toDouble() * totalSteps.toDouble()) / totalWeight.toDouble()).toInt()
            .coerceAtLeast(0)
    }.toMutableMap()

    var diff = totalSteps - scaled.values.sum()
    val orderedHours = entries.sortedByDescending { it.value }.map { it.key }
    var idx = 0
    while (diff != 0 && orderedHours.isNotEmpty()) {
        val hour = orderedHours[idx % orderedHours.size]
        val current = scaled[hour] ?: 0
        if (diff > 0) {
            scaled[hour] = current + 1
            diff--
        } else if (current > 0) {
            scaled[hour] = current - 1
            diff++
        }
        idx++
    }
    return scaled.toSortedMap()
}

private fun JsonObject.sampleHourOrNull(): Int? =
    listOf(
        TerraActivityJsonKeys.DateTime.TIMESTAMP,
        TerraActivityJsonKeys.DateTime.TIMESTAMP_LOCAL,
        TerraActivityJsonKeys.DateTime.START_TIME,
        TerraActivityJsonKeys.DateTime.START_TIME_LOCAL,
    )
        .firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull()?.toLocalDateTimeOrNull()?.hour
        }

private fun JsonObject.sampleStepsOrNull(): Int? =
    listOf(
        TerraActivityJsonKeys.Steps.STEPS,
        TerraActivityJsonKeys.Steps.STEP_COUNT,
    )
        .firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.intOrNull
                ?: (this[key] as? JsonPrimitive)?.floatOrNull?.toInt()
        }

private fun JsonObject.sampleTime(): LocalDateTime? =
    listOf(
        TerraActivityJsonKeys.DateTime.TIMESTAMP,
        TerraActivityJsonKeys.DateTime.TIMESTAMP_LOCAL,
        TerraActivityJsonKeys.DateTime.START_TIME,
        TerraActivityJsonKeys.DateTime.START_TIME_LOCAL,
    )
        .firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull()?.toLocalDateTimeOrNull()
        }

private fun JsonPrimitive.contentOrNull(): String? =
    if (isString) content else content.takeIf { it.isNotBlank() }

private fun JsonPrimitive.numberAsIntOrNull(): Int? =
    intOrNull
        ?: floatOrNull?.toInt()
        ?: contentOrNull()?.toFloatOrNull()?.toInt()

private fun JsonPrimitive.numberAsFloatOrNull(): Float? =
    floatOrNull
        ?: intOrNull?.toFloat()
        ?: contentOrNull()?.toFloatOrNull()

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

private data class HourlyActivityWeight(
    val hour: Int,
    val weight: Float,
)

private enum class ActivitySampleSource {
    Met,
    ActivityLevel,
}

private object ActivitySampleConstants {
    const val RESTING_MET_BASELINE = 0.9f
    const val RESTING_ACTIVITY_LEVEL = 1f
}

private fun List<HourlyStepSample>.normalizedToIncrements(): List<HourlyStepSample> {
    if (size < 2) return this
    val ordered = sortedBy { it.sortTime }
    val monotonic = ordered.zipWithNext().all { (a, b) -> b.steps >= a.steps }
    if (!monotonic) return ordered
    val deltas = buildList {
        add(
            HourlyStepSample(
                ordered.first().hour,
                ordered.first().steps.coerceAtLeast(0),
                ordered.first().sortTime
            )
        )
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
