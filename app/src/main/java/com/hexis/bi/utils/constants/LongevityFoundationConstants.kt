package com.hexis.bi.utils.constants

/**
 * Thresholds for the body-led Longevity trend model. Distinct from [LongevityConstants], which still
 * serves the retired 0–100 healthy-aging score used by Pace of Aging and the Home tile.
 */
internal object LongevityFoundationConstants {
    // Rolling windows, ending on the current date.
    const val WINDOW_WEEKS_SHORT = 4L
    const val WINDOW_MONTHS_MEDIUM = 6L
    const val WINDOW_YEARS_LONG = 1L

    /** Two observations are the minimum that can describe a direction at all. */
    const val MIN_SCANS_FOR_DIRECTION = 2

    /** Below this share of same-direction steps a change reads as noise rather than a trend. */
    const val MIN_PERSISTENCE = 0.6f

    /**
     * Multiple of a metric's meaningful delta beyond which a change is directional regardless of
     * persistence. Scan-to-scan measurements are noisy enough that a real, large move often fails
     * the persistence check; without this a large decline would be reported as Holding.
     */
    const val DECISIVE_CHANGE_MULTIPLE = 2f

    // Metabolic Health: change in waist-to-height ratio across the window that counts as meaningful.
    const val WAIST_TO_HEIGHT_MEANINGFUL_DELTA = 0.01f

    /** Body-fat percentage points across the window that count as a meaningful move. */
    const val BODY_FAT_MEANINGFUL_DELTA_PERCENT = 1.0f

    // Body Recomposition: kilograms across the window that count as a meaningful move.
    const val MASS_MEANINGFUL_DELTA_KG = 0.5f

    /**
     * Lean loss beyond this makes a weight-loss pattern weakening rather than merely mixed, per the
     * spec rule that weight loss is never automatically rewarded.
     */
    const val MAJOR_LEAN_LOSS_KG = 2.0f

    // Physical Foundation: centimetres across the window that count as a meaningful move.
    const val LOWER_BODY_MEANINGFUL_DELTA_CM = 0.5f

    // Functional Capacity.
    const val VO2_MEANINGFUL_DELTA = 1.5f
    const val RHR_MEANINGFUL_DELTA_BPM = 2f

    /** Days with wearable data needed before Functional Capacity is judged at all. */
    const val MIN_WEARABLE_DAYS = 7

    /** Share of days in the window with recorded activity that reads as consistent training. */
    const val CONSISTENT_ACTIVITY_DAY_FRACTION = 0.5f

    /** Steps in a day for it to count towards activity consistency. */
    const val ACTIVE_DAY_MIN_STEPS = 5_000

    /** Night-to-night spread in sleep duration that still reads as regular. */
    const val REGULAR_SLEEP_STD_DEV_MINUTES = 60.0

    // Overall direction: summed foundation weights required to call the whole picture.
    const val STRENGTHENING_WEIGHT_MIN = 0.60f
    const val WEAKENING_WEIGHT_MIN = 0.50f

    // Internal foundation weights (never shown to the user).
    const val WEIGHT_METABOLIC = 0.35f
    const val WEIGHT_RECOMPOSITION = 0.30f
    const val WEIGHT_PHYSICAL = 0.20f
    const val WEIGHT_FUNCTIONAL = 0.15f
}
