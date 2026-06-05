package com.hexis.bi.data.terra

import android.net.Uri
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.healthconnections.HealthConnectionsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * Parses Terra widget redirect URIs, persists successful connections to Firestore,
 * and emits each [Outcome] on [outcomes] for UI layers to observe.
 */
class TerraCallbackHandler(
    private val healthConnections: HealthConnectionsRepository,
) {

    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = 4)
    val outcomes: SharedFlow<Outcome> = _outcomes.asSharedFlow()

    suspend fun handle(uri: Uri): Outcome {
        val outcome = parse(uri)
        if (outcome !is Outcome.Ignored) _outcomes.emit(outcome)
        return outcome
    }

    private suspend fun parse(uri: Uri): Outcome {
        if (uri.scheme != TerraDeepLinks.SCHEME || uri.host != TerraDeepLinks.HOST) {
            return Outcome.Ignored
        }

        val userId = uri.getQueryParameter(TerraDeepLinks.PARAM_USER_ID)
        val referenceId = uri.getQueryParameter(TerraDeepLinks.PARAM_REFERENCE_ID)
        val resource = uri.getQueryParameter(TerraDeepLinks.PARAM_RESOURCE)

        return when (uri.path) {
            TerraDeepLinks.PATH_SUCCESS -> {
                if (userId.isNullOrBlank() || resource.isNullOrBlank()) {
                    Timber.w("Terra success callback missing user_id/resource: %s", uri)
                    return Outcome.Malformed
                }
                Timber.d("Terra connected: provider=%s user=%s ref=%s", resource, userId, referenceId)
                val result = healthConnections.upsertConnection(
                    HealthConnection(
                        terraUserId = userId,
                        provider = resource.uppercase(),
                        source = HealthConnection.SOURCE_API,
                        active = true,
                    ),
                )
                result.fold(
                    onSuccess = {
                        // REST/widget providers have no SDK pull, so advance the cache generation
                        // and ping screens — otherwise repos keep serving the stale pre-connect
                        // cache and the new source shows no data until the TTL expires.
                        TerraSdkSync.invalidateCachesAndNotify()
                        Outcome.Success(resource)
                    },
                    onFailure = { Outcome.SaveFailed(it.message) },
                )
            }
            TerraDeepLinks.PATH_FAILURE -> {
                val reason = uri.getQueryParameter(TerraDeepLinks.PARAM_REASON) ?: "unknown"
                Timber.w("Terra widget failure: %s", reason)
                Outcome.Failure(reason)
            }
            else -> Outcome.Ignored
        }
    }

    sealed interface Outcome {
        data object Ignored : Outcome
        data object Malformed : Outcome
        data class Success(val provider: String) : Outcome
        data class Failure(val reason: String) : Outcome
        data class SaveFailed(val cause: String?) : Outcome
    }
}
