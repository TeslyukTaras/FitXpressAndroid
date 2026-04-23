package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.Terra
import co.tryterra.terra.TerraManager
import co.tryterra.terra.enums.Connections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/** Holds the [TerraManager] after activity-scoped init. */
object TerraManagerHolder {

    private val _manager = MutableStateFlow<TerraManager?>(null)
    val manager: StateFlow<TerraManager?> = _manager.asStateFlow()

    val current: TerraManager? get() = _manager.value

    private var currentReferenceId: String? = null

    private val initMutex = Mutex()

    /**
     * Terra requires [Terra.instance] on each app start **and** each time the app enters the
     * foreground — not only when [referenceId] changes. Serialized so auth + lifecycle do not race.
     */
    suspend fun init(activity: Activity, referenceId: String?): Result<TerraManager> =
        initMutex.withLock {
            suspendCancellableCoroutine { cont ->
                Timber.d(
                    "Terra.instance(devId=%s, refIdSet=%s, env=%s)",
                    redactSensitiveId(TerraConfig.devId),
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
                                Result.failure(error ?: IllegalStateException("Null TerraManager")),
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

    fun isConnected(connection: Connections): Boolean =
        current?.getUserId(connection) != null
}
