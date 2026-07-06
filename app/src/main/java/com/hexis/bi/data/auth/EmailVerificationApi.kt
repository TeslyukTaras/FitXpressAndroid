package com.hexis.bi.data.auth

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Wraps the email-verification callables. Failures surface the underlying
 * [com.google.firebase.functions.FirebaseFunctionsException] so [FirebaseAuthRepository] can map
 * its `code` to a user-facing message.
 */
class EmailVerificationApi(private val functions: FirebaseFunctions) {

    suspend fun sendCode(): Result<Unit> = call(FUNCTION_SEND_CODE, emptyMap())

    suspend fun verifyCode(code: String): Result<Unit> =
        call(FUNCTION_VERIFY_CODE, mapOf(FIELD_CODE to code))

    private suspend fun call(name: String, payload: Map<String, Any>): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                functions.getHttpsCallable(name).call(payload).await()
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    companion object {
        private const val FUNCTION_SEND_CODE = "sendEmailVerificationCode"
        private const val FUNCTION_VERIFY_CODE = "verifyEmailCode"
        private const val FIELD_CODE = "code"
    }
}
