package com.hexis.bi.ui.main.scan.startscan

import android.app.Application
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StartScanViewModel(application: Application) : BaseViewModel(application) {

    private val _state = MutableStateFlow(StartScanState())
    val state: StateFlow<StartScanState> = _state.asStateFlow()

    fun updateVolume(volume: Float) {
        _state.update { it.copy(voiceVolume = volume) }
    }

    /**
     * Advances the scan one tick. Each tap marks the next pending instruction
     * of the current step as completed. Once every instruction in a step is
     * complete, the next tap jumps to the next step. The final tap after the
     * last step flips [StartScanState.isComplete] so the caller can navigate.
     */
    fun advance() {
        _state.update { state ->
            if (state.isComplete) return@update state

            val stepIndex = state.currentStep - 1
            val currentInstructions = state.steps[stepIndex]
            val nextPendingIndex = currentInstructions.indexOfFirst { !it.completed }

            if (nextPendingIndex != -1) {
                val updatedSteps = state.steps.toMutableList()
                val updatedInstructions = currentInstructions.toMutableList()
                updatedInstructions[nextPendingIndex] =
                    updatedInstructions[nextPendingIndex].copy(completed = true)
                updatedSteps[stepIndex] = updatedInstructions
                state.copy(steps = updatedSteps)
            } else if (state.currentStep < state.steps.size) {
                state.copy(currentStep = state.currentStep + 1)
            } else {
                state.copy(isComplete = true)
            }
        }
    }
}
