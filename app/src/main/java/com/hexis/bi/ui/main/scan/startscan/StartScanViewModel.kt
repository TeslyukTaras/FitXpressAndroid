package com.hexis.bi.ui.main.scan.startscan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanProgress
import com.hexis.bi.data.scan.ScanResult
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.avatar.prefetchMetricAvatarModel
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.scan.ScanPurpose
import com.hexis.bi.utils.calculateAge
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class StartScanViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val threeDLookRepository: ThreeDLookRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val scanReminderScheduler: ScanReminderScheduler,
    private val notificationInbox: NotificationInboxRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(StartScanState())
    val state: StateFlow<StartScanState> = _state.asStateFlow()
    private var scanPurpose: ScanPurpose = ScanPurpose.BodyScan
    private var personalizeHintEvaluated = false

    fun prepareForScan(purpose: ScanPurpose) {
        scanPurpose = purpose
    }

    fun onBodyResultsRevealed() {
        if (personalizeHintEvaluated) return
        personalizeHintEvaluated = true
        viewModelScope.launch {
            if (!userPreferencesRepository.isPersonalizeResultsHintShown()) {
                userPreferencesRepository.setPersonalizeResultsHintShown()
                _state.update { it.copy(showPersonalizeResultsHint = true) }
            }
        }
    }

    fun onPersonalizeResultsHintDismissed() {
        _state.update { it.copy(showPersonalizeResultsHint = false) }
    }

    fun startCamera() {
        _state.update { state ->
            if (state.isProcessing) {
                state
            } else {
                state.copy(
                    isComplete = false,
                    scanProgress = null,
                    isPreparingResults = false,
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
                isPreparingResults = false,
                shouldLaunchCamera = retake,
                shouldNavigateBack = !retake,
            )
        }
    }

    private fun submitPhotos(frontUri: Uri, sideUri: Uri) = launch(
        showLoading = scanPurpose == ScanPurpose.BodyScan,
        onError = { throwable ->
            val message =
                throwable.message ?: appContext.getString(R.string.scan_error_processing_failed)
            if (scanPurpose == ScanPurpose.SuitSizeScan) {
                _state.update {
                    it.copy(
                        scanProgress = null,
                        isPreparingResults = false,
                        scanErrorMessage = message,
                        retakeOnErrorDismiss = false,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        scanProgress = null,
                        isPreparingResults = false,
                    )
                }
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
            _state.update {
                it.copy(
                    scanProgress = progress,
                    isPreparingResults = progress is ScanProgress.Success,
                )
            }

            when (progress) {
                is ScanProgress.Success -> {
                    prewarmResultModelInBackground(progress.response.model3dUrl)

                    // Every scan is a real body scan and is persisted, so the suit-size flow sizes
                    // off the just-taken scan (and the order's scanId references it). A scan just
                    // happened, so the next-scan reminder is rescheduled regardless of purpose.
                    val savedAtMillis = System.currentTimeMillis()
                    val savedScanId = scanHistoryRepository
                        .saveScan(progress.response, savedAtMillis)
                        .getOrNull()

                    if (savedScanId == null) {
                        val message = appContext.getString(R.string.scan_error_processing_failed)
                        if (scanPurpose == ScanPurpose.SuitSizeScan) {
                            _state.update {
                                it.copy(
                                    scanProgress = null,
                                    isPreparingResults = false,
                                    scanErrorMessage = message,
                                    retakeOnErrorDismiss = false,
                                )
                            }
                        } else {
                            _state.update {
                                it.copy(
                                    scanProgress = null,
                                    isPreparingResults = false,
                                    retakeOnErrorDismiss = false,
                                )
                            }
                            setError(message)
                        }
                        return@collect
                    }

                    scanResultRepository.latestResult = ScanResult(
                        measurementId = progress.response.id,
                        response = progress.response,
                        scanId = savedScanId,
                        savedAtMillis = savedAtMillis,
                    )
                    scanResultRepository.selectedScanId = savedScanId
                    scanReminderScheduler.onNotificationSettingsOrScanChanged()

                    when (scanPurpose) {
                        // The "body scan done" inbox message belongs to the standalone body-scan
                        // flow; its Results screen uses the fresh in-memory result plus saved scan id
                        // to avoid racing Firestore's latest-scan query after upload.
                        ScanPurpose.BodyScan -> {
                            notificationInbox.appendInbox(
                                R.string.notif_body_scan_done_title,
                                R.string.notif_body_scan_done_body,
                            )
                            _state.update { it.copy(isComplete = true, isPreparingResults = false) }
                        }

                        ScanPurpose.SuitSizeScan -> {
                            _state.update { it.copy(isComplete = true, isPreparingResults = false) }
                        }
                    }
                }

                is ScanProgress.Failed -> {
                    val message = resolveFailureMessage(progress)
                    if (scanPurpose == ScanPurpose.SuitSizeScan) {
                        _state.update {
                            it.copy(
                                scanProgress = null,
                                isPreparingResults = false,
                                scanErrorMessage = message,
                                retakeOnErrorDismiss = false,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isPreparingResults = false,
                                retakeOnErrorDismiss = true,
                            )
                        }
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
                    isPreparingResults = false,
                    scanErrorMessage = message,
                    retakeOnErrorDismiss = false,
                )
            }
        } else {
            _state.update {
                it.copy(
                    isPreparingResults = false,
                    retakeOnErrorDismiss = false,
                )
            }
            setError(message)
        }
    }

    private fun resolveFailureMessage(failure: ScanProgress.Failed): String {
        val base = appContext.getString(failure.messageRes)
        val detail = failure.detail?.takeIf { it.isNotBlank() } ?: return base
        return appContext.getString(R.string.scan_error_with_detail, base, detail)
    }

    private fun prewarmResultModelInBackground(modelUrl: String?) {
        val url = modelUrl?.takeUnless { it.isBlank() } ?: return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                prefetchMetricAvatarModel(appContext, url)
            }.onFailure {
                Timber.w(it, "Unable to prewarm completed scan model")
            }
        }
    }
}
