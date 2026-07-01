package com.hexis.bi.ui.auth.onboarding

import android.app.Application
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.MeasurementConstants
import com.hexis.bi.utils.inchesToCm
import com.hexis.bi.utils.lbToKg
import com.hexis.bi.utils.parseDob
import com.hexis.bi.utils.persistedUserMeasurements
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val suitRepository: SuitRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    // Personal info actions
    fun updateDateOfBirth(value: String) = _state.update { it.copy(dateOfBirth = value) }
    fun selectGender(gender: GenderOption) = _state.update { it.copy(gender = gender) }
    fun selectMetric() = _state.update { it.copy(isMetric = true) }
    fun selectImperial() = _state.update { it.copy(isMetric = false) }

    fun updateHeight(sliderValue: Float) = _state.update {
        it.copy(heightCm = if (it.isMetric) sliderValue else sliderValue.inchesToCm())
    }

    fun updateWeight(sliderValue: Float) = _state.update {
        it.copy(weightKg = if (it.isMetric) sliderValue else sliderValue.lbToKg())
    }

    fun showDatePicker() = _state.update { it.copy(showDatePicker = true) }
    fun hideDatePicker() = _state.update { it.copy(showDatePicker = false) }

    // Suit actions
    fun updateSuitIdInput(value: String) = _state.update { it.copy(suitIdInput = value) }

    fun connectSuit() {
        val suitId = _state.value.suitIdInput.trim()
        if (suitId.isBlank()) return
        launch {
            suitRepository.connect(suitId)
            _state.update {
                it.copy(
                    isSuitConnected = true,
                    connectedSuitId = suitId,
                    connectedStatus = "Active",
                    showSuitCareSheet = true,
                    careInstructionsAccepted = false,
                )
            }
        }
    }

    fun reconnectSuit() {
        _state.update {
            it.copy(
                isSuitConnected = false,
                suitIdInput = it.connectedSuitId,
                connectedSuitId = "",
                connectedStatus = "",
            )
        }
        launch { suitRepository.disconnect() }
    }

    fun setCareInstructionsAccepted(accepted: Boolean) =
        _state.update { it.copy(careInstructionsAccepted = accepted) }

    fun dismissSuitCareSheet() = _state.update { it.copy(showSuitCareSheet = false) }

    fun finish() = launch {
        saveProfile()
            .onSuccess { emitEvent(OnboardingEvent.Finished) }
            .onFailure { setError(it.message) }
    }

    fun buySuit() = launch {
        saveProfile()
            .onSuccess { emitEvent(OnboardingEvent.BuySuitScan) }
            .onFailure { setError(it.message) }
    }

    private suspend fun saveProfile(): Result<Unit> {
        val s = _state.value
        val measurements = persistedUserMeasurements(s.heightCm, s.weightKg)
        val fields = mutableMapOf<String, Any?>(
            "gender" to s.gender.name,
            "heightCm" to measurements.heightCm,
            "weightKg" to measurements.weightKg,
            "heightIn" to measurements.heightIn,
            "weightLb" to measurements.weightLb,
            "unitSystem" to if (s.isMetric) MeasurementConstants.UNIT_SYSTEM_METRIC
            else MeasurementConstants.UNIT_SYSTEM_IMPERIAL,
        )
        s.dateOfBirth.parseDob()?.let { fields["dateOfBirth"] = it }
        if (s.isSuitConnected && s.connectedSuitId.isNotBlank()) {
            fields["suitId"] = s.connectedSuitId
        }
        return userRepository.updateFields(fields)
    }

    fun skip() {
        emitEvent(OnboardingEvent.Finished)
    }
}
