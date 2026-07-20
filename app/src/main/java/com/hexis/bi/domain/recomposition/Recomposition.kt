package com.hexis.bi.domain.recomposition

import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.domain.trend.TrendPoint
import com.hexis.bi.domain.trend.linearTrendChange
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
            fatChange = linearTrendChange(samples.map { TrendPoint(it.timeDays, it.fatMassKg) })
            leanChange = linearTrendChange(samples.map { TrendPoint(it.timeDays, it.leanMassKg) })
            weightChange = linearTrendChange(samples.map { TrendPoint(it.timeDays, it.weightKg) })
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
