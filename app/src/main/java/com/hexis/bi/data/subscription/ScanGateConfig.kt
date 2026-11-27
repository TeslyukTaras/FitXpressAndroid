package com.hexis.bi.data.subscription

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.hexis.bi.utils.fetchAndActivate
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class ScanGateConfig(
    private val remoteConfig: FirebaseRemoteConfig,
    private val minimumFetchIntervalSeconds: Long,
) {

    val enabled: Boolean
        get() = remoteConfig.getBoolean(SubscriptionConstants.REMOTE_CONFIG_SCAN_GATE_KEY)

    private val _enabledFlow = MutableStateFlow(enabled)
    val enabledFlow: StateFlow<Boolean> = _enabledFlow.asStateFlow()

    suspend fun refresh() {
        withTimeoutOrNull(SubscriptionConstants.SCAN_ACCESS_WAIT_MS) {
            runCatching { remoteConfig.fetchAndActivate(minimumFetchIntervalSeconds) }
                .onFailure { Timber.w(it, "Scan gate config fetch failed; using the last activated value") }
        }
        _enabledFlow.value = enabled
    }
}
