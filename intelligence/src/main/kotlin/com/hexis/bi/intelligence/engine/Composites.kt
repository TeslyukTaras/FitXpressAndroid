package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.config.PhysiqueConfig
import com.hexis.bi.intelligence.model.MetricPoint
import com.hexis.bi.intelligence.model.MetricSeries

internal object PhysiqueComponents {
    const val BODY_FAT = "body_fat"
    const val LEAN_MASS = "lean_mass"
    const val WAIST_SHAPE = "waist_shape"
    const val PROPORTION = "proportion"
}

internal data class ScorePart(val weight: Double, val score: Double)

private const val SCORE_DIGITS = 1
private const val PHYSIQUE_DIGITS = 2
private const val PERCENT = 100.0
private const val MAX_SCORE = 100.0

internal fun linearScore(value: Double, atZero: Double, atHundred: Double): Double {
    if (atZero == atHundred) return 0.0
    return (((value - atZero) / (atHundred - atZero)) * PERCENT).coerceIn(0.0, MAX_SCORE)
}

internal fun interpolate(value: Double, lowInput: Double, lowScore: Double, highInput: Double, highScore: Double): Double {
    if (lowInput == highInput) return lowScore
    val t = ((value - lowInput) / (highInput - lowInput)).coerceIn(0.0, 1.0)
    return lowScore + t * (highScore - lowScore)
}

private fun positive(day: Map<String, Double>, metric: String): Double? =
    day[metric]?.takeIf { it > 0.0 }

private fun signalScores(day: Map<String, Double>, config: EngineConfig): List<Pair<String, Double>> {
    val anchors = config.composites.longevity.anchors
    val height = config.composites.heightCm
    val scores = mutableListOf<Pair<String, Double>>()
    positive(day, Metrics.HRV_RMSSD)?.let { scores += "hrv" to linearScore(it, anchors.getValue("hrv")) }
    positive(day, Metrics.SLEEP_EFFICIENCY)?.let { scores += "sleep" to minOf(MAX_SCORE, it * PERCENT) }
    positive(day, Metrics.STEPS)?.let { scores += "activity" to linearScore(it, anchors.getValue("activity")) }
    positive(day, Metrics.RESTING_HR)?.let { scores += "rhr" to linearScore(it, anchors.getValue("rhr")) }
    positive(day, Metrics.BODY_FAT_PCT)?.let { scores += "body_fat" to linearScore(it, anchors.getValue("body_fat")) }
    if (height != null && height > 0.0) {
        positive(day, Metrics.WAIST)?.let {
            scores += "waist" to linearScore(it / height, anchors.getValue("waist_to_height"))
        }
    }
    positive(day, Metrics.VO2MAX)?.let { scores += "vo2" to linearScore(it, anchors.getValue("vo2")) }
    return scores
}

private fun linearScore(value: Double, anchor: List<Double>): Double =
    linearScore(value, anchor[0], anchor[1])

internal fun longevityScore(day: Map<String, Double>, config: EngineConfig): Double? {
    val weights = config.composites.longevity.weights
    val parts = signalScores(day, config).map { (signal, score) ->
        weights.getValue(signal) to score.coerceIn(0.0, MAX_SCORE)
    }
    val totalWeight = compensatedSum(parts.map { it.first })
    if (totalWeight <= 0.0) return null
    val score = compensatedSum(parts.map { it.first * it.second }) / totalWeight
    return roundHalfEven(score.coerceIn(0.0, MAX_SCORE), SCORE_DIGITS)
}

internal fun agingScore(day: Map<String, Double>, config: EngineConfig): Double? {
    val pace = config.composites.paceOfAging
    val effects = pace.effects
    val contributions = signalScores(day, config).map { (signal, score) ->
        effects.getValue(signal) * (score - pace.neutralScore) / pace.neutralScore
    }
    if (contributions.isEmpty()) return null
    val paceValue = (pace.baseline - compensatedSum(contributions)).coerceIn(pace.min, pace.max)
    val score = ((pace.max - paceValue) / (pace.max - pace.min)) * PERCENT
    return roundHalfEven(score.coerceIn(0.0, MAX_SCORE), SCORE_DIGITS)
}

internal fun stressScore(day: Map<String, Double>, config: EngineConfig): Double? {
    val hrv = positive(day, Metrics.HRV_RMSSD) ?: return null
    val anchors = config.composites.stress.hrvAnchors
    return roundHalfEven(linearScore(hrv, anchors[0], anchors[1]), SCORE_DIGITS)
}

internal fun physiqueParts(day: Map<String, Double>, config: EngineConfig): Map<String, ScorePart> {
    val physique = config.composites.physique
    val weights = physique.weights
    val height = config.composites.heightCm
    val weightKg = positive(day, Metrics.WEIGHT)
    val parts = LinkedHashMap<String, ScorePart>()

    positive(day, Metrics.BODY_FAT_PCT)?.let {
        parts[PhysiqueComponents.BODY_FAT] =
            ScorePart(weights.getValue(PhysiqueComponents.BODY_FAT), anchorScore(it, physique.bodyFatAnchors))
    }
    if (weightKg != null) {
        day[Metrics.LEAN_MASS]?.takeIf { it != 0.0 }?.let {
            parts[PhysiqueComponents.LEAN_MASS] = ScorePart(
                weights.getValue(PhysiqueComponents.LEAN_MASS),
                interpolate(it / weightKg * PERCENT, physique.leanMass),
            )
        }
    }
    if (height != null && height > 0.0) {
        positive(day, Metrics.WAIST)?.let {
            parts[PhysiqueComponents.WAIST_SHAPE] = ScorePart(
                weights.getValue(PhysiqueComponents.WAIST_SHAPE),
                interpolate(it / height, physique.waistShape),
            )
        }
    }
    if (physique.enableProportion) {
        positive(day, Metrics.SHOULDER_TO_WAIST)?.let {
            parts[PhysiqueComponents.PROPORTION] = ScorePart(
                weights.getValue(PhysiqueComponents.PROPORTION),
                interpolate(it, physique.proportion),
            )
        }
    }
    return parts
}

private fun interpolate(value: Double, anchor: List<Double>): Double =
    interpolate(value, anchor[0], anchor[1], anchor[2], anchor[3])

internal fun scoreFromParts(parts: Map<String, ScorePart>, physique: PhysiqueConfig): Double? {
    val totalWeight = compensatedSum(parts.values.map { it.weight })
    if (totalWeight <= 0.0) return null
    val score = compensatedSum(parts.values.map { it.weight * it.score }) / totalWeight
    return roundHalfEven(score.coerceIn(physique.min, physique.max), PHYSIQUE_DIGITS)
}

internal fun physiqueScore(day: Map<String, Double>, config: EngineConfig): Double? =
    scoreFromParts(physiqueParts(day, config), config.composites.physique)

internal fun anchorScore(value: Double, anchors: List<List<Double>>): Double {
    if (value <= anchors.first()[0]) return anchors.first()[1]
    if (value >= anchors.last()[0]) return anchors.last()[1]
    for (i in 0 until anchors.lastIndex) {
        val (lowInput, lowScore) = anchors[i]
        val (highInput, highScore) = anchors[i + 1]
        if (value <= highInput) return interpolate(value, lowInput, lowScore, highInput, highScore)
    }
    return anchors.last()[1]
}

internal fun buildCompositeSeries(
    series: List<MetricSeries>,
    windowDays: Int,
    config: EngineConfig,
): List<MetricSeries> {
    val dayRows = LinkedHashMap<String, LinkedHashMap<String, Double>>()
    for (metricSeries in series) {
        for (point in metricSeries.points) {
            dayRows.getOrPut(point.date) { LinkedHashMap() }[metricSeries.metric] = point.value
        }
    }

    val carryForward = config.composites.carryForwardDays
    val presentMetrics = dayRows.values.flatMap { it.keys }.toSet()
    val stableOptional = mutableSetOf<String>()
    if (Metrics.VO2MAX in presentMetrics) stableOptional += Metrics.VO2MAX
    if (Metrics.BODY_FAT_PCT in presentMetrics) stableOptional += Metrics.BODY_FAT_PCT
    val height = config.composites.heightCm
    if (height != null && height > 0.0 && Metrics.WAIST in presentMetrics) stableOptional += Metrics.WAIST
    val required = config.composites.requiredDailyInputs.toSet()

    val longevity = mutableListOf<MetricPoint>()
    val aging = mutableListOf<MetricPoint>()
    val stress = mutableListOf<MetricPoint>()
    val physique = mutableListOf<MetricPoint>()
    val lastSlow = LinkedHashMap<String, Pair<String, Double>>()

    for (date in dayRows.keys.sorted()) {
        val observed = dayRows.getValue(date)
        for (metric in carryForward.keys) {
            observed[metric]?.let { lastSlow[metric] = date to it }
        }
        val day = LinkedHashMap(observed)
        for ((metric, seen) in lastSlow) {
            val (observedDate, value) = seen
            val age = EngineDates.daysBetween(observedDate, date)
            if (age in 0..carryForward.getValue(metric)) day.putIfAbsent(metric, value)
        }

        val complete = day.keys.containsAll(required) && day.keys.containsAll(stableOptional)
        if (complete) {
            if (config.features.longevity) longevityScore(day, config)?.let {
                longevity += MetricPoint(date, Metrics.LONGEVITY_SCORE, it, Metrics.SOURCE_COMPOSITE)
            }
            if (config.features.paceOfAging) agingScore(day, config)?.let {
                aging += MetricPoint(date, Metrics.AGING_SCORE, it, Metrics.SOURCE_COMPOSITE)
            }
        }
        if (config.features.stress) stressScore(day, config)?.let {
            stress += MetricPoint(date, Metrics.STRESS_SCORE, it, Metrics.SOURCE_COMPOSITE)
        }
        if (config.features.physique && (Metrics.WEIGHT in observed || Metrics.BODY_FAT_PCT in observed)) {
            physiqueScore(observed, config)?.let {
                physique += MetricPoint(date, Metrics.PHYSIQUE_SCORE, it, Metrics.SOURCE_COMPOSITE)
            }
        }
    }

    return listOf(
        Metrics.LONGEVITY_SCORE to longevity,
        Metrics.AGING_SCORE to aging,
        Metrics.STRESS_SCORE to stress,
        Metrics.PHYSIQUE_SCORE to physique,
    ).filter { it.second.isNotEmpty() }.map { (metric, points) ->
        MetricSeries(
            metric = metric,
            domain = config.domainOf(metric),
            unit = Metrics.unitOf(metric),
            points = points,
            coverage = coverageOf(points.size, windowDays),
        )
    }
}
