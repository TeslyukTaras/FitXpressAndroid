package com.hexis.bi.utils.constants

internal object GlassConstants {

    const val LEVEL_DEFAULT = 45
    const val LEVEL_RAISED = 80
    const val LEVEL_SELECTED = 100

    /** Alpha applied to primary when used as a soft selection highlight (pickers, date selection). */
    const val SELECTION_HIGHLIGHT_ALPHA = 0.15f
    const val TEXT_FIELD_BACKGROUND_ALPHA = 0.75f
    const val TEXT_FIELD_BORDER_ALPHA = 0.25f

    const val RIM_BASE_ALPHA = 0.12f
    const val RIM_RANGE_ALPHA = 0.84f

    const val GLOW_BASE_ALPHA = 0.018f
    const val GLOW_RANGE_ALPHA = 0.055f

    /** Tight darkening just inside the bottom-left rim (occlusion). Scales with [level]. */
    const val INNER_BL_SHADOW_BASE_ALPHA = 0.07f
    const val INNER_BL_SHADOW_RANGE_ALPHA = 0.18f
    const val INNER_BL_SHADOW_RADIUS_FRAC = 0.52f

    /** Soft interior light from the top-right. Scales with [level]. */
    const val INNER_TR_HIGHLIGHT_BASE_ALPHA = 0.014f
    const val INNER_TR_HIGHLIGHT_RANGE_ALPHA = 0.065f
    const val INNER_TR_HIGHLIGHT_RADIUS_FRAC = 0.72f

    /** Very subtle dark “dead” zones away from the primary spec. */
    const val DEAD_ZONE_BASE_ALPHA = 0.010f
    const val DEAD_ZONE_RANGE_ALPHA = 0.035f

    /** Inset along corner ray on wide pills / rectangles (toward bbox TR / BL). */
    const val DEAD_ZONE_CENTER_INSET_FRAC = 0.93f

    /** Polar circles: sit slightly inside the rim so falloff is smooth (avoids jagged clip edge). */
    const val DEAD_ZONE_CENTER_INSET_POLAR_FRAC = 0.82f

    /** Use polar −45° / 135° dead-zone placement when min/max side ≥ this (circles & square-ish). */
    const val DEAD_ZONE_POLAR_ASPECT_THRESHOLD = 0.88f

    /** Mild bottom-left corner falloff for depth (does not replace BL shadow). */
    const val BL_VIGNETTE_BASE_ALPHA = 0.008f
    const val BL_VIGNETTE_RANGE_ALPHA = 0.028f

    const val RIM_NW = 1.00f
    const val RIM_SE = 0.92f
    const val RIM_TOP = 0.78f
    const val RIM_RIGHT = 0.78f
    const val RIM_LEFT = 0.55f
    const val RIM_BOTTOM = 0.38f
    const val RIM_NE = 0.12f
    const val RIM_SW = 0.03f

    /** Smaller = sharper, brighter NW rim band. */
    const val RIM_TRANSITION_FRACTION = 0.065f
    const val RIM_MIN_TRANSITION = 0.014f
    const val PILL_RIM_TRANSITION_MULTIPLIER = 3.25f
    const val PILL_RIM_MIN_TRANSITION_MULTIPLIER = 3.5f

    const val RIM_SWEEP_NW_SHIFT = 0.05f
}
