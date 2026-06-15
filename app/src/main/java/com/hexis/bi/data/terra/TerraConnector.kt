package com.hexis.bi.data.terra

import android.app.Activity
import co.tryterra.terra.enums.Connections
import co.tryterra.terra.enums.CustomPermissions
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

/** Runs Terra's SDK connection flow (Health Connect / Samsung Health). Must be called from an Activity. */
class TerraConnector(
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
            Timber.d("Terra already connected to %s", connection)
            return Result.success(true)
        }

        val token = authApi.generateAuthToken().getOrElse {
            Timber.e(it, "Terra generateAuthToken failed (env=%s)", TerraConfig.environment)
            return Result.failure(it)
        }

        val permissions = if (connection == Connections.HEALTH_CONNECT) HEALTH_CONNECT_PERMISSIONS else emptySet()

        return suspendCancellableCoroutine { cont ->
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
    }

    companion object {
        // Matches the <uses-permission android:name="android.permission.health.READ_*"> block in
        // AndroidManifest.xml. Passing these to initConnection ensures Terra requests the full set
        // at the Health Connect permission prompt, instead of its smaller default.
        private val HEALTH_CONNECT_PERMISSIONS: Set<CustomPermissions> = setOf(
            CustomPermissions.SLEEP_ANALYSIS,
            CustomPermissions.HEART_RATE,
            CustomPermissions.HEART_RATE_VARIABILITY,
            CustomPermissions.RESTING_HEART_RATE,
            CustomPermissions.STEPS,
            CustomPermissions.FLIGHTS_CLIMBED,
            CustomPermissions.EXERCISE_DISTANCE,
            CustomPermissions.CALORIES,
            CustomPermissions.ACTIVE_DURATIONS,
            CustomPermissions.WORKOUT_TYPE,
            CustomPermissions.ACTIVITY_SUMMARY,
            CustomPermissions.OXYGEN_SATURATION,
            CustomPermissions.RESPIRATORY_RATE,
            CustomPermissions.VO2MAX,
        )
    }
}
