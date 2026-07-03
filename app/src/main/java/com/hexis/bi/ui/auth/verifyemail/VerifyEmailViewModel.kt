package com.hexis.bi.ui.auth.verifyemail

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.AuthFlowConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyEmailUiState(
    val email: String = "",
    val code: String = "",
    val isCodeError: Boolean = false,
    val inlineError: String? = null,
    val resendSecondsLeft: Int = 0,
    val isResending: Boolean = false,
    val hasResent: Boolean = false,
    val showSuccessDialog: Boolean = false,
) {
    val isCodeComplete: Boolean get() = code.length == AuthFlowConstants.EMAIL_CODE_LENGTH

    // Blocks resend both while the cooldown is running and while a send is already in flight.
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isResending
}

class VerifyEmailViewModel(
    private val authRepository: AuthRepository,
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    override fun onInitialize() {
        _state.update { it.copy(email = authRepository.currentUserEmail.orEmpty()) }
        // The entry step already sent a code, so mirror the backend's resend cooldown from the start.
        startResendCountdown()
    }

    fun updateCode(value: String) = _state.update {
        it.copy(code = value, isCodeError = false, inlineError = null)
    }

    fun verify() {
        val code = _state.value.code
        if (code.length != AuthFlowConstants.EMAIL_CODE_LENGTH) return
        launch {
            authRepository.verifyEmailCode(code)
                .onSuccess { _state.update { it.copy(showSuccessDialog = true) } }
                .onFailure { error ->
                    _state.update { it.copy(isCodeError = true, inlineError = error.message) }
                }
        }
    }

    fun resendCode() {
        // Guard + claim happen synchronously on the caller (main) thread, before the async send —
        // so rapid taps in the window before the countdown starts can't fire multiple sends.
        if (!_state.value.canResend) return
        _state.update { it.copy(isResending = true) }
        launch(showLoading = false) {
            authRepository.sendEmailVerificationCode()
                .onSuccess {
                    _state.update {
                        it.copy(
                            code = "",
                            isCodeError = false,
                            inlineError = null,
                            hasResent = true,
                            isResending = false,
                        )
                    }
                    startResendCountdown()
                }
                .onFailure {
                    _state.update { it.copy(isResending = false) }
                    setError(it.message)
                }
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        // Set the cooldown synchronously so the guard is closed from the first emission, not only
        // once the coroutine's first tick runs.
        _state.update { it.copy(resendSecondsLeft = AuthFlowConstants.EMAIL_RESEND_COOLDOWN_SECONDS) }
        countdownJob = viewModelScope.launch {
            var remaining = AuthFlowConstants.EMAIL_RESEND_COOLDOWN_SECONDS
            while (remaining > 0) {
                _state.update { it.copy(resendSecondsLeft = remaining) }
                delay(1_000L)
                remaining--
            }
            _state.update { it.copy(resendSecondsLeft = 0) }
        }
    }
}
