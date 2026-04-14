package com.hexis.bi.data.scan.api

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hexis.bi.BuildConfig
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
        weightKg: Float,
        gender: String,
        age: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val frontBytes =
                context.contentResolver.openInputStream(frontPhoto)!!.use { it.readBytes() }
            val sideBytes =
                context.contentResolver.openInputStream(sidePhoto)!!.use { it.readBytes() }

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "front_photo",
                    "front.jpg",
                    frontBytes.toRequestBody("image/jpeg".toMediaType()),
                )
                .addFormDataPart(
                    "side_photo",
                    "side.jpg",
                    sideBytes.toRequestBody("image/jpeg".toMediaType()),
                )
                .addFormDataPart("height", heightCm.toInt().toString())
                .addFormDataPart("weight", weightKg.toInt().toString())
                .addFormDataPart("gender", gender)
                .addFormDataPart("age", age.toString())
                .build()

            val request = Request.Builder()
                .url("${BASE_URL}measurements/")
                .addHeader("Authorization", "Token ${BuildConfig.THREEDLOOK_API_TOKEN}")
                .post(body)
                .build()

            Log.d(TAG, "createMeasurement: POST ${BASE_URL}measurements/ (front=${frontBytes.size}B side=${sideBytes.size}B)")
            val response = client.newCall(request).execute()
            val responseBody = response.body.string()
                ?: error("Empty response body")
            Log.d(TAG, "createMeasurement: ${response.code} body=$responseBody")

            if (!response.isSuccessful) {
                error("Create measurement failed (${response.code}): $responseBody")
            }

            json.decodeFromString<CreateMeasurementResponse>(responseBody).id
        }
    }

    suspend fun getMeasurement(id: String): Result<MeasurementResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("${BASE_URL}measurements/$id/")
                    .addHeader("Authorization", "Token ${BuildConfig.THREEDLOOK_API_TOKEN}")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body.string()
                    ?: error("Empty response body")
                Log.d(TAG, "getMeasurement: ${response.code} body=$responseBody")

                if (!response.isSuccessful) {
                    error("Get measurement failed (${response.code}): $responseBody")
                }

                json.decodeFromString<MeasurementResponse>(responseBody)
            }
        }

    companion object {
        private const val TAG = "ThreeDLookApi"
        private const val BASE_URL = "https://backend.fitxpress.3dlook.me/api/1.0/"
    }
}
