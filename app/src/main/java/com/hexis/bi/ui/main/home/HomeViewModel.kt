package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.calculateAge
import com.hexis.bi.utils.constants.ProfileConstants
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
                        weight = if (profile.unitSystem == ProfileConstants.UNIT_SYSTEM_METRIC)
                            profile.weightKg?.let {
                                appContext.getString(
                                    R.string.unit_weight_kg,
                                    it
                                )
                            }
                        else
                            profile.weightLb?.let {
                                appContext.getString(
                                    R.string.unit_weight_lb,
                                    it
                                )
                            },
                        height = if (profile.unitSystem == ProfileConstants.UNIT_SYSTEM_METRIC)
                            profile.heightCm?.let {
                                appContext.getString(
                                    R.string.unit_height_cm,
                                    it
                                )
                            }
                        else
                            profile.heightIn?.let {
                                appContext.getString(
                                    R.string.unit_height_in,
                                    it
                                )
                            },
                        age = profile.dateOfBirth?.calculateAge()
                            ?.let { appContext.getString(R.string.unit_age_years, it) },
                    )
                }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)
    }
}
