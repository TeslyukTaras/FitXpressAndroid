package com.hexis.bi.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single on-device [DataStore] for all app [Preferences] (user settings, notification inbox, etc.).
 * Add new keys only under [AppPreferencesKeys]; do not create another `preferencesDataStore` file.
 */
object AppPreferences {
    const val DATA_STORE_FILE_NAME: String = "fitxpress_datastore"
}

object AppPreferencesKeys {
    object User {
        val onboardingShown = booleanPreferencesKey("onboarding_shown")
        val voiceGuidanceEnabled = booleanPreferencesKey("voice_guidance_enabled")
        val voiceMigratedToFirestore = booleanPreferencesKey("voice_migrated_to_firestore")
        val connectedSuitId = stringPreferencesKey("connected_suit_id")
        val lastScanTodayIsoWeek = stringPreferencesKey("last_scan_today_iso_week")
        val lastScanNudgeIsoWeek = stringPreferencesKey("last_scan_nudge_iso_week")
        val lastScanMissedIsoWeek = stringPreferencesKey("last_scan_missed_iso_week")
        val askedNotifPermissionUids = stringSetPreferencesKey("asked_notif_permission_uids")
    }

    object NotificationInbox {
        val itemsJson = stringPreferencesKey("inbox_items_v1")
    }
}

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = AppPreferences.DATA_STORE_FILE_NAME,
)

class AppPreferencesDataStore(
    context: Context,
) {
    val store: DataStore<Preferences> = context.applicationContext.appPreferencesDataStore
}
