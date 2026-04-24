package com.hexis.bi.data.user

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserSettings(
    val sleepGoalHours: Int? = null,
    val pushNotificationsEnabled: Boolean? = null,
    val voiceGuidanceEnabled: Boolean? = null,
    val scanRemindersEnabled: Boolean? = null,
    val reminderDay: String? = null,
    val reminderHour: Int? = null,
)
