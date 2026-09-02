package com.hexis.bi.data.user

internal object FirestoreSchema {
    const val USERS_COLLECTION = "users"
    const val SETTINGS_COLLECTION = "settings"
    const val USER_SETTINGS_DOC = "userSettings"
    const val PHYSIQUE_PREDICTION_DOC = "physiquePrediction"
    const val PREDICTION_HISTORY_COLLECTION = "history"
    const val HEALTH_CONNECTIONS_COLLECTION = "healthConnections"

    object UserFields {
        const val IMAGE_URL = "imageUrl"
        const val UNIT_SYSTEM = "unitSystem"
        const val SUIT_ID = "suitId"
    }

    object UserSettingsFields {
        const val SLEEP_GOAL_HOURS = "sleepGoalHours"
        const val STEPS_GOAL = "stepsGoal"
        const val SHOW_ACTIVE_CALORIES = "showActiveCalories"
        const val ACTIVITY_DATA_SOURCE = "activityDataSource"
        const val SLEEP_DATA_SOURCE = "sleepDataSource"
        const val PUSH_NOTIFICATIONS_ENABLED = "pushNotificationsEnabled"
        const val VOICE_GUIDANCE_ENABLED = "voiceGuidanceEnabled"
        const val SCAN_REMINDERS_ENABLED = "scanRemindersEnabled"
        const val REMINDER_DAY = "reminderDay"
        const val REMINDER_HOUR = "reminderHour"
        const val MEASUREMENT_ZONES = "measurementZones"
    }

    object PhysiquePredictionFields {
        const val SCHEMA_VERSION = "schemaVersion"
        const val ALGORITHM_VERSION = "algorithmVersion"
        const val GAINS = "gains"
        const val UPDATE_COUNT = "updateCount"
        const val UPDATED_AT = "updatedAt"
        const val PENDING = "pending"
        const val SOURCE_SCAN_ID = "sourceScanId"
        const val ACTUAL_SCAN_ID = "actualScanId"
        const val SOURCE_AT = "sourceAt"
        const val TARGET_AT = "targetAt"
        const val ACTUAL_AT = "actualAt"
        const val SCANS_USED = "scansUsed"
        const val ELAPSED_DAYS = "elapsedDays"
        const val GAINS_USED = "gainsUsed"
        const val GAINS_BEFORE = "gainsBefore"
        const val GAINS_AFTER = "gainsAfter"
        const val SLOPE_PER_DAY = "slopePerDay"
        const val SOURCE_VALUE = "sourceValue"
        const val ACTUAL_VALUE = "actualValue"
        const val PREDICTED = "predicted"
        const val EXPECTED_DELTA = "expectedDelta"
        const val ACTUAL_DELTA = "actualDelta"
        const val RATIO = "ratio"
        const val SKIPPED = "skipped"
    }

    object HealthConnectionFields {
        const val TERRA_USER_ID = "terraUserId"
        const val PROVIDER = "provider"
        const val SOURCE = "source"
        const val CONNECTED_AT = "connectedAt"
        const val ACTIVE = "active"
    }
}
