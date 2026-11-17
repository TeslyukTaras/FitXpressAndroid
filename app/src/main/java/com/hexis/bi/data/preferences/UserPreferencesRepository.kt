package com.hexis.bi.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.hexis.bi.data.store.AppPreferencesDataStore
import com.hexis.bi.data.store.AppPreferencesKeys
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    app: AppPreferencesDataStore,
) {

    private val store: DataStore<Preferences> = app.store
    private val k = AppPreferencesKeys.User

    val onboardingShown: Flow<Boolean> = store.data.map { prefs ->
        prefs[k.onboardingShown] ?: false
    }

    suspend fun setOnboardingShown() {
        store.edit { prefs ->
            prefs[k.onboardingShown] = true
        }
    }

    /**
     * One-time: copy local voice toggle to Firestore UserSettings when the field is still null
     * (e.g. existing installs that only had DataStore).
     */
    suspend fun ensureVoiceMigratedToFirestore(userRepository: UserRepository) {
        val already = store.data.map { it[k.voiceMigratedToFirestore] == true }.first()
        if (already) return
        val remote = userRepository.getUserSettings().getOrNull() ?: return
        if (remote.voiceGuidanceEnabled != null) {
            store.edit { it[k.voiceMigratedToFirestore] = true }
            return
        }
        val local = store.data.map { it[k.voiceGuidanceEnabled] ?: true }.first()
        userRepository.updateUserSettings(
            mapOf(FirestoreSchema.UserSettingsFields.VOICE_GUIDANCE_ENABLED to local),
        )
        store.edit { it[k.voiceMigratedToFirestore] = true }
    }

    suspend fun getLastScanTodayIsoWeek(): String? =
        store.data.map { it[k.lastScanTodayIsoWeek] }.first()

    suspend fun setLastScanTodayIsoWeek(weekKey: String) {
        store.edit { it[k.lastScanTodayIsoWeek] = weekKey }
    }

    suspend fun getLastScanNudgeIsoWeek(): String? =
        store.data.map { it[k.lastScanNudgeIsoWeek] }.first()

    suspend fun setLastScanNudgeIsoWeek(weekKey: String) {
        store.edit { it[k.lastScanNudgeIsoWeek] = weekKey }
    }

    suspend fun getLastScanMissedIsoWeek(): String? =
        store.data.map { it[k.lastScanMissedIsoWeek] }.first()

    suspend fun setLastScanMissedIsoWeek(weekKey: String) {
        store.edit { it[k.lastScanMissedIsoWeek] = weekKey }
    }

    suspend fun getFreeWeekAnchor(uid: String): String? =
        store.data.map { it[stringPreferencesKey(k.FREE_WEEK_ANCHOR_PREFIX + uid)] }.first()

    suspend fun setFreeWeekAnchor(uid: String, isoDate: String) {
        store.edit { it[stringPreferencesKey(k.FREE_WEEK_ANCHOR_PREFIX + uid)] = isoDate }
    }

    suspend fun isFreeWeekImported(uid: String): Boolean =
        store.data.map { it[stringPreferencesKey(k.FREE_WEEK_IMPORTED_PREFIX + uid)] != null }.first()

    suspend fun setFreeWeekImported(uid: String) {
        store.edit { it[stringPreferencesKey(k.FREE_WEEK_IMPORTED_PREFIX + uid)] = true.toString() }
    }

    suspend fun getFreeWeekImportRows(uid: String): Int? =
        store.data.map { it[stringPreferencesKey(k.FREE_WEEK_IMPORT_ROWS_PREFIX + uid)]?.toIntOrNull() }.first()

    suspend fun setFreeWeekImportRows(uid: String, rows: Int) {
        store.edit { it[stringPreferencesKey(k.FREE_WEEK_IMPORT_ROWS_PREFIX + uid)] = rows.toString() }
    }

    suspend fun getInsightsRunDate(uid: String): String? =
        store.data.map { it[stringPreferencesKey(k.INSIGHTS_RUN_DATE_PREFIX + uid)] }.first()

    suspend fun setInsightsRunDate(uid: String, isoDate: String) {
        store.edit { it[stringPreferencesKey(k.INSIGHTS_RUN_DATE_PREFIX + uid)] = isoDate }
    }

    suspend fun hasAskedNotifPermission(uid: String): Boolean =
        store.data.map { it[k.askedNotifPermissionUids].orEmpty() }.first().contains(uid)

    suspend fun markAskedNotifPermission(uid: String) {
        store.edit { prefs ->
            prefs[k.askedNotifPermissionUids] = prefs[k.askedNotifPermissionUids].orEmpty() + uid
        }
    }

    val bodyVisualMode: Flow<String?> = store.data.map { prefs ->
        prefs[k.bodyVisualMode]
    }

    suspend fun setBodyVisualMode(mode: String) {
        store.edit { prefs ->
            prefs[k.bodyVisualMode] = mode
        }
    }

    suspend fun isPersonalizeResultsHintShown(): Boolean =
        store.data.map { it[k.personalizeResultsHintShown] ?: false }.first()

    suspend fun setPersonalizeResultsHintShown() {
        store.edit { prefs ->
            prefs[k.personalizeResultsHintShown] = true
        }
    }

    suspend fun isMedicalDisclaimerAcknowledged(): Boolean =
        store.data.map { it[k.medicalDisclaimerAcknowledgedAt] != null }.first()

    suspend fun setMedicalDisclaimerAcknowledged() {
        store.edit { prefs ->
            prefs[k.medicalDisclaimerAcknowledgedAt] = System.currentTimeMillis()
        }
    }

    suspend fun clearAccountData() {
        store.edit { prefs ->
            val keepOnboarding = prefs[k.onboardingShown] == true
            prefs.clear()
            if (keepOnboarding) prefs[k.onboardingShown] = true
        }
    }
}
