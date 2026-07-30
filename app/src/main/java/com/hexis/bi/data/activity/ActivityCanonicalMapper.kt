package com.hexis.bi.data.activity

import com.hexis.bi.data.health.model.AggregateProvenance
import com.hexis.bi.data.health.model.AggregateQuality
import com.hexis.bi.data.health.model.CANONICAL_HEALTH_AGGREGATE_VERSION
import com.hexis.bi.data.health.model.CanonicalDailyAggregate
import com.hexis.bi.data.health.model.CanonicalDailyUi
import com.hexis.bi.data.health.model.CanonicalMetrics
import com.hexis.bi.data.health.model.METERS_PER_KILOMETER
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun CanonicalDailyAggregate.toActivitySummary(): ActivitySummary {
    require(recordType == "daily_activity") { "Expected daily_activity, got $recordType" }
    return ActivitySummary(
        date = LocalDate.parse(day),
        steps = metrics.steps?.roundToInt() ?: 0,
        distanceKm = ((metrics.distanceMeters ?: 0.0) / METERS_PER_KILOMETER).toFloat(),
        activeCalories = metrics.activeCaloriesKcal?.roundToInt() ?: 0,
        activeDurationSeconds = metrics.activeDurationSeconds?.roundToInt() ?: 0,
        hourlySteps = ui.hourlySteps.mapNotNull { (hour, steps) -> hour.toIntOrNull()?.let { it to steps } }.toMap(),
        vo2MaxMlPerMinPerKg = metrics.vo2MaxMlPerMinPerKg?.toFloat(),
    )
}

internal fun ActivitySummary.toCanonicalAggregate(): CanonicalDailyAggregate = CanonicalDailyAggregate(
    schemaVersion = CANONICAL_HEALTH_AGGREGATE_VERSION,
    recordType = "daily_activity",
    source = "daily",
    day = date.toString(),
    provenance = AggregateProvenance(provider = "merged"),
    metrics = CanonicalMetrics(
        steps = steps.toDouble(),
        distanceMeters = distanceKm * METERS_PER_KILOMETER,
        activeDurationSeconds = activeDurationSeconds.toDouble(),
        activeCaloriesKcal = activeCalories.toDouble(),
        vo2MaxMlPerMinPerKg = vo2MaxMlPerMinPerKg?.toDouble(),
    ),
    ui = CanonicalDailyUi(hourlySteps = hourlySteps.mapKeys { it.key.toString() }),
    quality = AggregateQuality(hasSteps = steps > 0, hasHourlySteps = hourlySteps.isNotEmpty()),
)
