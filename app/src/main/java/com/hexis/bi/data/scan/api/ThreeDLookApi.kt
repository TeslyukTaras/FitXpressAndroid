package com.hexis.bi.data.scan.api

import android.content.Context
import android.net.Uri
import com.hexis.bi.BuildConfig
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ThreeDLookApi(
    private val client: OkHttpClient,
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
        runCatching {
            val frontBytes = readPhoto(frontPhoto)
            val sideBytes = readPhoto(sidePhoto)

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    FIELD_FRONT_PHOTO,
                    FRONT_PHOTO_FILENAME,
                    frontBytes.toRequestBody(MEDIA_TYPE_JPEG.toMediaType()),
                )
                .addFormDataPart(
                    FIELD_SIDE_PHOTO,
                    SIDE_PHOTO_FILENAME,
                    sideBytes.toRequestBody(MEDIA_TYPE_JPEG.toMediaType()),
                )
                .addFormDataPart(FIELD_HEIGHT, heightCm.toInt().toString())
                .addFormDataPart(FIELD_GENDER, gender)
                .addFormDataPart(FIELD_AGE, age.toString())
            if (weightKg != null) {
                body.addFormDataPart(FIELD_WEIGHT, weightKg.toInt().toString())
            }
            val requestBody = body.build()

            val request = Request.Builder()
                .url("$BASE_URL$PATH_MEASUREMENTS")
                .addHeader(HEADER_AUTHORIZATION, "$AUTH_SCHEME ${BuildConfig.THREEDLOOK_API_TOKEN}")
                .post(requestBody)
                .build()

            Timber.d("createMeasurement: POST %s%s (front=%dB side=%dB)", BASE_URL, PATH_MEASUREMENTS, frontBytes.size, sideBytes.size)
            client.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                Timber.d("createMeasurement: %d body=%s", response.code, responseBody)

                if (!response.isSuccessful) {
                    error("Create measurement failed (${response.code}): $responseBody")
                }

                json.decodeFromString<CreateMeasurementResponse>(responseBody).id
            }
        }
    }

    suspend fun getMeasurement(id: String): Result<MeasurementResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$BASE_URL$PATH_MEASUREMENTS$id/")
                    .addHeader(HEADER_AUTHORIZATION, "$AUTH_SCHEME ${BuildConfig.THREEDLOOK_API_TOKEN}")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    Timber.d("getMeasurement: %d body=%s", response.code, responseBody)

                    if (!response.isSuccessful) {
                        error("Get measurement failed (${response.code}): $responseBody")
                    }

                    json.decodeFromString<MeasurementResponse>(responseBody)
                }
            }
        }

    suspend fun createBodyProgress(
        beforeMeasurementId: String,
        afterMeasurementId: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(
                BodyProgressRequest.serializer(),
                BodyProgressRequest(beforeMeasurementId, afterMeasurementId),
            )
            val request = Request.Builder()
                .url("$BASE_URL$PATH_BODY_PROGRESS")
                .addHeader(HEADER_AUTHORIZATION, "$AUTH_SCHEME ${BuildConfig.THREEDLOOK_API_TOKEN}")
                .post(payload.toRequestBody(MEDIA_TYPE_JSON.toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                Timber.d("createBodyProgress: %d body=%s", response.code, responseBody)
                if (!response.isSuccessful) {
                    error("Create body progress failed (${response.code}): $responseBody")
                }
                json.decodeFromString<BodyProgress3dResponse>(responseBody).id
            }
        }
    }

    suspend fun getBodyProgress(id: String): Result<BodyProgress3dResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$BASE_URL$PATH_BODY_PROGRESS$id/")
                    .addHeader(HEADER_AUTHORIZATION, "$AUTH_SCHEME ${BuildConfig.THREEDLOOK_API_TOKEN}")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body.string()
                    if (!response.isSuccessful) {
                        error("Get body progress failed (${response.code}): $responseBody")
                    }
                    json.decodeFromString<BodyProgress3dResponse>(responseBody)
                }
            }
        }

    private fun readPhoto(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: error("Unable to open photo: $uri")

    companion object {
        private const val BASE_URL = "https://backend.fitxpress.3dlook.me/api/1.0/"
        private const val PATH_MEASUREMENTS = "measurements/"
        private const val PATH_BODY_PROGRESS = "body_progress/"

        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val AUTH_SCHEME = "Token"

        private const val MEDIA_TYPE_JPEG = "image/jpeg"
        private const val MEDIA_TYPE_JSON = "application/json"

        private const val FIELD_FRONT_PHOTO = "front_photo"
        private const val FIELD_SIDE_PHOTO = "side_photo"
        private const val FIELD_HEIGHT = "height"
        private const val FIELD_WEIGHT = "weight"
        private const val FIELD_GENDER = "gender"
        private const val FIELD_AGE = "age"

        private const val FRONT_PHOTO_FILENAME = "front.jpg"
        private const val SIDE_PHOTO_FILENAME = "side.jpg"
    }
}
