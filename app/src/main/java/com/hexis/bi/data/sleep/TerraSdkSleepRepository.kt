package com.hexis.bi.data.sleep

import co.tryterra.terra.enums.Connections
import com.hexis.bi.data.terra.TerraManagerHolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.coroutines.resume

/**
 * SDK-backed sleep repository. Returns `Result.success(null)` whenever we have
 * no usable data so the UI renders zeros instead of an error state.
 */
class TerraSdkSleepRepository(
    private val connection: Connections = Connections.HEALTH_CONNECT,
) : TerraSleepRepository {

    override suspend fun getSessionForNight(date: LocalDate): Result<TerraSleepSession?> {
        val manager = TerraManagerHolder.current
            ?: return Result.success(null)

        if (manager.getUserId(connection) == null) {
            Timber.d("Terra not connected (%s) — no sleep data", connection)
            return Result.success(null)
        }

        val zone = ZoneId.systemDefault()
        val start = Date.from(date.minusDays(1).atStartOfDay(zone).toInstant())
        val end = Date.from(date.atStartOfDay(zone).plusHours(12).toInstant())

        return try {
            val payload = suspendCancellableCoroutine<Any?> { cont ->
                manager.getSleep(
                    type = connection,
                    startDate = start,
                    endDate = end,
                    toWebhook = false,
                ) { _, payload, error ->
                    if (error != null) {
                        Timber.e(error, "Terra getSleep failed")
                        if (cont.isActive) cont.resume(null)
                    } else {
                        if (cont.isActive) cont.resume(payload)
                    }
                }
            }
            Timber.d("Terra sleep payload: %s", payload)
            // TODO(terra-mapping): parse payload → TerraSleepSession once on-device shape is confirmed.
            Result.success(null)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
