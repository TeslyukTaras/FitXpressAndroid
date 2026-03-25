package com.hexis.bi.data.auth

import android.app.Activity
import android.content.Context

interface AuthRepository {
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
}
