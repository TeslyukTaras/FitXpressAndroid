package com.hexis.bi.data.health.remote

import com.hexis.bi.data.healthconnect.HealthConnectPermissionChecker
import com.hexis.bi.data.terra.MergedSourceResult
import com.hexis.bi.data.terra.TerraRestIdentity
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.terra.fetchMergedFromAllSources
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.utils.constants.TerraSyncConstants
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
) {

    private val gate = Semaphore(TerraSyncConstants.MAX_CONCURRENT_SOURCES)

    suspend fun identities(): Result<HealthIdentities> =
        sourceResolver.resolveOrderedIdentities().map { HealthIdentities(it, fetchableIdentities(it)) }

    suspend fun <T> fetchRange(
        identities: List<TerraRestIdentity>,
        start: LocalDate,
        end: LocalDate,
        fetchJson: suspend (terraUserId: String, LocalDate, LocalDate) -> Result<List<Any?>>,
        parse: (List<Any?>) -> List<T>,
        merge: (List<List<T>>) -> List<T>,
    ): Result<MergedSourceResult<T>> = fetchMergedFromAllSources(
        identities = identities,
        gate = gate,
        start = start,
        end = end,
        fetchJson = fetchJson,
        parse = parse,
        merge = merge,
    )

    fun fetchableIdentities(identities: List<TerraRestIdentity>): List<TerraRestIdentity> {
        val blocked = identities.filterTo(mutableSetOf()) { it.isHealthConnect() }
        if (blocked.isEmpty() || !healthConnectPermissions.status().isBlocked) {
            logSkipped(emptySet())
            return identities
        }
        logSkipped(blocked.mapTo(mutableSetOf()) { it.terraUserId })
        return identities - blocked
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
