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
        const val STEPS_GOAL = "stepsGoal"
        const val SHOW_ACTIVE_CALORIES = "showActiveCalories"
        const val ACTIVITY_DATA_SOURCE = "activityDataSource"
    }

    object HealthConnectionFields {
        const val TERRA_USER_ID = "terraUserId"
        const val PROVIDER = "provider"
        const val SOURCE = "source"
        const val CONNECTED_AT = "connectedAt"
        const val ACTIVE = "active"
    }
}
