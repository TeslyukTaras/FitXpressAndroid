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
import com.hexis.bi.utils.formatFullMonthDay
import com.hexis.bi.utils.formatShortDateRange
import com.hexis.bi.utils.formatShortMonthDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

class RecoveryViewModel(
    application: Application,
    private val recoveryRepository: RecoveryRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(RecoveryState())
    val state: StateFlow<RecoveryState> = _state.asStateFlow()

    private var dayOffset = 0
    private var weekOffset = 0
    private var dayLoadJob: Job? = null
    private var summaryLoadJob: Job? = null

    init {
        loadInitialData()
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

    private fun loadInitialData() {
        val today = LocalDate.now()
        val window = summaryWindow(0)
        _state.update {
            it.copy(
                dayLoadState = RecoveryLoadState.Loading,
                dayErrorMessage = null,
                dateLabel = today.formatFullMonthDay(),
                canGoNextDay = false,
                summaryLoadState = RecoveryLoadState.Loading,
                summaryErrorMessage = null,
                weekLabel = window.label,
                canGoNextWeek = false,
            )
        }

        dayLoadJob?.cancel()
        summaryLoadJob?.cancel()
        dayLoadJob = viewModelScope.launch {
            recoveryRepository.getSnapshotsForRange(window.previousStart, window.end).fold(
                onSuccess = { snapshots ->
                    val byDate = snapshots.associateBy { it.date }
                    val todaySnapshot = byDate[today]
                    _state.update {
                        it.copy(
                            dayLoadState = RecoveryLoadState.Ready,
                            dayErrorMessage = null,
                            score = todaySnapshot?.score ?: 0,
                            metrics = buildMetrics(todaySnapshot),
                            rmssdMs = todaySnapshot?.hrvMs ?: 0,
                            sdnnMs = todaySnapshot?.sdnnMs ?: 0,
                        ).withSummarySnapshots(
                            snapshots = snapshots,
                            window = window,
                            today = today,
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            dayLoadState = RecoveryLoadState.Error,
                            dayErrorMessage = err.message,
                            summaryLoadState = RecoveryLoadState.Error,
                            summaryErrorMessage = err.message,
                        )
                    }
                },
            )
        }
    }

    private fun loadDayData(offset: Int) {
        val day = LocalDate.now().plusDays(offset.toLong())
        val dateLabel = day.formatFullMonthDay()
        _state.update {
            it.copy(
                dayLoadState = RecoveryLoadState.Loading,
                dayErrorMessage = null,
                dateLabel = dateLabel,
                canGoNextDay = offset < 0,
            )
        }
        dayLoadJob?.cancel()
        dayLoadJob = viewModelScope.launch {
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
        val today = LocalDate.now()
        val window = summaryWindow(offset)

        _state.update {
            it.copy(
                summaryLoadState = RecoveryLoadState.Loading,
                summaryErrorMessage = null,
                weekLabel = window.label,
                canGoNextWeek = offset < 0,
            )
        }

        summaryLoadJob?.cancel()
        summaryLoadJob = viewModelScope.launch {
            recoveryRepository.getSnapshotsForRange(window.previousStart, window.end).fold(
                onSuccess = { snapshots ->
                    _state.update {
                        it.withSummarySnapshots(
                            snapshots = snapshots,
                            window = window,
                            today = today,
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

    private fun RecoveryState.withSummarySnapshots(
        snapshots: List<RecoverySnapshot>,
        window: SummaryWindow,
        today: LocalDate,
    ): RecoveryState {
        val byDate = snapshots.associateBy { it.date }
        val entries = (0..6).map { i ->
            val day = window.start.plusDays(i.toLong())
            DailyRecoveryEntry(
                dayLabel = day.dayOfMonth.toString(),
                score = byDate[day]?.score ?: 0,
                isHighlighted = day == today,
                tooltipLabel = day.formatShortMonthDay(),
            )
        }
        val avg = entries.filter { it.score > 0 }
            .let { nonEmpty -> if (nonEmpty.isEmpty()) 0 else nonEmpty.sumOf { it.score } / nonEmpty.size }

        val previousAvg = byDate.entries
            .filter { (d, _) -> !d.isBefore(window.previousStart) && !d.isAfter(window.previousEnd) }
            .map { it.value.score }
            .filter { it > 0 }
            .let { if (it.isEmpty()) 0 else it.sum() / it.size }

        return copy(
            summaryLoadState = RecoveryLoadState.Ready,
            summaryErrorMessage = null,
            weeklyEntries = entries,
            avgScore = avg,
            trend = trendFor(avg, previousAvg),
        )
    }

    private fun summaryWindow(offset: Int): SummaryWindow {
        val today = LocalDate.now()
        val end = today.plusDays(offset.toLong() * SUMMARY_WINDOW_DAYS)
        val start = end.minusDays(SUMMARY_WINDOW_DAYS - 1)
        val previousStart = start.minusDays(SUMMARY_WINDOW_DAYS)
        val previousEnd = start.minusDays(1)
        return SummaryWindow(
            start = start,
            end = end,
            previousStart = previousStart,
            previousEnd = previousEnd,
            label = formatShortDateRange(start, end),
        )
    }

    private fun buildMetrics(snapshot: RecoverySnapshot?): List<RecoveryMetric> {
        val unknown = appContext.getString(R.string.stat_unknown)
        val sleepValue = snapshot?.sleepScore?.takeIf { it > 0 }
            ?.let(::sleepLabelFor)
            ?.let(appContext::getString)
            ?: unknown
        val heartBpm = snapshot?.restingHeartRateBpm?.coerceAtLeast(0) ?: 0
        val stressValue =
            snapshot?.stressLevel?.let { appContext.getString(stressLabelFor(it)) } ?: unknown
        val loadValue =
            snapshot?.activityLoad?.let { appContext.getString(loadLabelFor(it)) } ?: unknown
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
        private const val SUMMARY_WINDOW_DAYS = 7L
        private const val TREND_FLAT_THRESHOLD = 3
    }

    private data class SummaryWindow(
        val start: LocalDate,
        val end: LocalDate,
        val previousStart: LocalDate,
        val previousEnd: LocalDate,
        val label: String,
    )
}
