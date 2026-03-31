package com.hexis.bi.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import com.hexis.bi.R
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val context: Context,
) : AuthRepository {

    override val authState: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            Unit
        }.mapAuthError(context)

    override suspend fun signUpWithEmail(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
    ): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName("$firstName $lastName")
            .build()
        result.user?.updateProfile(profileUpdate)?.await()
        Unit
    }.mapAuthError(context)

    override suspend fun signInWithGoogle(context: Context): Result<Unit> = runCatching {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(this.context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val result = credentialManager.getCredential(context = context, request = request)
        val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential).await()
        Unit
    }.mapAuthError(context)

    override suspend fun signInWithApple(activity: Activity): Result<Unit> = runCatching {
        val provider = OAuthProvider.newBuilder("apple.com").build()
        auth.startActivityForSignInWithProvider(activity, provider).await()
        Unit
    }.mapAuthError(context)

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }.mapAuthError(context)

    override suspend fun signOut() {
        auth.signOut()
    }
}

private fun <T> Result<T>.mapAuthError(context: Context): Result<T> {
    val error = exceptionOrNull() ?: return this
    val friendly = when ((error as? FirebaseAuthException)?.errorCode) {
        "ERROR_INVALID_EMAIL" -> context.getString(R.string.error_auth_invalid_email)
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL" -> context.getString(R.string.error_auth_wrong_password)
        "ERROR_USER_NOT_FOUND" -> context.getString(R.string.error_auth_user_not_found)
        "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.error_auth_email_in_use)
        "ERROR_WEAK_PASSWORD" -> context.getString(R.string.error_auth_weak_password)
        "ERROR_USER_DISABLED" -> context.getString(R.string.error_auth_user_disabled)
        "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.error_auth_too_many_requests)
        "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.error_auth_network)
        "ERROR_OPERATION_NOT_ALLOWED" -> context.getString(R.string.error_auth_operation_not_allowed)
        else -> return this
    }
    return Result.failure(Exception(friendly))
}
