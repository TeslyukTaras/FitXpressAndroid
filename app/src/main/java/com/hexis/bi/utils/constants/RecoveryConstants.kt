package com.hexis.bi.utils.constants

internal object RecoveryConstants {
    const val ARC_START_ANGLE = 150f
    const val ARC_TOTAL_SWEEP = 240f
    const val MAX_SCORE = 100f
    val GRID_LINES = listOf(0f, 50f, 75f, 100f)

    // Example: If 50 should be at 75% of the height (from the top) and 100 should be at 0% (the top).
    fun mapScoreToFraction(score: Float) = when {
        score >= 50f -> 0.75f - ((score - 50f) / 50f) * 0.75f
        else -> 1f - (score / 50f) * 0.25f
    }
}


