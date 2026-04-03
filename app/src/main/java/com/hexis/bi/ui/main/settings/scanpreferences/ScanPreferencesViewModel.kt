package com.hexis.bi.ui.main.settings.scanpreferences

import android.app.Application
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanPreferencesViewModel(
    application: Application,
    private val preferences: UserPreferencesRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ScanPreferencesState())
    val state: StateFlow<ScanPreferencesState> = _state.asStateFlow()

    init {
        launch(showLoading = false) {
            preferences.voiceGuidanceEnabled.collect { enabled ->
                _state.update { it.copy(voiceGuidanceEnabled = enabled) }
            }
        }
    }

    fun toggleVoiceGuidance(enabled: Boolean) {
        launch(showLoading = false) {
            preferences.setVoiceGuidanceEnabled(enabled)
        }
    }
}
