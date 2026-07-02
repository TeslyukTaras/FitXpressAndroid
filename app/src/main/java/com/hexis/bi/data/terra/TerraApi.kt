package com.hexis.bi.data.terra

import com.google.firebase.functions.FirebaseFunctions
import com.hexis.bi.data.firebase.toJsonElement
import com.hexis.bi.utils.redactSensitiveId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Thin REST client for Terra's server-side data endpoints.
 *
 * Calls Firebase Functions; the backend adds Terra's `dev-id` / `x-api-key` headers.
 * Returns parsed JSON — mapping into domain types happens at the repository layer.
 */
class TerraApi(private val functions: FirebaseFunctions) {

    suspend fun getDaily(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<TerraDataListResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = terraRangePayload(terraUserId, startDate, endDate, detail)
            val data = functions.getHttpsCallable(terraFunction(FUNCTION_GET_DAILY)).call(payload)
                .await().data
            logTerraRawJson("DAILY", terraUserId, startDate, endDate, data)
            Result.success(
                terraJson.decodeFromJsonElement(
                    TerraDataListResponse.serializer(),
                    data.toJsonElement()
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun getSleep(
        terraUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        detail: TerraDetail = TerraDetail.NONE,
    ): Result<TerraDataListResponse> = withContext(Dispatchers.IO) {
        try {
            val payload = terraRangePayload(terraUserId, startDate, endDate, detail)
            val data = functions.getHttpsCallable(terraFunction(FUNCTION_GET_SLEEP)).call(payload)
                .await().data
            logTerraRawJson("SLEEP", terraUserId, startDate, endDate, data)
            Result.success(
                terraJson.decodeFromJsonElement(
                    TerraDataListResponse.serializer(),
                    data.toJsonElement()
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * `GET /v2/userInfo?user_id=…` — the authentication state for a Terra user id. Used to
     * confirm a connection on return to the app instead of relying on the OAuth redirect.
     */
    suspend fun getUserInfo(terraUserId: String): Result<TerraUserInfoResponse> =
        withContext(Dispatchers.IO) {
            try {
                val data = functions
                    .getHttpsCallable(terraFunction(FUNCTION_GET_USER_INFO))
                    .call(mapOf(FIELD_TERRA_USER_ID to terraUserId))
                    .await()
                    .data
                Result.success(
                    terraJson.decodeFromJsonElement(
                        TerraUserInfoResponse.serializer(),
                        data.toJsonElement()
                    ),
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    /** `DELETE /v2/auth/deauthenticateUser?user_id=…` — revokes Terra’s access for that user id. */
    suspend fun deauthenticateUser(terraUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                functions
                    .getHttpsCallable(terraFunction(FUNCTION_DEAUTHENTICATE_USER))
                    .call(mapOf(FIELD_TERRA_USER_ID to terraUserId))
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    companion object {
        private const val FUNCTION_GET_DAILY = "GetDaily"
        private const val FUNCTION_GET_SLEEP = "GetSleep"
        private const val FUNCTION_GET_USER_INFO = "GetUserInfo"
        private const val FUNCTION_DEAUTHENTICATE_USER = "DeauthenticateUser"
        private const val FIELD_TERRA_USER_ID = "terraUserId"
    }
}

internal val terraJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun terraRangePayload(
    terraUserId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    detail: TerraDetail,
): Map<String, Any> = mapOf(
    "terraUserId" to terraUserId,
    "startDate" to startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
    "endDate" to endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
    "detail" to detail.wire,
)

private fun logTerraRawJson(
    endpoint: String,
    terraUserId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    body: Any?,
) {
    if (Timber.treeCount == 0) return
    Timber.tag("TerraApi").d(
        "[%s] user=%s range=[%s..%s] bytes=%d",
        endpoint, redactSensitiveId(terraUserId), startDate, endDate, body.toString().length,
    )
}

@Serializable
data class TerraDataListResponse(
    val status: String? = null,
    val type: String? = null,
    val data: List<JsonElement> = emptyList(),
    val message: String? = null,
)

@Serializable
data class TerraUserInfoResponse(
    val status: String? = null,
    val message: String? = null,
    // Top-level flag Terra returns from /userInfo; true only once the OAuth actually completed,
    // so it cleanly distinguishes a finished connection from a cancelled/abandoned attempt.
    val is_authenticated: Boolean? = null,
    val user: TerraUserInfo? = null,
) {
    /** Whether this user id represents a live, authorized connection. */
    val isConnected: Boolean
        get() = status?.equals("success", ignoreCase = true) == true &&
                is_authenticated == true &&
                !user?.provider.isNullOrBlank()
}

@Serializable
data class TerraUserInfo(
    val provider: String? = null,
)
