package com.hexis.bi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    preferencesRepository: UserPreferencesRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /**
     * Becomes true once both DataStore and Firebase Auth have emitted their
     * initial values. Used to hold the splash screen until the start
     * destination is known.
     */
    val isReady = combine(
        preferencesRepository.onboardingShown,
        authRepository.authState,
    ) { _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
}
