package com.hexis.bi.data.recovery

import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.activity.ActivitySummary
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.data.terra.TtlCache
import com.hexis.bi.utils.constants.RecoveryConstants
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.constants.TerraCacheConstants
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Recovery isn't its own Terra endpoint — we derive it from sleep + activity data
 * already fetched via [SleepRepository] and [ActivityRepository].
 */
class TerraDerivedRecoveryRepository(
    private val sleepRepository: SleepRepository,
    private val activityRepository: ActivityRepository,
) : RecoveryRepository {

    private val rangeCache = TtlCache<Pair<LocalDate, LocalDate>, List<RecoverySnapshot>>(
        ttlMs = TerraCacheConstants.RANGE_CACHE_TTL_MS,
    )

    override suspend fun getSnapshotForDate(date: LocalDate): Result<RecoverySnapshot?> =
        getSnapshotsForRange(date, date).map {
            it.firstOrNull { snap -> snap.date == date } ?: it.lastOrNull()
        }

    override suspend fun getSnapshotsForRange(
        start: LocalDate,
        end: LocalDate,
    ): Result<List<RecoverySnapshot>> {
        val key = start to end
        rangeCache.get(key)?.let { return Result.success(it) }

        val sleepRange = sleepRepository.getSessionsForRange(start.minusDays(1), end)
            .getOrElse { return Result.failure(it) }
        val activityRange = activityRepository.getSummariesForRange(start, end)
            .getOrElse { return Result.failure(it) }

        val sleepByWakeDay: Map<LocalDate, SleepSession> = sleepRange
            .groupBy { it.wakeTime.toLocalDate() }
            .mapValues { (_, sessions) -> sessions.maxByOrNull { it.durationMinutes }!! }
        val activityByDate: Map<LocalDate, ActivitySummary> = activityRange.associateBy { it.date }

        val days =
            generateSequence(start) { d -> if (d.isBefore(end)) d.plusDays(1) else null }.toList()
        val snapshots = days.mapNotNull { day ->
            val sleep = sleepByWakeDay[day]
            val activity = activityByDate[day]
            if (sleep == null && activity == null) return@mapNotNull null
            buildSnapshot(day, sleep, activity)
        }
        rangeCache.put(key, snapshots)
        return Result.success(snapshots)
    }

    private fun buildSnapshot(
        date: LocalDate,
        sleep: SleepSession?,
        activity: ActivitySummary?,
    ): RecoverySnapshot {
        val hasSleep = sleep != null && sleep.durationMinutes > 0
        val hasHrv = (sleep?.hrvMs ?: 0) > 0
        val hasRhr = (sleep?.restingHeartRateBpm ?: 0) > 0
        val hasActivity = activity != null && activity.activeCalories > 0

        val sleepScore = if (hasSleep)
            computeSleepScore(sleep.durationMinutes, sleep.efficiencyPercent) else 0
        val hrv = sleep?.hrvMs ?: 0
        val rhr = sleep?.restingHeartRateBpm ?: 0
        val calories = activity?.activeCalories ?: 0

        // HEX recovery formula: HRV + RHR + Sleep. Renormalize across components actually present
        // so a missing wearable signal doesn't artificially deflate the score.
        var weightedSum = 0f
        var totalWeight = 0f
        if (hasHrv) {
            weightedSum += RecoveryConstants.SCORE_HRV_WEIGHT *
                    mapTo0To100(
                        hrv.toFloat(),
                        RecoveryConstants.HRV_LOW_MS,
                        RecoveryConstants.HRV_HIGH_MS
                    )
            totalWeight += RecoveryConstants.SCORE_HRV_WEIGHT
        }
        if (hasRhr) {
            weightedSum += RecoveryConstants.SCORE_RHR_WEIGHT *
                    mapTo0To100Inverse(
                        rhr.toFloat(),
                        RecoveryConstants.RHR_LOW_BPM,
                        RecoveryConstants.RHR_HIGH_BPM
                    )
            totalWeight += RecoveryConstants.SCORE_RHR_WEIGHT
        }
        if (hasSleep) {
            weightedSum += RecoveryConstants.SCORE_SLEEP_WEIGHT * sleepScore
            totalWeight += RecoveryConstants.SCORE_SLEEP_WEIGHT
        }
        val recoveryScore = if (totalWeight > 0f)
            (weightedSum / totalWeight).roundToInt().coerceIn(0, 100) else 0

        return RecoverySnapshot(
            date = date,
            score = recoveryScore,
            sleepScore = sleepScore,
            hrvMs = hrv,
            restingHeartRateBpm = rhr,
            activeCalories = calories,
            stressLevel = if (hasHrv) stressFor(hrv) else null,
            activityLoad = if (hasActivity) loadFor(calories) else null,
        )
    }

    private fun computeSleepScore(durationMinutes: Int, efficiencyPercent: Float): Int {
        val hours = durationMinutes / 60f
        val durationScore = mapTo0To100(
            hours,
            SleepConstants.SCORE_DURATION_LOW_HOURS,
            SleepConstants.SCORE_DURATION_HIGH_HOURS,
        )
        val efficiencyScore = mapTo0To100(
            efficiencyPercent,
            SleepConstants.SCORE_EFFICIENCY_LOW_PCT,
            SleepConstants.SCORE_EFFICIENCY_HIGH_PCT,
        )
        return (
                SleepConstants.SCORE_DURATION_WEIGHT * durationScore +
                        SleepConstants.SCORE_EFFICIENCY_WEIGHT * efficiencyScore
                ).roundToInt().coerceIn(0, 100)
    }

    private fun mapTo0To100(value: Float, low: Float, high: Float): Float {
        if (high <= low) return 0f
        return ((value - low) / (high - low)).coerceIn(0f, 1f) * 100f
    }

    private fun mapTo0To100Inverse(value: Float, low: Float, high: Float): Float {
        if (value <= 0f) return 0f
        if (high <= low) return 0f
        return (1f - ((value - low) / (high - low)).coerceIn(0f, 1f)) * 100f
    }

    private fun stressFor(hrvMs: Int): StressLevel = when {
        hrvMs >= RecoveryConstants.STRESS_LOW_HRV_MIN -> StressLevel.Low
        hrvMs <= RecoveryConstants.STRESS_HIGH_HRV_MAX -> StressLevel.High
        else -> StressLevel.Medium
    }

    private fun loadFor(calories: Int): ActivityLoadLevel = when {
        calories <= RecoveryConstants.ACTIVITY_LOAD_LIGHT_MAX -> ActivityLoadLevel.Light
        calories >= RecoveryConstants.ACTIVITY_LOAD_HEAVY_MIN -> ActivityLoadLevel.Heavy
        else -> ActivityLoadLevel.Moderate
    }
}
