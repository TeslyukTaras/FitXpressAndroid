package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Thin REST client for Terra's server-side data endpoints.
 *
 * Uses our `dev-id` / `x-api-key` headers; Terra scopes responses by `user_id`.
 * Returns parsed JSON — mapping into domain types happens at the repository layer.
 */
class TerraApi(private val client: OkHttpClient) {

    suspend fun getSleep(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Result<TerraDataListResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$TERRA_BASE_URL/sleep".toHttpUrl().newBuilder()
                .addQueryParameter("user_id", terraUserId)
                .addQueryParameter("start_date", startDate.format(DATE_FMT))
                .addQueryParameter("end_date", endDate.format(DATE_FMT))
                .addQueryParameter("to_webhook", "false")
                .addQueryParameter("with_samples", "true")
                .build()

            client.newCall(terraRequest(url.toString()).get().build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("Terra /sleep ${response.code}: $body")
                Result.success(terraJson.decodeFromString(TerraDataListResponse.serializer(), body))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /** `DELETE /v2/auth/deauthenticateUser?user_id=…` — revokes Terra’s access for that user id. */
    suspend fun deauthenticateUser(terraUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$TERRA_BASE_URL/auth/deauthenticateUser".toHttpUrl().newBuilder()
                .addQueryParameter("user_id", terraUserId)
                .build()
            val request = terraRequest(url.toString()).delete().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful && response.code != 404) {
                    error("Terra DELETE /auth/deauthenticateUser ${response.code}: $body")
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

@Serializable
data class TerraDataListResponse(
    val status: String? = null,
    val type: String? = null,
    val data: List<JsonElement> = emptyList(),
    val message: String? = null,
)
