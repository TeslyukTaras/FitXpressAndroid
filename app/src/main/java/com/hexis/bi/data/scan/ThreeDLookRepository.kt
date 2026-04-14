package com.hexis.bi.data.scan

import android.net.Uri
import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.data.scan.api.ThreeDLookApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

sealed interface ScanProgress {
    data object Submitting : ScanProgress
    data object Processing : ScanProgress
    data class Success(val response: MeasurementResponse) : ScanProgress

    /**
     * @param messageRes user-facing fallback message (resolved at the VM boundary)
     * @param detail optional extra context from the API (already human-readable, e.g.
     *               "The body is not full") — may be appended to the message verbatim
     */
    data class Failed(
        @StringRes val messageRes: Int,
        val detail: String? = null,
    ) : ScanProgress
}

class ThreeDLookRepository(private val api: ThreeDLookApi) {

    fun submitScan(
        frontPhoto: Uri,
        sidePhoto: Uri,
        heightCm: Float,
        weightKg: Float,
        gender: String,
        age: Int,
    ): Flow<ScanProgress> = flow {
        Timber.d("submitScan: emitting Submitting")
        emit(ScanProgress.Submitting)

        val id = api.createMeasurement(
            frontPhoto = frontPhoto,
            sidePhoto = sidePhoto,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender,
            age = age,
        ).getOrElse { e ->
            Timber.e(e, "submitScan: createMeasurement failed")
            emit(ScanProgress.Failed(R.string.scan_error_submit_failed, e.message))
            return@flow
        }
        Timber.d("submitScan: created measurement id=%s", id)

        emit(ScanProgress.Processing)

        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            attempts++
            delay(POLL_INTERVAL_MS)

            val measurement = api.getMeasurement(id).getOrElse { e ->
                Timber.e(e, "submitScan: poll #%d failed", attempts)
                emit(ScanProgress.Failed(R.string.scan_error_retrieve_failed, e.message))
                return@flow
            }

            Timber.d("submitScan: poll #%d status=%s", attempts, measurement.status)
            when (measurement.status.lowercase()) {
                STATUS_SUCCESSFUL -> {
                    emit(ScanProgress.Success(measurement))
                    return@flow
                }
                STATUS_FAILED, STATUS_ERROR -> {
                    val details = measurement.errors
                        ?.mapNotNull { it.detail }
                        ?.distinct()
                        ?.joinToString("\n")
                        ?.takeIf { it.isNotBlank() }
                    emit(ScanProgress.Failed(R.string.scan_error_processing_failed, details))
                    return@flow
                }
                // pending, in_progress — keep polling
            }
        }
        Timber.e("submitScan: timed out after %d polls", MAX_POLL_ATTEMPTS)
        emit(ScanProgress.Failed(R.string.scan_error_timeout))
    }

    companion object {
        private const val POLL_INTERVAL_MS = 4_000L
        private const val MAX_POLL_ATTEMPTS = 60
        private const val STATUS_SUCCESSFUL = "successful"
        private const val STATUS_FAILED = "failed"
        private const val STATUS_ERROR = "error"
    }
}
