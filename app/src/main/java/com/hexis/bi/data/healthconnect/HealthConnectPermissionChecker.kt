package com.hexis.bi.data.healthconnect

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber

internal sealed interface HealthConnectPermissionStatus {

    data object Unknown : HealthConnectPermissionStatus

    data class Granted(val missingOptional: Set<String> = emptySet()) : HealthConnectPermissionStatus

    data class Missing(val missingCore: Set<String>) : HealthConnectPermissionStatus

    val isBlocked: Boolean get() = this is Missing
}

internal class HealthConnectPermissionChecker(private val context: Context) {

    fun status(): HealthConnectPermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return HealthConnectPermissionStatus.Unknown
        }
        val missing = HealthConnectPermissions.REQUIRED_MANIFEST_PERMISSIONS.filterNotTo(mutableSetOf()) {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        logIfChanged(missing)
        val missingCore = missing intersect HealthConnectPermissions.CORE_MANIFEST_PERMISSIONS
        return if (missingCore.isEmpty()) {
            HealthConnectPermissionStatus.Granted(missing)
        } else {
            HealthConnectPermissionStatus.Missing(missingCore)
        }
    }

    private fun logIfChanged(missing: Set<String>) {
        if (lastLoggedMissing.getAndSet(missing) == missing) return
        val missingCore = missing intersect HealthConnectPermissions.CORE_MANIFEST_PERMISSIONS
        when {
            missing.isEmpty() -> Timber.i("Health Connect read permissions granted")
            missingCore.isEmpty() -> Timber.i(
                "Health Connect granted all %d core permission(s); %d optional one(s) declined",
                HealthConnectPermissions.CORE_MANIFEST_PERMISSIONS.size, missing.size,
            )

            else -> Timber.w(
                "Health Connect missing %d of %d core read permission(s); treating as disconnected",
                missingCore.size, HealthConnectPermissions.CORE_MANIFEST_PERMISSIONS.size,
            )
        }
    }

    private val lastLoggedMissing = AtomicReference<Set<String>?>(null)
}
