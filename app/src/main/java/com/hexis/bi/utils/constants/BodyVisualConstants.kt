package com.hexis.bi.utils.constants

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion

internal object BodyVisualConstants {

    const val MEASUREMENT_VALUE_FORMAT = "%.1f"

    const val COMPARE_MODEL_DISTANCE_SCALE = 1.45f

    const val CURRENT_SCAN_MESH_GLOW = 0.01f

    const val COMPARE_MODEL_BLUR_TOP_BAND_FRACTION = 0.25f
    const val MODEL_BLUR_TOP_BAND_FRACTION = 0.4f

    const val MODEL_BLUR_BOTTOM_BAND_FRACTION = 0.15f

    const val MODEL_DARKEN_TOP_BAND_FRACTION = 0.15f

    const val MODEL_DARKEN_TOP_OPACITY = 0.2f

    const val MODEL_DARKEN_BOTTOM_BAND_FRACTION = 0.30f

    const val MODEL_DARKEN_BOTTOM_OPACITY = 0.40f

    const val BODY_PART_SELECTOR_INACTIVE_ALPHA = 0.4f
    const val BODY_PART_SELECTOR_INNER_TICK_COUNT = 4
    const val BODY_PART_SELECTOR_SCROLL_SPEED = 0.34f
    const val BODY_PART_SELECTOR_PRE_CONSUMED_SCROLL_RATIO =
        1f - BODY_PART_SELECTOR_SCROLL_SPEED
    const val BODY_PART_SELECTOR_EDGE_UNLOCK_ITEMS = 2.5f
    const val BODY_PART_SELECTOR_SNAP_EPSILON_PX = 1f

    val FULL_BODY_MEASUREMENT_ROWS = listOf(
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Neck, R.string.body_part_neck),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Shoulders,
            R.string.body_part_shoulders
        ),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Chest, R.string.body_part_chest),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Forearm,
            R.string.body_part_forearms
        ),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Bicep,
            R.string.body_part_biceps_upper_arm_single_line
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
            R.string.body_part_thighs
        ),
        BodyVisualMeasurementDisplayRow(BodyMeasurementRegion.Calf, R.string.body_part_calves),
        BodyVisualMeasurementDisplayRow(
            BodyMeasurementRegion.Ankle,
            R.string.body_part_ankles
        ),
    )

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
