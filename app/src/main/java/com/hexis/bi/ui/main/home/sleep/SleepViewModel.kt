package com.hexis.bi.ui.main.home.sleep

import android.app.Application
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.hexis.bi.domain.enums.WeekDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt
import kotlin.random.Random

class SleepViewModel(application: Application) : BaseViewModel(application) {

    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    private var weekOffset = 0

    init {
        loadWeekData(0)
    }

    fun selectTab(tab: SleepTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun showSettingsDialog() {
        _state.update { it.copy(showSettingsDialog = true, sleepGoalHoursDraft = it.sleepGoalHours) }
    }

    fun dismissSettingsDialog() {
        _state.update { it.copy(showSettingsDialog = false) }
    }

    fun updateSleepGoalDraft(hours: Int) {
        _state.update { it.copy(sleepGoalHoursDraft = hours) }
    }

    fun saveSettings() {
        _state.update {
            it.copy(
                showSettingsDialog = false,
                sleepGoalHours = it.sleepGoalHoursDraft,
            )
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

    fun showRecoverySheet() {
        _state.update { it.copy(showRecoverySheet = true) }
    }

    fun dismissRecoverySheet() {
        _state.update { it.copy(showRecoverySheet = false) }
    }

    private fun loadWeekData(offset: Int) {
        val monday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(offset.toLong())
        val sunday = monday.plusDays(6)
        val fmt = DateTimeFormatter.ofPattern("MMM d")
        val label = "${monday.format(fmt)} - ${sunday.format(fmt)}"

        val todayMinutes = if (offset == 0) _state.value.totalSleepMinutes else -1
        val (entries, stages) = generateWeekData(monday, sunday, todayMinutes)

        _state.update {
            it.copy(
                weekLabel = label,
                weeklyEntries = entries,
                weeklyStages = stages,
                avgSleepMinutes = avgMinutes(entries),
                canGoNextWeek = offset < 0,
            )
        }
    }

    // Seeded by monday's epoch day — same week always produces the same values.
    // todayMinutes >= 0 pins today's bar to match the Day tab instead of using a random value.
    fun generateWeekData(monday: LocalDate, sunday: LocalDate, todayMinutes: Int = -1): Pair<List<DailySleepEntry>, List<WeeklyStageData>> {
        val today = LocalDate.now()
        val random = Random(monday.toEpochDay())

        val entries = WeekDay.entries.mapIndexed { index, weekDay ->
            val day = monday.plusDays(index.toLong())
            val minutes = when {
                day.isAfter(today) -> 0
                day == today && todayMinutes >= 0 -> todayMinutes
                else -> random.nextInt(SLEEP_MIN_MINUTES, SLEEP_MAX_MINUTES + 1)
            }
            DailySleepEntry(weekDay.abbreviation, minutes, isHighlighted = day == today)
        }

        val avgNightMinutes = avgMinutes(entries)
        if (avgNightMinutes == 0) {
            return Pair(entries, emptyStageList())
        }

        val (deepPct, remPct, awakePct) = stageSplit(random)
        val lightPct = 1f - deepPct - remPct - awakePct

        // Previous week's split used only for trend direction
        val prevRandom = Random(monday.minusWeeks(1).toEpochDay())
        val (prevDeepPct, prevRemPct, prevAwakePct) = stageSplit(prevRandom)
        val prevLightPct = 1f - prevDeepPct - prevRemPct - prevAwakePct

        val deepMinutes = (avgNightMinutes * deepPct).roundToInt()
        val remMinutes = (avgNightMinutes * remPct).roundToInt()
        val awakeMinutes = (avgNightMinutes * awakePct).roundToInt()
        val lightMinutes = avgNightMinutes - deepMinutes - remMinutes - awakeMinutes

        val stages = listOf(
            WeeklyStageData(SleepStage.Deep, deepMinutes, trend(deepPct, prevDeepPct)),
            WeeklyStageData(SleepStage.REM, remMinutes, trend(remPct, prevRemPct)),
            WeeklyStageData(SleepStage.Light, lightMinutes, trend(lightPct, prevLightPct)),
            WeeklyStageData(SleepStage.Awake, awakeMinutes, trend(awakePct, prevAwakePct)),
        )

        return Pair(entries, stages)
    }

    private fun stageSplit(random: Random): Triple<Float, Float, Float> {
        val deep = DEEP_MIN_PCT + random.nextFloat() * (DEEP_MAX_PCT - DEEP_MIN_PCT)
        val rem = REM_MIN_PCT + random.nextFloat() * (REM_MAX_PCT - REM_MIN_PCT)
        val awake = AWAKE_MIN_PCT + random.nextFloat() * (AWAKE_MAX_PCT - AWAKE_MIN_PCT)
        return Triple(deep, rem, awake)
    }

    private fun trend(current: Float, previous: Float): StageTrend =
        if (current >= previous) StageTrend.Up else StageTrend.Down

    private fun emptyStageList() = listOf(
        WeeklyStageData(SleepStage.Deep, 0, StageTrend.Up),
        WeeklyStageData(SleepStage.REM, 0, StageTrend.Up),
        WeeklyStageData(SleepStage.Light, 0, StageTrend.Up),
        WeeklyStageData(SleepStage.Awake, 0, StageTrend.Up),
    )

    private fun avgMinutes(entries: List<DailySleepEntry>): Int {
        val nonEmpty = entries.filter { it.durationMinutes > 0 }
        return if (nonEmpty.isEmpty()) 0 else nonEmpty.sumOf { it.durationMinutes } / nonEmpty.size
    }

    companion object {
        private const val SLEEP_MIN_MINUTES = 300  // 5 h
        private const val SLEEP_MAX_MINUTES = 540  // 9 h

        private const val DEEP_MIN_PCT = 0.10f
        private const val DEEP_MAX_PCT = 0.20f
        private const val REM_MIN_PCT = 0.15f
        private const val REM_MAX_PCT = 0.25f
        private const val AWAKE_MIN_PCT = 0.03f
        private const val AWAKE_MAX_PCT = 0.08f
    }
}
