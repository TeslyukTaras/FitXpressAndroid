package com.hexis.bi.data.terra

import com.google.firebase.Timestamp
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.utils.redactSensitiveId
import timber.log.Timber

/**
 * Brings Firestore's health-connection rows in line with Terra, which is the source of truth for
 * what the user actually authorised.
 */
class TerraConnectionReconciler(
    private val terraApi: TerraApi,
    private val healthConnections: HealthConnectionsRepository,
) {

    /**
     * Stores any live Terra connection that Firestore is missing.
     *
     * Upsert-only: Terra drops a user when it is deauthenticated, so a provider the user
     * disconnected is absent here and will not be resurrected.
     *
     * @return the provider codes newly marked connected by this pass.
     */
    suspend fun reconcile(): Result<Set<String>> {
        val remote = terraApi.listConnections().getOrElse { return Result.failure(it) }
        val stored = healthConnections.getConnections().getOrElse { return Result.failure(it) }
        val storedActiveIds = stored.filter { it.active }.mapTo(mutableSetOf()) { it.terraUserId }

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
    }
}
