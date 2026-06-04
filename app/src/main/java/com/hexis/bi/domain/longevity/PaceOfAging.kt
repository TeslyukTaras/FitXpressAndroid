package com.hexis.bi.domain.longevity

import com.hexis.bi.data.recovery.StressLevel
import com.hexis.bi.utils.constants.PaceOfAgingConstants
import kotlin.math.roundToInt

enum class AgingLevel { Slower, Normal, Faster }

/** The signals that feed the pace, so the UI can name the one moving it most. */
enum class AgingSignal { Hrv, RestingHeartRate, Sleep, Recovery, Activity, BodyFat, Waist, Vo2Max, Stress }

/** One signal's signed effect on the pace: positive lowers it (slower aging), negative raises it. */
data class AgingContribution(val signal: AgingSignal, val effect: Float)

data class PaceOfAgingInputs(
    val hrvMs: Int? = null,
    val restingHeartRateBpm: Int? = null,
    val sleepScore: Int? = null,
    val recoveryScore: Int? = null,
    val steps: Int? = null,
    val bodyFatPercent: Float? = null,
    val waistToHeightRatio: Float? = null,
    val vo2Max: Float? = null,
    val stressLevel: StressLevel? = null,
)

data class PaceOfAgingResult(
    val pace: Float,
    val contributions: List<AgingContribution>,
)

/**
 * Pace of Aging: biological years per calendar year. Starts at [PaceOfAgingConstants.BASELINE] and
 * each available signal removes load (good values → slower aging) or adds it (poor values → faster),
 * mirroring the Longevity normalisation so the two features agree on what "good" looks like. Returns
 * null when no signal is available.
 */
fun computePaceOfAging(inputs: PaceOfAgingInputs): PaceOfAgingResult? {
    val contributions = buildList {
        inputs.hrvMs?.takeIf { it > 0 }
            ?.let { add(contribution(AgingSignal.Hrv, hrvScore(it), PaceOfAgingConstants.EFFECT_HRV)) }
        inputs.restingHeartRateBpm?.takeIf { it > 0 }
            ?.let { add(contribution(AgingSignal.RestingHeartRate, rhrScore(it), PaceOfAgingConstants.EFFECT_RHR)) }
        inputs.sleepScore?.takeIf { it > 0 }
            ?.let { add(contribution(AgingSignal.Sleep, it.toFloat().coerceIn(0f, 100f), PaceOfAgingConstants.EFFECT_SLEEP)) }
        inputs.recoveryScore?.takeIf { it > 0 }
            ?.let { add(contribution(AgingSignal.Recovery, it.toFloat().coerceIn(0f, 100f), PaceOfAgingConstants.EFFECT_RECOVERY)) }
        inputs.steps?.takeIf { it > 0 }
            ?.let { add(contribution(AgingSignal.Activity, activityScore(it), PaceOfAgingConstants.EFFECT_ACTIVITY)) }
        inputs.bodyFatPercent?.takeIf { it > 0f }
            ?.let { add(contribution(AgingSignal.BodyFat, bodyFatScore(it), PaceOfAgingConstants.EFFECT_BODY_FAT)) }
        inputs.waistToHeightRatio?.takeIf { it > 0f }
            ?.let { add(contribution(AgingSignal.Waist, waistScore(it), PaceOfAgingConstants.EFFECT_WAIST)) }
        inputs.vo2Max?.takeIf { it > 0f }
            ?.let { add(contribution(AgingSignal.Vo2Max, vo2Score(it), PaceOfAgingConstants.EFFECT_VO2)) }
        stressEffect(inputs.stressLevel)
            ?.let { add(AgingContribution(AgingSignal.Stress, it)) }
    }
    if (contributions.isEmpty()) return null

    val pace = (PaceOfAgingConstants.BASELINE - contributions.sumOf { it.effect.toDouble() }.toFloat())
        .coerceIn(PaceOfAgingConstants.MIN, PaceOfAgingConstants.MAX)
    return PaceOfAgingResult(pace, contributions)
}

fun agingLevel(pace: Float): AgingLevel = when {
    pace < PaceOfAgingConstants.THRESHOLD_SLOWER -> AgingLevel.Slower
    pace <= PaceOfAgingConstants.THRESHOLD_FASTER -> AgingLevel.Normal
    else -> AgingLevel.Faster
}

/**
 * The pace as a 0–100 score where higher means aging slower, for the Body Intelligence gauge.
 * The slowest pace ([PaceOfAgingConstants.MIN]) maps to 100, the fastest ([MAX]) to 0.
 */
fun agingScore(pace: Float): Int =
    (((PaceOfAgingConstants.MAX - pace) / (PaceOfAgingConstants.MAX - PaceOfAgingConstants.MIN)) * 100f)
        .roundToInt()
        .coerceIn(0, 100)

private fun contribution(signal: AgingSignal, score: Float, maxEffect: Float): AgingContribution =
    AgingContribution(signal, maxEffect * (score - PaceOfAgingConstants.NEUTRAL_SCORE) / PaceOfAgingConstants.NEUTRAL_SCORE)

private fun stressEffect(level: StressLevel?): Float? = when (level) {
    StressLevel.Low -> PaceOfAgingConstants.EFFECT_STRESS_LOW
    StressLevel.High -> PaceOfAgingConstants.EFFECT_STRESS_HIGH
    StressLevel.Medium, null -> null
}
