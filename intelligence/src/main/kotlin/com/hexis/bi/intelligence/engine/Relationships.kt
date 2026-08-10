package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.Trend

object FindingDirection {
    const val POSITIVE = "positive"
    const val NEGATIVE = "negative"
    const val NEUTRAL = "neutral"
    const val MIXED = "mixed"
}

object FindingSource {
    const val PATTERN = "pattern"
    const val METRIC = "metric"
}

internal data class Candidate(
    val id: String,
    val area: String,
    val interpretation: String,
    val direction: String,
    val facts: List<String>,
    val metrics: List<String>,
    val agreementCount: Int,
    val agreementExpected: Int,
    val contradiction: Double,
    val source: String = FindingSource.PATTERN,
    val informational: Boolean = false,
    val requiresCorroboration: Boolean = false,
    val corroboration: List<String> = emptyList(),
    val confidenceFactors: Map<String, Double>? = null,
    val supportingValues: Map<String, String> = emptyMap(),
    val kind: String = CandidateKind.RELATIONSHIP,
)

internal object CandidateKind {
    const val RELATIONSHIP = "relationship"
    const val COMPOSITE = "composite"
}

private const val RECOVERY_SIGNAL_COUNT = 3
private const val FRACTION_DIGITS = 3
private const val DRIFT_VALUE_DIGITS = 2

private val VERB = mapOf(
    Directions.UP to "increased",
    Directions.DOWN to "decreased",
    Directions.STABLE to "stable",
)

private val FOUNDATION_FINDINGS = mapOf(
    FoundationDirection.STRENGTHENING to
        (FindingDirection.POSITIVE to "longevity_foundations_strengthening"),
    FoundationDirection.WEAKENING to
        (FindingDirection.NEGATIVE to "longevity_foundations_weakening"),
    FoundationDirection.MIXED to (FindingDirection.MIXED to "longevity_foundations_mixed"),
)

private val FOUNDATION_METRICS = listOf(
    Metrics.WAIST, Metrics.BODY_FAT_PCT, Metrics.LEAN_MASS, Metrics.VO2MAX, Metrics.RESTING_HR,
)

internal fun evaluateRules(trends: Map<String, Trend>): List<Candidate> {
    val out = mutableListOf<Candidate>()
    val sleep = directionOf(trends, Metrics.SLEEP_DURATION)
    val hrv = directionOf(trends, Metrics.HRV_RMSSD)
    val rhr = directionOf(trends, Metrics.RESTING_HR)
    val recoverySignals = linkedMapOf(
        Metrics.SLEEP_DURATION to sleep,
        Metrics.HRV_RMSSD to hrv,
        Metrics.RESTING_HR to rhr,
    )

    val negative = count(sleep == Directions.DOWN, hrv == Directions.DOWN, rhr == Directions.UP)
    val positive = count(
        sleep == Directions.UP || sleep == Directions.STABLE,
        hrv == Directions.UP,
        rhr == Directions.DOWN || rhr == Directions.STABLE,
    )

    if (negative >= 2) {
        out += Candidate(
            id = "recovery_pressure_2w", area = Domains.RECOVERY, interpretation = "recovery_pressure",
            direction = FindingDirection.NEGATIVE,
            facts = factsFor(
                recoverySignals,
                linkedMapOf(
                    Metrics.SLEEP_DURATION to Directions.DOWN,
                    Metrics.HRV_RMSSD to Directions.DOWN,
                    Metrics.RESTING_HR to Directions.UP,
                ),
            ),
            metrics = recoverySignals.keys.toList(),
            agreementCount = negative, agreementExpected = RECOVERY_SIGNAL_COUNT,
            contradiction = fraction(positive, RECOVERY_SIGNAL_COUNT),
        )
    } else if (positive >= 2) {
        out += Candidate(
            id = "positive_recovery_2w", area = Domains.RECOVERY, interpretation = "positive_recovery",
            direction = FindingDirection.POSITIVE,
            facts = factsFor(
                recoverySignals,
                linkedMapOf(
                    Metrics.SLEEP_DURATION to Directions.UP,
                    Metrics.HRV_RMSSD to Directions.UP,
                    Metrics.RESTING_HR to Directions.DOWN,
                ),
            ),
            metrics = recoverySignals.keys.toList(),
            agreementCount = positive, agreementExpected = RECOVERY_SIGNAL_COUNT,
            contradiction = fraction(negative, RECOVERY_SIGNAL_COUNT),
        )
    }

    val activity = directionOf(trends, Metrics.ACTIVE_MINUTES)
    if (activity == Directions.UP &&
        (hrv == Directions.UP || hrv == Directions.STABLE) &&
        (rhr == Directions.DOWN || rhr == Directions.STABLE)
    ) {
        out += Candidate(
            id = "training_adaptation_2w", area = Domains.ACTIVITY, interpretation = "training_adaptation",
            direction = FindingDirection.POSITIVE,
            facts = listOf("activity_increased", "recovery_held"),
            metrics = listOf(Metrics.ACTIVE_MINUTES, Metrics.HRV_RMSSD, Metrics.RESTING_HR),
            agreementCount = 2, agreementExpected = 2, contradiction = 0.0,
        )
    }

    val waist = directionOf(trends, Metrics.WAIST)
    val bodyFat = directionOf(trends, Metrics.BODY_FAT_PCT)
    val lean = directionOf(trends, Metrics.LEAN_MASS)
    val weight = directionOf(trends, Metrics.WEIGHT)

    if (waist == Directions.DOWN && bodyFat == Directions.DOWN &&
        (lean == Directions.UP || lean == Directions.STABLE)
    ) {
        out += Candidate(
            id = "positive_recomposition_4w", area = Domains.BODY,
            interpretation = "positive_recomposition", direction = FindingDirection.POSITIVE,
            facts = listOf("waist_decreased", "body_fat_decreased", "lean_mass_stable"),
            metrics = listOf(Metrics.WAIST, Metrics.BODY_FAT_PCT, Metrics.LEAN_MASS),
            agreementCount = 3, agreementExpected = 3, contradiction = 0.0,
        )
    } else if (weight == Directions.UP && waist == Directions.UP && bodyFat == Directions.UP &&
        (lean == Directions.STABLE || lean == Directions.DOWN)
    ) {
        out += Candidate(
            id = "unfavorable_gain_4w", area = Domains.BODY, interpretation = "unfavorable_gain",
            direction = FindingDirection.NEGATIVE,
            facts = listOf("weight_increased", "waist_increased", "body_fat_increased"),
            metrics = listOf(Metrics.WEIGHT, Metrics.WAIST, Metrics.BODY_FAT_PCT),
            agreementCount = 3, agreementExpected = 3, contradiction = 0.0,
        )
    } else if (weight == Directions.DOWN && lean == Directions.DOWN) {
        out += Candidate(
            id = "lean_mass_concern_3w", area = Domains.BODY, interpretation = "lean_mass_concern",
            direction = FindingDirection.NEGATIVE,
            facts = listOf("weight_decreased", "lean_mass_decreased"),
            metrics = listOf(Metrics.WEIGHT, Metrics.LEAN_MASS),
            agreementCount = 2, agreementExpected = 2,
            contradiction = if (sleep == Directions.DOWN) 0.0 else 0.3,
        )
    }

    when (directionOf(trends, Metrics.LONGEVITY_SCORE)) {
        Directions.UP -> out += compositeCandidate(
            "longevity_improving", Domains.AGING, FindingDirection.POSITIVE,
            "longevity_trend_up", Metrics.LONGEVITY_SCORE,
        )
        Directions.DOWN -> out += compositeCandidate(
            "longevity_declining", Domains.AGING, FindingDirection.NEGATIVE,
            "longevity_trend_down", Metrics.LONGEVITY_SCORE,
        )
    }

    when (directionOf(trends, Metrics.AGING_SCORE)) {
        Directions.UP -> out += compositeCandidate(
            "aging_slowing", Domains.AGING, FindingDirection.POSITIVE,
            "pace_of_aging_improving", Metrics.AGING_SCORE,
        )
        Directions.DOWN -> out += compositeCandidate(
            "aging_accelerating", Domains.AGING, FindingDirection.NEGATIVE,
            "pace_of_aging_worsening", Metrics.AGING_SCORE,
        )
    }

    if (directionOf(trends, Metrics.PHYSIQUE_SCORE) == Directions.UP) {
        out += compositeCandidate(
            "physique_improving", Domains.BODY, FindingDirection.POSITIVE,
            "physique_trend_up", Metrics.PHYSIQUE_SCORE,
        )
    }

    if (directionOf(trends, Metrics.STRESS_SCORE) == Directions.UP) {
        val support = buildList {
            if (hrv == Directions.DOWN || rhr == Directions.UP) add(Domains.RECOVERY)
            if (sleep == Directions.DOWN) add(Domains.SLEEP)
            if (activity == Directions.UP) add(Domains.ACTIVITY)
        }
        out += Candidate(
            id = "rising_stress", area = Domains.STRESS, interpretation = "rising_stress",
            direction = FindingDirection.NEGATIVE,
            facts = listOf("stress_trend_up") + support.map { "corroborated_by_$it" },
            metrics = listOf(Metrics.STRESS_SCORE),
            agreementCount = 1, agreementExpected = 1, contradiction = 0.0,
            requiresCorroboration = true, corroboration = support,
            kind = CandidateKind.COMPOSITE,
        )
    }

    return out
}

internal fun enabledRuleCandidates(candidates: List<Candidate>, config: EngineConfig): List<Candidate> =
    candidates.filter { it.kind != CandidateKind.RELATIONSHIP || config.features.patternFindings }

internal fun singleMetricCandidates(
    trends: Map<String, Trend>,
    config: EngineConfig,
): List<Candidate> = trends.entries.sortedBy { it.key }.mapNotNull { (metric, trend) ->
    if (metric.endsWith("_score")) return@mapNotNull null
    if (trend.direction !in VERB.keys) return@mapNotNull null
    val area = config.domains[metric] ?: config.findings.defaultMetricArea
    val fact = "${metric}_${VERB.getValue(trend.direction)}"
    if (trend.direction == Directions.STABLE) {
        if (!config.features.stableFindings) return@mapNotNull null
        return@mapNotNull Candidate(
            id = "${metric}_stable", area = area, interpretation = "${metric}_holding",
            direction = FindingDirection.NEUTRAL, facts = listOf(fact), metrics = listOf(metric),
            agreementCount = 1, agreementExpected = 1, contradiction = 0.0,
            source = FindingSource.METRIC, informational = true,
        )
    }
    val direction = when (metric) {
        in config.findings.goodWhenUp -> if (trend.direction == Directions.UP) FindingDirection.POSITIVE else FindingDirection.NEGATIVE
        in config.findings.goodWhenDown -> if (trend.direction == Directions.DOWN) FindingDirection.POSITIVE else FindingDirection.NEGATIVE
        else -> FindingDirection.NEUTRAL
    }
    Candidate(
        id = "${metric}_${trend.direction}", area = area,
        interpretation = "${metric}_${trend.direction}", direction = direction,
        facts = listOf(fact), metrics = listOf(metric),
        agreementCount = 1, agreementExpected = 1, contradiction = 0.0,
        source = FindingSource.METRIC,
    )
}

internal fun foundationCandidates(foundations: FoundationsResult): List<Candidate> {
    val mapping = FOUNDATION_FINDINGS[foundations.direction] ?: return emptyList()
    val (direction, interpretation) = mapping
    return listOf(
        Candidate(
            id = interpretation, area = Domains.AGING, interpretation = interpretation,
            direction = direction,
            facts = foundations.statuses
                .filterValues { it != FoundationStatus.INSUFFICIENT }
                .map { (foundation, status) -> "${foundation}_$status" },
            metrics = FOUNDATION_METRICS,
            agreementCount = 2, agreementExpected = 2, contradiction = 0.0,
        ),
    )
}

internal fun driftCandidates(
    drift: PhysiqueDrift,
    config: EngineConfig,
    runDate: String,
): List<Candidate> {
    if (drift.status != DriftStatus.OK || drift.direction == DriftDirection.HOLDING) return emptyList()
    val direction = requireNotNull(drift.direction)
    val driftValue = requireNotNull(drift.drift)
    return listOf(
        Candidate(
            id = "physique_drift_$direction", area = Domains.BODY,
            interpretation = "physique_drift_$direction",
            direction = if (direction == DriftDirection.POSITIVE) {
                FindingDirection.POSITIVE
            } else {
                FindingDirection.NEGATIVE
            },
            facts = listOf("physique_drift_$direction", "driven_by_${drift.driver}"),
            metrics = listOf(Metrics.PHYSIQUE_SCORE),
            agreementCount = 0, agreementExpected = 1, contradiction = 0.0,
            confidenceFactors = driftConfidenceFactors(drift, config, runDate),
            supportingValues = mapOf(
                "physique_drift" to
                    "${formatSigned(driftValue, DRIFT_VALUE_DIGITS)} pts over ${drift.scans} scans",
            ),
        ),
    )
}

private fun compositeCandidate(
    id: String,
    area: String,
    direction: String,
    fact: String,
    metric: String,
) = Candidate(
    id = id, area = area, interpretation = id, direction = direction,
    facts = listOf(fact), metrics = listOf(metric),
    agreementCount = 1, agreementExpected = 1, contradiction = 0.0,
    kind = CandidateKind.COMPOSITE,
)

private fun directionOf(trends: Map<String, Trend>, metric: String): String =
    trends[metric]?.direction ?: Directions.INSUFFICIENT_DATA

private fun count(vararg conditions: Boolean): Int = conditions.count { it }

private fun factsFor(signals: Map<String, String>, wanted: Map<String, String>): List<String> =
    wanted.mapNotNull { (metric, want) ->
        if (signals[metric] == want) "${metric}_${VERB.getValue(want)}" else null
    }

private fun fraction(count: Int, total: Int): Double =
    if (total == 0) 0.0 else roundHalfEven(count.toDouble() / total, FRACTION_DIGITS)
