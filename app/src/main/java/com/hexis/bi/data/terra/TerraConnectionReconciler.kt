package com.hexis.bi.data.terra

import com.google.firebase.Timestamp
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.utils.constants.TerraSyncConstants
import com.hexis.bi.utils.redactSensitiveId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

/**
 * Brings Firestore's health-connection rows in line with Terra, which is the source of truth for
 * what the user actually authorised.
 */
class TerraConnectionReconciler(
    private val terraApi: TerraApi,
    private val healthConnections: HealthConnectionsRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun reconcile(): Result<Set<String>> {
        val remote = terraApi.listConnections().getOrElse { return Result.failure(it) }
        val stored = healthConnections.getConnections().getOrElse { return Result.failure(it) }
        val storedActiveIds = stored.filter { it.active }.mapTo(mutableSetOf()) { it.terraUserId }

        retireConnectionsTerraNoLongerLists(stored, remote)

        val recovered = mutableSetOf<String>()
        for (user in remote.users) {
            val terraUserId = user.user_id?.takeIf { it.isNotBlank() } ?: continue
            val provider = user.provider?.trim()?.uppercase()?.takeIf { it.isNotBlank() } ?: continue
            if (!user.active) continue
            if (provider !in RECONCILABLE_PROVIDERS) continue
            if (terraUserId in storedActiveIds) continue

            healthConnections.upsertConnection(
                HealthConnection(
                    terraUserId = terraUserId,
                    provider = provider,
                    source = sourceFor(provider),
                    connectedAt = user.createdAtTimestamp() ?: Timestamp.now(),
                    active = true,
                ),
            ).onSuccess {
                Timber.i(
                    "Terra reconcile: recovered %s (user=%s) missing from Firestore",
                    provider,
                    redactSensitiveId(terraUserId),
                )
                recovered += provider
            }.onFailure {
                Timber.w(it, "Terra reconcile: could not persist %s", provider)
            }
        }

        if (recovered.isNotEmpty()) {
            TerraSdkSync.invalidateCachesAndNotify()
        }
        return Result.success(recovered)
    }

    private suspend fun retireConnectionsTerraNoLongerLists(
        stored: List<HealthConnection>,
        remote: TerraConnectionsResponse,
    ) {
        if (!remote.isAuthoritative) {
            Timber.w("Terra reconcile: connection list not authoritative (status=%s); retiring nothing", remote.status)
            return
        }
        val liveIds = remote.users
            .filter { it.active }
            .mapNotNullTo(mutableSetOf()) { it.user_id?.takeIf(String::isNotBlank) }
        if (liveIds.isEmpty()) {
            Timber.w("Terra reconcile: connection list came back empty; retiring nothing")
            return
        }
        val absent = stored.filterTo(mutableSetOf()) {
            it.active &&
                it.terraUserId !in liveIds &&
                it.provider.uppercase() in RETIRABLE_PROVIDERS
        }
        val absentIds = absent.mapTo(mutableSetOf()) { it.terraUserId }
        val now = clock.instant()
        absentSince.keys.retainAll(absentIds)
        absentIds.forEach { absentSince.putIfAbsent(it, now) }

        val retired = absent.filter { connection ->
            val since = absentSince[connection.terraUserId] ?: now
            Duration.between(since, now) >= TerraSyncConstants.MIN_ABSENCE_BEFORE_RETIRE
        }
        if (retired.isEmpty()) {
            if (absent.isNotEmpty()) {
                Timber.i(
                    "Terra reconcile: %d connection(s) absent; retiring after %d more minute(s) absent",
                    absent.size,
                    absent.minOf { connection ->
                        val since = absentSince[connection.terraUserId] ?: now
                        (TerraSyncConstants.MIN_ABSENCE_BEFORE_RETIRE - Duration.between(since, now)).toMinutes()
                    },
                )
            }
            return
        }

        Timber.i("Terra reconcile: retiring %d connection(s) Terra no longer lists", retired.size)
        for (connection in retired) {
            healthConnections.deactivateConnection(connection.terraUserId).onFailure {
                Timber.w(
                    it, "Terra reconcile: could not retire %s (%s)",
                    connection.provider, redactSensitiveId(connection.terraUserId),
                )
            }
        }
        absentSince.keys.removeAll(retired.mapTo(mutableSetOf()) { it.terraUserId })
        TerraSdkSync.invalidateCachesAndNotify()
    }

    private val absentSince = ConcurrentHashMap<String, Instant>()

    private fun sourceFor(provider: String): String =
        if (provider == TerraProviders.HEALTH_CONNECT) {
            HealthConnection.SOURCE_SDK
        } else {
            HealthConnection.SOURCE_API
        }

    private companion object {
        val RECONCILABLE_PROVIDERS: Set<String> =
            TerraProviders.WEARABLE_CODES +
                TerraProviders.APP_CODES +
                TerraProviders.HEALTH_CONNECT +
                TerraProviders.DUMMY

        val RETIRABLE_PROVIDERS: Set<String> = RECONCILABLE_PROVIDERS - TerraProviders.DUMMY
    }
}
