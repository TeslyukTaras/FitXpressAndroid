package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/** Produces a Terra auth URL for web-based providers (Oura, Garmin, …). */
class TerraWidgetApi(private val client: OkHttpClient) {

    @Serializable
    private data class WidgetRequest(
        val reference_id: String,
        val auth_success_redirect_url: String,
        val auth_failure_redirect_url: String,
        val providers: String,
        val language: String = "en",
    )

    @Serializable
    private data class WidgetResponse(
        val url: String? = null,
        val session_id: String? = null,
        val status: String? = null,
        val message: String? = null,
    )

    @Serializable
    private data class AuthenticateUserResponse(
        val auth_url: String? = null,
        val user_id: String? = null,
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
    suspend fun authenticateUser(referenceId: String, resource: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$TERRA_BASE_URL${Path.AUTHENTICATE_USER}".toHttpUrl().newBuilder()
                    .addQueryParameter(Query.RESOURCE, resource)
                    .addQueryParameter(Query.REFERENCE_ID, referenceId)
                    .addQueryParameter(Query.AUTH_SUCCESS_REDIRECT_URL, TerraDeepLinks.SUCCESS_URL)
                    .addQueryParameter(Query.AUTH_FAILURE_REDIRECT_URL, TerraDeepLinks.FAILURE_URL)
                    .addQueryParameter(Query.LANGUAGE, DEFAULT_LANGUAGE)
                    .build()

                val request = terraRequest(url.toString())
                    .post(EMPTY_JSON_BODY.toRequestBody(TERRA_JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    if (!response.isSuccessful) error("Terra authenticateUser ${response.code}: $body")
                    val parsed = terraJson.decodeFromString(AuthenticateUserResponse.serializer(), body)
                    val authUrl = parsed.auth_url
                        ?: error("Terra authenticateUser returned no auth_url: $body")
                    Result.success(authUrl)
                }
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
                val payload = terraJson.encodeToString(
                    WidgetRequest.serializer(),
                    WidgetRequest(
                        reference_id = referenceId,
                        auth_success_redirect_url = TerraDeepLinks.SUCCESS_URL,
                        auth_failure_redirect_url = TerraDeepLinks.FAILURE_URL,
                        providers = providers,
                    ),
                )

                val request = terraRequest("$TERRA_BASE_URL${Path.GENERATE_WIDGET_SESSION}")
                    .post(payload.toRequestBody(TERRA_JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    if (!response.isSuccessful) error("Terra widget ${response.code}: $body")
                    val parsed = terraJson.decodeFromString(WidgetResponse.serializer(), body)
                    val url = parsed.url ?: error("Terra widget returned no url: $body")
                    Result.success(url)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    private object Path {
        const val AUTHENTICATE_USER = "/auth/authenticateUser"
        const val GENERATE_WIDGET_SESSION = "/auth/generateWidgetSession"
    }

    private object Query {
        const val RESOURCE = "resource"
        const val REFERENCE_ID = "reference_id"
        const val AUTH_SUCCESS_REDIRECT_URL = "auth_success_redirect_url"
        const val AUTH_FAILURE_REDIRECT_URL = "auth_failure_redirect_url"
        const val LANGUAGE = "language"
    }

    private companion object {
        const val DEFAULT_LANGUAGE = "en"
        const val EMPTY_JSON_BODY = "{}"
    }
}
