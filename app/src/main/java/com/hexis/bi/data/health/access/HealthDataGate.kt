package com.hexis.bi.data.health.access

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.subscription.ScanGateConfig
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.subscription.HealthDataAccess
import com.hexis.bi.domain.subscription.toHealthDataAccess
import com.hexis.bi.utils.constants.SubscriptionConstants
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

class HealthDataGate(
    private val repository: SubscriptionRepository,
    private val gateConfig: ScanGateConfig,
    private val preferences: UserPreferencesRepository,
    private val auth: FirebaseAuth,
) {

    val accessFlow: Flow<HealthDataAccess> = combine(repository.status, gateConfig.enabledFlow) { status, enabled ->
        if (enabled) status.toHealthDataAccess() ?: HealthDataAccess.FULL else HealthDataAccess.FULL
    }.distinctUntilChanged()

    val settledAccessFlow: Flow<HealthDataAccess> = combine(repository.status, gateConfig.enabledFlow) { status, enabled ->
        if (enabled) status.toHealthDataAccess() else HealthDataAccess.FULL
    }.filterNotNull().distinctUntilChanged()

    suspend fun refreshConfig() = gateConfig.refresh()

    suspend fun access(): HealthDataAccess {
        if (!gateConfig.enabled) return HealthDataAccess.FULL
        return withTimeoutOrNull(SubscriptionConstants.HEALTH_ACCESS_WAIT_MS) {
            repository.status.mapNotNull { it.toHealthDataAccess() }.first()
        } ?: HealthDataAccess.FULL
    }

    suspend fun openFreeWeek(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val anchor = freeWeekAnchor()
        if (anchor == null) {
            preferences.setFreeWeekAnchor(uid, LocalDate.now().toString())
            return true
        }
        return anchor == LocalDate.now() || !preferences.isFreeWeekImported(uid)
    }

    suspend fun recordFreeWeekImport(rows: Int): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val previous = preferences.getFreeWeekImportRows(uid)
        if (rows > 0 && previous == rows) {
            preferences.setFreeWeekImported(uid)
            return true
        }
        preferences.setFreeWeekImportRows(uid, rows)
        return false
    }

    private suspend fun freeWeekStillOpen(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val anchor = freeWeekAnchor()
        return anchor == null || anchor == LocalDate.now() || !preferences.isFreeWeekImported(uid)
    }

    suspend fun allowedBounds(): ClosedRange<LocalDate>? = when (access()) {
        HealthDataAccess.FULL -> LocalDate.MIN..LocalDate.MAX
        HealthDataAccess.FROZEN -> null
        HealthDataAccess.FIRST_WEEK -> freeWeekAnchor()?.let {
            it.minusDays(SubscriptionConstants.FREE_WEEK_DAYS - 1)..it
        }
    }

    suspend fun sdkLookbackDays(reason: String, force: Boolean, requested: Long): Long? = when (access()) {
        HealthDataAccess.FULL -> requested
        HealthDataAccess.FIRST_WEEK ->
            if (force && reason == SubscriptionConstants.TERRA_POST_CONNECT_REASON && freeWeekStillOpen()) {
                SubscriptionConstants.FREE_WEEK_DAYS
            } else {
                null
            }

        HealthDataAccess.FROZEN -> null
    }

    private suspend fun freeWeekAnchor(): LocalDate? {
        val uid = auth.currentUser?.uid ?: return null
        return preferences.getFreeWeekAnchor(uid)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
}
