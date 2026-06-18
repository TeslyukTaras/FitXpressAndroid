package com.hexis.bi.ui.main.home.sleep

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.SleepSample
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.data.sleep.SleepStageInterval
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.theme.SleepStageAwake
import com.hexis.bi.ui.theme.SleepStageDeep
import com.hexis.bi.ui.theme.SleepStageLight
import com.hexis.bi.ui.theme.SleepStageRem
import com.hexis.bi.utils.formatFullMonthDay
import com.hexis.bi.utils.formatShortDateRange
import com.hexis.bi.utils.weekDayAbbreviation
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
import java.time.Duration
import java.time.LocalDate
import kotlin.math.roundToInt
import timber.log.Timber

class SleepViewModel(
    application: Application,
    private val sleepRepository: SleepRepository,
    private val userRepository: UserRepository,
    private val sourceResolver: TerraRestSourceResolver,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    private var weekOffset = 0
    private var dayOffset = 0
    private val loadedTabs = mutableSetOf<SleepTab>()

    init {
        observeDataSource()
        userRepository.observeUserSettings()
            .onEach { settings ->
                val goal = settings.sleepGoalHours ?: SleepConstants.DEFAULT_SLEEP_GOAL_HOURS
                val settingsDataSource = settings.sleepDataSource
                _state.update { curr ->
                    curr.copy(
                        sleepGoalHours = goal,
                        sleepGoalHoursDraft = if (curr.showSettingsDialog) curr.sleepGoalHoursDraft else goal,
                        dataSource = settingsDataSource ?: curr.dataSource,
                    )
                }
            }
            .catch { e -> Timber.w(e, "observeUserSettings failed; keeping sleep goal defaults") }
            .launchIn(viewModelScope)

        loadDataForTab(_state.value.selectedTab)
    }

    private fun observeDataSource() {
        viewModelScope.launch {
            val provider = sourceResolver.resolveOrderedIdentities()
                .getOrDefault(emptyList())
                .firstOrNull()
                ?.provider
                ?: TerraProviders.HEALTH_CONNECT
            _state.update { it.copy(dataSource = provider) }
            userRepository.updateUserSettings(
                mapOf(FirestoreSchema.UserSettingsFields.SLEEP_DATA_SOURCE to provider)
            ).onFailure { setError(it.message) }
        }
    }

    fun selectTab(tab: SleepTab) {
        _state.update { it.copy(selectedTab = tab) }
        if (tab !in loadedTabs) loadDataForTab(tab)
    }

    private fun loadDataForTab(tab: SleepTab) {
        when (tab) {
            SleepTab.Day -> loadDaySession(dayOffset)
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

    fun retryLoad() = loadDaySession(dayOffset)

    fun retrySummaryLoad() = loadWeekData(weekOffset)

    fun previousDay() {
        dayOffset--
        loadDaySession(dayOffset)
    }

    fun nextDay() {
        if (dayOffset < 0) {
            dayOffset++
            loadDaySession(dayOffset)
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

    private fun loadDaySession(offset: Int) {
        val targetDay = LocalDate.now().plusDays(offset.toLong())
        val dateLabel = targetDay.formatFullMonthDay()
        _state.update {
            it.copy(
                dayLoadState = SleepLoadState.Loading,
                errorMessage = null,
                dayLabel = dateLabel,
                canGoNextDay = offset < 0,
            )
        }
        viewModelScope.launch {
            val result = withTimeoutOrNull(DAY_LOAD_TIMEOUT_MS) {
                sleepRepository.getSessionForNight(targetDay)
            }
            if (offset != dayOffset) return@launch
            (result ?: Result.success(null)).fold(
                onSuccess = { session ->
                    if (session.hasAnySleepData()) {
                        applySession(session!!)
                    } else {
                        if (offset == 0) loadBestSessionFromNeighborRange(targetDay) else applyEmptySession()
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
                stages = emptyStageData(),
                hrv = 0,
                restingHeartRate = 0,
                hrvSeries = emptyList(),
                rhrSeries = emptyList(),
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
                stages = buildStageData(session),
                hrv = session.hrvMs,
                restingHeartRate = session.restingHeartRateBpm,
                hrvSeries = buildSeries(session.hrvSamples, session),
                rhrSeries = buildSeries(session.heartRateSamples, session),
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

    private fun buildStageData(session: SleepSession): List<SleepStageData> {
        val byStage = session.stages.groupBy { it.stage }
        return STAGE_DISPLAY_ORDER.map { stage ->
            val intervals = byStage[stage].orEmpty()
            SleepStageData(
                stage = stage,
                durationMinutes = intervals.sumOf { it.durationMinutes },
                color = stageColor(stage),
                hrv = averageInIntervals(session.hrvSamples, intervals) ?: session.hrvMs,
                rhr = averageInIntervals(session.heartRateSamples, intervals)
                    ?: session.restingHeartRateBpm,
            )
        }
    }

    private fun emptyStageData(): List<SleepStageData> =
        STAGE_DISPLAY_ORDER.map { SleepStageData(it, durationMinutes = 0, color = stageColor(it)) }

    private fun stageColor(stage: SleepStage): Color = when (stage) {
        SleepStage.Deep -> SleepStageDeep
        SleepStage.REM -> SleepStageRem
        SleepStage.Light -> SleepStageLight
        SleepStage.Awake -> SleepStageAwake
    }

    /** Average value of [samples] whose timestamp falls inside any of [intervals], or null if none. */
    private fun averageInIntervals(
        samples: List<SleepSample>,
        intervals: List<SleepStageInterval>,
    ): Int? {
        if (samples.isEmpty() || intervals.isEmpty()) return null
        val values = samples
            .filter { sample ->
                intervals.any { !sample.time.isBefore(it.start) && sample.time.isBefore(it.end) }
            }
            .map { it.value }
        return if (values.isEmpty()) null else values.average().roundToInt()
    }

    /**
     * Maps real intra-night [samples] to chart points positioned by their fraction of the night.
     * Dense series are bucket-averaged down to [MAX_CHART_POINTS]; returns empty when the provider
     * reports no detailed samples, so the chart simply omits that line.
     */
    private fun buildSeries(
        samples: List<SleepSample>,
        session: SleepSession,
    ): List<ChartPoint> {
        val totalMinutes = Duration.between(session.bedtime, session.wakeTime).toMinutes()
            .toFloat()
            .coerceAtLeast(1f)
        val points = samples
            .map { sample ->
                val offset = Duration.between(session.bedtime, sample.time).toMinutes().toFloat()
                ChartPoint(fraction = (offset / totalMinutes).coerceIn(0f, 1f), value = sample.value)
            }
            .sortedBy { it.fraction }

        if (points.size <= MAX_CHART_POINTS) return points

        return points
            .groupBy { (it.fraction * MAX_CHART_POINTS).toInt().coerceAtMost(MAX_CHART_POINTS - 1) }
            .toSortedMap()
            .map { (_, bucket) ->
                ChartPoint(
                    fraction = bucket.map { it.fraction }.average().toFloat(),
                    value = bucket.map { it.value }.average().roundToInt(),
                )
            }
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
        // Rolling 7-day windows anchored to today: offset 0 = last 7 days, -1 = previous 7, …
        val end = LocalDate.now().plusDays(offset.toLong() * DAYS_PER_WEEK)
        val start = end.minusDays(DAYS_PER_WEEK - 1)
        val previousEnd = start.minusDays(1)
        val previousStart = previousEnd.minusDays(DAYS_PER_WEEK - 1)
        val label = formatShortDateRange(start, end)

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
                .getSessionsForRange(previousStart, end)
                .fold(
                    onSuccess = { sessions ->
                        val current = sessions.filter { session ->
                            val wakeDay = session.wakeTime.toLocalDate()
                            !wakeDay.isBefore(start) && !wakeDay.isAfter(end)
                        }
                        val previous = sessions.filter { session ->
                            val wakeDay = session.wakeTime.toLocalDate()
                            !wakeDay.isBefore(previousStart) && !wakeDay.isAfter(previousEnd)
                        }
                        val structure = buildWeekStructure(start, current)
                        val stages = buildWeeklyStages(current, previous)

                        loadedTabs.add(SleepTab.Summary)
                        _state.update {
                            it.copy(
                                summaryLoadState = SleepLoadState.Ready,
                                summaryErrorMessage = null,
                                weeklyStructure = structure,
                                weeklyStages = stages,
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

    private fun buildWeekStructure(
        start: LocalDate,
        sessions: List<SleepSession>,
    ): List<DailyStructure> {
        val today = LocalDate.now()
        val byDate = sessions.groupBy { it.wakeTime.toLocalDate() }
        return (0 until DAYS_PER_WEEK).map { index ->
            val day = start.plusDays(index)
            val daySessions = if (day.isAfter(today)) emptyList() else byDate[day].orEmpty()
            val stageMinutes = SleepStage.entries.associateWith { stage ->
                daySessions.sumOf { session ->
                    session.stages.filter { it.stage == stage }.sumOf { it.durationMinutes }
                }
            }
            DailyStructure(
                dayLabel = day.weekDayAbbreviation(),
                isHighlighted = day == today,
                stageMinutes = stageMinutes,
            )
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
        if (targetDay != LocalDate.now().plusDays(dayOffset.toLong())) return
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
        private const val MAX_CHART_POINTS = 96
        private const val DAYS_PER_WEEK = 7L
        private val STAGE_DISPLAY_ORDER =
            listOf(SleepStage.Deep, SleepStage.REM, SleepStage.Light, SleepStage.Awake)
    }
}
