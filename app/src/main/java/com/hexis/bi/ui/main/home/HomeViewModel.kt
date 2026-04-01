package com.hexis.bi.ui.main.home

import android.app.Application
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface HomeEvent : UiEvent {
    data object NavigateToLogin : HomeEvent
}

class HomeViewModel(
    private val authRepository: AuthRepository,
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    fun logout() = launch(showLoading = false) {
        authRepository.signOut()
        emitEvent(HomeEvent.NavigateToLogin)
    }
}
