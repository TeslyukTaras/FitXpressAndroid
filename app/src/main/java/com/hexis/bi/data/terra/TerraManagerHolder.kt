package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.Terra
import co.tryterra.terra.TerraManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Process-wide holder for the [TerraManager] returned by Terra’s SDK after [init].
 *
 * This is a normal class (injected as a Koin singleton) so the SDK handle is not kept in a
 * **static** field — Android lint `StaticFieldLeak` does not apply. [TerraManager] still retains
 * the [Activity] passed at init; [com.hexis.bi.ui.MainActivity] calls [clearLocalManager] from
 * [android.app.Activity.onDestroy] so we drop that handle when the activity is torn down.
 */
class TerraManagerHolder {

    @Volatile
    private var manager: TerraManager? = null

    val current: TerraManager?
        get() = manager

    private val initMutex = Mutex()

    /**
     * Terra requires [Terra.instance] on each app start **and** each time the app enters the
     * foreground — not only when [referenceId] changes. Serialized so auth + lifecycle do not race.
     */
    suspend fun init(activity: Activity, referenceId: String?): Result<TerraManager> =
        initMutex.withLock {
            suspendCancellableCoroutine { cont ->
                Terra.instance(TerraConfig.devId, referenceId, activity) { mgr, error ->
                    if (error != null) {
                        Timber.e(error, "Terra init failed")
                        if (cont.isActive) cont.resume(Result.failure(error))
                    } else {
                        manager = mgr
                        Timber.d("Terra init success (env=%s)", TerraConfig.environment)
                        if (cont.isActive) cont.resume(Result.success(mgr))
                    }
                }
            }
        }

    /**
     * Clears the cached [TerraManager] after server-side deauth so the next [init] can reconnect
     * without the SDK still reporting the old Health Connect user id.
     */
    fun clearLocalManager() {
        manager = null
    }

    /**
     * Waits until [current] is non-null (Terra [init] finished) or [timeoutMs] elapses. Used so
     * REST reads (e.g. sleep) do not run before the SDK has finished its first handshake.
     */
    suspend fun awaitCurrentOrTimeout(timeoutMs: Long = DEFAULT_AWAIT_TIMEOUT_MS): TerraManager? {
        current?.let { return it }
        val steps = (timeoutMs / AWAIT_POLL_INTERVAL_MS).toInt().coerceAtLeast(1)
        repeat(steps) {
            current?.let { return it }
            delay(AWAIT_POLL_INTERVAL_MS)
        }
        return current
    }

    private companion object {
        private const val DEFAULT_AWAIT_TIMEOUT_MS = 25_000L
        private const val AWAIT_POLL_INTERVAL_MS = 50L
    }
}
