package com.hexis.bi.utils.constants

internal object IntelligenceConstants {

    const val MAX_SCORE = 100f
    const val MAX_SCORE_INT = 100

    /** The gauge takes this fraction of the card's available width. */
    const val GAUGE_WIDTH_FRACTION = 0.6f

    /** Semicircle opening downward: from 9 o'clock, sweeping clockwise over the top to 3 o'clock. */
    const val ARC_START_ANGLE = 180f
    const val ARC_TOTAL_SWEEP = 180f

    // Sweep-gradient stops keyed to absolute angle fraction (angle / 360), so the colour band is
    // anchored to the arc: left (180°) red, top (270°) yellow, right (360°) green.
    const val GAUGE_GRADIENT_LEFT_STOP = 180f / 360f
    const val GAUGE_GRADIENT_TOP_STOP = 270f / 360f
    const val GAUGE_GRADIENT_RIGHT_STOP = 1f
}
