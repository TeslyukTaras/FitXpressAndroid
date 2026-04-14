package com.hexis.bi.data.scan.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MeasurementResponse(
    val id: String,
    val status: String,
    val url: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val gender: String? = null,
    val height: Double? = null,
    val weight: Double? = null,
    val age: Int? = null,
    val bmi: Double? = null,
    val bmr: Double? = null,
    @SerialName("fat_percentage") val fatPercentage: Double? = null,
    @SerialName("lean_body_mass") val leanBodyMass: Double? = null,
    @SerialName("fat_body_mass") val fatBodyMass: Double? = null,
    @SerialName("estimated_bmi") val estimatedBmi: Double? = null,
    @SerialName("estimated_bmr") val estimatedBmr: Double? = null,
    @SerialName("estimated_weight") val estimatedWeight: Double? = null,
    @SerialName("estimated_lean_body_mass") val estimatedLeanBodyMass: Double? = null,
    @SerialName("estimated_fat_body_mass") val estimatedFatBodyMass: Double? = null,
    @SerialName("model_3d_url") val model3dUrl: String? = null,
    val errors: List<MeasurementError>? = null,
    @SerialName("circumference_params") val circumferenceParams: JsonObject? = null,
    @SerialName("front_linear_params") val frontLinearParams: JsonObject? = null,
    @SerialName("side_linear_params") val sideLinearParams: JsonObject? = null,
    @SerialName("subscription_info") val subscriptionInfo: JsonObject? = null,
)

@Serializable
data class MeasurementError(
    @SerialName("error_source") val errorSource: String? = null,
    val detail: String? = null,
)

@Serializable
data class CreateMeasurementResponse(
    val id: String,
)
