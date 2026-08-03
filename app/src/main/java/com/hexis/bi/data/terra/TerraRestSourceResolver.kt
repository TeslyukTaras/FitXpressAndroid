package com.hexis.bi.data.terra

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.utils.constants.TerraCacheConstants
import com.hexis.bi.utils.constants.TerraProviders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
 * Order:
 *  1. Wearable widget connections (Oura, Whoop, Garmin, …), most recently connected first.
 *  2. Health Connect rows.
 *  3. App widget connections (Peloton, Strava-likes, nutrition apps, …), most recently connected first.
 *
 * Higher tiers win on per-key conflicts during gap-fill merge.
 */
class TerraRestSourceResolver(
    private val healthConnections: HealthConnectionsRepository,
    private val auth: FirebaseAuth,
) {

    private val identities = TtlCache<String, List<TerraRestIdentity>>(
        ttlMs = TerraCacheConstants.IDENTITY_CACHE_TTL_MS,
        generation = { TerraSdkSync.syncGeneration },
    )

    suspend fun resolveOrderedIdentities(): Result<List<TerraRestIdentity>> {
        val uid = auth.currentUser?.uid ?: return Result.success(emptyList())
        identities.get(uid)?.let { return Result.success(it) }
        return healthConnections.getConnections()
            .onFailure { Timber.w(it, "Terra identity lookup failed; no source list for this refresh") }
            .map(::orderTerraIdentities)
            .onSuccess { identities.put(uid, it) }
    }
}

internal fun orderTerraIdentities(connections: List<HealthConnection>): List<TerraRestIdentity> {
    val active = connections.filter { it.active }
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

private const val IDENTITY_FIELD_SEPARATOR = ":"

internal fun TerraRestIdentity.encodeForCursor(): String =
    "$provider$IDENTITY_FIELD_SEPARATOR$terraUserId"

internal fun decodeTerraRestIdentity(encoded: String): TerraRestIdentity? {
    val separator = encoded.indexOf(IDENTITY_FIELD_SEPARATOR)
    if (separator < 0) return encoded.takeIf { it.isNotBlank() }?.let { TerraRestIdentity(it, "") }
    return encoded.substring(separator + 1)
        .takeIf { it.isNotBlank() }
        ?.let { TerraRestIdentity(it, encoded.substring(0, separator)) }
}

private fun providerTier(provider: String): Int? = when (val code = provider.uppercase()) {
    in TerraProviders.WEARABLE_CODES -> 0
    TerraProviders.HEALTH_CONNECT -> 1
    in TerraProviders.APP_CODES -> 2
    TerraProviders.DUMMY -> 2
    else -> null.also { if (code.isNotEmpty()) Timber.w("Unknown Terra provider code %s excluded", code) }
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
    fetchJson: suspend (terraUserId: String, LocalDate, LocalDate) -> Result<List<Any?>>,
    parse: (List<Any?>) -> List<T>,
    merge: (List<List<T>>) -> List<T>,
): Result<MergedSourceResult<T>> {
    if (identities.isEmpty()) {
        return Result.success(
            MergedSourceResult(rows = emptyList(), complete = true, successfulSources = 0, totalSources = 0),
        )
    }

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
            perIdentity = identities.zip(results).mapNotNull { (identity, result) ->
                result.getOrNull()?.let { IdentityRows(identity.terraUserId, it) }
            },
            complete = results.all { it.isSuccess },
            successfulSources = results.count { it.isSuccess },
            totalSources = results.size,
        ),
    )
}

internal data class IdentityRows<T>(val terraUserId: String, val rows: List<T>)

internal data class MergedSourceResult<T>(
    val rows: List<T>,
    val perIdentity: List<IdentityRows<T>> = emptyList(),
    val complete: Boolean,
    val successfulSources: Int,
    val totalSources: Int,
)
