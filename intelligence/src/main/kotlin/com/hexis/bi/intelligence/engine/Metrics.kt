package com.hexis.bi.intelligence.engine

object Metrics {

    const val STEPS = "steps"
    const val DISTANCE = "distance"
    const val ACTIVE_MINUTES = "active_minutes"
    const val ACTIVE_CALORIES = "active_calories"
    const val VO2MAX = "vo2max"
    const val SLEEP_DURATION = "sleep_duration"
    const val SLEEP_EFFICIENCY = "sleep_efficiency"
    const val DEEP_SLEEP = "deep_sleep"
    const val REM_SLEEP = "rem_sleep"
    const val RESTING_HR = "resting_hr"
    const val HRV_RMSSD = "hrv_rmssd"
    const val WEIGHT = "weight"
    const val BODY_FAT_PCT = "body_fat_pct"
    const val LEAN_MASS = "lean_mass"
    const val WAIST = "waist"

    const val SHOULDER_TO_WAIST = "shoulder_to_waist"

    const val LONGEVITY_SCORE = "longevity_score"
    const val AGING_SCORE = "aging_score"
    const val PHYSIQUE_SCORE = "physique_score"
    const val STRESS_SCORE = "stress_score"

    val SCAN = setOf(WEIGHT, BODY_FAT_PCT, LEAN_MASS, WAIST)

    private val SLEEP = setOf(
        SLEEP_DURATION, SLEEP_EFFICIENCY, DEEP_SLEEP, REM_SLEEP, RESTING_HR, HRV_RMSSD,
    )

    private val UNITS = mapOf(
        STEPS to "count",
        DISTANCE to "m",
        ACTIVE_MINUTES to "min",
        ACTIVE_CALORIES to "kcal",
        VO2MAX to "ml/kg/min",
        SLEEP_DURATION to "h",
        SLEEP_EFFICIENCY to "ratio",
        DEEP_SLEEP to "h",
        REM_SLEEP to "h",
        RESTING_HR to "bpm",
        HRV_RMSSD to "ms",
        WEIGHT to "kg",
        BODY_FAT_PCT to "%",
        LEAN_MASS to "kg",
        WAIST to "cm",
        LONGEVITY_SCORE to "0-100",
        AGING_SCORE to "0-100",
        PHYSIQUE_SCORE to "0-10",
        STRESS_SCORE to "0-100",
    )

    fun unitOf(metric: String): String = UNITS[metric].orEmpty()

    fun sourceOf(metric: String): String = when (metric) {
        in SCAN -> SOURCE_SCAN
        in SLEEP -> SOURCE_SLEEP
        else -> SOURCE_DAILY
    }

    const val SOURCE_DAILY = "daily"
    const val SOURCE_SLEEP = "sleep"
    const val SOURCE_SCAN = "scan"
    const val SOURCE_COMPOSITE = "composite"
}

object Domains {
    const val BODY = "body"
    const val SLEEP = "sleep"
    const val RECOVERY = "recovery"
    const val ACTIVITY = "activity"
    const val AGING = "aging"
    const val STRESS = "stress"
}

object Directions {
    const val UP = "up"
    const val DOWN = "down"
    const val STABLE = "stable"
    const val INSUFFICIENT_DATA = "insufficient_data"
}
