package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.Terra
import co.tryterra.terra.TerraManager
import co.tryterra.terra.enums.Connections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/** Holds the [TerraManager] after activity-scoped init. */
object TerraManagerHolder {

    private val _manager = MutableStateFlow<TerraManager?>(null)
    val manager: StateFlow<TerraManager?> = _manager.asStateFlow()

    val current: TerraManager? get() = _manager.value

    private var currentReferenceId: String? = null

    /** No-op when [referenceId] matches the previous successful init. */
    suspend fun init(activity: Activity, referenceId: String?): Result<TerraManager> {
        current?.let { existing ->
            if (currentReferenceId == referenceId) {
                return Result.success(existing)
            }
        }
        return suspendCancellableCoroutine { cont ->
            Timber.d(
                "Terra.instance(devId=%s, refIdSet=%s, env=%s)",
                TerraConfig.devId.short(),
                referenceId != null,
                TerraConfig.environment,
            )
            Terra.instance(TerraConfig.devId, referenceId, activity) { manager, error ->
                if (error != null || manager == null) {
                    Timber.e(
                        error,
                        "Terra init failed: type=%s message=%s",
                        error?.let { it::class.java.simpleName } ?: "null",
                        error?.message,
                    )
                    if (cont.isActive) {
                        cont.resume(
                            Result.failure(error ?: IllegalStateException("Null TerraManager"))
                        )
                    }
                } else {
                    _manager.value = manager
                    currentReferenceId = referenceId
                    Timber.d(
                        "Terra init success (env=%s, refIdSet=%s)",
                        TerraConfig.environment,
                        referenceId != null,
                    )
                    if (cont.isActive) cont.resume(Result.success(manager))
                }
            }
        }
    }

    private fun String.short(): String =
        if (length <= 8) "***" else "${take(4)}…${takeLast(4)}"

    fun isConnected(connection: Connections): Boolean =
        current?.getUserId(connection) != null
}
