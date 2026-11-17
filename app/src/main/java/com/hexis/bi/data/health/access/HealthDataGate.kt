package com.hexis.bi.data.health.access

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.subscription.ScanGateConfig
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.domain.subscription.HealthDataAccess
import com.hexis.bi.data.terra.SdkPullRange
import com.hexis.bi.domain.subscription.toHealthDataAccess
import com.hexis.bi.utils.constants.SubscriptionConstants
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

    suspend fun markFreeWeekImported() {
        val uid = auth.currentUser?.uid ?: return
        preferences.setFreeWeekImported(uid)
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

    suspend fun sdkPullRange(reason: String, force: Boolean, requestedLookbackDays: Long): SdkPullRange? = when (access()) {
        HealthDataAccess.FULL -> Instant.now().let { now -> SdkPullRange(now.minus(requestedLookbackDays, ChronoUnit.DAYS), now) }
        HealthDataAccess.FIRST_WEEK ->
            if (force && reason == SubscriptionConstants.TERRA_POST_CONNECT_REASON && freeWeekStillOpen()) {
                freeWeekPullRange()
            } else {
                null
            }

        HealthDataAccess.FROZEN -> null
    }

    private suspend fun freeWeekPullRange(): SdkPullRange {
        val anchor = freeWeekAnchor() ?: LocalDate.now()
        val zone = ZoneId.systemDefault()
        val start = anchor.minusDays(SubscriptionConstants.FREE_WEEK_DAYS - 1).atStartOfDay(zone).toInstant()
        val end = minOf(anchor.plusDays(1).atStartOfDay(zone).toInstant(), Instant.now())
        return SdkPullRange(start, end)
    }

    private suspend fun freeWeekAnchor(): LocalDate? {
        val uid = auth.currentUser?.uid ?: return null
        return preferences.getFreeWeekAnchor(uid)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
}
