package com.hexis.bi.data.terra

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import timber.log.Timber
import com.hexis.bi.utils.redactSensitiveId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Thin REST client for Terra's server-side data endpoints.
 *
 * Uses our `dev-id` / `x-api-key` headers; Terra scopes responses by `user_id`.
 * Returns parsed JSON — mapping into domain types happens at the repository layer.
 */
class TerraApi(private val client: OkHttpClient) {

    suspend fun getDaily(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Result<TerraDataListResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$TERRA_BASE_URL${TerraApiConstants.Path.DAILY}".toHttpUrl().newBuilder()
                .addQueryParameter(TerraApiConstants.Query.USER_ID, terraUserId)
                .addQueryParameter(TerraApiConstants.Query.START_DATE, startDate.format(DATE_FMT))
                .addQueryParameter(TerraApiConstants.Query.END_DATE, endDate.format(DATE_FMT))
                .addQueryParameter(TerraApiConstants.Query.TO_WEBHOOK, TerraApiConstants.QueryValue.FALSE)
                .addQueryParameter(TerraApiConstants.Query.WITH_SAMPLES, TerraApiConstants.QueryValue.TRUE)
                .build()

            client.newCall(terraRequest(url.toString()).get().build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("Terra ${TerraApiConstants.Path.DAILY} ${response.code}: $body")
                logTerraRawJson("DAILY", terraUserId, startDate, endDate, body)
                Result.success(terraJson.decodeFromString(TerraDataListResponse.serializer(), body))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun getSleep(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Result<TerraDataListResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$TERRA_BASE_URL${TerraApiConstants.Path.SLEEP}".toHttpUrl().newBuilder()
                .addQueryParameter(TerraApiConstants.Query.USER_ID, terraUserId)
                .addQueryParameter(TerraApiConstants.Query.START_DATE, startDate.format(DATE_FMT))
                .addQueryParameter(TerraApiConstants.Query.END_DATE, endDate.format(DATE_FMT))
                .addQueryParameter(TerraApiConstants.Query.TO_WEBHOOK, TerraApiConstants.QueryValue.FALSE)
                .addQueryParameter(TerraApiConstants.Query.WITH_SAMPLES, TerraApiConstants.QueryValue.TRUE)
                .build()

            client.newCall(terraRequest(url.toString()).get().build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("Terra ${TerraApiConstants.Path.SLEEP} ${response.code}: $body")
                logTerraRawJson("SLEEP", terraUserId, startDate, endDate, body)
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
            val url = "$TERRA_BASE_URL${TerraApiConstants.Path.DEAUTHENTICATE_USER}".toHttpUrl().newBuilder()
                .addQueryParameter(TerraApiConstants.Query.USER_ID, terraUserId)
                .build()
            val request = terraRequest(url.toString()).delete().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful && response.code != 404) {
                    error("Terra DELETE ${TerraApiConstants.Path.DEAUTHENTICATE_USER} ${response.code}: $body")
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

private const val TERRA_LOG_CHUNK_CHARS = 3500

private fun logTerraRawJson(
    endpoint: String,
    terraUserId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    body: String,
) {
    val tag = "TERRA_RAW"
    val header = "[$endpoint] user=${redactSensitiveId(terraUserId)} range=[$startDate..$endDate] bytes=${body.length}"
    Timber.tag(tag).d(header)
    // Logcat truncates lines around 4 KB; chunk so the full JSON is recoverable.
    var i = 0
    var part = 1
    while (i < body.length) {
        val end = (i + TERRA_LOG_CHUNK_CHARS).coerceAtMost(body.length)
        Timber.tag(tag).d("[$endpoint][part $part] ${body.substring(i, end)}")
        i = end
        part++
    }
}

private object TerraApiConstants {
    object Path {
        const val DAILY = "/daily"
        const val SLEEP = "/sleep"
        const val DEAUTHENTICATE_USER = "/auth/deauthenticateUser"
    }

    object Query {
        const val USER_ID = "user_id"
        const val START_DATE = "start_date"
        const val END_DATE = "end_date"
        const val TO_WEBHOOK = "to_webhook"
        const val WITH_SAMPLES = "with_samples"
    }

    object QueryValue {
        const val TRUE = "true"
        const val FALSE = "false"
    }
}

@Serializable
data class TerraDataListResponse(
    val status: String? = null,
    val type: String? = null,
    val data: List<JsonElement> = emptyList(),
    val message: String? = null,
)
