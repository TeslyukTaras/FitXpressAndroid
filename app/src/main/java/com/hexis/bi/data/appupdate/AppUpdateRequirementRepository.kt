package com.hexis.bi.data.appupdate

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.hexis.bi.utils.constants.AppUpdateRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

internal class AppUpdateRequirementRepository(
    private val remoteConfig: FirebaseRemoteConfig,
    private val versionName: String,
    private val minimumFetchIntervalSeconds: Long,
) {

    private val _updateRequired = MutableStateFlow(false)
    val updateRequired: StateFlow<Boolean> = _updateRequired.asStateFlow()

    init {
        publishRequirement()
    }

    suspend fun refresh() {
        publishRequirement()
        runCatching {
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings {
                    minimumFetchIntervalInSeconds = minimumFetchIntervalSeconds
                },
            ).await()
            remoteConfig.fetchAndActivate().await()
        }.onFailure { Timber.w(it, "Minimum version fetch failed; reading last activated values") }
        publishRequirement()
    }

    private fun publishRequirement() {
        val minimumVersion = remoteConfig.getString(AppUpdateRemoteConfig.MINIMUM_VERSION_KEY)
        val required = isBelowMinimumVersion(minimumVersion)
        if (required != _updateRequired.value) {
            Timber.i(
                "Update gate: required=%b for %s, minimum %s",
                required,
                versionName,
                minimumVersion.ifBlank { "unset" },
            )
        }
        _updateRequired.value = required
    }

    private fun isBelowMinimumVersion(minimum: String): Boolean {
        val required = numericVersionParts(minimum) ?: return false
        val installed = numericVersionParts(versionName) ?: return false
        return compareVersions(installed, required) < 0
    }
}

private fun numericVersionParts(raw: String): List<Int>? =
    raw.trim()
        .takeWhile { it.isDigit() || it == '.' }
        .split('.')
        .mapNotNull { it.toIntOrNull() }
        .ifEmpty { null }

private fun compareVersions(installed: List<Int>, required: List<Int>): Int {
    repeat(maxOf(installed.size, required.size)) { index ->
        val difference = installed.getOrElse(index) { 0 } - required.getOrElse(index) { 0 }
        if (difference != 0) return difference
    }
    return 0
}
