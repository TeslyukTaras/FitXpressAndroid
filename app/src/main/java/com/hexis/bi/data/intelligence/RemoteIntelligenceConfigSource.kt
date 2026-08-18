package com.hexis.bi.data.intelligence

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.hexis.bi.utils.constants.IntelligenceRemoteConfig
import kotlinx.coroutines.tasks.await
import timber.log.Timber

internal class RemoteIntelligenceConfigSource(
    private val remoteConfig: FirebaseRemoteConfig,
    private val minimumFetchIntervalSeconds: Long,
    private val key: String = IntelligenceRemoteConfig.CONFIG_KEY,
) : IntelligenceConfigSource {

    override val name = "remote:$key"

    override suspend fun load(): Result<String> = runCatching {
        runCatching {
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds
                },
            ).await()
            remoteConfig.fetchAndActivate().await()
        }.onFailure { Timber.w(it, "Remote config refresh failed; reading last activated value") }

        remoteConfig.getString(key).ifBlank { error("$key is empty or unset") }
    }
}
