package com.hexis.bi.utils.constants

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal object GlassConstants {

    const val LEVEL_DEFAULT = 45
    const val LEVEL_RAISED = 80
    const val LEVEL_SELECTED = 100

    const val RIM_BASE_ALPHA = 0.12f
    const val RIM_RANGE_ALPHA = 0.84f

    const val GLOW_BASE_ALPHA = 0.018f
    const val GLOW_RANGE_ALPHA = 0.055f

    /** Tight darkening just inside the NW rim (occlusion). Scales with [level]. */
    const val INNER_NW_SHADOW_BASE_ALPHA = 0.102f
    const val INNER_NW_SHADOW_RANGE_ALPHA = 0.324f
    const val INNER_NW_SHADOW_RADIUS_FRAC = 0.36f

    /** Soft interior light opposite the rim (SE). Scales with [level]. */
    const val INNER_SE_HIGHLIGHT_BASE_ALPHA = 0.026f
    const val INNER_SE_HIGHLIGHT_RANGE_ALPHA = 0.15f
    const val INNER_SE_HIGHLIGHT_RADIUS_FRAC = 0.64f

    /** Very subtle dark “dead” zones away from the primary spec. */
    const val DEAD_ZONE_BASE_ALPHA = 0.022f
    const val DEAD_ZONE_RANGE_ALPHA = 0.074f

    /** Radial size of top-right / bottom-left dead zones (× min side). */
    const val DEAD_ZONE_RADIUS_TR_FRAC = 0.54f
    const val DEAD_ZONE_RADIUS_BL_FRAC = 0.50f

    /** Inset along corner ray on wide pills / rectangles (toward bbox TR / BL). */
    const val DEAD_ZONE_CENTER_INSET_FRAC = 0.93f

    /** Polar circles: sit slightly inside the rim so falloff is smooth (avoids jagged clip edge). */
    const val DEAD_ZONE_CENTER_INSET_POLAR_FRAC = 0.82f

    /** Use polar −45° / 135° dead-zone placement when min/max side ≥ this (circles & square-ish). */
    const val DEAD_ZONE_POLAR_ASPECT_THRESHOLD = 0.88f

    /** Circles: radius ≥ min side so falloff crosses most of the disc (avoids tiny dot). */
    const val DEAD_ZONE_RADIUS_TR_COMPACT_FRAC = 1.02f
    const val DEAD_ZONE_RADIUS_BL_COMPACT_FRAC = 0.98f

    /** Mild SE corner falloff for depth (does not replace SE highlight). */
    const val SE_VIGNETTE_BASE_ALPHA = 0.014f
    const val SE_VIGNETTE_RANGE_ALPHA = 0.05f

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

    val RIM_WIDTH = 0.5.dp

    const val RIM_SWEEP_NW_SHIFT = 0.05f

    val GLASS_TRACK_FILL = Color(0x0D090909)
    val GLASS_SELECTION_FILL = Color(0x4D090909)
}
