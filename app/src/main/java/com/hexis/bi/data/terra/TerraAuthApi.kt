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
 * Generates single-use mobile SDK auth tokens.
 *
 * Production note: hits Terra with the api-key directly; move behind our own backend before launch.
 */
class TerraAuthApi(private val client: OkHttpClient) {

    @Serializable
    private data class TokenResponse(val token: String? = null, val status: String? = null)

    suspend fun generateAuthToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ENDPOINT)
                .addHeader("dev-id", TerraConfig.devId)
                .addHeader("x-api-key", TerraConfig.apiKey)
                .addHeader("Accept", "application/json")
                .post(EMPTY_JSON.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("Terra auth ${response.code}: $body")
                val parsed = Json { ignoreUnknownKeys = true }.decodeFromString<TokenResponse>(body)
                val token = parsed.token ?: error("Terra auth returned no token: $body")
                Result.success(token)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.tryterra.co/v2/auth/generateAuthToken"
        private const val EMPTY_JSON = "{}"
    }
}
