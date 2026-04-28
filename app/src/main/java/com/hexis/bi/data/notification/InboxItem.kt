package com.hexis.bi.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class InboxItem(
    val id: String,
    val titleRes: Int = 0,
    val bodyRes: Int = 0,
    val timeLabelRes: Int = 0,
    val isRead: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
    /** Optional pre-resolved format argument for the [bodyRes] template (e.g. a localized day name). */
    val bodyFormatArg: String? = null,
    /** Raw text used by remote messages (FCM). When set, takes precedence over [titleRes]/[bodyRes]. */
    val titleText: String? = null,
    val bodyText: String? = null,
)
