package com.hexis.bi.ui.home

import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent

sealed interface HomeEvent : UiEvent {
    data object NavigateToLogin : HomeEvent
}

class HomeViewModel(private val authRepository: AuthRepository) : BaseViewModel() {

    fun logout() = launch(showLoading = false) {
        authRepository.signOut()
        emitEvent(HomeEvent.NavigateToLogin)
    }
}
