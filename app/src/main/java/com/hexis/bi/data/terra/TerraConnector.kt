package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.enums.Connections
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/** Runs Terra's `initConnection` flow. Must be called from an Activity. */
class TerraConnector(private val authApi: TerraAuthApi) {

    suspend fun connect(
        activity: Activity,
        connection: Connections = Connections.HEALTH_CONNECT,
    ): Result<Boolean> {
        val manager = TerraManagerHolder.current
            ?: return Result.failure(IllegalStateException("TerraManager not initialised"))

        if (manager.getUserId(connection) != null) {
            Timber.d("Terra already connected to %s", connection)
            return Result.success(true)
        }

        val tokenResult = authApi.generateAuthToken()
        val token = tokenResult.getOrElse {
            Timber.e(it, "Terra generateAuthToken failed (devId=%s, env=%s)",
                TerraConfig.devId.short(), TerraConfig.environment)
            return Result.failure(it)
        }

        Timber.d(
            "Terra initConnection start: connection=%s devId=%s env=%s tokenLen=%d",
            connection, TerraConfig.devId.short(), TerraConfig.environment, token.length,
        )

        return suspendCancellableCoroutine { cont ->
            manager.initConnection(
                connection = connection,
                token = token,
                context = activity,
                customPermissions = emptySet(),
                schedulerOn = true,
                startIntent = null,
            ) { success, error ->
                if (error != null) {
                    Timber.e(
                        error,
                        "Terra initConnection failed: type=%s message=%s connection=%s devId=%s env=%s",
                        error::class.java.simpleName,
                        error.message,
                        connection,
                        TerraConfig.devId.short(),
                        TerraConfig.environment,
                    )
                    if (cont.isActive) cont.resume(Result.failure(error))
                } else {
                    Timber.d("Terra connection success=%s connection=%s", success, connection)
                    if (cont.isActive) cont.resume(Result.success(success))
                }
            }
        }
    }

    private fun String.short(): String =
        if (length <= 8) "***" else "${take(4)}…${takeLast(4)}"
}
