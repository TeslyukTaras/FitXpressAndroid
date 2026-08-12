package com.hexis.bi.data.healthconnect

import co.tryterra.terra.enums.CustomPermissions

internal object HealthConnectPermissions {

    val TERRA_CUSTOM_PERMISSIONS: Set<CustomPermissions> = setOf(
        CustomPermissions.SLEEP_ANALYSIS,
        CustomPermissions.HEART_RATE,
        CustomPermissions.HEART_RATE_VARIABILITY,
        CustomPermissions.RESTING_HEART_RATE,
        CustomPermissions.STEPS,
        CustomPermissions.FLIGHTS_CLIMBED,
        CustomPermissions.EXERCISE_DISTANCE,
        CustomPermissions.CALORIES,
        CustomPermissions.ACTIVE_DURATIONS,
        CustomPermissions.WORKOUT_TYPE,
        CustomPermissions.ACTIVITY_SUMMARY,
        CustomPermissions.OXYGEN_SATURATION,
        CustomPermissions.RESPIRATORY_RATE,
        CustomPermissions.VO2MAX,
    )

    val CORE_MANIFEST_PERMISSIONS: Set<String> = setOf(
        "android.permission.health.READ_SLEEP",
        "android.permission.health.READ_STEPS",
        "android.permission.health.READ_DISTANCE",
        "android.permission.health.READ_ACTIVE_CALORIES_BURNED",
        "android.permission.health.READ_HEART_RATE",
    )

    val OPTIONAL_MANIFEST_PERMISSIONS: Set<String> = setOf(
        "android.permission.health.READ_HEART_RATE_VARIABILITY",
        "android.permission.health.READ_RESTING_HEART_RATE",
        "android.permission.health.READ_FLOORS_CLIMBED",
        "android.permission.health.READ_TOTAL_CALORIES_BURNED",
        "android.permission.health.READ_EXERCISE",
        "android.permission.health.READ_OXYGEN_SATURATION",
        "android.permission.health.READ_RESPIRATORY_RATE",
        "android.permission.health.READ_VO2_MAX",
    )

    val REQUIRED_MANIFEST_PERMISSIONS: Set<String> =
        CORE_MANIFEST_PERMISSIONS + OPTIONAL_MANIFEST_PERMISSIONS
}
