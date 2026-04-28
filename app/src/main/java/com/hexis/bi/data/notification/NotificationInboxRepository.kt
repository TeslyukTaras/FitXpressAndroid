package com.hexis.bi.data.notification

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
import java.util.UUID

class NotificationInboxRepository(
    app: AppPreferencesDataStore,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val itemEntriesKey: Preferences.Key<String> =
        AppPreferencesKeys.NotificationInbox.itemsJson
    private val store: DataStore<Preferences> = app.store

    val items: Flow<List<InboxItem>> = store.data.map { prefs ->
        val raw = prefs[itemEntriesKey] ?: return@map emptyList()
        decodeList(raw)
    }

    val unreadCount: Flow<Int> = items.map { list -> list.count { !it.isRead } }

    suspend fun appendInbox(
        titleRes: Int,
        bodyRes: Int,
        timeLabelRes: Int = R.string.notifications_time_just_now,
        bodyFormatArg: String? = null,
    ) {
        val now = System.currentTimeMillis()
        store.updateNotNull { list ->
            val new = InboxItem(
                id = UUID.randomUUID().toString(),
                titleRes = titleRes,
                bodyRes = bodyRes,
                timeLabelRes = timeLabelRes,
                isRead = false,
                createdAtEpochMillis = now,
                bodyFormatArg = bodyFormatArg,
            )
            (listOf(new) + list).take(NotificationInboxList.MAX_PERSISTED_ITEMS)
        }
    }

    suspend fun appendRawInbox(title: String, body: String) {
        val now = System.currentTimeMillis()
        store.updateNotNull { list ->
            val new = InboxItem(
                id = UUID.randomUUID().toString(),
                timeLabelRes = R.string.notifications_time_just_now,
                isRead = false,
                createdAtEpochMillis = now,
                titleText = title,
                bodyText = body,
            )
            (listOf(new) + list).take(NotificationInboxList.MAX_PERSISTED_ITEMS)
        }
    }

    suspend fun appendNextScheduledScanInboxDeduped(localizedDayName: String) {
        val titleRes = R.string.notif_next_scheduled_scan_title
        val bodyRes = R.string.notif_next_scheduled_scan_body
        store.updateNotNull { list ->
            if (list.any { it.titleRes == titleRes && it.bodyFormatArg == localizedDayName }) list
            else appendToList(
                list,
                InboxItem(
                    id = UUID.randomUUID().toString(),
                    titleRes = titleRes,
                    bodyRes = bodyRes,
                    timeLabelRes = R.string.notifications_time_just_now,
                    isRead = false,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    bodyFormatArg = localizedDayName,
                ),
            )
        }
    }

    private fun appendToList(
        list: List<InboxItem>,
        item: InboxItem,
    ) = (listOf(item) + list).take(NotificationInboxList.MAX_PERSISTED_ITEMS)

    suspend fun markAllRead() {
        store.updateNotNull { list -> list.map { it.copy(isRead = true) } }
    }

    suspend fun markRead(id: String) {
        store.updateNotNull { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    private fun decodeList(raw: String): List<InboxItem> = runCatching {
        json.decodeFromString(ListSerializer(InboxItem.serializer()), raw)
    }.getOrElse { emptyList() }

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
