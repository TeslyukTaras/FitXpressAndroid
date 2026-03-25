package com.hexis.bi.ui.auth.signup

import android.app.Activity
import android.content.Context
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.ui.auth.SignUpEvent
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
)

class SignUpViewModel(private val authRepository: AuthRepository) : BaseViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun updateFirstName(v: String) = _state.update { it.copy(firstName = v, firstNameError = null) }
    fun updateLastName(v: String) = _state.update { it.copy(lastName = v, lastNameError = null) }
    fun updateEmail(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun updatePassword(v: String) = _state.update { it.copy(password = v, passwordError = null) }
    fun updateConfirmPassword(v: String) = _state.update { it.copy(confirmPassword = v, confirmPasswordError = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun toggleConfirmPasswordVisibility() = _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    fun toggleTerms() = _state.update { it.copy(isTermsAccepted = !it.isTermsAccepted) }

    fun signUp() {
        val s = _state.value

        val firstNameError = if (s.firstName.isBlank()) "First name is required" else null
        val lastNameError = if (s.lastName.isBlank()) "Last name is required" else null
        val emailError = when {
            s.email.isBlank() -> "Email is required"
            !s.email.contains('@') -> "Enter a valid email address"
            else -> null
        }
        val passwordError = validatePassword(s.password)
        val confirmPasswordError = when {
            s.confirmPassword.isBlank() -> "Please confirm your password"
            s.confirmPassword != s.password -> "Passwords do not match"
            else -> null
        }

        if (firstNameError != null || lastNameError != null || emailError != null ||
            passwordError != null || confirmPasswordError != null
        ) {
            _state.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        if (!s.isTermsAccepted) {
            setError("Please accept the Terms & Privacy Policy to continue")
            return
        }

        launch {
            authRepository.signUpWithEmail(s.firstName, s.lastName, s.email, s.password)
                .onSuccess { emitEvent(SignUpEvent.NavigateToHome) }
                .onFailure { setError(it.message) }
        }
    }

    fun signUpWithGoogle(context: Context) = launch {
        authRepository.signInWithGoogle(context)
            .onSuccess { emitEvent(SignUpEvent.NavigateToHome) }
            .onFailure { setError(it.message) }
    }

    fun signUpWithApple(activity: Activity) = launch {
        authRepository.signInWithApple(activity)
            .onSuccess { emitEvent(SignUpEvent.NavigateToHome) }
            .onFailure { setError(it.message) }
    }

    fun navigateToLogin() = emitEvent(SignUpEvent.NavigateToLogin)
}

private fun validatePassword(password: String): String? = when {
    password.isBlank() -> "Password is required"
    password.length < 6 -> "Must be at least 6 characters"
    !password.any { it.isUpperCase() } -> "Must contain an uppercase letter"
    !password.any { it.isLowerCase() } -> "Must contain a lowercase letter"
    !password.any { it.isDigit() } -> "Must contain a number"
    !password.any { !it.isLetterOrDigit() } -> "Must contain a special character"
    else -> null
}
