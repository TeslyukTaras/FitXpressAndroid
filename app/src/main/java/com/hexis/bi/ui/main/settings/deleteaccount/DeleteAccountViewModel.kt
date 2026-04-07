package com.hexis.bi.ui.main.settings.deleteaccount

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.R
import com.hexis.bi.data.auth.AuthRepository
import com.hexis.bi.data.auth.FirebaseAuthProviders
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeleteAccountViewModel(
    application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(DeleteAccountState())
    val state = _state.asStateFlow()

    fun showDialog() {
        val providerIds = firebaseAuth.currentUser?.providerData?.map { it.providerId }.orEmpty()
        val provider = when {
            FirebaseAuthProviders.GOOGLE in providerIds -> AuthProvider.GOOGLE
            FirebaseAuthProviders.APPLE in providerIds -> AuthProvider.APPLE
            FirebaseAuthProviders.EMAIL_PASSWORD in providerIds -> AuthProvider.EMAIL
            else -> AuthProvider.UNKNOWN
        }
        _state.update {
            it.copy(
                showDialog = true,
                provider = provider,
                password = "",
                passwordVisible = false,
                passwordError = null,
            )
        }
        clearError()
    }

    fun dismissDialog() = _state.update { it.copy(showDialog = false) }

    fun updatePassword(value: String) = _state.update {
        it.copy(password = value, passwordError = null)
    }

    fun togglePasswordVisibility() = _state.update {
        it.copy(passwordVisible = !it.passwordVisible)
    }

    fun deleteAccountWithPassword() {
        val password = _state.value.password
        if (password.isBlank()) {
            _state.update { it.copy(passwordError = appContext.getString(R.string.error_password_required)) }
            return
        }
        launch {
            userRepository.deleteUser()
                .onFailure { setError(it.message); return@launch }
            authRepository.deleteAccountWithPassword(password)
                .onSuccess { emitEvent(DeleteAccountEvent.DeleteSuccess) }
                .onFailure { setError(it.message) }
        }
    }

    fun deleteAccountWithGoogle(context: Context) = launch {
        userRepository.deleteUser()
            .onFailure { setError(it.message); return@launch }
        authRepository.deleteAccountWithGoogle(context)
            .onSuccess { emitEvent(DeleteAccountEvent.DeleteSuccess) }
            .onFailure { setError(it.message) }
    }

    fun deleteAccountWithApple(activity: Activity) = launch {
        userRepository.deleteUser()
            .onFailure { setError(it.message); return@launch }
        authRepository.deleteAccountWithApple(activity)
            .onSuccess { emitEvent(DeleteAccountEvent.DeleteSuccess) }
            .onFailure { setError(it.message) }
    }
}
