package com.hexis.bi.data.terra

import com.google.firebase.functions.FirebaseFunctions
import com.hexis.bi.data.firebase.toJsonElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

data class TerraAuthSession(val authUrl: String, val userId: String?)

/** Produces a Terra auth URL for web-based providers (Oura, Garmin, …). */
class TerraWidgetApi(private val functions: FirebaseFunctions) {

    @Serializable
    private data class WidgetResponse(
        val url: String? = null,
        val session_id: String? = null,
        val status: String? = null,
        val message: String? = null,
    )

    @Serializable
    private data class AuthenticateUserResponse(
        val authUrl: String? = null,
        val userId: String? = null,
        val status: String? = null,
        val message: String? = null,
    )

    /**
     * Generates a direct provider OAuth URL via `/auth/authenticateUser` — skips the Terra
     * widget landing/agree page that [generateWidgetSession] shows. Use this for single-provider
     * connect flows (one row click → one OAuth page).
     *
     * @param referenceId Firebase UID — Terra echoes it back via webhook/redirect.
     * @param resource Single Terra provider code (e.g. `OURA`).
     */
    suspend fun authenticateUser(referenceId: String, resource: String): Result<TerraAuthSession> =
        withContext(Dispatchers.IO) {
            try {
                val data = functions
                    .getHttpsCallable(terraFunction(FUNCTION_AUTHENTICATE_USER))
                    .call(mapOf(FIELD_RESOURCE to resource))
                    .await()
                    .data
                val parsed = terraJson.decodeFromJsonElement(
                    AuthenticateUserResponse.serializer(),
                    data.toJsonElement(),
                )
                val authUrl = parsed.authUrl
                    ?: error("Terra authenticateUser returned no auth_url")
                Result.success(TerraAuthSession(authUrl = authUrl, userId = parsed.userId))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    /**
     * @param referenceId Firebase UID — Terra echoes it back via webhook/redirect.
     * @param providers Comma-separated Terra provider codes (e.g. `OURA`, `DUMMY`).
     */
    suspend fun generateWidgetSession(referenceId: String, providers: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val data = functions
                    .getHttpsCallable(terraFunction(FUNCTION_GENERATE_WIDGET_SESSION))
                    .call(mapOf(FIELD_PROVIDERS to providers))
                    .await()
                    .data
                val parsed = terraJson.decodeFromJsonElement(WidgetResponse.serializer(), data.toJsonElement())
                val url = parsed.url ?: error("Terra widget returned no url")
                Result.success(url)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    private companion object {
        const val FUNCTION_AUTHENTICATE_USER = "AuthenticateUser"
        const val FUNCTION_GENERATE_WIDGET_SESSION = "GenerateWidgetSession"
        const val FIELD_RESOURCE = "resource"
        const val FIELD_PROVIDERS = "providers"
    }
}
