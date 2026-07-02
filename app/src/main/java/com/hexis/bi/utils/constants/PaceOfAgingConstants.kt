package com.hexis.bi.utils.constants

internal object PaceOfAgingConstants {
    const val BASELINE = 1.00f

    const val MIN = 0.70f
    const val MAX = 1.30f

    const val NEUTRAL_SCORE = 50f

    const val EFFECT_HRV = 0.06f
    const val EFFECT_SLEEP = 0.05f
    const val EFFECT_VO2 = 0.04f
    const val EFFECT_RECOVERY = 0.04f
    const val EFFECT_ACTIVITY = 0.03f
    const val EFFECT_BODY_FAT = 0.03f
    const val EFFECT_RHR = 0.03f
    const val EFFECT_WAIST = 0.02f

    const val EFFECT_STRESS_LOW = 0.02f
    const val EFFECT_STRESS_HIGH = -0.03f

    const val THRESHOLD_SLOWER = 0.95f
    const val THRESHOLD_FASTER = 1.05f
}
