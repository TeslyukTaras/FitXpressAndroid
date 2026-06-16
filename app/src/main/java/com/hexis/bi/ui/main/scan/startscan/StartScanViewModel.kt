package com.hexis.bi.ui.main.scan.startscan

import android.app.Application
import android.net.Uri
import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanProgress
import com.hexis.bi.data.scan.ScanResult
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.scan.ScanPurpose
import com.hexis.bi.utils.calculateAge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class StartScanViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val threeDLookRepository: ThreeDLookRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val scanReminderScheduler: ScanReminderScheduler,
    private val notificationInbox: NotificationInboxRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(StartScanState())
    val state: StateFlow<StartScanState> = _state.asStateFlow()
    private var scanPurpose: ScanPurpose = ScanPurpose.BodyScan

    fun prepareForScan(purpose: ScanPurpose) {
        scanPurpose = purpose
    }

    fun startCamera() {
        _state.update { state ->
            if (state.isProcessing) {
                state
            } else {
                state.copy(
                    isComplete = false,
                    scanProgress = null,
                    scanErrorMessage = null,
                    shouldLaunchCamera = false,
                    shouldNavigateBack = false,
                    retakeOnErrorDismiss = false,
                )
            }
        }
        _state.update { it.copy(shouldLaunchCamera = true) }
    }

    fun updateVolume(volume: Float) {
        _state.update { it.copy(voiceVolume = volume) }
    }

    /**
     * DEAD CODE — retained temporarily.
     *
     * This method (plus [updateVolume], [StartScanState.currentStep]/`steps`/`voiceVolume`,
     * and the `VoiceGuidanceCard` / `StepIndicator` / `InstructionRow` composables in
     * [StartScanScreen]) drives a manual "how to scan" instruction flow that used to
     * precede the camera launch.
     *
     * The current design immediately launches the 3DLOOK SDK on screen entry
     * (`shouldLaunchCamera` defaults to true), so nothing calls `advance()` anymore.
     * We're keeping the code in place until the product decides whether the pre-scan
     * instructions flow is coming back; if it's abandoned, delete all of the above
     * and the matching fields on [StartScanState].
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
                state.copy(shouldLaunchCamera = true)
            }
        }
    }

    fun onCameraLaunched() {
        _state.update { it.copy(shouldLaunchCamera = false) }
    }

    fun onPhotosReceived(frontUri: Uri, sideUri: Uri) {
        Timber.d("onPhotosReceived front=%s side=%s", frontUri, sideUri)
        submitPhotos(frontUri, sideUri)
    }

    /** SDK returned OK but photos were missing — relaunch camera. */
    fun onPhotoError() {
        Timber.w("onPhotoError — relaunching camera")
        _state.update { it.copy(shouldLaunchCamera = true) }
    }

    /** User backed out of the camera — leave the screen. */
    fun onCameraCancelled() {
        Timber.d("onCameraCancelled — navigating back")
        _state.update { it.copy(shouldNavigateBack = true) }
    }

    /**
     * Invoked when the user dismisses the error snackbar. If the failure came
     * from a rejected scan we relaunch the camera for a retake; otherwise we
     * back out of the screen.
     */
    fun onErrorDismissed() {
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
    }

    private fun submitPhotos(frontUri: Uri, sideUri: Uri) = launch(
        showLoading = scanPurpose == ScanPurpose.BodyScan,
        onError = { throwable ->
            val message = throwable.message ?: appContext.getString(R.string.scan_error_processing_failed)
            if (scanPurpose == ScanPurpose.SuitSizeScan) {
                _state.update {
                    it.copy(
                        scanProgress = null,
                        scanErrorMessage = message,
                        retakeOnErrorDismiss = false,
                    )
                }
            } else {
                setError(message)
            }
        },
    ) {
        Timber.d("submitPhotos: loading user profile")
        val profile = userRepository.getUser().getOrElse {
            Timber.e(it, "submitPhotos: failed to load profile")
            failWithMissingProfileField(
                appContext.getString(R.string.scan_error_profile_load, it.message.orEmpty()),
            )
            return@launch
        }

        val heightCm = profile.heightCm?.toFloat()
            ?: return@launch failWithMissingProfileField(
                appContext.getString(R.string.scan_error_missing_height),
            )
        val weightKg = profile.weightKg?.toFloat()
        if (scanPurpose == ScanPurpose.BodyScan && weightKg == null) {
            return@launch failWithMissingProfileField(
                appContext.getString(R.string.scan_error_missing_weight),
            )
        }
        val gender = profile.gender?.lowercase()
            ?: return@launch failWithMissingProfileField(
                appContext.getString(R.string.scan_error_missing_gender),
            )
        val age = profile.dateOfBirth?.calculateAge()
            ?: return@launch failWithMissingProfileField(
                appContext.getString(R.string.scan_error_missing_age),
            )
        Timber.d(
            "submitPhotos: h=%s w=%s gender=%s age=%d — submitting",
            heightCm,
            weightKg,
            gender,
            age
        )

        threeDLookRepository.submitScan(
            frontPhoto = frontUri,
            sidePhoto = sideUri,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender,
            age = age,
        ).collect { progress ->
            Timber.d("submitPhotos: progress=%s", progress)
            _state.update { it.copy(scanProgress = progress) }

            when (progress) {
                is ScanProgress.Success -> {
                    scanResultRepository.latestResult = ScanResult(
                        measurementId = progress.response.id,
                        response = progress.response,
                    )

                    // Every scan is a real body scan and is persisted, so the suit-size flow sizes
                    // off the just-taken scan (and the order's scanId references it). A scan just
                    // happened, so the next-scan reminder is rescheduled regardless of purpose.
                    val savedScanId = scanHistoryRepository.saveScan(progress.response).getOrNull()
                    scanReminderScheduler.onNotificationSettingsOrScanChanged()

                    when (scanPurpose) {
                        // The "body scan done" inbox message belongs to the standalone body-scan
                        // flow; its Results screen uses the fresh in-memory result, so selectedScanId
                        // stays cleared (see MainScreen).
                        ScanPurpose.BodyScan -> {
                            notificationInbox.appendInbox(
                                R.string.notif_body_scan_done_title,
                                R.string.notif_body_scan_done_body,
                            )
                            _state.update { it.copy(isComplete = true) }
                        }
                        ScanPurpose.SuitSizeScan -> if (savedScanId != null) {
                            scanResultRepository.selectedScanId = savedScanId
                            _state.update { it.copy(isComplete = true) }
                        } else {
                            _state.update {
                                it.copy(
                                    scanProgress = null,
                                    scanErrorMessage = appContext.getString(R.string.scan_error_processing_failed),
                                    retakeOnErrorDismiss = false,
                                )
                            }
                        }
                    }
                }

                is ScanProgress.Failed -> {
                    val message = resolveFailureMessage(progress)
                    if (scanPurpose == ScanPurpose.SuitSizeScan) {
                        _state.update {
                            it.copy(
                                scanProgress = null,
                                scanErrorMessage = message,
                                retakeOnErrorDismiss = false,
                            )
                        }
                    } else {
                        _state.update { it.copy(retakeOnErrorDismiss = true) }
                        setError(message)
                    }
                }

                else -> {} // Submitting, Processing — UI shows progress
            }
        }
    }

    /**
     * Missing-profile failures cannot be retried by retaking the scan, so dismissing
     * the snackbar should leave the screen rather than relaunch the camera.
     */
    private fun failWithMissingProfileField(message: String) {
        if (scanPurpose == ScanPurpose.SuitSizeScan) {
            _state.update {
                it.copy(
                    scanProgress = null,
                    scanErrorMessage = message,
                    retakeOnErrorDismiss = false,
                )
            }
        } else {
            _state.update { it.copy(retakeOnErrorDismiss = false) }
            setError(message)
        }
    }

    private fun resolveFailureMessage(failure: ScanProgress.Failed): String {
        val base = appContext.getString(failure.messageRes)
        val detail = failure.detail?.takeIf { it.isNotBlank() } ?: return base
        return appContext.getString(R.string.scan_error_with_detail, base, detail)
    }
}
