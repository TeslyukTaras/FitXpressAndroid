package com.hexis.bi.ui.main.settings.scanpreferences

import android.app.Application
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanPreferencesViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val preferences: UserPreferencesRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ScanPreferencesState())
    val state: StateFlow<ScanPreferencesState> = _state.asStateFlow()

    init {
        launch(showLoading = false) {
            preferences.ensureVoiceMigratedToFirestore(userRepository)
        }
        launch(showLoading = false) {
            userRepository.observeUserSettings().collect { settings ->
                _state.update {
                    it.copy(voiceGuidanceEnabled = settings.voiceGuidanceEnabled ?: true)
                }
            }
        }
    }

    fun toggleVoiceGuidance(enabled: Boolean) = launch(showLoading = false) {
        runCatching {
            userRepository.updateUserSettings(
                mapOf(FirestoreSchema.UserSettingsFields.VOICE_GUIDANCE_ENABLED to enabled),
            )
        }.onFailure { setError(it.message ?: "Could not save settings") }
    }
}
