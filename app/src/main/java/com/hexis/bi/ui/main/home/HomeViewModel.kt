package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.calculateAge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

sealed interface HomeEvent : UiEvent {
    data object NavigateToLogin : HomeEvent
}

class HomeViewModel(
    application: Application,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        userRepository.observeUser()
            .onEach { profile ->
                _state.update { current ->
                    current.copy(
                        userName = "${profile.firstName} ${profile.lastName}".trim(),
                        avatarUrl = profile.avatarUrl,
                        weight = if (profile.unitSystem == "Metric") "${profile.weightKg} kg" else "${profile.weightLb} lb",
                        height = if (profile.unitSystem == "Metric") "${profile.heightCm} cm" else "${profile.heightIn} in",
                        age = "${profile.dateOfBirth?.calculateAge() ?: "n/a"} years",
                    )
                }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)
    }
}
