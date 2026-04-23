package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.enums.Connections
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/** Runs Terra's SDK connection flow (Health Connect / Samsung Health). Must be called from an Activity. */
interface TerraConnector {
    suspend fun connect(
        activity: Activity,
        connection: Connections = Connections.HEALTH_CONNECT,
    ): Result<Boolean>
}

/** Production implementation — drives `Terra.initConnection` on the Android SDK. */
class TerraSdkConnector(private val authApi: TerraAuthApi) : TerraConnector {

    override suspend fun connect(activity: Activity, connection: Connections): Result<Boolean> {
        val manager = TerraManagerHolder.current
            ?: return Result.failure(IllegalStateException("TerraManager not initialised"))

        if (manager.getUserId(connection) != null) {
            Timber.d("Terra already connected to %s", connection)
            return Result.success(true)
        }

        val tokenResult = authApi.generateAuthToken()
        val token = tokenResult.getOrElse {
            Timber.e(
                it, "Terra generateAuthToken failed (devId=%s, env=%s)",
                redactSensitiveId(TerraConfig.devId), TerraConfig.environment,
            )
            return Result.failure(it)
        }

        Timber.d(
            "Terra initConnection start: connection=%s devId=%s env=%s tokenLen=%d",
            connection, redactSensitiveId(TerraConfig.devId), TerraConfig.environment, token.length,
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
                        redactSensitiveId(TerraConfig.devId),
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

}
