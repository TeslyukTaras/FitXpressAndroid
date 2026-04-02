package com.hexis.bi.ui.auth.login

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.R
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.user.UserProfile
import com.hexis.bi.data.user.UserRepository
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
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
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
        val result = authRepository.signInWithGoogle(context)
        if (result.isFailure) { setError(result.exceptionOrNull()?.message); return@launch }
        provisionUserIfAbsent()
        emitEvent(LoginEvent.NavigateToHome)
    }

    fun loginWithApple(activity: Activity) = launch {
        val result = authRepository.signInWithApple(activity)
        if (result.isFailure) { setError(result.exceptionOrNull()?.message); return@launch }
        provisionUserIfAbsent()
        emitEvent(LoginEvent.NavigateToHome)
    }

    fun navigateToSignUp() = emitEvent(LoginEvent.NavigateToSignUp)

    private suspend fun provisionUserIfAbsent() {
        val user = firebaseAuth.currentUser ?: return
        userRepository.createUserIfAbsent(
            UserProfile(
                uid = user.uid,
                firstName = user.displayName?.substringBefore(" ").orEmpty(),
                lastName = user.displayName?.substringAfter(" ", "").orEmpty(),
                email = user.email.orEmpty(),
                avatarUrl = user.photoUrl?.toString(),
            )
        )
    }
}
