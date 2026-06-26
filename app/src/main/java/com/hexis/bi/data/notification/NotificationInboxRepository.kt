package com.hexis.bi.data.notification

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hexis.bi.R
import com.hexis.bi.data.store.AppPreferencesDataStore
import com.hexis.bi.data.store.AppPreferencesKeys
import com.hexis.bi.utils.constants.NotificationInboxList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

/**
 * Inbox of in-app notification rows, persisted in DataStore.
 *
 * Strings are resolved against [context] at **write time** and stored as plain text — see
 * [InboxItem] for why we don't persist `R.string` resource IDs.
 */
class NotificationInboxRepository(
    private val context: Context,
    app: AppPreferencesDataStore,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val itemEntriesKey: Preferences.Key<String> =
        AppPreferencesKeys.NotificationInbox.itemsJson
    private val store: DataStore<Preferences> = app.store

    val items: Flow<List<InboxItem>> = store.data.map { prefs ->
        val raw = prefs[itemEntriesKey] ?: return@map emptyList()
        // Drop legacy rows written under the previous schema: their JSON had `titleRes` /
        // `bodyRes` ints but no `title` / `body` strings, so they decode with blank fields.
        decodeList(raw).filter { it.title.isNotBlank() }
    }

    val unreadCount: Flow<Int> = items.map { list -> list.count { !it.isRead } }

    suspend fun appendInbox(
        @StringRes titleRes: Int,
        @StringRes bodyRes: Int,
        bodyFormatArg: String? = null,
    ) {
        val title = context.getString(titleRes)
        val body = if (bodyFormatArg != null) context.getString(bodyRes, bodyFormatArg)
        else context.getString(bodyRes)
        appendResolved(title, body)
    }

    suspend fun appendRawInbox(title: String, body: String) {
        appendResolved(title, body)
    }

    suspend fun appendNextScheduledScanInboxDeduped(localizedDayName: String) {
        val title = context.getString(R.string.notif_next_scheduled_scan_title)
        val body = context.getString(R.string.notif_next_scheduled_scan_body, localizedDayName)
        store.updateNotNull { list ->
            if (list.any { it.title == title && it.body == body }) list
            else appendToList(list, newItem(title, body))
        }
    }

    suspend fun markAllRead() {
        store.updateNotNull { list -> list.map { it.copy(isRead = true) } }
    }

    suspend fun markRead(id: String) {
        store.updateNotNull { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    private suspend fun appendResolved(title: String, body: String) {
        store.updateNotNull { list -> appendToList(list, newItem(title, body)) }
    }

    private fun newItem(title: String, body: String) = InboxItem(
        id = UUID.randomUUID().toString(),
        title = title,
        body = body,
        isRead = false,
        createdAtEpochMillis = System.currentTimeMillis(),
    )

    private fun appendToList(
        list: List<InboxItem>,
        item: InboxItem,
    ) = (listOf(item) + list).take(NotificationInboxList.MAX_PERSISTED_ITEMS)

    private fun decodeList(raw: String): List<InboxItem> = runCatching {
        json.decodeFromString(ListSerializer(InboxItem.serializer()), raw)
    }.getOrElse { error ->
        Timber.w(error, "Failed to decode inbox payload")
        emptyList()
    }

    private fun encodeList(list: List<InboxItem>): String =
        json.encodeToString(ListSerializer(InboxItem.serializer()), list)

    private suspend fun DataStore<Preferences>.updateNotNull(
        block: (List<InboxItem>) -> List<InboxItem>,
    ) = edit { prefs ->
        val raw = prefs[itemEntriesKey]
        val list = if (raw == null) emptyList() else decodeList(raw)
        prefs[itemEntriesKey] = encodeList(block(list))
    }
}
