package com.hexis.bi.ui.main.home.sleep

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.data.sleep.TerraSleepRepository
import com.hexis.bi.data.sleep.TerraSleepSession
import com.hexis.bi.data.sleep.TerraSleepStage
import com.hexis.bi.data.sleep.TerraSleepStageInterval
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.enums.WeekDay
import androidx.lifecycle.viewModelScope
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.BlueFadedIndicator100
import com.hexis.bi.ui.theme.BlueFadedIndicator200
import com.hexis.bi.ui.theme.BlueFadedIndicator300
import com.hexis.bi.utils.constants.SleepConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt
import kotlin.random.Random

class SleepViewModel(
    application: Application,
    private val terraSleepRepository: TerraSleepRepository,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    private var weekOffset = 0

    init {
        loadSleepGoal()
        loadTodaySession()
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
        val newGoal = _state.value.sleepGoalHoursDraft
        _state.update { it.copy(showSettingsDialog = false, sleepGoalHours = newGoal) }
        viewModelScope.launch {
            userRepository.updateUserSettings(
                mapOf(FirestoreSchema.UserSettingsFields.SLEEP_GOAL_HOURS to newGoal)
            ).onFailure { setError(it.message) }
        }
        // Re-derive week snapshot so today's bar reflects updated goal context.
        loadWeekData(weekOffset)
    }

    fun retryLoad() = loadTodaySession()

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

    private fun loadSleepGoal() = viewModelScope.launch {
        userRepository.getUserSettings().onSuccess { settings ->
            val goal = settings.sleepGoalHours ?: return@onSuccess
            _state.update { it.copy(sleepGoalHours = goal, sleepGoalHoursDraft = goal) }
        }
    }

    private fun loadTodaySession() {
        _state.update { it.copy(dayLoadState = SleepDayLoadState.Loading, errorMessage = null) }
        viewModelScope.launch {
            val result = terraSleepRepository.getSessionForNight(LocalDate.now())
            result.fold(
                onSuccess = { session ->
                    if (session == null) {
                        applyEmptySession()
                    } else {
                        applySession(session)
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            dayLoadState = SleepDayLoadState.Error,
                            errorMessage = err.message,
                        )
                    }
                },
            )
        }
    }

    private fun applyEmptySession() {
        _state.update {
            it.copy(
                dayLoadState = SleepDayLoadState.Ready,
                errorMessage = null,
                totalSleepMinutes = 0,
                sleepQuality = SleepQuality.Fair,
                stages = aggregateStages(emptyList()),
                restfulness = 0,
                restfulnessMax = 100,
                hrv = 0,
                restingHeartRate = 0,
                timelineStartHour = 23,
                timelineEndHour = 6,
                timelineSegments = emptyList(),
                insightRes = R.string.sleep_recovery_subtitle,
            )
        }
        loadWeekData(weekOffset)
    }

    private fun applySession(session: TerraSleepSession) {
        val score = computeSleepScore(session.durationMinutes, session.efficiencyPercent)
        val quality = qualityFor(score)
        val stages = aggregateStages(session.stages)
        val segments = buildTimelineSegments(session)
        val startHour = session.bedtime.hour
        val endHour = session.wakeTime.hour

        _state.update {
            it.copy(
                dayLoadState = SleepDayLoadState.Ready,
                errorMessage = null,
                totalSleepMinutes = session.durationMinutes,
                sleepQuality = quality,
                stages = stages,
                restfulness = score,
                restfulnessMax = 100,
                hrv = session.hrvMs,
                restingHeartRate = session.restingHeartRateBpm,
                timelineStartHour = startHour,
                timelineEndHour = endHour,
                timelineSegments = segments,
                insightRes = insightFor(quality),
            )
        }
        // Refresh summary so today's bar reflects the just-loaded session.
        loadWeekData(weekOffset)
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
        val score = SleepConstants.SCORE_DURATION_WEIGHT * durationScore +
                SleepConstants.SCORE_EFFICIENCY_WEIGHT * efficiencyScore
        return score.roundToInt().coerceIn(0, 100)
    }

    private fun mapTo0To100(value: Float, low: Float, high: Float): Float {
        if (high <= low) return 0f
        val normalized = ((value - low) / (high - low)).coerceIn(0f, 1f)
        return normalized * 100f
    }

    private fun qualityFor(score: Int): SleepQuality = when {
        score >= SleepConstants.QUALITY_GOOD_MIN -> SleepQuality.Good
        score >= SleepConstants.QUALITY_FAIR_MIN -> SleepQuality.Fair
        else -> SleepQuality.Poor
    }

    private fun insightFor(quality: SleepQuality): Int = when (quality) {
        SleepQuality.Good -> R.string.sleep_insight_good
        SleepQuality.Fair -> R.string.sleep_insight_fair
        SleepQuality.Poor -> R.string.sleep_insight_poor
    }

    private fun aggregateStages(intervals: List<TerraSleepStageInterval>): List<SleepStageData> {
        val sums = intervals.groupBy { it.stage }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
        return listOf(
            SleepStageData(SleepStage.Deep, sums[TerraSleepStage.Deep] ?: 0, Blue300),
            SleepStageData(SleepStage.REM, sums[TerraSleepStage.REM] ?: 0, BlueFadedIndicator300),
            SleepStageData(SleepStage.Light, sums[TerraSleepStage.Light] ?: 0, BlueFadedIndicator200),
            SleepStageData(SleepStage.Awake, sums[TerraSleepStage.Awake] ?: 0, BlueFadedIndicator100),
        )
    }

    private fun buildTimelineSegments(session: TerraSleepSession): List<TimelineSegment> {
        val totalMinutes = Duration.between(session.bedtime, session.wakeTime).toMinutes()
            .toFloat()
            .coerceAtLeast(1f)
        return session.stages.map { interval ->
            val startOffset = Duration.between(session.bedtime, interval.start).toMinutes().toFloat()
            val endOffset = Duration.between(session.bedtime, interval.end).toMinutes().toFloat()
            TimelineSegment(
                stage = interval.stage.toUiStage(),
                startFraction = (startOffset / totalMinutes).coerceIn(0f, 1f),
                endFraction = (endOffset / totalMinutes).coerceIn(0f, 1f),
            )
        }
    }

    private fun TerraSleepStage.toUiStage(): SleepStage = when (this) {
        TerraSleepStage.Deep -> SleepStage.Deep
        TerraSleepStage.REM -> SleepStage.REM
        TerraSleepStage.Light -> SleepStage.Light
        TerraSleepStage.Awake -> SleepStage.Awake
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

    // Summary tab keeps seeded mock data until the weekly Terra endpoint is wired in.
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
