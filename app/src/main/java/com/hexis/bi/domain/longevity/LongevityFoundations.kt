package com.hexis.bi.domain.longevity

import com.hexis.bi.data.activity.ActivitySummary
import com.hexis.bi.data.recovery.RecoverySnapshot
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.recomposition.CompositionState
import com.hexis.bi.domain.recomposition.RecompositionCalculator
import com.hexis.bi.domain.recomposition.RecompositionResult
import com.hexis.bi.domain.trend.TrendPoint
import com.hexis.bi.domain.trend.linearTrendChange
import com.hexis.bi.domain.trend.trendPersistence
import com.hexis.bi.domain.trend.winsorized
import com.hexis.bi.utils.constants.LongevityFoundationConstants
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class FoundationStatus {
    Strengthening,
    Holding,
    Mixed,
    Weakening,
    InsufficientData,
}

enum class LongevityDirection {
    Strengthening,
    Holding,
    Mixed,
    Weakening,
    BuildingYourTrend,
}

data class MetabolicFoundation(
    val status: FoundationStatus,
    val waistToHeightStart: Float? = null,
    val waistToHeightEnd: Float? = null,
)

data class RecompositionFoundation(
    val status: FoundationStatus,
    val fatChangeKg: Float? = null,
    val leanChangeKg: Float? = null,
)

data class PhysicalFoundation(
    val status: FoundationStatus,
    val thighChangeCm: Float? = null,
    val calfChangeCm: Float? = null,
)

data class FunctionalFoundation(
    val status: FoundationStatus,
    val vo2Start: Float? = null,
    val vo2End: Float? = null,
    val restingHeartRateStart: Int? = null,
    val restingHeartRateEnd: Int? = null,
)

data class LongevityResult(
    val direction: LongevityDirection,
    val metabolic: MetabolicFoundation,
    val recomposition: RecompositionFoundation,
    val physical: PhysicalFoundation,
    val functional: FunctionalFoundation,
)

object LongevityCalculator {
    fun evaluateBody(
        scans: List<ScanRecord>,
        heightCm: Float?,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LongevityResult {
        val inWindow = scans
            .filter { it.dateIn(windowStart, windowEnd, zoneId) }
            .sortedBy { it.timestamp }

        val metabolic = evaluateMetabolic(inWindow, heightCm)
        val recomposition = evaluateRecomposition(
            RecompositionCalculator.buildWindow(inWindow, windowStart, windowEnd, zoneId)
        )
        val physical = evaluatePhysical(inWindow)
        val functional = FunctionalFoundation(FoundationStatus.InsufficientData)

        return LongevityResult(
            direction = combineDirection(metabolic, recomposition, physical, functional),
            metabolic = metabolic,
            recomposition = recomposition,
            physical = physical,
            functional = functional,
        )
    }

    fun LongevityResult.withFunctional(functional: FunctionalFoundation): LongevityResult = copy(
        direction = combineDirection(metabolic, recomposition, physical, functional),
        functional = functional,
    )


    // ---- Foundation 1: Metabolic Health -------------------------------------------------------

    private fun evaluateMetabolic(
        scans: List<ScanRecord>,
        heightCm: Float?,
    ): MetabolicFoundation {
        val height = heightCm?.takeIf { it > 0f }
        val ratioMove = height
            ?.let { scans.trendPoints { scan -> waistOf(scan)?.div(it) } }
            ?.movement(LongevityFoundationConstants.WAIST_TO_HEIGHT_MEANINGFUL_DELTA)
            ?: return MetabolicFoundation(FoundationStatus.InsufficientData)

        val fatMove = scans.trendPoints { it.fatPercentage }
            .movement(LongevityFoundationConstants.BODY_FAT_MEANINGFUL_DELTA_PERCENT)

        val signals = buildList {
            add(-ratioMove.direction)
            fatMove?.let { add(-it.direction) }
        }

        return MetabolicFoundation(
            status = combineSignals(*signals.toIntArray()),
            waistToHeightStart = ratioMove.start,
            waistToHeightEnd = ratioMove.end,
        )
    }

    // ---- Foundation 2: Body Recomposition ------------------------------------------------------

    private fun evaluateRecomposition(result: RecompositionResult): RecompositionFoundation {
        val fat = result.fatChangeKg
        val lean = result.leanChangeKg
        if (fat == null || lean == null) return RecompositionFoundation(FoundationStatus.InsufficientData)

        val threshold = LongevityFoundationConstants.MASS_MEANINGFUL_DELTA_KG
        val fatDown = fat <= -threshold
        val fatUp = fat >= threshold
        val leanDown = lean <= -threshold
        val leanUp = lean >= threshold
        val majorLeanLoss = lean <= -LongevityFoundationConstants.MAJOR_LEAN_LOSS_KG

        val status = when {
            result.state == CompositionState.Recomposition -> FoundationStatus.Strengthening
            fatDown && leanUp -> FoundationStatus.Strengthening
            fatUp && leanDown -> FoundationStatus.Weakening
            fatDown && leanDown -> if (majorLeanLoss) FoundationStatus.Weakening else FoundationStatus.Mixed
            fatUp && leanUp -> FoundationStatus.Mixed
            fatDown || leanUp -> FoundationStatus.Strengthening
            fatUp || majorLeanLoss -> FoundationStatus.Weakening
            leanDown -> FoundationStatus.Mixed
            else -> FoundationStatus.Holding
        }

        return RecompositionFoundation(
            status = status,
            fatChangeKg = fat,
            leanChangeKg = lean,
        )
    }

    // ---- Foundation 3: Physical Foundation -----------------------------------------------------

    private fun evaluatePhysical(scans: List<ScanRecord>): PhysicalFoundation {
        val threshold = LongevityFoundationConstants.LOWER_BODY_MEANINGFUL_DELTA_CM
        val thighMove = scans.trendPoints { measurementOf(it, BodyMeasurementKeys.Thigh) }
            .movement(threshold)
        val calfMove = scans.trendPoints { measurementOf(it, BodyMeasurementKeys.Calf) }
            .movement(threshold)
        if (thighMove == null && calfMove == null) {
            return PhysicalFoundation(FoundationStatus.InsufficientData)
        }

        val signals = listOfNotNull(thighMove?.direction, calfMove?.direction)

        return PhysicalFoundation(
            status = combineSignals(*signals.toIntArray()),
            thighChangeCm = thighMove?.change,
            calfChangeCm = calfMove?.change,
        )
    }

    // ---- Foundation 4: Functional Capacity -----------------------------------------------------

    fun evaluateFunctional(
        allActivity: List<ActivitySummary>,
        allRecovery: List<RecoverySnapshot>,
        allSleep: List<SleepSession>,
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): FunctionalFoundation {
        val activity = allActivity.filter { it.date in windowStart..windowEnd }
        val recovery = allRecovery.filter { it.date in windowStart..windowEnd }
        val sleep = allSleep.filter { it.wakeTime.toLocalDate() in windowStart..windowEnd }

        val observedDays = (activity.map { it.date } + recovery.map { it.date }).distinct().size
        if (observedDays < LongevityFoundationConstants.MIN_WEARABLE_DAYS) {
            return FunctionalFoundation(FoundationStatus.InsufficientData)
        }
        val activeDays = activity.count { it.steps >= LongevityFoundationConstants.ACTIVE_DAY_MIN_STEPS }

        val vo2Points = activity
            .mapNotNull { summary ->
                summary.vo2MaxMlPerMinPerKg?.takeIf { it > 0f }
                    ?.let { TrendPoint(summary.date.toEpochDay().toDouble(), it) }
            }
        val rhrPoints = recovery
            .mapNotNull { snapshot ->
                snapshot.restingHeartRateBpm.takeIf { it > 0 }
                    ?.let { TrendPoint(snapshot.date.toEpochDay().toDouble(), it.toFloat()) }
            }

        val vo2Move = vo2Points.movement(LongevityFoundationConstants.VO2_MEANINGFUL_DELTA)
        val rhrMove = rhrPoints.movement(LongevityFoundationConstants.RHR_MEANINGFUL_DELTA_BPM)

        val signals = buildList {
            vo2Move?.let { add(it.direction) }
            rhrMove?.let { add(-it.direction) }
            add(activityConsistency(activeDays, observedDays))
            sleepConsistency(sleep)?.let { add(it) }
        }

        return FunctionalFoundation(
            status = combineSignals(*signals.toIntArray()),
            vo2Start = vo2Move?.start,
            vo2End = vo2Move?.end,
            restingHeartRateStart = rhrMove?.start?.roundToInt(),
            restingHeartRateEnd = rhrMove?.end?.roundToInt(),
        )
    }

    private fun activityConsistency(activeDays: Int, observedDays: Int): Int {
        if (observedDays <= 0) return 0
        val fraction = activeDays.toFloat() / observedDays
        return if (fraction >= LongevityFoundationConstants.CONSISTENT_ACTIVITY_DAY_FRACTION) 1 else 0
    }

    private fun sleepConsistency(sleep: List<SleepSession>): Int? {
        val durations = sleep.filter { !it.isNap && it.durationMinutes > 0 }.map { it.durationMinutes }
        if (durations.size < LongevityFoundationConstants.MIN_WEARABLE_DAYS) return null
        val mean = durations.average()
        val standardDeviation = sqrt(durations.sumOf { (it - mean) * (it - mean) } / durations.size)
        return if (standardDeviation <= LongevityFoundationConstants.REGULAR_SLEEP_STD_DEV_MINUTES) 1 else 0
    }

    // ---- Overall direction ---------------------------------------------------------------------
    private fun combineDirection(
        metabolic: MetabolicFoundation,
        recomposition: RecompositionFoundation,
        physical: PhysicalFoundation,
        functional: FunctionalFoundation,
    ): LongevityDirection {
        val weighted = listOf(
            metabolic.status to LongevityFoundationConstants.WEIGHT_METABOLIC,
            recomposition.status to LongevityFoundationConstants.WEIGHT_RECOMPOSITION,
            physical.status to LongevityFoundationConstants.WEIGHT_PHYSICAL,
            functional.status to LongevityFoundationConstants.WEIGHT_FUNCTIONAL,
        )

        if (metabolic.status == FoundationStatus.InsufficientData &&
            recomposition.status == FoundationStatus.InsufficientData
        ) {
            return LongevityDirection.BuildingYourTrend
        }

        fun weightOf(status: FoundationStatus): Float =
            weighted.filter { it.first == status }.sumOf { it.second.toDouble() }.toFloat()

        val strengthening = weightOf(FoundationStatus.Strengthening)
        val weakening = weightOf(FoundationStatus.Weakening)
        val mixed = weightOf(FoundationStatus.Mixed)

        val fatUpLeanDown = recomposition.status == FoundationStatus.Weakening &&
            metabolic.status != FoundationStatus.Strengthening

        return when {
            weakening >= LongevityFoundationConstants.WEAKENING_WEIGHT_MIN || fatUpLeanDown ->
                LongevityDirection.Weakening

            strengthening > 0f && weakening > 0f -> LongevityDirection.Mixed

            strengthening >= LongevityFoundationConstants.STRENGTHENING_WEIGHT_MIN ->
                LongevityDirection.Strengthening

            mixed >= LongevityFoundationConstants.WEAKENING_WEIGHT_MIN -> LongevityDirection.Mixed

            else -> LongevityDirection.Holding
        }
    }

    // ---- Shared helpers ------------------------------------------------------------------------
    private data class Movement(
        val change: Float,
        val direction: Int,
        val start: Float,
        val end: Float,
    )

    private fun List<TrendPoint>.movement(meaningfulDelta: Float): Movement? {
        if (size < LongevityFoundationConstants.MIN_SCANS_FOR_DIRECTION) return null
        val robust = winsorized(this)
        val change = linearTrendChange(robust)
        val start = robust.first().value
        val end = robust.last().value

        val magnitude = abs(change)
        val direction = when {
            magnitude < meaningfulDelta -> 0
            magnitude >= meaningfulDelta * LongevityFoundationConstants.DECISIVE_CHANGE_MULTIPLE -> 1
            trendPersistence(robust, change) < LongevityFoundationConstants.MIN_PERSISTENCE -> 0
            else -> 1
        }
        return Movement(
            change = change,
            direction = if (direction == 0) 0 else if (change > 0f) 1 else -1,
            start = start,
            end = end,
        )
    }

    private fun combineSignals(vararg directions: Int): FoundationStatus {
        if (directions.isEmpty()) return FoundationStatus.InsufficientData
        val positive = directions.any { it > 0 }
        val negative = directions.any { it < 0 }
        return when {
            positive && negative -> FoundationStatus.Mixed
            positive -> FoundationStatus.Strengthening
            negative -> FoundationStatus.Weakening
            else -> FoundationStatus.Holding
        }
    }

    private fun List<ScanRecord>.trendPoints(selector: (ScanRecord) -> Float?): List<TrendPoint> =
        mapNotNull { scan ->
            selector(scan)?.takeIf { it > 0f }
                ?.let { TrendPoint(scan.timestamp / MILLIS_PER_DAY.toDouble(), it) }
        }.sortedBy { it.timeDays }

    private fun waistOf(scan: ScanRecord): Float? = measurementOf(scan, BodyMeasurementKeys.Waist)

    private fun measurementOf(scan: ScanRecord, key: String): Float? =
        scan.measurements[key]?.takeIf { it > 0f }

    private fun ScanRecord.dateIn(start: LocalDate, end: LocalDate, zoneId: ZoneId): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
        return !date.isBefore(start) && !date.isAfter(end)
    }

    private const val MILLIS_PER_DAY = 86_400_000L
}
