package com.hexis.bi.data.scan

import android.net.Uri
import android.util.Log
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.data.scan.api.ThreeDLookApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface ScanProgress {
    data object Submitting : ScanProgress
    data object Processing : ScanProgress
    data class Success(val response: MeasurementResponse) : ScanProgress
    data class Failed(val message: String) : ScanProgress
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
        Log.d(TAG, "submitScan: emitting Submitting")
        emit(ScanProgress.Submitting)

        val id = api.createMeasurement(
            frontPhoto = frontPhoto,
            sidePhoto = sidePhoto,
            heightCm = heightCm,
            weightKg = weightKg,
            gender = gender,
            age = age,
        ).getOrElse { e ->
            Log.e(TAG, "submitScan: createMeasurement failed", e)
            emit(ScanProgress.Failed(e.message ?: "Failed to submit scan"))
            return@flow
        }
        Log.d(TAG, "submitScan: created measurement id=$id")

        emit(ScanProgress.Processing)

        var attempts = 0
        while (attempts < MAX_POLL_ATTEMPTS) {
            attempts++
            delay(POLL_INTERVAL_MS)

            val measurement = api.getMeasurement(id).getOrElse { e ->
                Log.e(TAG, "submitScan: poll #$attempts failed", e)
                emit(ScanProgress.Failed(e.message ?: "Failed to retrieve results"))
                return@flow
            }

            Log.d(TAG, "submitScan: poll #$attempts status=${measurement.status}")
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
                    val message = details ?: "Scan processing failed (${measurement.status})"
                    emit(ScanProgress.Failed(message))
                    return@flow
                }
                // pending, in_progress — keep polling
            }
        }
        Log.e(TAG, "submitScan: timed out after $MAX_POLL_ATTEMPTS polls")
        emit(ScanProgress.Failed("Scan timed out"))
    }

    companion object {
        private const val TAG = "ThreeDLookRepo"
        private const val POLL_INTERVAL_MS = 4_000L
        private const val MAX_POLL_ATTEMPTS = 60
        private const val STATUS_SUCCESSFUL = "successful"
        private const val STATUS_FAILED = "failed"
        private const val STATUS_ERROR = "error"
    }
}
