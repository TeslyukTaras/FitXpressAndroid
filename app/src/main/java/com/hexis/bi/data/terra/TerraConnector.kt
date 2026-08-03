package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.enums.Connections
import com.hexis.bi.data.healthconnect.HealthConnectPermissions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume

/** Runs Terra's SDK connection flow (Health Connect / Samsung Health). Must be called from an Activity. */
class TerraConnector internal constructor(
    private val authApi: TerraAuthApi,
    private val terraManagerHolder: TerraManagerHolder,
) {

    suspend fun connect(
        activity: Activity,
        connection: Connections = Connections.HEALTH_CONNECT,
    ): Result<Boolean> {
        val manager = terraManagerHolder.current
            ?: return Result.failure(IllegalStateException("TerraManager not initialised"))

        if (manager.getUserId(connection) != null) {
            Timber.d("Terra already linked to %s; re-running connection flow to refresh grants", connection)
        }

        val token = authApi.generateAuthToken().getOrElse {
            Timber.e(it, "Terra generateAuthToken failed (env=%s)", TerraConfig.environment)
            return Result.failure(it)
        }

        val permissions = if (connection == Connections.HEALTH_CONNECT) {
            HealthConnectPermissions.TERRA_CUSTOM_PERMISSIONS
        } else {
            emptySet()
        }

        return withTimeoutOrNull(INIT_CONNECTION_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                manager.initConnection(
                    connection = connection,
                    token = token,
                    context = activity,
                    customPermissions = permissions,
                    schedulerOn = true,
                    startIntent = null,
                ) { success, error ->
                    if (error != null) {
                        Timber.e(error, "Terra initConnection failed: connection=%s", connection)
                        if (cont.isActive) cont.resume(Result.failure(error))
                    } else {
                        Timber.d("Terra initConnection success=%s connection=%s", success, connection)
                        if (cont.isActive) cont.resume(Result.success(success))
                    }
                }
            }
        } ?: Result.failure(IllegalStateException("Terra initConnection timed out"))
    }

    companion object {
        private const val INIT_CONNECTION_TIMEOUT_MS = 30_000L
    }
}
