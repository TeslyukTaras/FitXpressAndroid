package com.hexis.bi.domain.recomposition

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.utils.constants.RecompositionConstants
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

enum class CompositionState {
    Recomposition,
    WeightLoss,
    WeightGain,
    CompositionShift,
    NotEnoughScans,
    NeedsAnotherScan,
}

data class RecompositionResult(
    val state: CompositionState,
    val recomposedKg: Float? = null,
    val fatChangeKg: Float? = null,
    val leanChangeKg: Float? = null,
    val weightChangeKg: Float? = null,
    val observedStart: LocalDate? = null,
    val observedEnd: LocalDate? = null,
)

private data class CompositionSample(
    val date: LocalDate,
    val timeDays: Double,
    val weightKg: Float,
    val fatMassKg: Float,
    val leanMassKg: Float,
)

object RecompositionCalculator {

    fun buildWindow(
        scans: List<ScanRecord>,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): RecompositionResult {
        val samples = scans
            .mapNotNull { it.toCompositionSample(zoneId) }
            .filter { !it.date.isBefore(windowStart) && !it.date.isAfter(windowEnd) }
            .sortedBy { it.timeDays }

        when (samples.size) {
            0 -> return RecompositionResult(CompositionState.NotEnoughScans)
            1 -> return RecompositionResult(CompositionState.NeedsAnotherScan)
        }

        val first = samples.first()
        val last = samples.last()

        val fatChange: Float
        val leanChange: Float
        val weightChange: Float
        if (samples.size >= RecompositionConstants.MIN_SCANS_FOR_TREND) {
            val spanDays = (last.timeDays - first.timeDays).toFloat()
            fatChange = trendChange(samples, spanDays) { it.fatMassKg }
            leanChange = trendChange(samples, spanDays) { it.leanMassKg }
            weightChange = trendChange(samples, spanDays) { it.weightKg }
        } else {
            fatChange = last.fatMassKg - first.fatMassKg
            leanChange = last.leanMassKg - first.leanMassKg
            weightChange = last.weightKg - first.weightKg
        }

        val recomposition = fatChange < 0f && leanChange > 0f
        return RecompositionResult(
            state = if (recomposition) {
                CompositionState.Recomposition
            } else {
                classifyState(fatChange, leanChange)
            },
            recomposedKg = if (recomposition) min(abs(fatChange), abs(leanChange)) else null,
            fatChangeKg = fatChange,
            leanChangeKg = leanChange,
            weightChangeKg = weightChange,
            observedStart = first.date,
            observedEnd = last.date,
        )
    }

    private fun classifyState(fatChange: Float, leanChange: Float): CompositionState = when {
        fatChange <= 0f && leanChange <= 0f -> CompositionState.WeightLoss
        fatChange >= 0f && leanChange >= 0f -> CompositionState.WeightGain
        else -> CompositionState.CompositionShift
    }

    private inline fun trendChange(
        samples: List<CompositionSample>,
        spanDays: Float,
        selector: (CompositionSample) -> Float,
    ): Float {
        if (spanDays <= 0f) return selector(samples.last()) - selector(samples.first())
        val n = samples.size
        val meanX = samples.sumOf { it.timeDays } / n
        val meanY = samples.sumOf { selector(it).toDouble() } / n
        var numerator = 0.0
        var denominator = 0.0
        for (s in samples) {
            val dx = s.timeDays - meanX
            numerator += dx * (selector(s) - meanY)
            denominator += dx * dx
        }
        if (denominator == 0.0) return selector(samples.last()) - selector(samples.first())
        val slope = numerator / denominator
        return (slope * spanDays).toFloat()
    }
}

private fun ScanRecord.toCompositionSample(zoneId: ZoneId): CompositionSample? {
    val weight = estimatedWeightKg ?: weightKg ?: return null
    val fatMass = fatBodyMassKg
        ?: fatPercentage?.let { weight * it / 100f }
        ?: leanBodyMassKg?.let { weight - it }
        ?: return null
    val leanMass = leanBodyMassKg ?: (weight - fatMass)
    val instant = Instant.ofEpochMilli(timestamp)
    val date = instant.atZone(zoneId).toLocalDate()
    return CompositionSample(
        date = date,
        timeDays = timestamp / MILLIS_PER_DAY.toDouble(),
        weightKg = weight,
        fatMassKg = fatMass,
        leanMassKg = leanMass,
    )
}

private const val MILLIS_PER_DAY = 86_400_000L
