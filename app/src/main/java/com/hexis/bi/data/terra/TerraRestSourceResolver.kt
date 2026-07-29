package com.hexis.bi.data.terra

import co.tryterra.terra.enums.Connections
import com.google.firebase.Timestamp
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.utils.constants.TerraProviders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate
import java.security.MessageDigest

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
 * Order:
 *  1. Wearable widget connections (Oura, Whoop, Garmin, …), most recently connected first.
 *  2. Health Connect rows from Firestore, then the live SDK-only Health Connect user id.
 *  3. App widget connections (Peloton, Strava-likes, nutrition apps, …), most recently connected first.
 *
 * Higher tiers win on per-key conflicts during gap-fill merge.
 */
class TerraRestSourceResolver(
    private val healthConnections: HealthConnectionsRepository,
    private val terraManagerHolder: TerraManagerHolder,
) {

    suspend fun resolveOrderedIdentities(): Result<List<TerraRestIdentity>> {
        val connections = healthConnections.getConnections().getOrElse { e ->
            Timber.w(
                e,
                "healthConnections.getConnections failed; continuing with SDK Health Connect id only"
            )
            emptyList()
        }

        val sdkHealthConnectId = terraManagerHolder.current?.getUserId(Connections.HEALTH_CONNECT)
        val sdkConnectedAt = Timestamp.now()
        if (!sdkHealthConnectId.isNullOrBlank() && connections.none { it.terraUserId == sdkHealthConnectId }) {
            healthConnections.upsertConnection(
                HealthConnection(
                    terraUserId = sdkHealthConnectId,
                    provider = TerraProviders.HEALTH_CONNECT,
                    source = HealthConnection.SOURCE_SDK,
                    connectedAt = sdkConnectedAt,
                    active = true,
                ),
            ).onFailure {
                Timber.w(it, "Unable to persist live SDK Health Connect id before Terra REST fetch")
            }
        }

        return Result.success(orderTerraIdentities(connections, sdkHealthConnectId, sdkConnectedAt))
    }
}

internal fun orderTerraIdentities(
    connections: List<HealthConnection>,
    sdkHealthConnectId: String? = null,
    sdkConnectedAt: Timestamp = Timestamp.now(),
): List<TerraRestIdentity> {
    val active = connections.filter { it.active }.toMutableList()
    if (!sdkHealthConnectId.isNullOrBlank() && active.none { it.terraUserId == sdkHealthConnectId }) {
        active += HealthConnection(
            terraUserId = sdkHealthConnectId,
            provider = TerraProviders.HEALTH_CONNECT,
            source = HealthConnection.SOURCE_SDK,
            connectedAt = sdkConnectedAt,
            active = true,
        )
    }
    val seen = LinkedHashSet<String>()
    return active
        .mapNotNull { connection -> providerTier(connection.provider)?.let { it to connection } }
        .sortedWith(
            compareBy<Pair<Int, HealthConnection>> { it.first }
                .thenByDescending { it.second.connectedAt?.toDate()?.time ?: 0L }
                .thenBy { it.second.terraUserId },
        )
        .mapNotNull { (_, connection) ->
            if (seen.add(connection.terraUserId)) {
                TerraRestIdentity(terraUserId = connection.terraUserId, provider = connection.provider)
            } else {
                null
            }
        }
}

private fun providerTier(provider: String): Int? = when (val code = provider.uppercase()) {
    in TerraProviders.WEARABLE_CODES -> 0
    TerraProviders.HEALTH_CONNECT -> 1
    in TerraProviders.APP_CODES -> 2
    TerraProviders.DUMMY -> 2
    else -> null.also { if (code.isNotEmpty()) Timber.w("Unknown Terra provider code %s excluded", code) }
}

internal fun sourceFingerprint(identities: List<TerraRestIdentity>): String {
    val material = identities.joinToString("|") { "${it.provider.uppercase()}:${it.terraUserId}" }
    return MessageDigest.getInstance("SHA-256").digest(material.toByteArray())
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}

/**
 * Resolves identities, fetches JSON per identity, parses to rows, then merges with gap-fill so
 * higher-priority sources win per logical key (e.g. wake day for sleep).
 *
 * Share this across Terra REST repositories (sleep today, daily / activity later).
 */
internal suspend fun <T> fetchMergedFromAllSources(
    identities: List<TerraRestIdentity>,
    gate: Semaphore,
    start: LocalDate,
    end: LocalDate,
    fetchJson: suspend (terraUserId: String, LocalDate, LocalDate) -> Result<List<JsonElement>>,
    parse: (List<JsonElement>) -> List<T>,
    merge: (List<List<T>>) -> List<T>,
): Result<MergedSourceResult<T>> {
    if (identities.isEmpty()) return Result.success(MergedSourceResult(emptyList(), false, 0, 0))

    val results: List<Result<List<T>>> = coroutineScope {
        identities.map { id ->
            async {
                gate.withPermit {
                fetchJson(id.terraUserId, start, end).mapCatching { rows ->
                    runCatching { parse(rows) }
                        .onFailure {
                            Timber.w(it, "Terra row parse failed for provider %s; skipping source", id.provider)
                        }
                        .getOrThrow()
                    }
                }
            }
        }.awaitAll()
    }

    val perSource = results.mapNotNull { it.getOrNull() }
    if (perSource.isEmpty()) {
        val firstError = results.firstNotNullOfOrNull { it.exceptionOrNull() }
        if (firstError != null) return Result.failure(firstError)
    } else {
        results.forEach { result ->
            result.exceptionOrNull()?.let { Timber.w(it, "Terra source fetch failed; excluded from merge") }
        }
    }
    return Result.success(
        MergedSourceResult(
            rows = merge(perSource),
            complete = results.all { it.isSuccess },
            successfulSources = results.count { it.isSuccess },
            totalSources = results.size,
        ),
    )
}

internal data class MergedSourceResult<T>(
    val rows: List<T>,
    val complete: Boolean,
    val successfulSources: Int,
    val totalSources: Int,
)
