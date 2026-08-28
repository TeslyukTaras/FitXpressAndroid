package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.BuildConfig
import com.hexis.bi.R
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.health.sync.HealthSyncScheduler
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.scan.MeasurementMapper
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.aggregateSleepSessionsForWakeDay
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.domain.longevity.LongevityCalculator
import com.hexis.bi.domain.longevity.PaceOfAgingInputs
import com.hexis.bi.domain.longevity.agingScore
import com.hexis.bi.domain.longevity.computePaceOfAging
import com.hexis.bi.domain.order.Order
import com.hexis.bi.domain.order.OrderRepository
import com.hexis.bi.domain.order.OrderShippingAddress
import com.hexis.bi.domain.order.OrderStatus
import com.hexis.bi.domain.recomposition.RecompositionCalculator
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.main.home.intelligence.EngineFindingsMapper
import com.hexis.bi.ui.main.home.intelligence.BackfillTransition
import com.hexis.bi.ui.main.home.intelligence.backfillTransition
import com.hexis.bi.domain.intelligence.IntelligenceCoordinator
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.ui.main.buysuit.orderdetails.OrderDetailsUi
import com.hexis.bi.ui.main.buysuit.orderdetails.OrderTimelineStepUi
import com.hexis.bi.ui.main.home.longevity.longevityScoreWindow
import com.hexis.bi.ui.main.home.longevity.waistToHeightRatio
import com.hexis.bi.ui.main.scan.results.MeasurementChange
import com.hexis.bi.utils.calculateAge
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.FindingMetricAliases
import com.hexis.bi.utils.constants.LongevityFoundationConstants
import com.hexis.bi.utils.constants.OrderConstants
import com.hexis.bi.utils.constants.RecompositionConstants
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.inchesToFeetAndInches
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.millisToOrderTimelineTimestamp
import com.hexis.bi.utils.millisToShortMonthDay
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

sealed interface HomeEvent : UiEvent {
    data object NavigateToLogin : HomeEvent
}

@OptIn(FlowPreview::class)
class HomeViewModel internal constructor(
    application: Application,
    private val userRepository: UserRepository,
    suitRepository: SuitRepository,
    private val sleepRepository: SleepRepository,
    private val activityRepository: ActivityRepository,
    private val recoveryRepository: RecoveryRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val terraManagerHolder: TerraManagerHolder,
    notificationInbox: NotificationInboxRepository,
    private val orderRepository: OrderRepository,
    private val firebaseAuth: FirebaseAuth,
    private val healthSyncScheduler: HealthSyncScheduler,
    private val intelligenceCoordinator: IntelligenceCoordinator,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private fun loadInsights(clearUpdatingOnComplete: Boolean = false) {
        if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) {
            _state.update {
                it.copy(
                    insightsUpdating = if (clearUpdatingOnComplete) false else it.insightsUpdating,
                )
            }
            return
        }
        intelligenceCoordinator.refreshNow()
    }

    /** Pokes the overview pipeline; replay-less since every Home RESUME re-pokes. */
    private var terraTileContext: TerraTileContext? = null
    private val overviewMutex = Mutex()
    private var backfillInFlight = false
    private var activeUserId: String? = firebaseAuth.currentUser?.uid
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val newUserId = auth.currentUser?.uid
        if (newUserId == activeUserId) return@AuthStateListener
        activeUserId = newUserId
        terraTileContext = null
        _state.value = HomeState()
        if (newUserId != null) {
            loadInsights()
            refreshTrigger.tryEmit(Unit)
        }
    }

    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        loadInsights()
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

        merge(activityRepository.updates, sleepRepository.updates)
            .debounce(CanonicalCacheConstants.UPDATE_DEBOUNCE_MS)
            .onEach {
                onHealthDataChanged()
                if (!backfillInFlight) loadInsights()
            }
            .launchIn(viewModelScope)

        scanHistoryRepository.updates
            .debounce(CanonicalCacheConstants.UPDATE_DEBOUNCE_MS)
            .onEach {
                reloadOverview()
                if (!backfillInFlight) loadInsights()
            }
            .launchIn(viewModelScope)

        healthSyncScheduler.backfillInFlight()
            .onEach { inFlight ->
                val transition = backfillTransition(backfillInFlight, inFlight)
                val settled = transition == BackfillTransition.SETTLED
                backfillInFlight = inFlight
                when (transition) {
                    BackfillTransition.ACTIVE -> _state.update { it.copy(insightsUpdating = true) }
                    BackfillTransition.SETTLED -> loadInsights(clearUpdatingOnComplete = true)
                    BackfillTransition.IDLE -> _state.update { it.copy(insightsUpdating = false) }
                }
                terraTileContext?.let { refreshSyncingState(it.today, it.window.first()) }
                if (settled) refreshTerraTiles()
            }
            .launchIn(viewModelScope)

        intelligenceCoordinator.state
            .onEach { reportState ->
                val run = reportState.run
                _state.update {
                    it.copy(
                        insights = run?.let { current ->
                            EngineFindingsMapper.simpleFindings(
                                report = current.report,
                                copy = current.copy,
                                windowDays = current.config.windows.analysisDays,
                                isMetric = current.isMetric,
                            )
                        } ?: it.insights,
                        insightsUpdating = reportState.updating,
                        physiqueScore = run?.report?.latestValues
                            ?.get(FindingMetricAliases.PHYSIQUE_SCORE_METRIC)
                            ?.toFloat() ?: it.physiqueScore,
                    )
                }
            }
            .launchIn(viewModelScope)

        refreshTrigger
            .onEach { loadSuitOrder() }
            .launchIn(viewModelScope)

        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        firebaseAuth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }

    private suspend fun loadSuitOrder() {
        val order = orderRepository.getLatestActiveOrder().getOrNull()?.takeIf { it.isShownOnHome }
        _state.update { current ->
            current.copy(
                suitOrder = order?.toOverview(),
                orderDetails = order?.toDetailsUi(),
                suitSectionResolved = true,
                showOrderDetails = current.showOrderDetails && order != null,
            )
        }
    }

    fun showOrderDetails() =
        _state.update { it.copy(showOrderDetails = it.orderDetails != null) }

    fun dismissOrderDetails() =
        _state.update { it.copy(showOrderDetails = false) }

    fun showMedicalDisclaimer() =
        _state.update { it.copy(showMedicalDisclaimer = true) }

    fun dismissMedicalDisclaimer() =
        _state.update { it.copy(showMedicalDisclaimer = false) }

    /** The card tracks the order until the delivered suit is activated (linked via suitId). */
    private val Order.isShownOnHome: Boolean
        get() = status != OrderStatus.CANCELLED &&
                (status != OrderStatus.DELIVERED || suitId == null)

    private fun Order.toOverview(): SuitOrderOverview {
        val hasTracking = trackingNumber != null
        return SuitOrderOverview(
            status = appContext.getString(status.displayRes()),
            referenceLabel = appContext.getString(
                if (hasTracking) R.string.home_suit_order_tracking_label
                else R.string.home_suit_order_number_label
            ),
            referenceValue = maskReference(trackingNumber ?: orderNumber),
            eta = estimatedDeliveryMillis?.millisToShortMonthDay(),
        )
    }

    private fun Order.toDetailsUi(): OrderDetailsUi =
        OrderDetailsUi(
            orderId = id,
            reference = trackingNumber ?: orderNumber,
            referenceIsTracking = trackingNumber != null,
            eta = estimatedDeliveryMillis?.millisToShortMonthDay(),
            steps = statusHistory.map { event ->
                OrderTimelineStepUi(
                    label = appContext.getString(event.status.displayRes()),
                    timestamp = (event.atMillis ?: event.estimatedAtMillis)
                        ?.millisToOrderTimelineTimestamp(),
                    reached = event.atMillis != null,
                )
            },
            address = effectiveShippingAddress.toSingleLine(),
            canEditAddress = isAddressEditable,
            addressChangePending = hasPendingAddressChange,
        )

    private fun OrderShippingAddress.toSingleLine(): String =
        listOf(addressLine, apartment, city, region, postalCode, countryName)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    private fun OrderStatus.displayRes(): Int = when (this) {
        OrderStatus.PLACED -> R.string.order_status_placed
        OrderStatus.CONFIRMED -> R.string.order_status_confirmed
        OrderStatus.IN_PRODUCTION -> R.string.order_status_in_production
        OrderStatus.SHIPPED -> R.string.order_status_shipped
        OrderStatus.DELIVERED -> R.string.order_status_delivered
        OrderStatus.CANCELLED -> R.string.order_status_cancelled
    }

    private fun maskReference(reference: String): String {
        val visible =
            OrderConstants.REFERENCE_MASK_PREFIX_CHARS + OrderConstants.REFERENCE_MASK_SUFFIX_CHARS
        if (reference.length <= visible) return reference
        return reference.take(OrderConstants.REFERENCE_MASK_PREFIX_CHARS) +
                OrderConstants.REFERENCE_MASK +
                reference.takeLast(OrderConstants.REFERENCE_MASK_SUFFIX_CHARS)
    }

    /** Re-derives every overview card. Called on each Home RESUME; Terra syncs also trigger it. */
    fun refreshOverview() {
        refreshTrigger.tryEmit(Unit)
        if (!backfillInFlight) loadInsights()
    }

    /**
     * Re-derives all overview cards from one consistent read so recovery, activity and the longevity
     * score (which shares their data) can't drift apart. Errors degrade the affected card to its
     * empty state rather than blocking the screen — individual reads already return null on failure.
     */
    private suspend fun reloadOverview() = overviewMutex.withLock {
        reloadOverviewLocked()
    }

    private suspend fun reloadOverviewLocked() {
        try {
            TerraSdkSync.invalidateCaches()
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
                val recompositionScansDeferred = async {
                    val windowStartMillis = today
                        .minusYears(RecompositionConstants.WINDOW_YEARS_LONG)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    scanHistoryRepository
                        .getScansSavedSince(windowStartMillis, ScanFetchProjection.LIST_SUMMARY)
                        .getOrNull()
                }
                val terraDeferred = async {
                    loadTerraOverview(today, windowStart)
                }

                val profile = profileDeferred.await()
                val isMetric = profile?.unitSystem.isMetricUnitSystem()
                val heightCm = profile?.heightCm?.toFloat()
                val scans = scanListDeferred.await()
                terraTileContext = TerraTileContext(today, window, scans?.firstOrNull(), heightCm)
                val recompositionScans = recompositionScansDeferred.await()
                publishScanOverview(scans, isMetric)
                publishRecompositionOverview(recompositionScans, today)
                publishLongevityDirection(recompositionScans, heightCm, today)
                val terra = terraDeferred.await()
                val latestScan = scans?.firstOrNull()
                applyTerraTiles(today, terra, latestScan, heightCm)
                refreshSyncingState(today, windowStart)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            setError(e.message)
        }
    }

    private suspend fun onHealthDataChanged() {
        if (!backfillInFlight) return refreshTerraTiles()
        val context = terraTileContext ?: return
        val wasSyncing = _state.value.isAnyHealthDataSyncing
        refreshSyncingState(context.today, context.window.first())
        if (wasSyncing && !_state.value.isAnyHealthDataSyncing) refreshTerraTiles()
    }

    private suspend fun refreshTerraTiles() = overviewMutex.withLock {
        val context = terraTileContext ?: return@withLock reloadOverviewLocked()
        try {
            val terra = loadTerraOverview(context.today, context.window.first())
            applyTerraTiles(context.today, terra, context.latestScan, context.heightCm)
            refreshSyncingState(context.today, context.window.first())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Cached health refresh failed; keeping the tiles already shown")
        }
    }

    private fun applyTerraTiles(
        today: LocalDate,
        terra: TerraOverview,
        latestScan: ScanRecord?,
        heightCm: Float?,
    ) {
        val todayActivity = terra.activity.firstOrNull { it.date == today }
        val todayRecovery = terra.recovery.firstOrNull { it.date == today }
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
                paceOfAgingValue = pace?.let { p ->
                    String.format(
                        Locale.US,
                        PACE_FORMAT,
                        p
                    )
                },
                paceOfAgingScore = pace?.let { p -> agingScore(p) },
            )
        }
    }

    /**
     * Sleep is filed under its wake day, so its window opens a day earlier than activity's — the
     * same offset [loadTerraOverview] reads with. Local-cache read only; no extra requests.
     */
    private suspend fun refreshSyncingState(today: LocalDate, windowStart: LocalDate) {
        val activitySyncing =
            backfillInFlight && activityRepository.coverage(windowStart, today).isPartial
        val sleepSyncing =
            backfillInFlight && sleepRepository.coverage(windowStart.minusDays(1), today).isPartial
        _state.update {
            it.copy(isActivitySyncing = activitySyncing, isSleepSyncing = sleepSyncing)
        }
    }

    private data class TerraTileContext(
        val today: LocalDate,
        val window: List<LocalDate>,
        val latestScan: ScanRecord?,
        val heightCm: Float?,
    )

    private suspend fun loadTerraOverview(today: LocalDate, windowStart: LocalDate): TerraOverview {
        terraManagerHolder.awaitCurrentOrTimeout()
        return coroutineScope {
            val sleepDeferred = async {
                sleepRepository.getSessionsForRange(windowStart.minusDays(1), today)
                    .getOrNull()
                    .orEmpty()
            }
            val activityDeferred = async {
                activityRepository.getSummariesForRange(windowStart, today)
                    .getOrNull()
                    .orEmpty()
            }

            val sleep = sleepDeferred.await()
            val activity = activityDeferred.await()
            val sleepSession = aggregateSleepSessionsForWakeDay(
                sleep.filter { it.wakeTime.toLocalDate() == today },
            )
            val todayActivity = activity.firstOrNull { it.date == today }
            val steps = (todayActivity?.steps ?: 0).coerceAtLeast(0)
            val hourlySteps = todayActivity?.let { s ->
                (0 until ActivityConstants.HOURS_IN_DAY).map { (s.hourlySteps[it] ?: 0).toFloat() }
            }.orEmpty()

            _state.update {
                it.copy(
                    sleep = it.sleep.copy(
                        durationMinutes = (sleepSession?.timeInBedMinutes ?: 0).coerceAtLeast(0),
                        goalHours = it.sleepGoalHours,
                    ),
                    activity = ActivityOverview(
                        steps = formatSteps(steps),
                        hourlySteps = hourlySteps,
                    ),
                )
            }

            val recovery =
                recoveryRepository.getSnapshotsForRange(windowStart, today).getOrNull().orEmpty()
            TerraOverview(activity = activity, recovery = recovery)
        }
    }

    private fun publishScanOverview(scans: List<ScanRecord>?, isMetric: Boolean) {
        val scanDate = scans?.firstOrNull()?.timestamp?.millisToShortMonthDay()
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
        val dateLabel = latest.timestamp.millisToShortMonthDay()
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

    private fun publishRecompositionOverview(scans: List<ScanRecord>?, today: LocalDate) {
        val result = RecompositionCalculator.buildWindow(
            scans = scans.orEmpty(),
            windowStart = today.minusYears(RecompositionConstants.WINDOW_YEARS_LONG),
            windowEnd = today,
        )
        val recomposedKg = result.recomposedKg?.takeIf { it > 0f } ?: 0f
        _state.update {
            it.copy(
                recompositionValue = String.format(Locale.US, RECOMPOSITION_FORMAT, recomposedKg),
            )
        }
    }

    private fun publishLongevityDirection(
        scans: List<ScanRecord>?,
        heightCm: Float?,
        today: LocalDate,
    ) {
        val direction = LongevityCalculator.evaluateBody(
            scans = scans.orEmpty(),
            heightCm = heightCm,
            windowStart = today.minusWeeks(LongevityFoundationConstants.WINDOW_WEEKS_SHORT),
            windowEnd = today,
        ).direction
        _state.update { it.copy(longevityDirection = direction) }
    }

    private fun formatSteps(steps: Int): String = "%,d".format(steps.coerceAtLeast(0))

    private companion object {
        const val SCAN_TREND_LIMIT = 8L
        const val PACE_FORMAT = "%.2fx"
        const val RECOMPOSITION_FORMAT = "%.1f"
    }

    private data class TerraOverview(
        val activity: List<com.hexis.bi.data.activity.ActivitySummary>,
        val recovery: List<com.hexis.bi.data.recovery.RecoverySnapshot>,
    )
}
