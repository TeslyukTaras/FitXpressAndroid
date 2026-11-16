package com.hexis.bi.data.subscription

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class ScanGateConfig(
    private val remoteConfig: FirebaseRemoteConfig,
    private val minimumFetchIntervalSeconds: Long,
) {

    val enabled: Boolean
        get() = remoteConfig.getBoolean(SubscriptionConstants.REMOTE_CONFIG_SCAN_GATE_KEY)

    suspend fun refresh() {
        withTimeoutOrNull(SubscriptionConstants.SCAN_ACCESS_WAIT_MS) {
            runCatching {
                remoteConfig.setConfigSettingsAsync(
                    remoteConfigSettings { minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds },
                ).await()
                remoteConfig.fetchAndActivate().await()
            }
                .onFailure { Timber.w(it, "Scan gate config fetch failed; using the last activated value") }
        }
    }
}
