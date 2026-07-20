package com.hexis.bi.utils.constants

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion

internal object BodyVisualConstants {

    const val MEASUREMENT_VALUE_FORMAT = "%.1f"

    /** Figure height as a fraction of the tab's height; the renderer solves the distance from it. */
    const val FULL_BODY_FIGURE_HEIGHT_FRACTION = 0.554f

    /** My Body reserves only half a card, so its panel is taller and takes a taller figure. */
    const val MY_BODY_FIGURE_HEIGHT_FRACTION = 0.68f

    /** Negative sits lower on screen; each panel carries a different amount of chrome above it. */
    const val VISUAL_MODEL_CENTER_Y = -0.12f
    const val COMPARE_MODEL_CENTER_Y = -0.30f
    const val MY_BODY_MODEL_CENTER_Y = 0.0f

    const val MY_BODY_CARD_HEIGHT_FRACTION = 0.5f

    /** My Body looks slightly down on the model, which opens out its horizontal body rings. */
    const val UPRIGHT_MODEL_PITCH_DEG = 0f
    const val MY_BODY_MODEL_PITCH_DEG = 10f

    const val MODEL_BLUR_ZOOM_START = 1.05f
    const val MODEL_BLUR_ZOOM_MAX = 3.50f

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

    /** Horizontal (Compare) selector damps less than the vertical one so it scrolls more freely. */
    const val BODY_PART_SELECTOR_HORIZONTAL_SCROLL_SPEED = 0.5f
    const val BODY_PART_SELECTOR_HORIZONTAL_PRE_CONSUMED_SCROLL_RATIO =
        1f - BODY_PART_SELECTOR_HORIZONTAL_SCROLL_SPEED
    const val BODY_PART_SELECTOR_EDGE_UNLOCK_ITEMS = 2.5f
    const val BODY_PART_SELECTOR_SNAP_EPSILON_PX = 1f
    const val BODY_PART_SELECTOR_SNAP_SEARCH_ITERATIONS = 18

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
