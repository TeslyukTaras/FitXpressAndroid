package com.hexis.bi.ui.main.settings.scanpreferences

import android.app.Application
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.constants.MeasurementConstants
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScanPreferencesViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val preferences: UserPreferencesRepository,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(ScanPreferencesState())
    val state: StateFlow<ScanPreferencesState> = _state.asStateFlow()

    override fun onInitialize() {
        launch(showLoading = false) {
            preferences.ensureVoiceMigratedToFirestore(userRepository)
        }
        loadPreferences()
    }

    private fun loadPreferences() = launch {
        val isMetric = userRepository.getUser().getOrNull()
            ?.unitSystem.isMetricUnitSystem()
        userRepository.getUserSettings().onSuccess { settings ->
            val zones = BodyMeasurementRegion.visibleRegionsOrDefault(settings.measurementZones)
            _state.update {
                it.copy(
                    isMetric = isMetric,
                    voiceGuidanceEnabled = settings.voiceGuidanceEnabled ?: true,
                    selectedZones = zones,
                )
            }
        }
    }

    fun selectMetric() = _state.update { it.copy(isMetric = true) }

    fun selectImperial() = _state.update { it.copy(isMetric = false) }

    fun toggleVoiceGuidance(enabled: Boolean) =
        _state.update { it.copy(voiceGuidanceEnabled = enabled) }

    fun toggleZone(zone: BodyMeasurementRegion) = _state.update {
        val zones = if (zone in it.selectedZones) it.selectedZones - zone else it.selectedZones + zone
        it.copy(selectedZones = zones)
    }

    fun toggleAllZones() = _state.update {
        val zones = if (it.allZonesSelected) emptySet() else BodyMeasurementRegion.measurableRegions.toSet()
        it.copy(selectedZones = zones)
    }

    fun save() = launch(showLoading = false) {
        _state.update { it.copy(isSaving = true) }
        val current = _state.value
        val unitSystem = if (current.isMetric) {
            MeasurementConstants.UNIT_SYSTEM_METRIC
        } else {
            MeasurementConstants.UNIT_SYSTEM_IMPERIAL
        }
        val result = userRepository.updateFields(
            mapOf(FirestoreSchema.UserFields.UNIT_SYSTEM to unitSystem),
        ).mapCatching {
            userRepository.updateUserSettings(
                mapOf(
                    FirestoreSchema.UserSettingsFields.VOICE_GUIDANCE_ENABLED to current.voiceGuidanceEnabled,
                    FirestoreSchema.UserSettingsFields.MEASUREMENT_ZONES to
                        BodyMeasurementRegion.measurableRegions
                            .filter { it in current.selectedZones }
                            .map { it.name },
                ),
            ).getOrThrow()
        }
        _state.update { it.copy(isSaving = false) }
        result
            .onSuccess { emitEvent(UiEvent.NavigateBack) }
            .onFailure { setError(it.message ?: "Could not save settings") }
    }
}
