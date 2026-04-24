package com.hexis.bi.data.terra

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request

internal const val TERRA_BASE_URL = "https://api.tryterra.co/v2"

internal val TERRA_JSON_MEDIA = "application/json".toMediaType()

internal val terraJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun terraRequest(url: String): Request.Builder =
    Request.Builder()
        .url(url)
        .addHeader("dev-id", TerraConfig.devId)
        .addHeader("x-api-key", TerraConfig.apiKey)
        .addHeader("Accept", "application/json")
