package com.hexis.bi.ui.main.home.sleep

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.data.sleep.SleepStageInterval
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.enums.WeekDay
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.BlueFadedIndicator100
import com.hexis.bi.ui.theme.BlueFadedIndicator200
import com.hexis.bi.ui.theme.BlueFadedIndicator300
import com.hexis.bi.utils.constants.SleepConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt
import timber.log.Timber

class SleepViewModel(
    application: Application,
    private val sleepRepository: SleepRepository,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    private var weekOffset = 0
    private val loadedTabs = mutableSetOf<SleepTab>()

    init {
        userRepository.observeUserSettings()
            .onEach { settings ->
                val goal = settings.sleepGoalHours ?: SleepConstants.DEFAULT_SLEEP_GOAL_HOURS
                _state.update { curr ->
                    if (curr.showSettingsDialog) {
                        curr.copy(sleepGoalHours = goal)
                    } else {
                        curr.copy(sleepGoalHours = goal, sleepGoalHoursDraft = goal)
                    }
                }
            }
            .catch { e -> Timber.w(e, "observeUserSettings failed; keeping sleep goal defaults") }
            .launchIn(viewModelScope)

        loadDataForTab(_state.value.selectedTab)
    }

    fun selectTab(tab: SleepTab) {
        _state.update { it.copy(selectedTab = tab) }
        if (tab !in loadedTabs) loadDataForTab(tab)
    }

    private fun loadDataForTab(tab: SleepTab) {
        when (tab) {
            SleepTab.Day -> loadTodaySession()
            SleepTab.Summary -> loadWeekData(weekOffset)
        }
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
        if (SleepTab.Summary in loadedTabs) loadWeekData(weekOffset)
    }

    fun retryLoad() = loadTodaySession()

    fun retrySummaryLoad() = loadWeekData(weekOffset)

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

    private fun loadTodaySession() {
        val targetDay = LocalDate.now()
        _state.update { it.copy(dayLoadState = SleepLoadState.Loading, errorMessage = null) }
        viewModelScope.launch {
            val result = withTimeoutOrNull(DAY_LOAD_TIMEOUT_MS) {
                sleepRepository.getSessionForNight(targetDay)
            }
            (result ?: Result.success(null)).fold(
                onSuccess = { session ->
                    if (session.hasAnySleepData()) {
                        applySession(session!!)
                    } else {
                        loadBestSessionFromNeighborRange(targetDay)
                    }
                },
                onFailure = { err ->
                    applyEmptySession()
                    _state.update { it.copy(errorMessage = err.message) }
                },
            )
        }
    }

    private fun applyEmptySession() {
        loadedTabs.add(SleepTab.Day)
        _state.update {
            it.copy(
                dayLoadState = SleepLoadState.Ready,
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
        prefetchSummaryIfNeeded()
    }

    private fun applySession(session: SleepSession) {
        loadedTabs.add(SleepTab.Day)
        val score = computeSleepScore(session.durationMinutes, session.efficiencyPercent)
        val quality = qualityFor(score)

        _state.update {
            it.copy(
                dayLoadState = SleepLoadState.Ready,
                errorMessage = null,
                totalSleepMinutes = session.durationMinutes,
                sleepQuality = quality,
                stages = aggregateStages(session.stages),
                restfulness = score,
                restfulnessMax = 100,
                hrv = session.hrvMs,
                restingHeartRate = session.restingHeartRateBpm,
                timelineStartHour = session.bedtime.hour,
                timelineEndHour = session.wakeTime.hour,
                timelineSegments = buildTimelineSegments(session),
                insightRes = insightFor(quality),
            )
        }
        prefetchSummaryIfNeeded()
    }

    private fun prefetchSummaryIfNeeded() {
        if (_state.value.selectedTab == SleepTab.Day && SleepTab.Summary !in loadedTabs) {
            loadWeekData(weekOffset)
        }
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

    private fun aggregateStages(intervals: List<SleepStageInterval>): List<SleepStageData> {
        val sums = intervals.groupBy { it.stage }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
        return listOf(
            SleepStageData(SleepStage.Deep, sums[SleepStage.Deep] ?: 0, Blue300),
            SleepStageData(SleepStage.REM, sums[SleepStage.REM] ?: 0, BlueFadedIndicator300),
            SleepStageData(SleepStage.Light, sums[SleepStage.Light] ?: 0, BlueFadedIndicator200),
            SleepStageData(SleepStage.Awake, sums[SleepStage.Awake] ?: 0, BlueFadedIndicator100),
        )
    }

    private fun buildTimelineSegments(session: SleepSession): List<TimelineSegment> {
        val totalMinutes = Duration.between(session.bedtime, session.wakeTime).toMinutes()
            .toFloat()
            .coerceAtLeast(1f)
        return session.stages.map { interval ->
            val startOffset = Duration.between(session.bedtime, interval.start).toMinutes().toFloat()
            val endOffset = Duration.between(session.bedtime, interval.end).toMinutes().toFloat()
            TimelineSegment(
                stage = interval.stage,
                startFraction = (startOffset / totalMinutes).coerceIn(0f, 1f),
                endFraction = (endOffset / totalMinutes).coerceIn(0f, 1f),
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
        val fmt = DateTimeFormatter.ofPattern("MMM d")
        val label = "${monday.format(fmt)} - ${sunday.format(fmt)}"

        _state.update {
            it.copy(
                summaryLoadState = SleepLoadState.Loading,
                summaryErrorMessage = null,
                weekLabel = label,
                canGoNextWeek = offset < 0,
            )
        }

        viewModelScope.launch {
            sleepRepository
                .getSessionsForRange(previousMonday.minusDays(1), sunday)
                .fold(
                    onSuccess = { sessions ->
                        val current = sessions.filter { session ->
                            val wakeDay = session.wakeTime.toLocalDate()
                            !wakeDay.isBefore(monday) && !wakeDay.isAfter(sunday)
                        }
                        val previous = sessions.filter { session ->
                            val wakeDay = session.wakeTime.toLocalDate()
                            !wakeDay.isBefore(previousMonday) && !wakeDay.isAfter(previousSunday)
                        }
                        val entries = buildWeekEntries(monday, current)
                        val stages = buildWeeklyStages(current, previous)

                        loadedTabs.add(SleepTab.Summary)
                        _state.update {
                            it.copy(
                                summaryLoadState = SleepLoadState.Ready,
                                summaryErrorMessage = null,
                                weeklyEntries = entries,
                                weeklyStages = stages,
                                avgSleepMinutes = avgMinutes(entries),
                            )
                        }
                    },
                    onFailure = { failure ->
                        _state.update {
                            it.copy(
                                summaryLoadState = SleepLoadState.Error,
                                summaryErrorMessage = failure.message,
                            )
                        }
                    },
                )
        }
    }

    private fun buildWeekEntries(
        monday: LocalDate,
        sessions: List<SleepSession>,
    ): List<DailySleepEntry> {
        val today = LocalDate.now()
        val byDate = sessions.groupBy { it.wakeTime.toLocalDate() }
        return WeekDay.entries.mapIndexed { index, weekDay ->
            val day = monday.plusDays(index.toLong())
            val minutes = if (day.isAfter(today)) 0
            else byDate[day]?.sumOf { it.durationMinutes } ?: 0
            DailySleepEntry(weekDay.abbreviation, minutes, isHighlighted = day == today)
        }
    }

    private fun buildWeeklyStages(
        current: List<SleepSession>,
        previous: List<SleepSession>,
    ): List<WeeklyStageData> {
        if (current.isEmpty()) return emptyStageList()
        val currAvg = averageStageMinutes(current)
        val prevAvg = if (previous.isEmpty()) null else averageStageMinutes(previous)
        return SleepStage.entries.map { stage ->
            val cur = currAvg[stage] ?: 0
            val prev = prevAvg?.get(stage) ?: 0
            val trend = when {
                prevAvg == null -> null
                cur > prev -> StageTrend.Up
                cur < prev -> StageTrend.Down
                else -> null
            }
            WeeklyStageData(stage = stage, durationMinutes = cur, trend = trend)
        }
    }

    private fun averageStageMinutes(sessions: List<SleepSession>): Map<SleepStage, Int> {
        val totals = mutableMapOf<SleepStage, Int>()
        sessions.forEach { session ->
            session.stages.forEach { interval ->
                totals.merge(interval.stage, interval.durationMinutes, Int::plus)
            }
        }
        return totals.mapValues { (_, total) -> total / sessions.size }
    }

    private fun emptyStageList() = listOf(
        WeeklyStageData(SleepStage.Deep, 0),
        WeeklyStageData(SleepStage.REM, 0),
        WeeklyStageData(SleepStage.Light, 0),
        WeeklyStageData(SleepStage.Awake, 0),
    )

    private fun avgMinutes(entries: List<DailySleepEntry>): Int {
        val nonEmpty = entries.filter { it.durationMinutes > 0 }
        return if (nonEmpty.isEmpty()) 0 else nonEmpty.sumOf { it.durationMinutes } / nonEmpty.size
    }

    private suspend fun loadBestSessionFromNeighborRange(targetDay: LocalDate) {
        val rangeResult = withTimeoutOrNull(DAY_LOAD_TIMEOUT_MS) {
            sleepRepository.getSessionsForRange(
                targetDay.minusDays(SLEEP_DAY_FALLBACK_RANGE_DAYS + 1),
                targetDay.plusDays(SLEEP_DAY_FALLBACK_RANGE_DAYS),
            )
        }
        val fallback = rangeResult
            ?.getOrNull()
            .orEmpty()
            .bestForTargetDay(targetDay)
        if (fallback.hasAnySleepData()) applySession(fallback!!) else applyEmptySession()
    }

    private fun List<SleepSession>.bestForTargetDay(targetDay: LocalDate): SleepSession? =
        minWithOrNull(
            compareBy<SleepSession> { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(it.wakeTime.toLocalDate(), targetDay)) }
                .thenByDescending { it.sleepMagnitude() },
        )

    private fun SleepSession?.hasAnySleepData(): Boolean =
        this != null && (durationMinutes > 0 || stages.isNotEmpty())

    private fun SleepSession.sleepMagnitude(): Long =
        durationMinutes.toLong() + hrvMs.toLong() + restingHeartRateBpm.toLong()

    private companion object {
        private const val DAY_LOAD_TIMEOUT_MS = 12_000L
        private const val SLEEP_DAY_FALLBACK_RANGE_DAYS = 1L
    }
}
