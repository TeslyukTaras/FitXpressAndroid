package com.hexis.bi.data.terra

import co.tryterra.terra.enums.Connections
import com.hexis.bi.data.healthconnect.HealthConnectPermissionChecker
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.utils.redactSensitiveId
import timber.log.Timber

internal fun ownedSdkUserIds(
    sdkReportedIds: Set<String>,
    connections: List<HealthConnection>,
): Set<String> {
    if (sdkReportedIds.isEmpty()) return emptySet()
    val active = connections.filterTo(mutableSetOf()) { it.active }.mapTo(mutableSetOf()) { it.terraUserId }
    return sdkReportedIds.intersect(active)
}

internal fun syncableSdkUserIds(
    sdkReportedIds: Set<String>,
    healthConnectUserId: String?,
    healthConnectBlocked: Boolean,
    connections: List<HealthConnection>,
): Set<String> {
    val readable =
        if (healthConnectBlocked && healthConnectUserId != null) sdkReportedIds - healthConnectUserId
        else sdkReportedIds
    return ownedSdkUserIds(readable, connections)
}

class TerraSdkConnectionOwnership internal constructor(
    private val healthConnections: HealthConnectionsRepository,
    private val terraManagerHolder: TerraManagerHolder,
    private val healthConnectPermissions: HealthConnectPermissionChecker,
) {

    suspend fun syncableSdkUserIds(): Result<Set<String>> {
        val manager = terraManagerHolder.current ?: return Result.success(emptySet())
        val sdkIds = enumValues<Connections>().mapNotNullTo(mutableSetOf(), manager::getUserId)
        if (sdkIds.isEmpty()) return Result.success(emptySet())
        return healthConnections.getConnections().map { connections ->
            val owned = ownedSdkUserIds(sdkIds, connections)
            val disowned = sdkIds - owned
            if (disowned.isNotEmpty()) {
                Timber.w(
                    "IDENTITY-OWNERSHIP sdk: dropping %s reported by the SDK but absent from this account's connections (owned=%s)",
                    disowned.map(::redactSensitiveId),
                    owned.map(::redactSensitiveId),
                )
            }
            syncableSdkUserIds(
                sdkReportedIds = sdkIds,
                healthConnectUserId = manager.getUserId(Connections.HEALTH_CONNECT),
                healthConnectBlocked = healthConnectPermissions.status().isBlocked,
                connections = connections,
            )
        }
    }
}
