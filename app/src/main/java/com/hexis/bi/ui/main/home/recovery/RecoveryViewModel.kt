package com.hexis.bi.ui.main.home.recovery

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

class RecoveryViewModel(application: Application) : BaseViewModel(application) {

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

    private fun loadDayData(offset: Int) {
        val day = LocalDate.now().plusDays(offset.toLong())
        val fmt = DateTimeFormatter.ofPattern("MMMM d")
        val random = Random(day.toEpochDay())

        val score = random.nextInt(SCORE_MIN, SCORE_MAX + 1)

        val metrics = listOf(
            RecoveryMetric(
                R.string.recovery_metric_sleep,
                sleepOptions[random.nextInt(sleepOptions.size)]
            ),
            RecoveryMetric(
                R.string.recovery_metric_heart,
                "${random.nextInt(HEART_RATE_MIN, HEART_RATE_MAX + 1)} bpm"
            ),
            RecoveryMetric(
                R.string.recovery_metric_stress,
                stressOptions[random.nextInt(stressOptions.size)]
            ),
            RecoveryMetric(
                R.string.recovery_metric_activity_load,
                loadOptions[random.nextInt(loadOptions.size)]
            ),
        )

        _state.update {
            it.copy(
                dateLabel = day.format(fmt),
                score = score,
                metrics = metrics,
                canGoNextDay = offset < 0,
            )
        }
    }

    private fun loadWeekData(offset: Int) {
        val monday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(offset.toLong())
        val sunday = monday.plusDays(6)
        val fmt = DateTimeFormatter.ofPattern("MMM d")
        val label = "${monday.format(fmt)} - ${sunday.format(fmt)}"

        val today = LocalDate.now()
        val random = Random(monday.toEpochDay())

        val entries = (0..6).map { i ->
            val day = monday.plusDays(i.toLong())
            val score = when {
                day.isAfter(today) -> 0
                else -> random.nextInt(SCORE_MIN, SCORE_MAX + 1)
            }
            DailyRecoveryEntry(
                dayLabel = day.format(DateTimeFormatter.ofPattern("MMM d")),
                score = score,
                isHighlighted = day == today,
            )
        }

        val nonEmpty = entries.filter { it.score > 0 }
        val avg = if (nonEmpty.isEmpty()) 0 else nonEmpty.sumOf { it.score } / nonEmpty.size

        _state.update {
            it.copy(
                weekLabel = label,
                weeklyEntries = entries,
                avgScore = avg,
                canGoNextWeek = offset < 0,
            )
        }
    }

    companion object {
        private const val SCORE_MIN = 0
        private const val SCORE_MAX = 100
        private const val HEART_RATE_MIN = 55
        private const val HEART_RATE_MAX = 100
        private val sleepOptions = listOf("Good", "Fair", "Poor")
        private val stressOptions = listOf("Low", "Medium", "High")
        private val loadOptions = listOf("Light", "Moderate", "Heavy")
    }
}
