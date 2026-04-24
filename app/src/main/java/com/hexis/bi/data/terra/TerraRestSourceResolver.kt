package com.hexis.bi.data.terra

import co.tryterra.terra.enums.Connections
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.utils.constants.TerraProviders
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate

/**
 * One identity in a multi-source pull: [terraUserId] is the query key for Terra REST v2;
 * [provider] is a display or Firestore label (e.g. OURA, HEALTH_CONNECT).
 */
data class TerraRestIdentity(
    val terraUserId: String,
    val provider: String,
)

/**
 * Builds a stable ordered list of Terra REST identities for multi-source pulls.
 *
 * Order: wearable / widget connections first (most recently connected first), then Health Connect
 * rows from Firestore, then a live SDK-only Health Connect user id when it is not already listed.
 */
class TerraRestSourceResolver(
    private val healthConnections: HealthConnectionsRepository,
    private val terraManagerHolder: TerraManagerHolder,
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
            .filter { !it.provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true) }
            .sortedByConnectionRecency()
            .forUniqueTerraIds(seen, out)

        active
            .filter { it.provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true) }
            .sortedByConnectionRecency()
            .forUniqueTerraIds(seen, out)

        val sdkHc = terraManagerHolder.current?.getUserId(Connections.HEALTH_CONNECT)
        if (!sdkHc.isNullOrBlank() && seen.add(sdkHc)) {
            out.add(TerraRestIdentity(terraUserId = sdkHc, provider = TerraProviders.HEALTH_CONNECT))
        }

        return Result.success(out)
    }
}

/**
 * Resolves identities, fetches JSON per identity, parses to rows, then merges with gap-fill so
 * higher-priority sources win per logical key (e.g. wake day for sleep).
 *
 * Share this across Terra REST repositories (sleep today, daily / activity later).
 */
internal suspend fun <T> TerraRestSourceResolver.fetchMergedFromAllSources(
    start: LocalDate,
    end: LocalDate,
    fetchJson: suspend (terraUserId: String, LocalDate, LocalDate) -> Result<List<JsonElement>>,
    parse: (List<JsonElement>) -> List<T>,
    merge: (List<List<T>>) -> List<T>,
): Result<List<T>> {
    val identities = resolveOrderedIdentities().getOrElse { return Result.failure(it) }
    if (identities.isEmpty()) return Result.success(emptyList())

    val perSource = ArrayList<List<T>>(identities.size)
    for (id in identities) {
        val rows = fetchJson(id.terraUserId, start, end).getOrElse { return Result.failure(it) }
        perSource.add(parse(rows))
    }
    return Result.success(merge(perSource))
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
