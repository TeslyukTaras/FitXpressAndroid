package com.hexis.bi.data.terra

import android.os.SystemClock
import co.tryterra.terra.TerraError
import co.tryterra.terra.TerraManager
import co.tryterra.terra.enums.Connections
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.coroutines.resume

/**
 * Debounced foreground hooks that drive Terra SDK pull APIs so normalized data reaches REST
 * (e.g. `/v2/daily`, `/v2/sleep`).
 *
 * [TerraManager.getDaily] and [TerraManager.getSleep] are separate pipelines — syncing only daily
 * summaries leaves `/v2/sleep` empty until [TerraManager.getSleep] runs for the same window.
 */
object TerraSdkSync {

    private val debounceMutex = Mutex()
    private var lastSyncElapsedRealtimeMs = 0L

    private const val FOREGROUND_DEBOUNCE_MS = 20L * 60L * 1000L
    private const val DEFAULT_LOOKBACK_DAYS = 30L

    /**
     * For each SDK [Connections] with a Terra user id, runs [TerraManager.getDaily] then
     * [TerraManager.getSleep] over [lookbackDays] through today, with `toWebhook = true`.
     *
     * @param force skips the foreground debounce (e.g. right after a successful `initConnection`).
     */
    suspend fun syncLinkedConnections(
        manager: TerraManager?,
        reason: String,
        force: Boolean = false,
        lookbackDays: Long = DEFAULT_LOOKBACK_DAYS,
    ) {
        val mgr = manager ?: return
        val linked = enumValues<Connections>().filter { mgr.getUserId(it) != null }
        if (linked.isEmpty()) {
            Timber.d("TerraSdkSync skip (%s): no linked SDK connections", reason)
            return
        }
        if (!force) {
            val allowed = debounceMutex.withLock {
                val now = SystemClock.elapsedRealtime()
                if (now - lastSyncElapsedRealtimeMs < FOREGROUND_DEBOUNCE_MS) {
                    false
                } else {
                    lastSyncElapsedRealtimeMs = now
                    true
                }
            }
            if (!allowed) {
                Timber.d("TerraSdkSync skip (%s): debounced", reason)
                return
            }
        } else {
            debounceMutex.withLock {
                lastSyncElapsedRealtimeMs = SystemClock.elapsedRealtime()
            }
        }
        val window = SdkSyncWindow.lastDays(lookbackDays)
        for (connection in linked) {
            mgr.pullDailyThenSleep(connection, reason, window)
        }
    }

    private suspend fun TerraManager.pullDailyThenSleep(
        connection: Connections,
        reason: String,
        window: SdkSyncWindow,
    ) {
        awaitRangePull("getDaily", connection, reason, window) { c, s, e, tw, cb ->
            getDaily(c, s, e, tw, cb)
        }
        awaitRangePull("getSleep", connection, reason, window) { c, s, e, tw, cb ->
            getSleep(c, s, e, tw, cb)
        }
    }

    private suspend fun TerraManager.awaitRangePull(
        label: String,
        connection: Connections,
        reason: String,
        window: SdkSyncWindow,
        pull: TerraManager.(
            Connections,
            Date,
            Date,
            Boolean,
            (Boolean, Any?, TerraError?) -> Unit,
        ) -> Unit,
    ): Unit = suspendCancellableCoroutine { cont ->
        Timber.d(
            "Terra %s start connection=%s reason=%s %s..%s toWebhook=true",
            label,
            connection,
            reason,
            window.start,
            window.end,
        )
        pull(connection, window.start, window.end, true) { success, _, error ->
            logSdkPullCompletion(label, connection, reason, success, error)
            if (cont.isActive) cont.resume(Unit)
        }
    }

    private fun logSdkPullCompletion(
        label: String,
        connection: Connections,
        reason: String,
        success: Boolean,
        error: TerraError?,
    ) {
        when {
            error != null ->
                Timber.w(error, "Terra %s failed connection=%s reason=%s", label, connection, reason)

            else ->
                Timber.d(
                    "Terra %s done connection=%s reason=%s success=%s",
                    label,
                    connection,
                    reason,
                    success,
                )
        }
    }

    private data class SdkSyncWindow(val start: Date, val end: Date) {
        companion object {
            fun lastDays(days: Long): SdkSyncWindow {
                val end = Date()
                val start = Date.from(Instant.now().minus(days, ChronoUnit.DAYS))
                return SdkSyncWindow(start, end)
            }
        }
    }
}
