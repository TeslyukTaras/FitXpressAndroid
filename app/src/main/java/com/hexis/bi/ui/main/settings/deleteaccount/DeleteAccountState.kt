package com.hexis.bi.ui.main.settings.deleteaccount

import com.hexis.bi.domain.subscription.StorePlatform

enum class AuthProvider { EMAIL, GOOGLE, APPLE, UNKNOWN }

data class RenewingSubscription(val platform: StorePlatform)

data class DeleteAccountState(
    val showDialog: Boolean = false,
    val provider: AuthProvider = AuthProvider.UNKNOWN,
    val password: String = "",
    val passwordVisible: Boolean = false,
    val passwordError: String? = null,
    val renewingSubscription: RenewingSubscription? = null,
)
