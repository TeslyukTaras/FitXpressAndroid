package com.hexis.bi.data.terra

import com.google.firebase.functions.FirebaseFunctions
import com.hexis.bi.data.firebase.toJsonElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Generates single-use mobile SDK auth tokens.
 */
class TerraAuthApi(private val functions: FirebaseFunctions) {

    @Serializable
    private data class TokenResponse(val token: String? = null, val status: String? = null)

    suspend fun generateAuthToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val data = functions
                .getHttpsCallable(terraFunction(FUNCTION_GENERATE_AUTH_TOKEN))
                .call()
                .await()
                .data
            val parsed =
                terraJson.decodeFromJsonElement(TokenResponse.serializer(), data.toJsonElement())
            val token = parsed.token ?: error("Terra auth returned no token")
            Result.success(token)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    companion object {
        private const val FUNCTION_GENERATE_AUTH_TOKEN = "GenerateAuthToken"
    }
}
