package com.hexis.bi.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val onboardingShownKey = booleanPreferencesKey("onboarding_shown")

    val onboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingShownKey] ?: false
    }

    suspend fun setOnboardingShown() {
        context.dataStore.edit { prefs ->
            prefs[onboardingShownKey] = true
        }
    }
}
