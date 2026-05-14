package com.hexis.bi.data.notification

import kotlinx.serialization.Serializable

/**
 * Persisted inbox row. Title/body are stored as **resolved strings**, never as `R.string`
 * resource IDs — Android resource IDs are not stable across rebuilds, so persisting them
 * causes a stale ID to resolve to an unrelated string on the next install/update.
 *
 * Trade-off: strings are resolved at write time, so the row stays in the locale the user
 * had when it was created. A later language switch will not retranslate existing rows.
 */
@Serializable
data class InboxItem(
    val id: String,
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAtEpochMillis: Long = 0L,
)
