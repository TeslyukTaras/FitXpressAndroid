package com.hexis.bi.utils.constants

internal data class ChangeTolerance(
    val floor: Float,
    val fraction: Float = 0f,
) {
    fun forBaseline(baseline: Float?): Float {
        val size = baseline?.takeIf { it.isFinite() && it > 0f } ?: return floor
        return maxOf(floor, fraction * size)
    }
}

internal object ChangeTolerances {
    val BODY_LENGTH_CM = ChangeTolerance(floor = 1.0f, fraction = 0.012f)
    val MASS_KG = ChangeTolerance(floor = 0.5f)
    val BODY_FAT_PERCENT = ChangeTolerance(floor = 1.0f)
    val LEAN_MASS_PERCENT = ChangeTolerance(floor = 1.0f)
    val WAIST_TO_HEIGHT_RATIO = ChangeTolerance(floor = 0.006f)
    val PHYSIQUE_POINTS = ChangeTolerance(floor = 0.5f)
    val SCORE_POINTS = ChangeTolerance(floor = 3f)
    val RESTING_HR_BPM = ChangeTolerance(floor = 2f)
    val VO2MAX = ChangeTolerance(floor = 1.5f)
    val TREND_PERCENT = ChangeTolerance(floor = 2f)

    const val MONITORING_BAND_FRACTION = 0.5f
}
