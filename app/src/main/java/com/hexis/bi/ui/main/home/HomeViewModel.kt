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
import com.hexis.bi.domain.longevity.LongevityInputs
import com.hexis.bi.domain.longevity.computeLongevityScore
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.time.LocalDate
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

    /**
     * Pokes the overview pipeline. Replay-less: a poke before the collector is active is covered by
     * the next Home RESUME, which always pokes again.
     */
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

        // Single reactive pipeline for the overview cards. A poke (Home RESUME / manual) reads
        // through the 60s repo caches; a Terra sync emits on dataSynced and forces a fresh read so
        // newly-synced data shows without the user leaving the screen.
        merge(
            refreshTrigger.map { false },
            TerraSdkSync.dataSynced.map { true },
        )
            .onEach { forceFresh -> reloadOverview(forceFresh) }
            .launchIn(viewModelScope)
    }

    /**
     * Pokes the overview pipeline to re-derive every card. Call when Home is shown so values
     * refresh after returning from a detail screen; a Terra sync refreshes them automatically.
     */
    fun refreshOverview() {
        refreshTrigger.tryEmit(Unit)
    }

    /**
     * Re-derives last night's sleep, today's activity, recovery, the latest scan and the longevity
     * score from a **single** consistent read, then commits them in one state update. Recovery and
     * longevity share the same fetched snapshot, and activity feeds both its own card and longevity,
     * so the cards can no longer drift apart.
     *
     * @param forceFresh true after a Terra sync — invalidates the 60s repo caches so the read
     * reflects the data that was just pulled rather than a pre-sync cached value.
     */
    private suspend fun reloadOverview(forceFresh: Boolean) {
        try {
            terraManagerHolder.awaitCurrentOrTimeout()
            if (forceFresh) {
                sleepRepository.invalidate()
                activityRepository.invalidate()
                recoveryRepository.invalidate()
            }
            coroutineScope {
                val today = LocalDate.now()
                val profileDeferred = async { userRepository.getUser().getOrNull() }
                val sleepDeferred = async { sleepRepository.getSessionForNight(today).getOrNull() }
                val activityDeferred = async { activityRepository.getSummaryForDate(today).getOrNull() }
                val recoveryDeferred = async { recoveryRepository.getSnapshotForDate(today).getOrNull() }
                // LIST_SUMMARY carries the circumference subdoc — all the key-change + trend need.
                val scanListDeferred = async {
                    scanHistoryRepository
                        .getRecentScans(SCAN_TREND_LIMIT, ScanFetchProjection.LIST_SUMMARY)
                        .getOrNull()
                }
                // FULL latest scan: longevity needs fatPercentage, which LIST_SUMMARY omits.
                val latestFullScanDeferred = async {
                    scanHistoryRepository.getRecentScans(limit = 1L).getOrNull()?.firstOrNull()
                }

                val profile = profileDeferred.await()
                val isMetric = profile?.unitSystem.isMetricUnitSystem()
                val sleepSession = sleepDeferred.await()
                val activitySummary = activityDeferred.await()
                val recoverySnapshot = recoveryDeferred.await()
                val scans = scanListDeferred.await()
                val latestFullScan = latestFullScanDeferred.await()

                val steps = (activitySummary?.steps ?: 0).coerceAtLeast(0)
                val hourlySteps = activitySummary?.let { s ->
                    (0 until ActivityConstants.HOURS_IN_DAY).map { (s.hourlySteps[it] ?: 0).toFloat() }
                }.orEmpty()
                val scanDate = scans?.getOrNull(0)?.let {
                    SimpleDateFormat(DateFormatConstants.SHORT_MONTH_DAY, Locale.getDefault())
                        .format(Date(it.timestamp))
                }
                val longevityScore = computeLongevityScore(
                    LongevityInputs(
                        hrvMs = recoverySnapshot?.hrvMs,
                        restingHeartRateBpm = recoverySnapshot?.restingHeartRateBpm,
                        sleepScore = recoverySnapshot?.sleepScore,
                        steps = activitySummary?.steps,
                        bodyFatPercent = latestFullScan?.fatPercentage,
                        waistToHeightRatio = waistToHeightRatio(latestFullScan, profile?.heightCm?.toFloat()),
                        vo2Max = activitySummary?.vo2MaxMlPerMinPerKg,
                    )
                )

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
                        recoveryScore = (recoverySnapshot?.score ?: 0).coerceAtLeast(0),
                        scan = buildScanOverview(scans, isMetric),
                        latestScanDate = scanDate,
                        longevityScore = longevityScore,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setError(e.message)
        }
    }

    private fun waistToHeightRatio(scan: ScanRecord?, heightCm: Float?): Float? {
        val height = heightCm?.takeIf { it > 0f } ?: return null
        val waist = scan?.let {
            BodyMeasurementKeys.valueFor(it.measurements, BodyMeasurementRegion.Waist)
        } ?: return null
        return waist / height
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
    }
}
