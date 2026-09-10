package com.hexis.bi.data.health.remote

import com.hexis.bi.data.healthconnect.HealthConnectPermissionChecker
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.data.terra.MergedSourceResult
import com.hexis.bi.data.terra.TerraRestIdentity
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.utils.constants.TerraSyncConstants
import com.hexis.bi.utils.redactSensitiveId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.sync.Semaphore
import java.time.LocalDate
import timber.log.Timber

internal data class HealthIdentities(
    val all: List<TerraRestIdentity>,
    val fetchable: List<TerraRestIdentity>,
)

internal class HealthRemoteDataSource(
    private val sourceResolver: TerraRestSourceResolver,
    private val healthConnectPermissions: HealthConnectPermissionChecker,
    private val healthConnections: HealthConnectionsRepository,
) {

    private val gate = Semaphore(TerraSyncConstants.MAX_CONCURRENT_SOURCES)

    private val rejectedByTerra = ConcurrentHashMap.newKeySet<String>()

    suspend fun identities(): Result<HealthIdentities> =
        sourceResolver.resolveOrderedIdentities().map { HealthIdentities(it, fetchableIdentities(it)) }

    suspend fun <T> fetchRange(
        identities: List<TerraRestIdentity>,
        start: LocalDate,
        end: LocalDate,
        fetchJson: suspend (terraUserId: String, LocalDate, LocalDate) -> Result<List<Any?>>,
        parse: (List<Any?>) -> List<T>,
    ): Result<MergedSourceResult<T>> = fetchMergedFromAllSources(
        identities = identities,
        gate = gate,
        start = start,
        end = end,
        fetchJson = fetchJson,
        parse = parse,
        onIdentityRejected = ::retireRejectedIdentity,
    )

    fun fetchableIdentities(identities: List<TerraRestIdentity>): List<TerraRestIdentity> {
        val queryable = identities.filterNot { it.terraUserId in rejectedByTerra }
        val blocked = queryable.filterTo(mutableSetOf()) { it.isHealthConnect() }
        if (blocked.isEmpty() || !healthConnectPermissions.status().isBlocked) {
            logSkipped(emptySet())
            return queryable
        }
        logSkipped(blocked.mapTo(mutableSetOf()) { it.terraUserId })
        return queryable - blocked
    }

    private suspend fun retireRejectedIdentity(identity: TerraRestIdentity) {
        if (!rejectedByTerra.add(identity.terraUserId)) return
        Timber.w(
            "Terra rejected %s (%s) as an unknown user id; retiring the connection",
            identity.provider, redactSensitiveId(identity.terraUserId),
        )
        healthConnections.deactivateConnection(identity.terraUserId)
            .onSuccess { TerraSdkSync.invalidateCachesAndNotify() }
            .onFailure {
                rejectedByTerra.remove(identity.terraUserId)
                Timber.w(
                    it, "Could not retire the rejected %s connection %s",
                    identity.provider, redactSensitiveId(identity.terraUserId),
                )
            }
    }

    private fun logSkipped(skipped: Set<String>) {
        if (lastSkippedLogged.getAndSet(skipped) == skipped) return
        if (skipped.isEmpty()) {
            Timber.i("Health Connect read permissions restored; identities queryable again")
        } else {
            Timber.w(
                "Skipping %d Health Connect identity/identities: read permissions revoked",
                skipped.size,
            )
        }
    }

    private val lastSkippedLogged = AtomicReference(emptySet<String>())

    private fun TerraRestIdentity.isHealthConnect(): Boolean =
        provider.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true)
}
