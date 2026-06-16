package com.hexis.bi.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

    val connectedSuitId: Flow<String> = store.data.map { prefs ->
        prefs[k.connectedSuitId] ?: ""
    }

    suspend fun setConnectedSuitId(id: String) {
        store.edit { prefs ->
            prefs[k.connectedSuitId] = id
        }
    }

    suspend fun clearConnectedSuitId() {
        store.edit { prefs ->
            prefs.remove(k.connectedSuitId)
        }
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

    suspend fun clearAccountData() {
        store.edit { prefs ->
            val keepOnboarding = prefs[k.onboardingShown] == true
            prefs.clear()
            if (keepOnboarding) prefs[k.onboardingShown] = true
        }
    }
}
