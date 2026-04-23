package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Thin REST client for Terra's server-side data endpoints.
 *
 * Uses our `dev-id` / `x-api-key` headers; Terra scopes responses by [TerraUserId].
 * All methods return parsed JSON — mapping into domain types happens at the
 * repository layer so we can swap schema pieces without touching HTTP code.
 */
interface TerraApi {
    suspend fun getSleep(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Result<TerraSleepApiResponse>
}

@Serializable
data class TerraSleepApiResponse(
    val status: String? = null,
    val type: String? = null,
    val data: List<JsonElement> = emptyList(),
    val message: String? = null,
)

class TerraApiImpl(private val client: OkHttpClient) : TerraApi {

    override suspend fun getSleep(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Result<TerraSleepApiResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/sleep".toHttpUrl().newBuilder()
                .addQueryParameter("user_id", terraUserId)
                .addQueryParameter("start_date", startDate.format(DATE_FMT))
                .addQueryParameter("end_date", endDate.format(DATE_FMT))
                .addQueryParameter("to_webhook", "false")
                .addQueryParameter("with_samples", "true")
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("dev-id", TerraConfig.devId)
                .addHeader("x-api-key", TerraConfig.apiKey)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Terra /sleep ${response.code}: $body")
                }
                val parsed = json.decodeFromString(TerraSleepApiResponse.serializer(), body)
                Result.success(parsed)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://api.tryterra.co/v2"
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
