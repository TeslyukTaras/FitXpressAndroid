package com.hexis.bi.ui.main.scan.startscan

import android.app.Application
import android.net.Uri
import android.util.Log
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanProgress
import com.hexis.bi.data.scan.ScanResult
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.calculateAge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StartScanViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val threeDLookRepository: ThreeDLookRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(StartScanState())
    val state: StateFlow<StartScanState> = _state.asStateFlow()

    fun updateVolume(volume: Float) {
        _state.update { it.copy(voiceVolume = volume) }
    }

    /**
     * Advances the scan one tick. Each tap marks the next pending instruction
     * of the current step as completed. Once every instruction in a step is
     * complete, the next tap jumps to the next step. The final tap after the
     * last step triggers the camera SDK launch.
     */
    fun advance() {
        _state.update { state ->
            if (state.isComplete || state.isProcessing) return@update state

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
                // All steps done — request camera launch
                state.copy(shouldLaunchCamera = true)
            }
        }
    }

    fun onCameraLaunched() {
        _state.update { it.copy(shouldLaunchCamera = false) }
    }

    fun onPhotosReceived(frontUri: Uri, sideUri: Uri) {
        Log.d(TAG, "onPhotosReceived front=$frontUri side=$sideUri")
        submitPhotos(frontUri, sideUri)
    }

    /** SDK returned OK but photos were missing — relaunch camera. */
    fun onPhotoError() {
        Log.w(TAG, "onPhotoError — relaunching camera")
        _state.update { it.copy(shouldLaunchCamera = true) }
    }

    /** User backed out of the camera — leave the screen. */
    fun onCameraCancelled() {
        Log.d(TAG, "onCameraCancelled — navigating back")
        _state.update { it.copy(shouldNavigateBack = true) }
    }

    /**
     * Invoked when the user dismisses the error snackbar. If the failure came
     * from a rejected scan we relaunch the camera for a retake; otherwise we
     * back out of the screen.
     */
    fun onErrorDismissed(): Boolean {
        val retake = _state.value.retakeOnErrorDismiss
        clearError()
        _state.update {
            it.copy(
                retakeOnErrorDismiss = false,
                scanProgress = null,
                shouldLaunchCamera = retake,
                shouldNavigateBack = !retake,
            )
        }
        return retake
    }

    private fun submitPhotos(frontUri: Uri, sideUri: Uri) = launch {
        Log.d(TAG, "submitPhotos: loading user profile")
        val profile = userRepository.getUser().getOrElse {
            Log.e(TAG, "submitPhotos: failed to load profile", it)
            setError("Failed to load profile: ${it.message}")
            return@launch
        }

        val heightCm = profile.heightCm?.toFloat() ?: run {
            Log.e(TAG, "submitPhotos: missing height")
            setError("Height is required for scanning")
            return@launch
        }
        val weightKg = profile.weightKg?.toFloat() ?: run {
            Log.e(TAG, "submitPhotos: missing weight")
            setError("Weight is required for scanning")
            return@launch
        }
        val gender = profile.gender?.lowercase() ?: "male"
        val age = profile.dateOfBirth?.calculateAge() ?: 25
        Log.d(TAG, "submitPhotos: h=$heightCm w=$weightKg gender=$gender age=$age — submitting")

        threeDLookRepository.submitScan(
            frontPhoto = frontUri,
            sidePhoto = sideUri,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender,
            age = age,
        ).collect { progress ->
            Log.d(TAG, "submitPhotos: progress=$progress")
            _state.update { it.copy(scanProgress = progress) }

            when (progress) {
                is ScanProgress.Success -> {
                    // Store result for Results screen
                    scanResultRepository.latestResult = ScanResult(
                        measurementId = progress.response.id,
                        response = progress.response,
                    )

                    // Save to Firestore history
                    scanHistoryRepository.saveScan(progress.response)

                    _state.update { it.copy(isComplete = true) }
                }
                is ScanProgress.Failed -> {
                    _state.update { it.copy(retakeOnErrorDismiss = true) }
                    setError(progress.message)
                }
                else -> { /* Submitting, Processing — UI shows progress */ }
            }
        }
    }

    private companion object {
        const val TAG = "StartScanVM"
    }
}
