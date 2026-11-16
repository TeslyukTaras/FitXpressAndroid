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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull

class HealthDataGate(
    private val repository: SubscriptionRepository,
    private val gateConfig: ScanGateConfig,
    private val preferences: UserPreferencesRepository,
    private val auth: FirebaseAuth,
) {

    val accessFlow: Flow<HealthDataAccess> = repository.status
        .map { status ->
            if (gateConfig.enabled) status.toHealthDataAccess() ?: HealthDataAccess.FULL else HealthDataAccess.FULL
        }
        .distinctUntilChanged()

    suspend fun access(): HealthDataAccess {
        if (!gateConfig.enabled) return HealthDataAccess.FULL
        return withTimeoutOrNull(SubscriptionConstants.HEALTH_ACCESS_WAIT_MS) {
            repository.status.mapNotNull { it.toHealthDataAccess() }.first()
        } ?: HealthDataAccess.FULL
    }

    suspend fun markFreeWeek() {
        val uid = auth.currentUser?.uid ?: return
        preferences.setFreeWeekAnchor(uid, LocalDate.now().toString())
    }

    suspend fun allowsRange(start: LocalDate, end: LocalDate): Boolean = when (access()) {
        HealthDataAccess.FULL -> true
        HealthDataAccess.FROZEN -> false
        HealthDataAccess.FIRST_WEEK -> {
            val anchor = freeWeekAnchor()
            anchor != null &&
                start >= anchor.minusDays(SubscriptionConstants.FREE_WEEK_DAYS) &&
                end <= anchor.plusDays(1)
        }
    }

    suspend fun sdkLookbackDays(reason: String, force: Boolean, requested: Long): Long? = when (access()) {
        HealthDataAccess.FULL -> requested
        HealthDataAccess.FIRST_WEEK ->
            if (force && reason == SubscriptionConstants.TERRA_POST_CONNECT_REASON) {
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
