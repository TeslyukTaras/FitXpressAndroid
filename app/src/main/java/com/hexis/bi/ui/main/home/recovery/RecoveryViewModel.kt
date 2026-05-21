package com.hexis.bi.ui.main.home.recovery

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.recovery.ActivityLoadLevel
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.recovery.RecoverySnapshot
import com.hexis.bi.data.recovery.StressLevel
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.SleepConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

class RecoveryViewModel(
    application: Application,
    private val recoveryRepository: RecoveryRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(RecoveryState())
    val state: StateFlow<RecoveryState> = _state.asStateFlow()

    private var dayOffset = 0
    private var weekOffset = 0

    init {
        loadDayData(0)
        loadWeekData(0)
    }

    fun selectTab(tab: RecoveryTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun previousDay() {
        dayOffset--
        loadDayData(dayOffset)
    }

    fun nextDay() {
        if (dayOffset < 0) {
            dayOffset++
            loadDayData(dayOffset)
        }
    }

    fun previousWeek() {
        weekOffset--
        loadWeekData(weekOffset)
    }

    fun nextWeek() {
        if (weekOffset < 0) {
            weekOffset++
            loadWeekData(weekOffset)
        }
    }

    fun showInfoSheet() {
        _state.update { it.copy(showInfoSheet = true) }
    }

    fun dismissInfoSheet() {
        _state.update { it.copy(showInfoSheet = false) }
    }

    fun retryDayLoad() = loadDayData(dayOffset)

    fun retrySummaryLoad() = loadWeekData(weekOffset)

    private fun loadDayData(offset: Int) {
        val day = LocalDate.now().plusDays(offset.toLong())
        val dateLabel = day.format(DAY_FMT)
        _state.update {
            it.copy(
                dayLoadState = RecoveryLoadState.Loading,
                dayErrorMessage = null,
                dateLabel = dateLabel,
                canGoNextDay = offset < 0,
            )
        }
        viewModelScope.launch {
            recoveryRepository.getSnapshotForDate(day).fold(
                onSuccess = { snapshot ->
                    _state.update {
                        it.copy(
                            dayLoadState = RecoveryLoadState.Ready,
                            dayErrorMessage = null,
                            score = snapshot?.score ?: 0,
                            metrics = buildMetrics(snapshot),
                            rmssdMs = snapshot?.hrvMs ?: 0,
                            sdnnMs = snapshot?.sdnnMs ?: 0,
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            dayLoadState = RecoveryLoadState.Error,
                            dayErrorMessage = err.message,
                        )
                    }
                },
            )
        }
    }

    private fun loadWeekData(offset: Int) {
        val monday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(offset.toLong())
        val sunday = monday.plusDays(6)
        val previousMonday = monday.minusWeeks(1)
        val previousSunday = sunday.minusWeeks(1)
        val label = "${monday.format(WEEK_FMT)} - ${sunday.format(WEEK_FMT)}"

        _state.update {
            it.copy(
                summaryLoadState = RecoveryLoadState.Loading,
                summaryErrorMessage = null,
                weekLabel = label,
                canGoNextWeek = offset < 0,
            )
        }

        viewModelScope.launch {
            recoveryRepository.getSnapshotsForRange(previousMonday, sunday).fold(
                onSuccess = { snapshots ->
                    val byDate = snapshots.associateBy { it.date }
                    val today = LocalDate.now()
                    val entries = (0..6).map { i ->
                        val day = monday.plusDays(i.toLong())
                        val score = if (day.isAfter(today)) 0 else byDate[day]?.score ?: 0
                        DailyRecoveryEntry(
                            dayLabel = day.format(WEEK_FMT),
                            score = score,
                            isHighlighted = day == today,
                        )
                    }
                    val avg = entries.filter { it.score > 0 }
                        .let { nonEmpty -> if (nonEmpty.isEmpty()) 0 else nonEmpty.sumOf { it.score } / nonEmpty.size }

                    val previousAvg = byDate.entries
                        .filter { (d, _) -> !d.isBefore(previousMonday) && !d.isAfter(previousSunday) }
                        .map { it.value.score }
                        .filter { it > 0 }
                        .let { if (it.isEmpty()) 0 else it.sum() / it.size }

                    _state.update {
                        it.copy(
                            summaryLoadState = RecoveryLoadState.Ready,
                            summaryErrorMessage = null,
                            weeklyEntries = entries,
                            avgScore = avg,
                            trend = trendFor(avg, previousAvg),
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            summaryLoadState = RecoveryLoadState.Error,
                            summaryErrorMessage = err.message,
                        )
                    }
                },
            )
        }
    }

    private fun buildMetrics(snapshot: RecoverySnapshot?): List<RecoveryMetric> {
        val unknown = appContext.getString(R.string.stat_unknown)
        val sleepValue = snapshot?.sleepScore?.takeIf { it > 0 }
            ?.let(::sleepLabelFor)
            ?.let(appContext::getString)
            ?: unknown
        val heartBpm = snapshot?.restingHeartRateBpm?.coerceAtLeast(0) ?: 0
        val stressValue = snapshot?.stressLevel?.let { appContext.getString(stressLabelFor(it)) } ?: unknown
        val loadValue = snapshot?.activityLoad?.let { appContext.getString(loadLabelFor(it)) } ?: unknown
        return listOf(
            RecoveryMetric(R.string.recovery_metric_sleep, sleepValue),
            RecoveryMetric(R.string.recovery_metric_stress, stressValue),
            RecoveryMetric(R.string.recovery_metric_activity_load, loadValue),
            RecoveryMetric(
                R.string.recovery_metric_resting_hr,
                value = heartBpm.toString(),
                unit = appContext.getString(R.string.unit_bpm),
            ),
        )
    }

    private fun sleepLabelFor(score: Int): Int = when {
        score >= SleepConstants.QUALITY_GOOD_MIN -> R.string.sleep_quality_good
        score >= SleepConstants.QUALITY_FAIR_MIN -> R.string.sleep_quality_fair
        else -> R.string.sleep_quality_poor
    }

    private fun stressLabelFor(level: StressLevel): Int = when (level) {
        StressLevel.Low -> R.string.recovery_stress_low
        StressLevel.Medium -> R.string.recovery_stress_medium
        StressLevel.High -> R.string.recovery_stress_high
    }

    private fun loadLabelFor(level: ActivityLoadLevel): Int = when (level) {
        ActivityLoadLevel.Light -> R.string.recovery_load_light
        ActivityLoadLevel.Moderate -> R.string.recovery_load_moderate
        ActivityLoadLevel.Heavy -> R.string.recovery_load_heavy
    }

    private fun trendFor(currentAvg: Int, previousAvg: Int): RecoveryTrend {
        if (previousAvg <= 0 || currentAvg <= 0) return RecoveryTrend.Stable
        val delta = currentAvg - previousAvg
        return when {
            abs(delta) <= TREND_FLAT_THRESHOLD -> RecoveryTrend.Stable
            delta > 0 -> RecoveryTrend.Improving
            else -> RecoveryTrend.Decreasing
        }
    }

    private companion object {
        private val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d")
        private val WEEK_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
        private const val TREND_FLAT_THRESHOLD = 3
    }
}
