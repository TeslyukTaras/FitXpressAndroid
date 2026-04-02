package com.hexis.bi.ui.auth.signup

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.R
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.user.UserProfile
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.utils.isValidEmail
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

class SignUpViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth,
    application: Application,
) : BaseViewModel(application) {

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

        val firstNameError = if (s.firstName.isBlank()) appContext.getString(R.string.error_first_name_required) else null
        val lastNameError = if (s.lastName.isBlank()) appContext.getString(R.string.error_last_name_required) else null
        val emailError = when {
            s.email.isBlank() -> appContext.getString(R.string.error_email_required)
            !s.email.isValidEmail() -> appContext.getString(R.string.error_email_invalid)
            else -> null
        }
        val passwordError = validatePassword(s.password)
        val confirmPasswordError = when {
            s.confirmPassword.isBlank() -> appContext.getString(R.string.error_confirm_password_required)
            s.confirmPassword != s.password -> appContext.getString(R.string.error_passwords_do_not_match)
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
            setError(appContext.getString(R.string.error_terms_not_accepted))
            return
        }

        launch {
            val authResult = authRepository.signUpWithEmail(s.firstName, s.lastName, s.email, s.password)
            if (authResult.isFailure) {
                setError(authResult.exceptionOrNull()?.message)
                return@launch
            }
            val uid = firebaseAuth.currentUser?.uid ?: return@launch
            val createResult = userRepository.createUser(
                UserProfile(uid = uid, firstName = s.firstName, lastName = s.lastName, email = s.email)
            )
            if (createResult.isFailure) {
                setError(createResult.exceptionOrNull()?.message)
                return@launch
            }
            emitEvent(SignUpEvent.NavigateToHome)
        }
    }

    fun signUpWithGoogle(context: Context) = launch {
        val result = authRepository.signInWithGoogle(context)
        if (result.isFailure) { setError(result.exceptionOrNull()?.message); return@launch }
        provisionUserIfNeeded()
    }

    fun signUpWithApple(activity: Activity) = launch {
        val result = authRepository.signInWithApple(activity)
        if (result.isFailure) { setError(result.exceptionOrNull()?.message); return@launch }
        provisionUserIfNeeded()
    }

    private suspend fun provisionUserIfNeeded() {
        val user = firebaseAuth.currentUser ?: return
        val result = userRepository.createUserIfAbsent(
            UserProfile(
                uid = user.uid,
                firstName = user.displayName?.substringBefore(" ").orEmpty(),
                lastName = user.displayName?.substringAfter(" ", "").orEmpty(),
                email = user.email.orEmpty(),
                avatarUrl = user.photoUrl?.toString(),
            )
        )
        if (result.isFailure) {
            setError(result.exceptionOrNull()?.message)
            return
        }
        emitEvent(SignUpEvent.NavigateToHome)
    }

    fun navigateToLogin() = emitEvent(SignUpEvent.NavigateToLogin)

    private fun validatePassword(password: String): String? = when {
        password.isBlank() -> appContext.getString(R.string.error_password_required)
        password.length < 6 -> appContext.getString(R.string.error_password_too_short)
        !password.any { it.isUpperCase() } -> appContext.getString(R.string.error_password_no_uppercase)
        !password.any { it.isLowerCase() } -> appContext.getString(R.string.error_password_no_lowercase)
        !password.any { it.isDigit() } -> appContext.getString(R.string.error_password_no_digit)
        !password.any { !it.isLetterOrDigit() } -> appContext.getString(R.string.error_password_no_special)
        else -> null
    }
}
