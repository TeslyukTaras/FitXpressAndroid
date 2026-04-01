package com.hexis.bi.ui.auth.login

import android.app.Activity
import android.app.Application
import android.content.Context
import com.hexis.bi.R
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.utils.isValidEmail
import com.hexis.bi.ui.auth.LoginEvent
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun updateEmail(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun updatePassword(value: String) = _state.update { it.copy(password = value, passwordError = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun login() {
        val s = _state.value
        val emailError = when {
            s.email.isBlank() -> appContext.getString(R.string.error_email_required)
            !s.email.isValidEmail() -> appContext.getString(R.string.error_email_invalid)
            else -> null
        }
        val passwordError = if (s.password.isBlank()) appContext.getString(R.string.error_password_required) else null

        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        launch {
            authRepository.signInWithEmail(s.email, s.password)
                .onSuccess { emitEvent(LoginEvent.NavigateToHome) }
                .onFailure { setError(it.message) }
        }
    }

    fun loginWithGoogle(context: Context) = launch {
        authRepository.signInWithGoogle(context)
            .onSuccess { emitEvent(LoginEvent.NavigateToHome) }
            .onFailure { setError(it.message) }
    }

    fun loginWithApple(activity: Activity) = launch {
        authRepository.signInWithApple(activity)
            .onSuccess { emitEvent(LoginEvent.NavigateToHome) }
            .onFailure { setError(it.message) }
    }

    fun navigateToSignUp() = emitEvent(LoginEvent.NavigateToSignUp)
}
