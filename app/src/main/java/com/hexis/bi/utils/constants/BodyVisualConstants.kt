package com.hexis.bi.utils.constants

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion

internal object BodyVisualConstants {

    /** One-decimal pattern for centimetre measurement values. */
    const val CM_VALUE_FORMAT = "%.1f"

    /** Model blur — top band height as a fraction of the model height. */
    const val MODEL_BLUR_TOP_BAND_FRACTION = 0.4f

    /** Model blur — bottom band height as a fraction of the model height. */
    const val MODEL_BLUR_BOTTOM_BAND_FRACTION = 0.15f

    /** Model edge darkening — top band height as a fraction of the model height. */
    const val MODEL_DARKEN_TOP_BAND_FRACTION = 0.15f

    /** Model edge darkening — black opacity at the very top edge (ramps to 0). */
    const val MODEL_DARKEN_TOP_OPACITY = 0.2f

    /** Model edge darkening — bottom band height as a fraction of the model height. */
    const val MODEL_DARKEN_BOTTOM_BAND_FRACTION = 0.30f

    /** Model edge darkening — black opacity at the very bottom edge (ramps to 0). */
    const val MODEL_DARKEN_BOTTOM_OPACITY = 0.40f

    val FULL_BODY_MEASUREMENT_ROWS = listOf(
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Neck, R.string.body_part_neck),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Shoulders,
            R.string.body_part_shoulders
        ),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Chest, R.string.body_part_chest),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Forearm,
            R.string.body_part_left_forearm
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Forearm,
            R.string.body_part_right_forearm
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Bicep,
            R.string.body_part_left_biceps_upper_arm
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Bicep,
            R.string.body_part_right_biceps_upper_arm
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.UpperWaist,
            R.string.body_part_upper_waist
        ),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Waist, R.string.body_part_mid_waist),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.LowerWaist,
            R.string.body_part_lower_waist
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.HipsGlutes,
            R.string.body_part_hips_glutes
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Thigh,
            R.string.body_part_right_thigh
        ),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Thigh, R.string.body_part_left_thigh),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Calf, R.string.body_part_right_calf),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Calf, R.string.body_part_left_calf),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Ankle,
            R.string.body_part_right_ankle
        ),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Ankle, R.string.body_part_left_ankle),
    )

    /** Visual-tab label for a region; the scroll selector lists [BodyMeasurementRegion.entries]. */
    @StringRes
    fun visualLabelRes(region: BodyMeasurementRegion): Int = when (region) {
        BodyMeasurementRegion.FullBody -> R.string.body_part_full_body
        BodyMeasurementRegion.Neck -> R.string.body_part_neck
        BodyMeasurementRegion.Shoulders -> R.string.body_part_shoulders
        BodyMeasurementRegion.Chest -> R.string.body_part_chest
        BodyMeasurementRegion.Forearm -> R.string.body_part_forearms
        BodyMeasurementRegion.Bicep -> R.string.body_part_biceps_upper_arm
        BodyMeasurementRegion.UpperWaist -> R.string.body_part_upper_waist
        BodyMeasurementRegion.Waist -> R.string.body_part_mid_waist
        BodyMeasurementRegion.LowerWaist -> R.string.body_part_lower_waist
        BodyMeasurementRegion.HipsGlutes -> R.string.body_part_hips_glutes
        BodyMeasurementRegion.Thigh -> R.string.body_part_thighs
        BodyMeasurementRegion.Calf -> R.string.body_part_calves
        BodyMeasurementRegion.Ankle -> R.string.body_part_ankles
    }

    /** Visual measurement-card header label. Keeps selector/table labels independent. */
    @StringRes
    fun visualHeaderLabelRes(region: BodyMeasurementRegion): Int = when (region) {
        BodyMeasurementRegion.Bicep -> R.string.body_part_biceps_upper_arm_single_line
        else -> visualLabelRes(region)
    }
}

internal data class BodyVisualMeasurementDisplayRow(
    val region: BodyMeasurementRegion,
    @StringRes val labelRes: Int,
)
