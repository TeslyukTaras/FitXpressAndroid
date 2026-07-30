package com.hexis.bi.domain.longevity

import com.hexis.bi.utils.constants.LongevityConstants

// Per-signal 0–100 normalisations feeding the Pace of Aging formula. Each maps a raw wearable or
// scan reading onto a common scale so signals in different units can be weighted against each other.

internal fun hrvScore(ms: Int): Float =
    linearScore(ms.toFloat(), LongevityConstants.HRV_SCORE_AT_ZERO_MS, LongevityConstants.HRV_SCORE_AT_HUNDRED_MS)

internal fun rhrScore(bpm: Int): Float =
    linearScore(bpm.toFloat(), LongevityConstants.RHR_SCORE_AT_ZERO_BPM, LongevityConstants.RHR_SCORE_AT_HUNDRED_BPM)

internal fun activityScore(steps: Int): Float =
    linearScore(steps.toFloat(), LongevityConstants.ACTIVITY_SCORE_AT_ZERO_STEPS, LongevityConstants.ACTIVITY_SCORE_AT_HUNDRED_STEPS)

internal fun bodyFatScore(percent: Float): Float =
    linearScore(percent, LongevityConstants.BODY_FAT_SCORE_AT_ZERO_PERCENT, LongevityConstants.BODY_FAT_SCORE_AT_HUNDRED_PERCENT)

internal fun waistScore(ratio: Float): Float =
    linearScore(ratio, LongevityConstants.WAIST_HEIGHT_SCORE_AT_ZERO, LongevityConstants.WAIST_HEIGHT_SCORE_AT_HUNDRED)

internal fun vo2Score(value: Float): Float =
    linearScore(value, LongevityConstants.VO2_SCORE_AT_ZERO, LongevityConstants.VO2_SCORE_AT_HUNDRED)

/** Maps [value] to 0–100, linear between [atZero] and [atHundred]; handles inverted scales. */
private fun linearScore(value: Float, atZero: Float, atHundred: Float): Float {
    if (atZero == atHundred) return 0f
    return (((value - atZero) / (atHundred - atZero)) * 100f).coerceIn(0f, 100f)
}
