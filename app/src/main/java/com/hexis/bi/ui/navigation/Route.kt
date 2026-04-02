package com.hexis.bi.ui.navigation

internal object Route {
    const val APP_INFO = "app_info"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val MAIN = "main"
    const val FORGOT_PASSWORD = "forgot_password"

    object Main {
        const val HOME = "main/home"
        const val BODY = "main/body"
        const val NOTIFICATIONS = "main/notifications"
        const val SETTINGS = "main/settings"
        const val EDIT_PROFILE = "main/edit_profile"
        const val HEALTH_CONNECTIONS = "main/health_connections"

        /** Routes that show the bottom navigation bar. */
        val tabRoutes = setOf(HOME, BODY)
    }
}
