package com.hexis.bi.data.auth

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import timber.log.Timber

class AccountDeletionApi(private val functions: FirebaseFunctions) {

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = functions.getHttpsCallable(FUNCTION_DELETE_ACCOUNT)
                .apply { setTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
                .call()
                .await()
            Timber.i("Account deletion succeeded: %s", result.data)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Account deletion failed")
            Result.failure(e)
        }
    }

    companion object {
        private const val CALL_TIMEOUT_SECONDS = 360L
        private const val FUNCTION_DELETE_ACCOUNT = "deleteAccount"
    }
}
