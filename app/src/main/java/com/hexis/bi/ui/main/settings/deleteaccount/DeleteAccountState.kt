package com.hexis.bi.ui.main.settings.deleteaccount

enum class AuthProvider { EMAIL, GOOGLE, APPLE, UNKNOWN }

data class DeleteAccountState(
    val showDialog: Boolean = false,
    val provider: AuthProvider = AuthProvider.UNKNOWN,
    val password: String = "",
    val passwordVisible: Boolean = false,
    val passwordError: String? = null,
)
