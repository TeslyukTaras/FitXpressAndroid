package com.hexis.bi.data.user

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserSettings(
    val sleepGoalHours: Int? = null,
)
