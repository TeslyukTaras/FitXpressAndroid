package com.hexis.bi.domain.longevity

import com.hexis.bi.utils.constants.LongevityConstants
import kotlin.math.roundToInt

/** Signals feeding the healthy-aging score; any may be null when the data isn't available. */
data class LongevityInputs(
    val hrvMs: Int? = null,
    val restingHeartRateBpm: Int? = null,
    val sleepScore: Int? = null,
    val steps: Int? = null,
    val bodyFatPercent: Float? = null,
    val waistToHeightRatio: Float? = null,
    val vo2Max: Float? = null,
)

/**
 * Weighted 0–100 healthy-aging score. Each available signal is normalised to 0–100 and combined
 * with the documented weights (HRV 25%, Sleep 20%, Activity 15%, RHR 15%, VO2 10%, and a 15%
 * body-composition bucket split evenly between Body fat 7.5% and Waist-to-height 7.5%); missing
 * signals drop out and the remaining weights are renormalised. Returns null when no signal is
 * available.
 */
fun computeLongevityScore(inputs: LongevityInputs): Int? {
    val parts = buildList {
        inputs.hrvMs?.takeIf { it > 0 }?.let { add(LongevityConstants.WEIGHT_HRV to hrvScore(it)) }
        inputs.sleepScore?.takeIf { it > 0 }
            ?.let { add(LongevityConstants.WEIGHT_SLEEP to it.toFloat().coerceIn(0f, 100f)) }
        inputs.steps?.takeIf { it > 0 }
            ?.let { add(LongevityConstants.WEIGHT_ACTIVITY to activityScore(it)) }
        inputs.restingHeartRateBpm?.takeIf { it > 0 }
            ?.let { add(LongevityConstants.WEIGHT_RHR to rhrScore(it)) }
        inputs.bodyFatPercent?.takeIf { it > 0f }
            ?.let { add(LongevityConstants.WEIGHT_BODY_FAT to bodyFatScore(it)) }
        inputs.waistToHeightRatio?.takeIf { it > 0f }
            ?.let { add(LongevityConstants.WEIGHT_WAIST to waistScore(it)) }
        inputs.vo2Max?.takeIf { it > 0f }
            ?.let { add(LongevityConstants.WEIGHT_VO2 to vo2Score(it)) }
    }
    val totalWeight = parts.sumOf { it.first.toDouble() }
    if (totalWeight <= 0.0) return null
    val weighted = parts.sumOf { (weight, score) -> (weight * score).toDouble() }
    return (weighted / totalWeight).roundToInt().coerceIn(0, 100)
}

// Per-signal 0–100 normalisations, shared with the Pace of Aging formula.
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
