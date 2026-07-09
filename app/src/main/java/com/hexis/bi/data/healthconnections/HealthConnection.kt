package com.hexis.bi.data.healthconnections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

/** Stored at `users/{uid}/settings/userSettings/healthConnections/{terraUserId}`. */
@IgnoreExtraProperties
data class HealthConnection(
    val terraUserId: String = "",
    val provider: String = "",
    val source: String = SOURCE_API,
    val connectedAt: Timestamp? = null,
    val active: Boolean = true,
) {
    companion object {
        const val SOURCE_SDK = "sdk"
        const val SOURCE_API = "api"
    }
}
