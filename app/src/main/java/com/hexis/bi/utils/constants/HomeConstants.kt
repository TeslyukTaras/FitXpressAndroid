package com.hexis.bi.utils.constants

internal object HomeConstants {

    // Promo banner imagery (man behind, woman in front). Aspect = width / height of each cutout,
    // so the image width tracks the column-driven height instead of the intrinsic bitmap width.
    const val PROMO_MAN_ASPECT_RATIO = 0.843f
    const val PROMO_WOMAN_ASPECT_RATIO = 0.757f

    // Activity mini bar chart
    const val ACTIVITY_BAR_COUNT_DEFAULT = 24
    // Idle (zero-step) bars are purely decorative: a randomized height across this fraction range.
    const val ACTIVITY_IDLE_BAR_MIN_FRACTION = 0.2f
    const val ACTIVITY_IDLE_BAR_MAX_FRACTION = 0.9f

    // Sleep progress bar
    const val SLEEP_PROGRESS_TRACK_ALPHA = 0.2f

    // Scan sparkline
    const val SPARKLINE_VERTICAL_PADDING_FRACTION = 0.18f
    const val SPARKLINE_FILL_TOP_ALPHA = 0.25f
    const val SPARKLINE_FILL_BOTTOM_ALPHA = 0f
}
