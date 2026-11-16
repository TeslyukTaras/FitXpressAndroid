package com.hexis.bi.data.subscription

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.PlanInfo
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
private data class PlanEntry(
    val planId: String,
    val rank: Int,
    val scansPerMonth: Int,
    val badge: String? = null,
)

class PlanCatalog(
    private val remoteConfig: FirebaseRemoteConfig,
    private val minimumFetchIntervalSeconds: Long,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _plans = MutableStateFlow(defaults())
    val plans: StateFlow<Map<PlanId, PlanInfo>> = _plans.asStateFlow()

    init {
        publish()
    }

    suspend fun refresh() {
        runCatching {
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds },
            ).await()
            remoteConfig.fetchAndActivate().await()
        }.onFailure { Timber.w(it, "Plan catalog fetch failed; using the last activated values") }
        publish()
    }

    private fun publish() {
        val raw = remoteConfig.getString(SubscriptionConstants.REMOTE_CONFIG_PLANS_KEY)
        if (raw.isBlank()) return
        runCatching { parse(raw) }
            .onSuccess { _plans.value = it }
            .onFailure { Timber.w(it, "Ignoring an invalid %s value", SubscriptionConstants.REMOTE_CONFIG_PLANS_KEY) }
    }

    private fun parse(raw: String): Map<PlanId, PlanInfo> {
        val result = defaults().toMutableMap()
        json.decodeFromString<List<PlanEntry>>(raw).forEach { entry ->
            val id = PlanId.entries.firstOrNull { it.name.equals(entry.planId, ignoreCase = true) } ?: return@forEach
            if (entry.rank <= 0 || entry.scansPerMonth < 0) return@forEach
            result[id] = PlanInfo(
                id = id,
                rank = entry.rank,
                scansPerMonth = entry.scansPerMonth,
                mostPopular = entry.badge == SubscriptionConstants.PLAN_BADGE_MOST_POPULAR,
            )
        }
        return result
    }

    private fun defaults(): Map<PlanId, PlanInfo> =
        PlanId.entries.associateWith { PlanInfo(it, it.rank, it.scansPerMonth, mostPopular = it == PlanId.PROGRESS) }
}
