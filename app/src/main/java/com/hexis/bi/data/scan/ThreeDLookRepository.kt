package com.hexis.bi.data.scan

import android.net.Uri
import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.scan.api.MeasurementResponse
import com.hexis.bi.data.scan.api.ThreeDLookApi
import com.hexis.bi.utils.constants.MeasurementConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

class ThreeDLookRepository(
    private val api: ThreeDLookApi,
    private val scanHistoryRepository: ScanHistoryRepository,
) {
    private val colorAnalysisMutex = Mutex()
    private val colorAnalysisMeshCache = mutableMapOf<Pair<String, String>, String>()
    private val colorAnalysisProgressIdCache = mutableMapOf<Pair<String, String>, String>()

    fun submitScan(
        frontPhoto: Uri,
        sidePhoto: Uri,
        heightCm: Float,
        weightKg: Float?,
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

    /** Loads the cached body-progress colored mesh for two measurements, or generates it once. */
    suspend fun loadColorAnalysisMeshUrl(
        beforeMeasurementId: String,
        afterMeasurementId: String,
    ): Result<String> = colorAnalysisMutex.withLock {
        val pair = beforeMeasurementId to afterMeasurementId
        colorAnalysisMeshCache[pair]?.let { return@withLock Result.success(it) }

        runCatching {
            val persisted = scanHistoryRepository
                .getBodyProgressCache(beforeMeasurementId, afterMeasurementId)
                .getOrNull()
            persisted?.modelUrl?.let {
                colorAnalysisMeshCache[pair] = it
                return@runCatching it
            }
            val id = colorAnalysisProgressIdCache[pair]
                ?: persisted?.id
            ?: api.createBodyProgress(beforeMeasurementId, afterMeasurementId).getOrThrow()
                .also {
                    colorAnalysisProgressIdCache[pair] = it
                    scanHistoryRepository.saveBodyProgressCache(
                        beforeMeasurementId = beforeMeasurementId,
                        afterMeasurementId = afterMeasurementId,
                        progressId = it,
                    ).onFailure { error ->
                        Timber.w(error, "Unable to persist created body progress pair=%s", pair)
                    }
                }
            colorAnalysisProgressIdCache[pair] = id

            var attempts = 0
            while (attempts < MeasurementConstants.BODY_PROGRESS_MAX_POLL_ATTEMPTS) {
                attempts++
                delay(MeasurementConstants.BODY_PROGRESS_POLL_INTERVAL_MS)
                val progress = api.getBodyProgress(id).getOrThrow()
                Timber.d("loadColorAnalysisMeshUrl: poll #%d status=%s", attempts, progress.status)
                when (progress.status.lowercase()) {
                    STATUS_SUCCESSFUL ->
                        return@runCatching progress.model
                            ?: error("Color analysis succeeded without a model URL")
                    STATUS_FAILED, STATUS_ERROR -> error("Color analysis processing failed")
                }
            }
            error("Color analysis timed out")
        }.onSuccess { meshUrl ->
            colorAnalysisMeshCache[pair] = meshUrl
            colorAnalysisProgressIdCache[pair]?.let { progressId ->
                scanHistoryRepository.saveBodyProgressCache(
                    beforeMeasurementId = beforeMeasurementId,
                    afterMeasurementId = afterMeasurementId,
                    progressId = progressId,
                    modelUrl = meshUrl,
                ).onFailure {
                    Timber.w(it, "Unable to persist completed body progress pair=%s", pair)
                }
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 4_000L
        private const val MAX_POLL_ATTEMPTS = 60
        private const val STATUS_SUCCESSFUL = "successful"
        private const val STATUS_FAILED = "failed"
        private const val STATUS_ERROR = "error"
    }
}
