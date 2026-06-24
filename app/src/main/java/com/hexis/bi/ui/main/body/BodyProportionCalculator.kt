package com.hexis.bi.ui.main.body

import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.domain.body.BodyMeasurementKeys
import kotlin.math.abs

internal fun buildBodyProportion(
    latest: ScanRecord?,
    heightCm: Float?,
    gender: String?,
): BodyProportionState {
    val measurements = latest?.measurements.orEmpty()
    val frontLinear = latest?.frontLinearParams.orEmpty()
    val isFemaleProfile = isFemale(gender)

    val waistGirth = measurements[BodyMeasurementKeys.Waist]
        ?: measurements[BodyMeasurementKeys.AlternativeWaistGirth]
        ?: measurements[BodyMeasurementKeys.Abdomen]
    val hipGirth = measurements[BodyMeasurementKeys.LowHips]
        ?: measurements[BodyMeasurementKeys.HighHips]
    val thigh = measurements[BodyMeasurementKeys.Thigh]
    val calf = measurements[BodyMeasurementKeys.Calf]

    val shoulderGirth = measurements[BodyMeasurementKeys.OverarmGirth]
    val shoulderWidth = frontLinear[BodyMeasurementKeys.Shoulders]
        ?: measurements[BodyMeasurementKeys.Shoulders]
    val waistWidth = frontLinear[BodyMeasurementKeys.Waist]
    val hipWidth = frontLinear[BodyMeasurementKeys.HighHips]

    val waistHeight = ratio(waistGirth, heightCm)
    val thighWaist = ratio(thigh, waistGirth)
    val calfThigh = ratio(calf, thigh)

    val upperMarkers = if (isFemaleProfile) {
        val waistHip = ratio(waistGirth, hipGirth)
        val hipShoulder = ratio(hipGirth, shoulderGirth) ?: ratio(hipWidth, shoulderWidth)
        listOf(
            BodyProportionMarker(
                labelRes = R.string.body_proportion_label_waist_hip,
                value = waistHip,
                statusRes = waistHipStatus(waistHip),
                progress = inverseNormalize(waistHip, min = 0.6f, max = 1.0f),
            ),
            BodyProportionMarker(
                labelRes = R.string.body_proportion_label_hip_shoulder,
                value = hipShoulder,
                statusRes = hipShoulderStatus(hipShoulder),
                progress = balancedProgress(hipShoulder, ideal = 1.0f, spread = 0.2f),
            ),
        )
    } else {
        val shoulderWaist = ratio(shoulderGirth, waistGirth) ?: ratio(shoulderWidth, waistWidth)
        val shoulderHip = ratio(shoulderGirth, hipGirth) ?: ratio(shoulderWidth, hipWidth)
        listOf(
            BodyProportionMarker(
                labelRes = R.string.body_proportion_label_shoulder_waist,
                value = shoulderWaist,
                statusRes = vTaperStatus(shoulderWaist),
                progress = normalize(shoulderWaist, min = 1.1f, max = 1.7f),
            ),
            BodyProportionMarker(
                labelRes = R.string.body_proportion_label_shoulder_hip,
                value = shoulderHip,
                statusRes = shoulderHipStatus(shoulderHip),
                progress = normalize(shoulderHip, min = 0.9f, max = 1.35f),
            ),
        )
    }

    val groups = listOf(
        BodyProportionGroup(
            titleRes = R.string.body_proportion_group_upper,
            markers = upperMarkers,
        ),
        BodyProportionGroup(
            titleRes = R.string.body_proportion_group_mid,
            markers = listOf(
                BodyProportionMarker(
                    labelRes = R.string.body_proportion_label_waist_height,
                    value = waistHeight,
                    statusRes = waistHeightStatus(waistHeight),
                    progress = inverseNormalize(waistHeight, min = 0.35f, max = 0.65f),
                ),
            ),
        ),
        BodyProportionGroup(
            titleRes = R.string.body_proportion_group_lower,
            markers = listOf(
                BodyProportionMarker(
                    labelRes = R.string.body_proportion_label_thigh_waist,
                    value = thighWaist,
                    statusRes = thighWaistStatus(thighWaist),
                    progress = normalize(thighWaist, min = 0.45f, max = 0.9f),
                ),
                BodyProportionMarker(
                    labelRes = R.string.body_proportion_label_calf_thigh,
                    value = calfThigh,
                    statusRes = calfThighStatus(calfThigh),
                    progress = balancedProgress(calfThigh, ideal = 0.65f, spread = 0.18f),
                ),
            ),
        ),
    )

    return BodyProportionState(
        hasData = latest != null && groups.any { group -> group.markers.any { it.value != null } },
        isFemaleProfile = isFemaleProfile,
        groups = groups,
    )
}

private fun isFemale(gender: String?): Boolean =
    gender?.trim()?.equals("female", ignoreCase = true) == true

private fun ratio(numerator: Float?, denominator: Float?): Float? =
    if (numerator != null && denominator != null && denominator > 0f) numerator / denominator
    else null

private fun normalize(value: Float?, min: Float, max: Float): Float =
    value?.let { ((it - min) / (max - min)).coerceIn(0f, 1f) } ?: 0f

private fun inverseNormalize(value: Float?, min: Float, max: Float): Float =
    value?.let { (1f - ((it - min) / (max - min))).coerceIn(0f, 1f) } ?: 0f

private fun balancedProgress(value: Float?, ideal: Float, spread: Float): Float =
    value?.let { (1f - (abs(it - ideal) / spread)).coerceIn(0f, 1f) } ?: 0f

private fun vTaperStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value >= 1.6f -> R.string.body_proportion_status_elite_vtaper
    value >= 1.5f -> R.string.body_proportion_status_excellent
    value >= 1.4f -> R.string.body_proportion_status_athletic
    value >= 1.3f -> R.string.body_proportion_status_fit
    else -> R.string.body_proportion_status_developing
}

private fun shoulderHipStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value > 1.3f -> R.string.body_proportion_status_upper_dominant
    value in 1.05f..1.3f -> R.string.body_proportion_status_athletic_frame
    else -> R.string.body_proportion_status_balanced
}

private fun waistHeightStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value < 0.45f -> R.string.body_proportion_status_excellent
    value < 0.5f -> R.string.body_proportion_status_healthy
    else -> R.string.body_proportion_status_needs_attention
}

private fun thighWaistStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value >= 0.85f -> R.string.body_proportion_status_very_muscular
    value >= 0.75f -> R.string.body_proportion_status_athletic
    value >= 0.7f -> R.string.body_proportion_status_good
    else -> R.string.body_proportion_status_developing
}

private fun calfThighStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value in 0.6f..0.68f -> R.string.body_proportion_status_balanced
    value < 0.6f -> R.string.body_proportion_status_developing
    else -> R.string.body_proportion_status_strong_calves
}

private fun waistHipStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value < 0.75f -> R.string.body_proportion_status_hourglass
    value < 0.85f -> R.string.body_proportion_status_balanced
    else -> R.string.body_proportion_status_developing
}

private fun hipShoulderStatus(value: Float?): Int = when {
    value == null -> R.string.body_proportion_status_not_available
    value >= 1.05f -> R.string.body_proportion_status_pear
    value > 0.95f -> R.string.body_proportion_status_hourglass
    else -> R.string.body_proportion_status_inverted_triangle
}
