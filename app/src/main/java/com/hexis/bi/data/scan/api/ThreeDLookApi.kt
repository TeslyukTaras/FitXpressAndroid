package com.hexis.bi.data.scan.api

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.hexis.bi.data.firebase.toJsonElement
import com.hexis.bi.utils.ImageCompressor
import com.hexis.bi.utils.constants.ScanPhotoConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

class ThreeDLookApi(
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createMeasurement(
        frontPhoto: Uri,
        sidePhoto: Uri,
        heightCm: Float,
        weightKg: Float?,
        gender: String,
        age: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        val scanId = UUID.randomUUID().toString()
        val frontPath = "$TMP_SCAN_ROOT/$uid/$scanId/$FRONT_PHOTO_FILENAME"
        val sidePath = "$TMP_SCAN_ROOT/$uid/$scanId/$SIDE_PHOTO_FILENAME"
        val frontRef = storage.reference.child(frontPath)
        val sideRef = storage.reference.child(sidePath)
        try {
            val frontBytes = readPhoto(frontPhoto)
            val sideBytes = readPhoto(sidePhoto)

            val metadata = StorageMetadata.Builder()
                .setContentType(MEDIA_TYPE_JPEG)
                .build()
            coroutineScope {
                listOf(
                    async { frontRef.putBytes(frontBytes, metadata).await() },
                    async { sideRef.putBytes(sideBytes, metadata).await() },
                ).awaitAll()
            }

            Timber.d(
                "createMeasurement: uploaded temp scan files scanId=%s front=%dB side=%dB",
                scanId,
                frontBytes.size,
                sideBytes.size,
            )

            val payload = buildMap<String, Any> {
                put(FIELD_FRONT_PATH, frontPath)
                put(FIELD_SIDE_PATH, sidePath)
                put(FIELD_HEIGHT_CM, heightCm)
                put(FIELD_GENDER, gender)
                put(FIELD_AGE, age)
                weightKg?.let { put(FIELD_WEIGHT_KG, it) }
            }

            val data = functions
                .getHttpsCallable(FUNCTION_CREATE_MEASUREMENT)
                .call(payload)
                .await()
                .data
            Result.success(
                json.decodeFromJsonElement(
                    CreateMeasurementResponse.serializer(),
                    data.toJsonElement()
                ).id,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            deleteQuietly(frontRef, sideRef)
            Result.failure(e)
        }
    }

    suspend fun getMeasurement(id: String): Result<MeasurementResponse> =
        withContext(Dispatchers.IO) {
            try {
                val data = functions
                    .getHttpsCallable(FUNCTION_GET_MEASUREMENT)
                    .call(mapOf(FIELD_ID to id))
                    .await()
                    .data
                Result.success(
                    json.decodeFromJsonElement(
                        MeasurementResponse.serializer(),
                        data.toJsonElement()
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    suspend fun createBodyProgress(
        beforeMeasurementId: String,
        afterMeasurementId: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val data = functions
                .getHttpsCallable(FUNCTION_CREATE_BODY_PROGRESS)
                .call(
                    mapOf(
                        FIELD_MEASUREMENT_BEFORE_ID to beforeMeasurementId,
                        FIELD_MEASUREMENT_AFTER_ID to afterMeasurementId,
                    ),
                )
                .await()
                .data
            Result.success(
                json.decodeFromJsonElement(
                    BodyProgress3dResponse.serializer(),
                    data.toJsonElement()
                ).id
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun getBodyProgress(id: String): Result<BodyProgress3dResponse> =
        withContext(Dispatchers.IO) {
            try {
                val data = functions
                    .getHttpsCallable(FUNCTION_GET_BODY_PROGRESS)
                    .call(mapOf(FIELD_ID to id))
                    .await()
                    .data
                Result.success(
                    json.decodeFromJsonElement(
                        BodyProgress3dResponse.serializer(),
                        data.toJsonElement()
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }

    private fun readPhoto(uri: Uri): ByteArray =
        ImageCompressor.readAsJpeg(
            context = context,
            uri = uri,
            targetBytes = ScanPhotoConstants.SCAN_PHOTO_COMPRESSION_THRESHOLD_BYTES,
            startQuality = ScanPhotoConstants.SCAN_PHOTO_JPEG_QUALITY,
            minQuality = ScanPhotoConstants.SCAN_PHOTO_MIN_JPEG_QUALITY,
            qualityStep = ScanPhotoConstants.SCAN_PHOTO_QUALITY_STEP,
            hardCapBytes = ScanPhotoConstants.SCAN_PHOTO_MAX_BYTES,
            skipIfAtMostBytes = ScanPhotoConstants.SCAN_PHOTO_COMPRESSION_THRESHOLD_BYTES,
        )

    private suspend fun deleteQuietly(vararg refs: StorageReference) {
        refs.forEach { ref ->
            runCatching { ref.delete().await() }
                .onFailure { Timber.w(it, "Failed to delete orphaned temp scan file %s", ref.path) }
        }
    }

    companion object {
        private const val TMP_SCAN_ROOT = "tmpScans"
        private const val MEDIA_TYPE_JPEG = "image/jpeg"

        private const val FUNCTION_CREATE_MEASUREMENT = "threeDLookCreateMeasurement"
        private const val FUNCTION_GET_MEASUREMENT = "threeDLookGetMeasurement"
        private const val FUNCTION_CREATE_BODY_PROGRESS = "threeDLookCreateBodyProgress"
        private const val FUNCTION_GET_BODY_PROGRESS = "threeDLookGetBodyProgress"

        private const val FIELD_ID = "id"
        private const val FIELD_FRONT_PATH = "frontPath"
        private const val FIELD_SIDE_PATH = "sidePath"
        private const val FIELD_HEIGHT_CM = "heightCm"
        private const val FIELD_WEIGHT_KG = "weightKg"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_AGE = "age"
        private const val FIELD_MEASUREMENT_BEFORE_ID = "measurementBeforeId"
        private const val FIELD_MEASUREMENT_AFTER_ID = "measurementAfterId"

        private const val FRONT_PHOTO_FILENAME = "front.jpg"
        private const val SIDE_PHOTO_FILENAME = "side.jpg"
    }
}
