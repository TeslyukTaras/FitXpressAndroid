package com.hexis.bi.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

suspend fun FirebaseRemoteConfig.fetchAndActivate(minimumFetchIntervalSeconds: Long) {
    setConfigSettingsAsync(remoteConfigSettings { minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds }).await()
    fetchAndActivate().await()
}
