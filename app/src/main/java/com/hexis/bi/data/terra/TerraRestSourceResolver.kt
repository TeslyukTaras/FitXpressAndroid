package com.hexis.bi.data.terra

import co.tryterra.terra.TerraManager
import co.tryterra.terra.enums.Connections
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import timber.log.Timber

/**
 * Builds a stable ordered list of Terra REST identities for multi-source pulls.
 *
 * Order: wearable / widget connections first (most recently connected first), then Health Connect
 * rows from Firestore, then a live SDK-only Health Connect user id when it is not already listed.
 */
class TerraRestSourceResolver(
    private val healthConnections: HealthConnectionsRepository,
    private val terraManagerProvider: () -> TerraManager? = { TerraManagerHolder.current },
) {

    suspend fun resolveOrderedIdentities(): Result<List<TerraRestIdentity>> {
        val connections = healthConnections.getConnections().getOrElse { e ->
            Timber.w(e, "healthConnections.getConnections failed; continuing with SDK Health Connect id only")
            emptyList()
        }

        val active = connections.filter { it.active }
        val seen = LinkedHashSet<String>()
        val out = ArrayList<TerraRestIdentity>()

        active
            .filter { !it.provider.equals(PROVIDER_HEALTH_CONNECT, ignoreCase = true) }
            .sortedByConnectionRecency()
            .forUniqueTerraIds(seen, out)

        active
            .filter { it.provider.equals(PROVIDER_HEALTH_CONNECT, ignoreCase = true) }
            .sortedByConnectionRecency()
            .forUniqueTerraIds(seen, out)

        val sdkHc = terraManagerProvider()?.getUserId(Connections.HEALTH_CONNECT)
        if (!sdkHc.isNullOrBlank() && seen.add(sdkHc)) {
            out.add(TerraRestIdentity(terraUserId = sdkHc, provider = PROVIDER_HEALTH_CONNECT))
        }

        return Result.success(out)
    }

    companion object {
        const val PROVIDER_HEALTH_CONNECT: String = "HEALTH_CONNECT"
    }
}

private fun List<HealthConnection>.sortedByConnectionRecency(): List<HealthConnection> =
    sortedWith(
        compareByDescending<HealthConnection> { it.connectedAt?.toDate()?.time ?: 0L }
            .thenBy { it.terraUserId },
    )

private fun List<HealthConnection>.forUniqueTerraIds(
    seen: MutableSet<String>,
    into: MutableList<TerraRestIdentity>,
) {
    for (c in this) {
        if (seen.add(c.terraUserId)) {
            into.add(TerraRestIdentity(terraUserId = c.terraUserId, provider = c.provider))
        }
    }
}
