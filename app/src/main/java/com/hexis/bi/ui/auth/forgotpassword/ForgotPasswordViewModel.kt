package com.hexis.bi.ui.auth.forgotpassword

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.utils.isValidEmail
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val showSuccessDialog: Boolean = false,
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun updateEmail(value: String) = _state.update { it.copy(email = value, emailError = null) }

    fun sendCode() {
        val email = _state.value.email
        val emailError = when {
            email.isBlank() -> appContext.getString(R.string.error_email_required)
            !email.isValidEmail() -> appContext.getString(R.string.error_email_invalid)
            else -> null
        }
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess { _state.update { it.copy(showSuccessDialog = true) } }
                .onFailure { setError(it.message) }
        }
    }

    fun dismissSuccessDialog() = _state.update { it.copy(showSuccessDialog = false) }
}
