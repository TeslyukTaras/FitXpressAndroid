package com.hexis.bi.data.auth

internal object FirebaseAuthErrorCodes {
    const val INVALID_EMAIL = "ERROR_INVALID_EMAIL"
    const val WRONG_PASSWORD = "ERROR_WRONG_PASSWORD"
    const val INVALID_CREDENTIAL = "ERROR_INVALID_CREDENTIAL"
    const val USER_NOT_FOUND = "ERROR_USER_NOT_FOUND"
    const val EMAIL_ALREADY_IN_USE = "ERROR_EMAIL_ALREADY_IN_USE"
    const val WEAK_PASSWORD = "ERROR_WEAK_PASSWORD"
    const val USER_DISABLED = "ERROR_USER_DISABLED"
    const val TOO_MANY_REQUESTS = "ERROR_TOO_MANY_REQUESTS"
    const val NETWORK_REQUEST_FAILED = "ERROR_NETWORK_REQUEST_FAILED"
    const val OPERATION_NOT_ALLOWED = "ERROR_OPERATION_NOT_ALLOWED"

    // Custom codes for precondition failures not covered by FirebaseAuthException
    const val NO_CURRENT_USER = "ERROR_NO_CURRENT_USER"
    const val NO_EMAIL_ON_ACCOUNT = "ERROR_NO_EMAIL_ON_ACCOUNT"
}

/**
 * Thrown for auth preconditions that have no matching [com.google.firebase.auth.FirebaseAuthException].
 * Processed by [mapAuthError] alongside Firebase SDK exceptions.
 */
internal class FirebaseAuthCodeException(val errorCode: String) : Exception(errorCode)
