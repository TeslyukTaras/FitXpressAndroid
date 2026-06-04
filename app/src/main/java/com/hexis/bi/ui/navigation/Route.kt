package com.hexis.bi.ui.navigation

internal object Route {
    const val APP_INFO = "app_info"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val MAIN = "main"
    const val PROFILE_ONBOARDING = "profile_onboarding"
    const val FORGOT_PASSWORD = "forgot_password"

    object Main {
        const val HOME = "main/home"
        const val BODY = "main/body"
        const val PHYSIQUE_BALANCE = "main/body/physique_balance"
        const val NOTIFICATIONS = "main/notifications"
        const val SETTINGS = "main/settings"
        const val EDIT_PROFILE = "main/edit_profile"
        const val HEALTH_CONNECTIONS = "main/health_connections"
        const val NOTIFICATION_SETTINGS = "main/notification_settings"
        const val SCAN_PREFERENCES = "main/scan_preferences"
        const val MY_SUIT = "main/my_suit"
        const val SLEEP = "main/sleep"
        const val RECOVERY = "main/recovery"
        const val LONGEVITY = "main/longevity"
        const val PHYSIQUE_DRIFT = "main/physique_drift"
        const val PACE_OF_AGING = "main/pace_of_aging"
        const val ACTIVITY = "main/activity"
        const val SCAN = "main/scan"
        const val SCAN_RESULTS = "main/scan/results"
        const val SCAN_HISTORY = "main/scan/history"

        /** Routes that show the bottom navigation bar. */
        val tabRoutes = setOf(HOME, BODY)
    }
}
