package com.hexis.bi.data.terra

import android.os.SystemClock
import co.tryterra.terra.TerraManager
import co.tryterra.terra.enums.Connections
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.coroutines.resume

/**
 * Debounced foreground hooks that drive Terra SDK pull APIs so normalized data reaches REST
 * (`/v2/daily`, `/v2/sleep`). `getDaily` and `getSleep` are separate pipelines — both must run.
 */
object TerraSdkSync {

    private val debounceMutex = Mutex()
    private var lastSyncElapsedRealtimeMs = 0L

    private const val FOREGROUND_DEBOUNCE_MS = 20L * 60L * 1000L
    private const val DEFAULT_LOOKBACK_DAYS = 30L

    /**
     * @param force skips the foreground debounce (e.g. right after a successful `initConnection`).
     * @return true when a pull actually ran; false when there was nothing to sync or it was debounced.
     */
    suspend fun syncLinkedConnections(
        manager: TerraManager?,
        reason: String,
        force: Boolean = false,
        lookbackDays: Long = DEFAULT_LOOKBACK_DAYS,
    ): Boolean {
        val mgr = manager ?: return false
        val linked = enumValues<Connections>().filter { mgr.getUserId(it) != null }
        if (linked.isEmpty()) return false

        if (!shouldProceed(force)) {
            Timber.d("TerraSdkSync skip (%s): debounced", reason)
            return false
        }

        val end = Date()
        val start = Date.from(Instant.now().minus(lookbackDays, ChronoUnit.DAYS))

        for (connection in linked) {
            awaitPull("getDaily", connection, reason) { cb ->
                mgr.getDaily(
                    connection,
                    start,
                    end,
                    true,
                    cb
                )
            }
            awaitPull("getSleep", connection, reason) { cb ->
                mgr.getSleep(
                    connection,
                    start,
                    end,
                    true,
                    cb
                )
            }
        }
        return true
    }

    private suspend fun shouldProceed(force: Boolean): Boolean = debounceMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        val fresh = now - lastSyncElapsedRealtimeMs < FOREGROUND_DEBOUNCE_MS
        if (!force && fresh) return@withLock false
        lastSyncElapsedRealtimeMs = now
        true
    }

    private suspend fun awaitPull(
        label: String,
        connection: Connections,
        reason: String,
        start: (callback: (Boolean, Any?, co.tryterra.terra.TerraError?) -> Unit) -> Unit,
    ) = suspendCancellableCoroutine { cont ->
        start { success, _, error ->
            if (error != null) Timber.w(
                error,
                "Terra %s failed connection=%s reason=%s",
                label,
                connection,
                reason
            )
            else Timber.d(
                "Terra %s done connection=%s reason=%s success=%s",
                label,
                connection,
                reason,
                success
            )
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
