package com.hexis.bi.domain.intelligence

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.BuildConfig
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.sync.HealthSyncScheduler
import com.hexis.bi.data.intelligence.IntelligenceConfigRepository
import com.hexis.bi.data.intelligence.observationWindow
import com.hexis.bi.data.terra.decodeTerraRestIdentity
import com.hexis.bi.utils.constants.IntelligenceCacheConstants
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal data class IntelligenceReportState(
    val run: IntelligenceRun? = null,
    val loading: Boolean = true,
    val updating: Boolean = false,
    val error: Throwable? = null,
)

/** App-scoped owner of intelligence readiness and report generation. */
@OptIn(FlowPreview::class)
internal class IntelligenceCoordinator(
    private val runIntelligence: RunIntelligenceUseCase,
    private val local: HealthLocalDataSource,
    private val configRepository: IntelligenceConfigRepository,
    private val auth: FirebaseAuth,
    healthSyncScheduler: HealthSyncScheduler,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(IntelligenceReportState())
    val state = _state.asStateFlow()

    private val refreshLock = Mutex()
    @Volatile private var backfillInFlight = false
    private var loadedUserId: String? = null
    private var lastRunAtMillis = 0L

    init {
        healthSyncScheduler.backfillInFlight()
            .onEach { active ->
                val settled = backfillInFlight && !active
                backfillInFlight = active
                _state.update {
                    it.copy(
                        loading = intelligenceLoading(hasReport = it.run != null, backfillInFlight = active),
                        updating = active && it.run != null,
                    )
                }
                if (settled) refresh(force = true)
            }
            .launchIn(scope)

        local.changes
            .debounce(500)
            .onEach { refresh(force = !backfillInFlight) }
            .launchIn(scope)

        scope.launch { refresh(force = true) }
    }

    fun refreshNow() {
        scope.launch { refresh(force = true) }
    }

    fun refreshIfStale() {
        scope.launch { refresh(force = false) }
    }

    private suspend fun refresh(force: Boolean) {
        refreshLock.withLock {
            if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) {
                _state.value = IntelligenceReportState(loading = false)
                return@withLock
            }
            val uid = auth.currentUser?.uid
            if (uid == null) {
                loadedUserId = null
                _state.value = IntelligenceReportState(loading = false)
                return@withLock
            }
            if (loadedUserId != uid) {
                loadedUserId = uid
                _state.value = IntelligenceReportState(updating = backfillInFlight)
            }

            val now = System.currentTimeMillis()
            val throttle = IntelligenceCacheConstants.BACKFILL_RERUN_INTERVAL.toMillis()
            if (!force && now - lastRunAtMillis < throttle) return@withLock

            val previous = _state.value.run
            _state.update {
                it.copy(loading = previous == null, updating = backfillInFlight, error = null)
            }
            runIntelligence().fold(
                onSuccess = { run ->
                    lastRunAtMillis = System.currentTimeMillis()
                    _state.value = IntelligenceReportState(
                        run = run,
                        loading = false,
                        updating = backfillInFlight,
                    )
                },
                onFailure = { error ->
                    Timber.w(error, "Intelligence refresh failed")
                    _state.update {
                        it.copy(
                            loading = intelligenceLoading(
                                hasReport = it.run != null,
                                backfillInFlight = backfillInFlight,
                            ),
                            error = error,
                        )
                    }
                },
            )
        }
    }

}

/**
 * A report is pending only while one can still arrive. Without this the UI shimmers forever for
 * anyone who has no report yet and no sync running.
 */
internal fun intelligenceLoading(hasReport: Boolean, backfillInFlight: Boolean): Boolean =
    !hasReport && backfillInFlight
