package com.hexis.bi.ui.auth

import com.hexis.bi.ui.base.UiEvent

sealed interface LoginEvent : UiEvent {
    data object NavigateToHome : LoginEvent
    data object NavigateToSignUp : LoginEvent
    data object NavigateToVerifyEmail : LoginEvent
}

sealed interface SignUpEvent : UiEvent {
    data object NavigateToHome : SignUpEvent
    data object NavigateToLogin : SignUpEvent
    data object NavigateToVerifyEmail : SignUpEvent
}
