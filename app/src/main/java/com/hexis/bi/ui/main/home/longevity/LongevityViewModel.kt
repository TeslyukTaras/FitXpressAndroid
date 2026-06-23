package com.hexis.bi.ui.main.home.longevity

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.activity.ActivitySummary
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.recovery.RecoverySnapshot
import com.hexis.bi.data.recovery.StressLevel
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.data.terra.TerraDetail
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.domain.body.comparablePhysiqueScoreDelta
import com.hexis.bi.domain.longevity.LongevityInputs
import com.hexis.bi.domain.longevity.computeLongevityScore
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.LongevityConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.hexis.bi.utils.formatShortDateRange
import com.hexis.bi.utils.formatShortMonthDay
import com.hexis.bi.utils.formatShortMonthDayYear
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Loads the Longevity screen from the app's existing health data. The healthy-aging score combines
 * recovery (HRV, RHR, sleep), activity (steps), the latest body scan (body fat and waist-to-height)
 * and VO2 max via [computeLongevityScore]. The Daily tab plots today's intraday score up to the
 * current hour; the Weekly tab plots one score per day for the last 7 days. Missing signals render
 * as "—".
 */
class LongevityViewModel(
    application: Application,
    private val recoveryRepository: RecoveryRepository,
    private val sleepRepository: SleepRepository,
    private val activityRepository: ActivityRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
    private val terraManagerHolder: TerraManagerHolder,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(LongevityState())
    val state: StateFlow<LongevityState> = _state.asStateFlow()

    init {
        load()
    }

    fun selectTab(tab: LongevityTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun showInfoSheet() {
        _state.update { it.copy(showInfoSheet = true) }
    }

    fun dismissInfoSheet() {
        _state.update { it.copy(showInfoSheet = false) }
    }

    private fun load() {
        setLoading(true)
        viewModelScope.launch {
            // Paint from whatever Terra already has first, so the screen doesn't wait on a sync.
            renderFromRepositories()
            setLoading(false)

            // Then pull recent Health Connect data into Terra (debounced) and refresh silently if a
            // sync actually ran. Dropping the previous force=true lets the 20-min debounce skip the
            // expensive pull on repeat opens, which was the main source of the slow load.
            val synced = TerraSdkSync.syncLinkedConnections(
                terraManagerHolder.current,
                reason = "longevity",
            )
            if (synced) renderFromRepositories()
        }
    }

    private suspend fun renderFromRepositories() = coroutineScope {
        val today = LocalDate.now()
        val days = (0 until DAYS_PER_WEEK).map { today.minusDays((DAYS_PER_WEEK - 1 - it).toLong()) }
        val windowStart = days.first()

        // Independent reads run in parallel rather than one after another.
        val recoveryDef = async {
            recoveryRepository.getSnapshotsForRange(windowStart, today).getOrNull().orEmpty()
        }
        val activityDef = async {
            activityRepository.getSummariesForRange(windowStart, today, TerraDetail.FULL).getOrNull().orEmpty()
        }
        val sleepDef = async {
            sleepRepository.getSessionsForRange(windowStart, today).getOrNull().orEmpty()
        }
        val scansDef = async { scanHistoryRepository.getRecentScans(limit = 2).getOrNull().orEmpty() }
        val heightDef = async { userRepository.getUser().getOrNull()?.heightCm?.toFloat() }

        val recovery = recoveryDef.await()
        val activity = activityDef.await()
        val sleep = sleepDef.await()
        val scans = scansDef.await()
        val heightCm = heightDef.await()

        val recoveryByDate = recovery.associateBy { it.date }
        val activityByDate = activity.associateBy { it.date }
        val latestScan = scans.getOrNull(0)
        val previousScan = scans.getOrNull(1)
        val bodyFat = latestScan?.fatPercentage
        // Body composition and VO2 max change slowly, so the most recent reading stands in for
        // every day. Waist-to-height feeds the score's body-composition bucket alongside body fat.
        val waistToHeightRatio = waistToHeight(latestScan, heightCm)
        val vo2Max = activity.filter { (it.vo2MaxMlPerMinPerKg ?: 0f) > 0f }
            .maxByOrNull { it.date }?.vo2MaxMlPerMinPerKg

        fun dayScore(date: LocalDate): Int? = computeLongevityScore(
            LongevityInputs(
                hrvMs = recoveryByDate[date]?.hrvMs,
                restingHeartRateBpm = recoveryByDate[date]?.restingHeartRateBpm,
                sleepScore = recoveryByDate[date]?.sleepScore,
                steps = activityByDate[date]?.steps,
                bodyFatPercent = bodyFat,
                waistToHeightRatio = waistToHeightRatio,
                vo2Max = vo2Max,
            )
        )

        // Weekly tab: one longevity score per day for the last 7 days. Days with no data carry
        // forward the previous day's score so occasional usage still draws a continuous line.
        var lastKnownScore = 0f
        val weeklyPoints = days.map { day ->
            dayScore(day)?.toFloat()?.also { lastKnownScore = it } ?: lastKnownScore
        }
        val weeklyLabels = days.map { it.dayOfMonth.toString() }
        val weekRangeLabel = formatShortDateRange(days.first(), days.last())

        val score = currentLongevityScore(days, recovery, activity, latestScan, heightCm)

        // Daily tab: intraday "current longevity". Steps accrue hour by hour (real hourly data),
        // while the overnight signals (HRV, RHR, sleep) and body fat stay constant for the day,
        // so the score climbs as the day progresses. The line is only drawn up to the current
        // hour — the rest of the day hasn't happened yet — while the axis still spans the full day.
        val todayRecovery = recoveryByDate[today]
        val hourlySteps = activityByDate[today]?.hourlySteps.orEmpty()
        val currentHour = LocalTime.now().hour
        val dailyPoints = if (todayRecovery != null || hourlySteps.isNotEmpty()) {
            var cumulativeSteps = 0
            (0..currentHour).map { hour ->
                cumulativeSteps += hourlySteps[hour] ?: 0
                computeLongevityScore(
                    LongevityInputs(
                        hrvMs = todayRecovery?.hrvMs,
                        restingHeartRateBpm = todayRecovery?.restingHeartRateBpm,
                        sleepScore = todayRecovery?.sleepScore,
                        steps = cumulativeSteps.takeIf { it > 0 },
                        bodyFatPercent = bodyFat,
                        waistToHeightRatio = waistToHeightRatio,
                        vo2Max = vo2Max,
                    )
                )?.toFloat() ?: 0f
            }
        } else {
            emptyList()
        }

        val latestRecovery = recovery.maxByOrNull { it.date }
        val latestActivity = activity.filter { it.steps > 0 }.maxByOrNull { it.date }
        val latestSleep = sleep.maxByOrNull { it.wakeTime }

        _state.update {
            it.copy(
                score = score,
                syncedDate = today.formatShortMonthDayYear(),
                daily = LongevityTrendData(
                    points = dailyPoints,
                    axisLabels = listOf(
                        string(R.string.longevity_daily_axis_start),
                        string(R.string.longevity_daily_axis_end),
                    ),
                    currentLabelIndex = -1,
                    xAxisSpanCount = HOURS_PER_DAY,
                    dateLabel = today.formatShortMonthDay(),
                    trend = trendFor(dailyPoints),
                ),
                weekly = LongevityTrendData(
                    points = weeklyPoints,
                    axisLabels = weeklyLabels,
                    currentLabelIndex = weeklyLabels.lastIndex,
                    dateLabel = weekRangeLabel,
                    trend = trendFor(weeklyPoints),
                ),
                signals = buildSignals(latestRecovery, latestActivity, latestSleep, vo2Max),
                statusSignals = buildStatusSignals(latestRecovery, latestScan, previousScan, heightCm),
            )
        }
    }

    private fun buildSignals(
        recovery: RecoverySnapshot?,
        activity: ActivitySummary?,
        sleep: SleepSession?,
        vo2Max: Float?,
    ): List<LongevitySignal> {
        val unknown = string(R.string.stat_unknown)

        val hrv = recovery?.hrvMs?.takeIf { it > 0 }?.toString()
        val rhr = recovery?.restingHeartRateBpm?.takeIf { it > 0 }?.toString()
        val sleepDuration = sleep?.durationMinutes?.takeIf { it > 0 }
            ?.let { "${it / 60} h ${it % 60} m" }
        val recoveryScore = recovery?.score?.takeIf { it > 0 }?.let { "$it %" }
        val steps = activity?.steps?.takeIf { it > 0 }?.let { "%,d".format(it) }
        val vo2 = vo2Max?.takeIf { it > 0f }?.roundToInt()?.toString()

        return listOf(
            LongevitySignal(R.string.longevity_signal_hrv, hrv ?: unknown, R.string.longevity_unit_ms.takeIf { hrv != null }),
            LongevitySignal(R.string.longevity_signal_rhr, rhr ?: unknown, R.string.unit_bpm.takeIf { rhr != null }),
            LongevitySignal(R.string.longevity_signal_sleep, sleepDuration ?: unknown),
            LongevitySignal(R.string.longevity_signal_recovery, recoveryScore ?: unknown),
            LongevitySignal(R.string.longevity_signal_activity, steps ?: unknown, R.string.longevity_unit_steps.takeIf { steps != null }),
            LongevitySignal(R.string.longevity_signal_vo2_max, vo2 ?: unknown),
        )
    }

    private fun buildStatusSignals(
        recovery: RecoverySnapshot?,
        latestScan: ScanRecord?,
        previousScan: ScanRecord?,
        heightCm: Float?,
    ): List<LongevitySignal> {
        val unknown = string(R.string.stat_unknown)

        val waist = waistTrend(latestScan, previousScan, heightCm)?.let { string(it.labelRes) } ?: unknown
        val physiqueDelta = latestScan?.let { comparablePhysiqueScoreDelta(it, previousScan, heightCm) }
        val physique = physiqueDelta?.let { string(trendFromDelta(it).labelRes) } ?: unknown
        val stress = stressValue(recovery, unknown)

        return listOf(
            LongevitySignal(R.string.longevity_signal_waist_profile, waist),
            LongevitySignal(R.string.longevity_signal_physique_trend, physique),
            LongevitySignal(R.string.longevity_signal_stress_load, stress),
        )
    }

    private fun stressValue(recovery: RecoverySnapshot?, unknown: String): String =
        when (recovery?.stressLevel) {
            StressLevel.Low -> string(R.string.recovery_stress_low)
            StressLevel.Medium -> string(R.string.recovery_stress_medium)
            StressLevel.High -> string(R.string.recovery_stress_high)
            null -> unknown
        }

    /** Waist-to-height ratio trend across the two most recent scans (a lower ratio is improving). */
    private fun waistTrend(latest: ScanRecord?, previous: ScanRecord?, heightCm: Float?): LongevityTrend? {
        val latestRatio = waistToHeight(latest, heightCm) ?: return null
        val previousRatio = waistToHeight(previous, heightCm) ?: return null
        if (previousRatio <= 0f) return null
        val deltaPercent = (latestRatio - previousRatio) / previousRatio * 100f
        return when {
            deltaPercent < -LongevityConstants.TREND_FLAT_THRESHOLD -> LongevityTrend.Improving
            deltaPercent > LongevityConstants.TREND_FLAT_THRESHOLD -> LongevityTrend.Decreasing
            else -> LongevityTrend.Stable
        }
    }

    private fun waistToHeight(scan: ScanRecord?, heightCm: Float?): Float? {
        val height = heightCm?.takeIf { it > 0f } ?: return null
        val waist = scan?.let {
            BodyMeasurementKeys.valueFor(it.measurements, BodyMeasurementRegion.Waist)
        } ?: return null
        return waist / height
    }

    /** A higher score (physique, longevity) trending up is improving. */
    private fun trendFromDelta(delta: Float): LongevityTrend = when {
        delta > LongevityConstants.TREND_FLAT_THRESHOLD -> LongevityTrend.Improving
        delta < -LongevityConstants.TREND_FLAT_THRESHOLD -> LongevityTrend.Decreasing
        else -> LongevityTrend.Stable
    }

    private fun trendFor(points: List<Float>): LongevityTrend {
        val present = points.filter { it > 0f }
        if (present.size < 2) return LongevityTrend.Stable
        return trendFromDelta(present.last() - present.first())
    }

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val HOURS_PER_DAY = 24
    }
}
