package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Generates Terra Widget auth sessions for web-based providers (Oura, Garmin, Fitbit, …).
 *
 * Production note: hits Terra with the api-key directly; move behind our own backend before launch.
 */
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

    /** @param referenceId Firebase UID — Terra echoes it back via webhook/redirect. */
    suspend fun generateWidgetSession(referenceId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val payload = json.encodeToString(
                    WidgetRequest.serializer(),
                    WidgetRequest(
                        reference_id = referenceId,
                        auth_success_redirect_url = TerraDeepLinks.SUCCESS_URL,
                        auth_failure_redirect_url = TerraDeepLinks.FAILURE_URL,
                        providers = DEFAULT_PROVIDERS,
                    ),
                )

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("dev-id", TerraConfig.devId)
                    .addHeader("x-api-key", TerraConfig.apiKey)
                    .addHeader("Accept", "application/json")
                    .post(payload.toRequestBody(JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("Terra widget ${response.code}: $body")
                    }
                    val parsed = json.decodeFromString(WidgetResponse.serializer(), body)
                    val url = parsed.url ?: error("Terra widget returned no url: $body")
                    Result.success(url)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    companion object {
        private const val ENDPOINT = "https://api.tryterra.co/v2/auth/generateWidgetSession"
        private val JSON_MEDIA = "application/json".toMediaType()
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        // HC / Samsung Health still go through the on-device SDK path.
        private const val DEFAULT_PROVIDERS =
            "OURA,GARMIN,FITBIT,WHOOP,POLAR,COROS,SUUNTO,WITHINGS,STRAVA"
    }
}
