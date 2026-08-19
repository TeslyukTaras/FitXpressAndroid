package com.hexis.bi.utils.constants

internal object FindingWindows {

    const val SLEEP_DAY = 7
    const val SLEEP_SUMMARY = 7
    const val ACTIVITY_WEEK = 7
    const val ACTIVITY_MONTH = 30
    const val ACTIVITY_YEAR = 365
}

internal object EngineUnits {

    const val COUNT = "count"
    const val MINUTES = "min"
    const val KCAL = "kcal"
    const val BPM = "bpm"
    const val MILLISECONDS = "ms"
    const val PERCENT = "%"
    const val RATIO = "ratio"
    const val HOURS = "h"
    const val VO2MAX = "ml/kg/min"
    const val SCORE_100 = "0-100"
    const val SCORE_10 = "0-10"

    const val METRES = "m"
    const val KILOGRAMS = "kg"
    const val CENTIMETRES = "cm"

    const val KILOMETRES = "km"
    const val MILES = "mi"
    const val POUNDS = "lb"
    const val INCHES = "in"
}

internal object InsightSubjects {

    const val RECOMPOSITION = "recomposition"
    const val PHYSIQUE_DRIFT = "physique_drift"
    const val LONGEVITY = "longevity"
    const val PACE_OF_AGING = "pace_of_aging"

    val BY_INTERPRETATION = mapOf(
        "positive_recomposition" to RECOMPOSITION,
        "unfavorable_gain" to RECOMPOSITION,
        "lean_mass_concern" to RECOMPOSITION,
        "physique_improving" to PHYSIQUE_DRIFT,
        "physique_drift_positive" to PHYSIQUE_DRIFT,
        "physique_drift_negative" to PHYSIQUE_DRIFT,
        "longevity_improving" to LONGEVITY,
        "longevity_declining" to LONGEVITY,
        "longevity_foundations_strengthening" to LONGEVITY,
        "longevity_foundations_weakening" to LONGEVITY,
        "longevity_foundations_mixed" to LONGEVITY,
        "aging_slowing" to PACE_OF_AGING,
        "aging_accelerating" to PACE_OF_AGING,
    )
}

internal object FindingMetricAliases {

    const val PHYSIQUE_DRIFT_KEY = "physique_drift"
    const val PHYSIQUE_SCORE_METRIC = "physique_score"
}

internal object FindingValues {

    const val MAX_SUPPORTING_METRICS = 3
    const val MAX_VALUES = 2
    const val DAY_Z_THRESHOLD = 1.0
    const val HOME_PREVIEW_CARDS = 3

    const val ONE_DECIMAL = "%.1f"
    const val RATIO_TO_PERCENT = 100
    const val MINUTES_PER_HOUR = 60
    const val HOUR_SUFFIX = " h"
    const val MINUTE_SUFFIX = " m"
    const val METRES_PER_KM = 1000.0
    const val METRES_PER_MILE = 1609.344
}

internal object IntelligenceRemoteConfig {

    const val CONFIG_KEY = "intelligence_engine_config"
    const val WORDING_KEY = "intelligence_wording_config"

    const val DEBUG_FETCH_INTERVAL_SECONDS = 60L
    const val RELEASE_FETCH_INTERVAL_SECONDS = 12L * 60 * 60
}

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
