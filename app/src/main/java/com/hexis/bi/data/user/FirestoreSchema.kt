package com.hexis.bi.data.user

internal object FirestoreSchema {
    const val USERS_COLLECTION = "users"
    const val SETTINGS_COLLECTION = "settings"
    const val USER_SETTINGS_DOC = "userSettings"
    const val HEALTH_CONNECTIONS_COLLECTION = "healthConnections"

    object UserFields {
        const val AVATAR_URL = "avatarUrl"
    }

    object UserSettingsFields {
        const val SLEEP_GOAL_HOURS = "sleepGoalHours"
        const val PUSH_NOTIFICATIONS_ENABLED = "pushNotificationsEnabled"
        const val VOICE_GUIDANCE_ENABLED = "voiceGuidanceEnabled"
        const val SCAN_REMINDERS_ENABLED = "scanRemindersEnabled"
        const val REMINDER_DAY = "reminderDay"
        const val REMINDER_HOUR = "reminderHour"
    }

    object HealthConnectionFields {
        const val TERRA_USER_ID = "terraUserId"
        const val PROVIDER = "provider"
        const val SOURCE = "source"
        const val CONNECTED_AT = "connectedAt"
        const val ACTIVE = "active"
    }
}
