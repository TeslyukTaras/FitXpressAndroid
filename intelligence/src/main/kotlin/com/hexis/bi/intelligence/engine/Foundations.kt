package com.hexis.bi.intelligence.engine

import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.model.Trend
import kotlin.math.abs

object FoundationStatus {
    const val STRENGTHENING = "strengthening"
    const val HOLDING = "holding"
    const val MIXED = "mixed"
    const val WEAKENING = "weakening"
    const val INSUFFICIENT = "insufficient_data"
}

object FoundationDirection {
    const val STRENGTHENING = "strengthening"
    const val HOLDING = "holding"
    const val MIXED = "mixed"
    const val WEAKENING = "weakening"
    const val BUILDING_YOUR_TREND = "building_your_trend"
}

internal object Foundations {
    const val METABOLIC = "metabolic"
    const val RECOMPOSITION = "recomposition"
    const val PHYSICAL = "physical"
    const val FUNCTIONAL = "functional"
}

internal data class FoundationsResult(val direction: String, val statuses: Map<String, String>)

internal fun evaluateFoundations(
    trends: Map<String, Trend>,
    config: EngineConfig,
): FoundationsResult {
    val metabolic = combineSignals(
        listOf(
            signalOf(directionOf(trends, Metrics.WAIST), goodIsUp = false),
            signalOf(directionOf(trends, Metrics.BODY_FAT_PCT), goodIsUp = false),
        ),
    )
    val recomposition = recompositionStatus(trends, config)
    val functional = combineSignals(
        listOf(
            signalOf(directionOf(trends, Metrics.VO2MAX), goodIsUp = true),
            signalOf(directionOf(trends, Metrics.RESTING_HR), goodIsUp = false),
        ),
    )
    val statuses = linkedMapOf(
        Foundations.METABOLIC to metabolic,
        Foundations.RECOMPOSITION to recomposition,
        Foundations.PHYSICAL to FoundationStatus.INSUFFICIENT,
        Foundations.FUNCTIONAL to functional,
    )

    val direction =
        if (metabolic == FoundationStatus.INSUFFICIENT && recomposition == FoundationStatus.INSUFFICIENT) {
            FoundationDirection.BUILDING_YOUR_TREND
        } else {
            combineDirection(statuses, metabolic, recomposition, config)
        }
    return FoundationsResult(direction, statuses)
}

private fun directionOf(trends: Map<String, Trend>, metric: String): String =
    trends[metric]?.direction ?: Directions.INSUFFICIENT_DATA

private fun signalOf(direction: String, goodIsUp: Boolean): Int? = when (direction) {
    Directions.INSUFFICIENT_DATA -> null
    Directions.STABLE -> 0
    else -> if ((direction == Directions.UP) == goodIsUp) 1 else -1
}

private fun combineSignals(signals: List<Int?>): String {
    val present = signals.filterNotNull()
    if (present.isEmpty()) return FoundationStatus.INSUFFICIENT
    val positive = present.any { it > 0 }
    val negative = present.any { it < 0 }
    return when {
        positive && negative -> FoundationStatus.MIXED
        positive -> FoundationStatus.STRENGTHENING
        negative -> FoundationStatus.WEAKENING
        else -> FoundationStatus.HOLDING
    }
}

private fun recompositionStatus(trends: Map<String, Trend>, config: EngineConfig): String {
    val fat = directionOf(trends, Metrics.BODY_FAT_PCT)
    val lean = trends[Metrics.LEAN_MASS]
    val leanDirection = lean?.direction ?: Directions.INSUFFICIENT_DATA
    if (fat == Directions.INSUFFICIENT_DATA && leanDirection == Directions.INSUFFICIENT_DATA) {
        return FoundationStatus.INSUFFICIENT
    }
    val fatDown = fat == Directions.DOWN
    val fatUp = fat == Directions.UP
    val leanDown = leanDirection == Directions.DOWN
    val leanUp = leanDirection == Directions.UP
    val majorLeanLoss = lean != null && leanDown &&
        abs(lean.absChange) >= config.composites.foundations.majorLeanLossKg

    return when {
        fatDown && leanUp -> FoundationStatus.STRENGTHENING
        fatUp && leanDown -> FoundationStatus.WEAKENING
        fatDown && leanDown -> if (majorLeanLoss) FoundationStatus.WEAKENING else FoundationStatus.MIXED
        fatUp && leanUp -> FoundationStatus.MIXED
        fatDown || leanUp -> FoundationStatus.STRENGTHENING
        fatUp || majorLeanLoss -> FoundationStatus.WEAKENING
        leanDown -> FoundationStatus.MIXED
        else -> FoundationStatus.HOLDING
    }
}

private fun combineDirection(
    statuses: Map<String, String>,
    metabolic: String,
    recomposition: String,
    config: EngineConfig,
): String {
    val foundations = config.composites.foundations
    fun weightOf(status: String) = compensatedSum(
        statuses.filterValues { it == status }.keys.map { foundations.weights.getValue(it) },
    )

    val strengthening = weightOf(FoundationStatus.STRENGTHENING)
    val weakening = weightOf(FoundationStatus.WEAKENING)
    val mixed = weightOf(FoundationStatus.MIXED)
    val fatUpLeanDown = recomposition == FoundationStatus.WEAKENING &&
        metabolic != FoundationStatus.STRENGTHENING

    return when {
        weakening >= foundations.weakeningWeightMin || fatUpLeanDown -> FoundationDirection.WEAKENING
        strengthening > 0.0 && weakening > 0.0 -> FoundationDirection.MIXED
        strengthening >= foundations.strengtheningWeightMin -> FoundationDirection.STRENGTHENING
        mixed >= foundations.weakeningWeightMin -> FoundationDirection.MIXED
        else -> FoundationDirection.HOLDING
    }
}
