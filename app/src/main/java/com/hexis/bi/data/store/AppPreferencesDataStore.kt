package com.hexis.bi.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val scanReminderTodaySent = stringPreferencesKey("scan_reminder_today_sent")
        val scanReminderNudgeSent = stringPreferencesKey("scan_reminder_nudge_sent")
        val scanReminderMissedSent = stringPreferencesKey("scan_reminder_missed_sent")
        val askedNotifPermissionUids = stringSetPreferencesKey("asked_notif_permission_uids")
        val bodyVisualMode = stringPreferencesKey("body_visual_mode")
        val personalizeResultsHintShown = booleanPreferencesKey("personalize_results_hint_shown")
        val medicalDisclaimerAcknowledgedAt = longPreferencesKey("medical_disclaimer_acknowledged_at")
        const val FREE_WEEK_ANCHOR_PREFIX = "free_week_anchor_"
        const val FREE_WEEK_IMPORTED_PREFIX = "free_week_imported_"
        const val FREE_WEEK_IMPORT_ROWS_PREFIX = "free_week_import_rows_"
        const val INSIGHTS_RUN_DATE_PREFIX = "insights_run_date_"
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
