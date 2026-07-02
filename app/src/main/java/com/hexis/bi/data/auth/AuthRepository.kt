package com.hexis.bi.data.auth

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<Boolean>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
    ): Result<Unit>
    suspend fun signInWithGoogle(context: Context): Result<Unit>
    suspend fun signInWithApple(activity: Activity): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun deleteAccountWithPassword(password: String): Result<Unit>
    suspend fun deleteAccountWithGoogle(context: Context): Result<Unit>
    suspend fun deleteAccountWithApple(activity: Activity): Result<Unit>
    suspend fun signOut()
}
