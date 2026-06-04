package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.domain.body.physiqueScore
import com.hexis.bi.domain.longevity.PaceOfAgingInputs
import com.hexis.bi.domain.longevity.agingScore
import com.hexis.bi.domain.longevity.computePaceOfAging
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.ui.main.home.longevity.currentLongevityScore
import com.hexis.bi.ui.main.home.longevity.longevityScoreWindow
import com.hexis.bi.ui.main.home.longevity.waistToHeightRatio
import com.hexis.bi.ui.main.scan.results.MeasurementChange
import com.hexis.bi.utils.calculateAge
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.DateFormatConstants
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.inchesToFeetAndInches
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs

sealed interface HomeEvent : UiEvent {
    data object NavigateToLogin : HomeEvent
}

class HomeViewModel(
    application: Application,
    private val userRepository: UserRepository,
    suitRepository: SuitRepository,
    private val sleepRepository: SleepRepository,
    private val activityRepository: ActivityRepository,
    private val recoveryRepository: RecoveryRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val terraManagerHolder: TerraManagerHolder,
    notificationInbox: NotificationInboxRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    /** Pokes the overview pipeline; replay-less since every Home RESUME re-pokes. */
    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        combine(
            userRepository.observeUser(),
            userRepository.observeUserSettings(),
        ) { profile, settings ->
            profile to settings
        }
            .onEach { (profile, settings) ->
                val isMetric = profile.unitSystem.isMetricUnitSystem()
                val goalHours = settings.sleepGoalHours ?: SleepConstants.DEFAULT_SLEEP_GOAL_HOURS
                val activityGoalSteps = settings.stepsGoal ?: ActivityConstants.DEFAULT_STEP_GOAL
                _state.update { current ->
                    current.copy(
                        userName = "${profile.firstName} ${profile.lastName}".trim(),
                        imageUrl = profile.imageUrl,
                        weight = if (isMetric)
                            profile.weightKg?.let {
                                appContext.getString(R.string.unit_weight_kg, it)
                            }
                        else
                            profile.weightLb?.let {
                                appContext.getString(R.string.unit_weight_lb, it)
                            },
                        height = if (isMetric)
                            profile.heightCm?.let {
                                appContext.getString(R.string.unit_height_cm, it)
                            }
                        else
                            profile.heightIn?.let {
                                val (ft, inches) = it.inchesToFeetAndInches()
                                appContext.getString(R.string.unit_height_ft_in, ft, inches)
                            },
                        age = profile.dateOfBirth?.calculateAge()?.toString(),
                        sleepGoalHours = goalHours,
                        activityGoalSteps = activityGoalSteps,
                        sleep = current.sleep.copy(goalHours = goalHours),
                    )
                }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        suitRepository.connectionState
            .onEach { info ->
                _state.update { current ->
                    current.copy(isSuitConnected = info != null)
                }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        notificationInbox.unreadCount
            .onEach { count ->
                _state.update { it.copy(hasUnreadNotifications = count > 0) }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        // A poke (RESUME/manual) reads through the repo caches; a Terra sync (dataSynced) bumps the
        // cache generation, so the same read transparently refetches the just-synced data.
        merge(refreshTrigger, TerraSdkSync.dataSynced)
            .onEach { reloadOverview() }
            .launchIn(viewModelScope)
    }

    /** Re-derives every overview card. Called on each Home RESUME; Terra syncs also trigger it. */
    fun refreshOverview() {
        refreshTrigger.tryEmit(Unit)
    }

    /**
     * Re-derives all overview cards from one consistent read so recovery, activity and the longevity
     * score (which shares their data) can't drift apart. Errors degrade the affected card to its
     * empty state rather than blocking the screen — individual reads already return null on failure.
     */
    private suspend fun reloadOverview() {
        try {
            coroutineScope {
                val today = LocalDate.now()
                val window = longevityScoreWindow(today)
                val windowStart = window.first()

                val profileDeferred = async { userRepository.getUser().getOrNull() }
                val scanListDeferred = async {
                    scanHistoryRepository
                        .getRecentScans(SCAN_TREND_LIMIT, ScanFetchProjection.LIST_SUMMARY)
                        .getOrNull()
                }
                val terraDeferred = async {
                    loadTerraOverview(today, windowStart)
                }

                val profile = profileDeferred.await()
                val isMetric = profile?.unitSystem.isMetricUnitSystem()
                val heightCm = profile?.heightCm?.toFloat()
                val scans = scanListDeferred.await()
                publishScanOverview(scans, isMetric)
                // physiqueScore needs a FULL scan: shoulders comes from front_linear_params, so the
                // LIST_SUMMARY `scans` record (circumference only) would drop the Proportion component
                // and yield a different score than the Physique Drift screen. Refetch the latest in full.
                val latestFullScan = scans?.firstOrNull()?.id
                    ?.let { scanHistoryRepository.getScanRecordById(it).getOrNull() }
                _state.update { it.copy(physiqueScore = latestFullScan?.physiqueScore(heightCm)) }

                val terra = terraDeferred.await()
                val latestScan = scans?.firstOrNull()
                val todayActivity = terra.activity.firstOrNull { it.date == today }
                val todayRecovery = terra.recovery.firstOrNull { it.date == today }
                val longevityScore =
                    currentLongevityScore(window, terra.recovery, terra.activity, latestScan, heightCm)
                val pace = computePaceOfAging(
                    PaceOfAgingInputs(
                        hrvMs = todayRecovery?.hrvMs,
                        restingHeartRateBpm = todayRecovery?.restingHeartRateBpm,
                        sleepScore = todayRecovery?.sleepScore,
                        recoveryScore = todayRecovery?.score,
                        steps = todayActivity?.steps,
                        bodyFatPercent = latestScan?.fatPercentage,
                        waistToHeightRatio = waistToHeightRatio(latestScan, heightCm),
                        vo2Max = todayActivity?.vo2MaxMlPerMinPerKg,
                        stressLevel = todayRecovery?.stressLevel,
                    )
                )?.pace

                _state.update {
                    it.copy(
                        recoveryScore = (todayRecovery?.score ?: 0).coerceAtLeast(0),
                        longevityScore = longevityScore,
                        paceOfAgingValue = pace?.let { p -> String.format(Locale.US, PACE_FORMAT, p) },
                        paceOfAgingScore = pace?.let { p -> agingScore(p) },
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setError(e.message)
        }
    }

    private suspend fun loadTerraOverview(today: LocalDate, windowStart: LocalDate): TerraOverview {
        terraManagerHolder.awaitCurrentOrTimeout()
        return coroutineScope {
            val sleepDeferred = async {
                sleepRepository.getSessionsForRange(windowStart.minusDays(1), today).getOrNull().orEmpty()
            }
            val activityDeferred = async {
                activityRepository.getSummariesForRange(windowStart, today).getOrNull().orEmpty()
            }

            val sleep = sleepDeferred.await()
            val activity = activityDeferred.await()
            val sleepSession = sleep.minByOrNull {
                abs(ChronoUnit.DAYS.between(it.wakeTime.toLocalDate(), today))
            }
            val todayActivity = activity.firstOrNull { it.date == today }
            val steps = (todayActivity?.steps ?: 0).coerceAtLeast(0)
            val hourlySteps = todayActivity?.let { s ->
                (0 until ActivityConstants.HOURS_IN_DAY).map { (s.hourlySteps[it] ?: 0).toFloat() }
            }.orEmpty()

            _state.update {
                it.copy(
                    sleep = it.sleep.copy(
                        durationMinutes = (sleepSession?.durationMinutes ?: 0).coerceAtLeast(0),
                        goalHours = it.sleepGoalHours,
                    ),
                    activity = ActivityOverview(
                        steps = formatSteps(steps),
                        hourlySteps = hourlySteps,
                    ),
                )
            }

            val recovery = recoveryRepository.getSnapshotsForRange(windowStart, today).getOrNull().orEmpty()
            TerraOverview(activity = activity, recovery = recovery)
        }
    }

    private fun publishScanOverview(scans: List<ScanRecord>?, isMetric: Boolean) {
        val scanDate = scans?.firstOrNull()?.let {
            SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY, Locale.getDefault())
                .format(Date(it.timestamp))
        }
        _state.update {
            it.copy(
                scan = buildScanOverview(scans, isMetric),
                latestScanDate = scanDate,
            )
        }
    }

    private fun buildScanOverview(scans: List<ScanRecord>?, isMetric: Boolean): ScanOverview {
        val latest = scans?.getOrNull(0) ?: return ScanOverview(
            value = appContext.getString(R.string.stat_unknown),
            subtitle = appContext.getString(R.string.home_scan_no_data),
        )
        val dateLabel = SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY, Locale.getDefault())
            .format(Date(latest.timestamp))
        val topChange = MeasurementMapper.topChangeVsPreviousScan(latest, scans.getOrNull(1))
            ?: return ScanOverview(
                value = dateLabel,
                subtitle = appContext.getString(R.string.home_scan_last_scan),
            )
        val magnitude = abs(if (isMetric) topChange.deltaCm else topChange.deltaCm.cmToInches())
        val unit = appContext.getString(if (isMetric) R.string.unit_cm else R.string.unit_in)
        val arrow = appContext.getString(
            if (topChange.deltaCm < 0f) R.string.home_scan_arrow_down else R.string.home_scan_arrow_up
        )
        return ScanOverview(
            value = String.format(Locale.US, "%.1f", magnitude),
            unit = unit,
            valueLabel = appContext.getString(
                R.string.home_scan_change_label, arrow, appContext.getString(topChange.bodyPartRes),
            ),
            subtitle = appContext.getString(R.string.home_scan_key_change, dateLabel),
            changePositive = topChange.change?.let { it == MeasurementChange.Positive },
            trend = scanTrend(scans, topChange.region),
        )
    }

    /** Measurement values (oldest → newest) for the most-changed region, for the sparkline. */
    private fun scanTrend(scans: List<ScanRecord>, region: BodyMeasurementRegion): List<Float> =
        scans.reversed().mapNotNull { BodyMeasurementKeys.valueFor(it.measurements, region) }

    private fun formatSteps(steps: Int): String = "%,d".format(steps.coerceAtLeast(0))

    private companion object {
        const val SCAN_TREND_LIMIT = 8L
        const val PACE_FORMAT = "%.2fx"
    }

    private data class TerraOverview(
        val activity: List<com.hexis.bi.data.activity.ActivitySummary>,
        val recovery: List<com.hexis.bi.data.recovery.RecoverySnapshot>,
    )
}
