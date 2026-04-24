package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

/** Produces a Terra Widget auth URL for web-based providers (Oura, Garmin, …). */
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

                val request = terraRequest("$TERRA_BASE_URL/auth/generateWidgetSession")
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
}
