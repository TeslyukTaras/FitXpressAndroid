package com.hexis.bi.utils.constants

import androidx.compose.ui.graphics.Color

internal object DarkBackgroundConstants {

    const val MESH_GRADIENT_ANGLE_DEG = 190.26f
    const val MESH_GRADIENT_LINE_LENGTH_FACTOR = 1.2323f

    const val CARD_FILL_GRADIENT_ANGLE_DEG = 183.87f
    const val CARD_FILL_STOP_START = 0.0372f
    const val CARD_FILL_STOP_END = 0.9901f

    const val PROMO_BANNER_GRADIENT_ANGLE_DEG = 246.16f
    const val PROMO_BANNER_STOP_START = 0.0557f
    const val PROMO_BANNER_STOP_END = 0.8058f

    const val TOP_SCRIM_HEIGHT_FRACTION = 0.18f
    const val TOP_SCRIM_HOLD_FRACTION = 0.2f
    const val BOTTOM_SCRIM_HEIGHT_FRACTION = 0.20f
    val BOTTOM_SCRIM_START: Color = Color(8 / 255f, 8 / 255f, 8 / 255f, 0f)
    val BOTTOM_SCRIM_END: Color = Color(8 / 255f, 8 / 255f, 8 / 255f, 0.5f)

    const val MAIN_NAV_BAR_GRADIENT_ANGLE_DEG = 146.44f
    const val MAIN_NAV_BAR_HOLD_FRACTION = 0.3932f
    val MAIN_NAV_BAR_START_COLOR: Color = Color(36 / 255f, 74 / 255f, 73 / 255f, 0.32f)
    val MAIN_NAV_BAR_END_COLOR: Color = Color(3 / 255f, 9 / 255f, 9 / 255f, 0.32f)

    const val SCAN_FAB_GRADIENT_ANGLE_DEG = 262.52f
    const val SCAN_FAB_HOLD_FRACTION = 0.0721f
    val SCAN_FAB_START_COLOR: Color = Color(29 / 255f, 196 / 255f, 179 / 255f, 0.3f)
    val SCAN_FAB_END_COLOR: Color = Color(3 / 255f, 9 / 255f, 9 / 255f, 0.3f)

    /**
     * Vertical gradient Y fractions for component surfaces using the
     * teal→ink palette (switch active track, primary button fill).
     */
    const val COMPONENT_VERTICAL_GRADIENT_START_FRACTION = -0.5312f
    const val COMPONENT_VERTICAL_GRADIENT_END_FRACTION = 1.3021f

    /** Wider end fraction used for the outlined-button stroke gradient. */
    const val COMPONENT_VERTICAL_GRADIENT_END_FRACTION_WIDE = 1.7292f
}
