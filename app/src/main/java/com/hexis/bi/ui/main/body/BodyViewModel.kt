package com.hexis.bi.ui.main.body

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.BodyConstants
import com.hexis.bi.utils.constants.DateFormatConstants
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.kgToLb
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private const val BodyVisualScanOptionLimit = 5

class BodyViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(BodyState())
    val state: StateFlow<BodyState> = _state.asStateFlow()

    private var allScans: List<ScanRecord> = emptyList()

    init {
        loadData()
    }

    fun selectTab(tab: BodyTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun selectMassUnit(unit: BodyMassUnit) {
        _state.update { it.copy(massUnit = unit) }
        rebuildChart()
    }

    fun selectTimeRange(range: BodyTimeRange) {
        _state.update { it.copy(timeRange = range) }
        rebuildChart()
    }

    fun showBisInfo() = _state.update { it.copy(showBisInfo = true) }
    fun dismissBisInfo() = _state.update { it.copy(showBisInfo = false) }

    fun selectBodyPart(region: BodyMeasurementRegion) {
        _state.update { it.copy(visual = it.visual.copy(selectedBodyPart = region)) }
    }

    fun selectVisualScan(timestamp: Long) {
        updateVisualScan(selectedTimestamp = timestamp)
    }

    fun retry() = loadData()

    private fun loadData() {
        _state.update { it.copy(loadState = BodyLoadState.Loading) }
        viewModelScope.launch {
            val isMetric = userRepository.getUser().getOrNull()
                ?.unitSystem
                .isMetricUnitSystem()

            val scansResult = scanHistoryRepository.getRecentScans(
                limit = BodyConstants.TREND_HISTORY_LIMIT,
                projection = ScanFetchProjection.FULL,
            )
            val scans = scansResult.getOrNull()

            if (scans == null) {
                _state.update {
                    it.copy(loadState = BodyLoadState.Error, isMetric = isMetric)
                }
                return@launch
            }

            allScans = scans.sortedBy { it.timestamp }

            val latest = allScans.lastOrNull()
            val previous = allScans.dropLast(1).lastOrNull()

            val composition = if (latest == null) BodyComposition.empty()
            else buildComposition(latest = latest, previous = previous)

            _state.update {
                it.copy(
                    loadState = BodyLoadState.Ready,
                    isMetric = isMetric,
                    composition = composition,
                )
            }
            updateVisualScan(selectedTimestamp = _state.value.visual.latestScanTimestamp)
            rebuildChart()
        }
    }

    private fun updateVisualScan(selectedTimestamp: Long?) {
        val latest = allScans.lastOrNull()
        val selected = selectedTimestamp
            ?.let { timestamp -> allScans.lastOrNull { it.timestamp == timestamp } }
            ?: latest
        val selectedIndex = selected?.let { allScans.indexOf(it) } ?: -1
        val previous = if (selectedIndex > 0) allScans[selectedIndex - 1] else null
        val beforePrevious = if (selectedIndex > 1) allScans[selectedIndex - 2] else null
        val options = allScans
            .takeLast(BodyVisualScanOptionLimit)
            .asReversed()
            .map { VisualScanOption(timestamp = it.timestamp) }

        _state.update {
            it.copy(
                visual = it.visual.copy(
                    selectedBodyPart = it.visual.selectedBodyPart,
                    hasData = latest != null,
                    isLatestScanSelected = selected?.timestamp == latest?.timestamp,
                    scanOptions = options,
                    latestScanTimestamp = selected?.timestamp,
                    previousScanTimestamp = previous?.timestamp,
                    beforePreviousScanTimestamp = beforePrevious?.timestamp,
                    latestModel3dUrl = selected?.model3dUrl?.takeUnless { url -> url.isBlank() },
                    previousModel3dUrl = previous?.model3dUrl?.takeUnless { url -> url.isBlank() },
                    latestMeasurements = selected?.measurements.orEmpty(),
                    previousMeasurements = previous?.measurements.orEmpty(),
                    beforePreviousMeasurements = beforePrevious?.measurements.orEmpty(),
                ),
            )
        }
    }

    private fun rebuildChart() {
        val s = _state.value
        val now = System.currentTimeMillis()
        val chart = when (s.timeRange) {
            BodyTimeRange.Month -> buildMonthChart(allScans, now, s.massUnit, s.isMetric)
            BodyTimeRange.Year -> buildYearChart(allScans, now, s.massUnit, s.isMetric)
        }
        _state.update { it.copy(chart = chart) }
    }

    private fun buildMonthChart(
        scans: List<ScanRecord>,
        nowMillis: Long,
        massUnit: BodyMassUnit,
        isMetric: Boolean,
    ): BodyChartData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val previewStartDay = today.minusDays(BodyConstants.MONTH_RANGE_DAYS - 1L)
        val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val effectiveStartDay = scans.minOfOrNull { it.timestamp }
            ?.let { LocalDate.ofInstant(Date(it).toInstant(), zone) }
            ?: previewStartDay
        val effectiveStart = effectiveStartDay.atStartOfDay(zone).toInstant().toEpochMilli()

        val series = buildSeries(scans, zone, massUnit, isMetric)

        val totalDays = ChronoUnit.DAYS.between(effectiveStartDay, today).coerceAtLeast(0L)
        val labelFormatter = SimpleDateFormat(DateFormatConstants.DAY_MONTH_NUMERIC, Locale.getDefault())
        val labels = (0 until BodyConstants.MONTH_LABEL_COUNT).map { i ->
            val day = effectiveStartDay.plusDays(
                (totalDays * i) / (BodyConstants.MONTH_LABEL_COUNT - 1).coerceAtLeast(1)
            )
            val ts = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            BodyChartAxisLabel(timestamp = ts, text = labelFormatter.format(Date(ts)))
        }.distinctBy { it.text }

        return BodyChartData(
            rangeStartMillis = effectiveStart,
            rangeEndMillis = rangeEnd,
            points = series.points,
            axisLabels = labels,
            rangeLabel = formatRangeLabel(effectiveStart, rangeEnd),
            yAxisBound = series.yAxisBound,
            gridLines = series.gridLines,
        )
    }

    private fun buildYearChart(
        scans: List<ScanRecord>,
        nowMillis: Long,
        massUnit: BodyMassUnit,
        isMetric: Boolean,
    ): BodyChartData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val previewStartDay = YearMonth.from(today)
            .minusMonths(BodyConstants.YEAR_RANGE_MONTHS - 1L).atDay(1)
        val effectiveStartDay = scans.minOfOrNull { it.timestamp }
            ?.let { LocalDate.ofInstant(Date(it).toInstant(), zone) }
            ?: previewStartDay
        val effectiveStart = effectiveStartDay.atStartOfDay(zone).toInstant().toEpochMilli()

        val series = buildSeries(scans, zone, massUnit, isMetric)

        val firstMonth = YearMonth.from(effectiveStartDay)
        val thisMonth = YearMonth.from(today)
        val totalMonths = ChronoUnit.MONTHS.between(firstMonth, thisMonth).coerceAtLeast(0L)
        val monthFormatter = SimpleDateFormat(DateFormatConstants.MONTH_SHORT, Locale.getDefault())
        val labels = (0L..totalMonths).mapNotNull { i ->
            val ym = firstMonth.plusMonths(i)
            val firstOfMonth = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val ts = maxOf(effectiveStart, firstOfMonth)
            if (ts > rangeEnd) return@mapNotNull null
            val showLabel = totalMonths <= BodyConstants.YEAR_LABEL_ALL_BELOW_MONTHS ||
                i % BodyConstants.YEAR_LABEL_STEP.toLong() == 0L
            BodyChartAxisLabel(
                timestamp = ts,
                text = if (showLabel) monthFormatter.format(Date(ts)) else "",
            )
        }.distinctBy { it.timestamp }

        return BodyChartData(
            rangeStartMillis = effectiveStart,
            rangeEndMillis = rangeEnd,
            points = series.points,
            axisLabels = labels,
            rangeLabel = formatRangeLabel(effectiveStart, rangeEnd),
            yAxisBound = series.yAxisBound,
            gridLines = series.gridLines,
        )
    }

    private data class ChartSeries(
        val points: List<BodyTrendPoint>,
        val yAxisBound: Float,
        val gridLines: List<Float>,
    )

    private fun buildSeries(
        scans: List<ScanRecord>,
        zone: ZoneId,
        massUnit: BodyMassUnit,
        isMetric: Boolean,
    ): ChartSeries {
        val daily = bucketByDay(scans, zone, massUnit, isMetric)
        val points = applyBaselineDeltas(fillGapsLinearly(daily, zone))
        val maxAbs = points.maxOfOrNull { maxOf(abs(it.deltaFat), abs(it.deltaMuscle)) } ?: 0f
        val bound = BodyConstants.niceYHalfRange(maxAbs)
        return ChartSeries(points, bound, BodyConstants.gridLinesFor(bound))
    }

    private fun bucketByDay(
        scans: List<ScanRecord>,
        zone: ZoneId,
        massUnit: BodyMassUnit,
        isMetric: Boolean,
    ): List<BodyTrendPoint> {
        val byDay = scans.groupBy { LocalDate.ofInstant(Date(it.timestamp).toInstant(), zone) }
        return byDay.entries
            .sortedBy { it.key }
            .map { (day, dayScans) ->
                val tsMidday = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                averagePoint(tsMidday, dayScans, massUnit, isMetric)
            }
    }

    private fun averagePoint(
        timestamp: Long,
        scans: List<ScanRecord>,
        massUnit: BodyMassUnit,
        isMetric: Boolean,
    ): BodyTrendPoint = when (massUnit) {
        BodyMassUnit.Percent -> BodyTrendPoint(
            timestamp = timestamp,
            deltaFat = 0f,
            deltaMuscle = 0f,
            absoluteFat = scans.mapNotNull { it.fatPercentage }.averageOrZero(),
            absoluteMuscle = scans.mapNotNull { it.muscleMassPercentage() }.averageOrZero(),
        )
        BodyMassUnit.Mass -> {
            fun Float.toDisplayMass() = if (isMetric) this else this.kgToLb()
            BodyTrendPoint(
                timestamp = timestamp,
                deltaFat = 0f,
                deltaMuscle = 0f,
                absoluteFat = scans.mapNotNull { it.derivedFatKg() }.averageOrZero().toDisplayMass(),
                absoluteMuscle = scans.mapNotNull { it.leanBodyMassKg }.averageOrZero().toDisplayMass(),
            )
        }
    }

    private fun List<Float>.averageOrZero(): Float =
        if (isEmpty()) 0f else (sum() / size)

    private fun fillGapsLinearly(
        points: List<BodyTrendPoint>,
        zone: ZoneId,
    ): List<BodyTrendPoint> {
        if (points.size < 2) return points
        val out = mutableListOf<BodyTrendPoint>()
        for (i in points.indices) {
            out += points[i]
            val next = points.getOrNull(i + 1) ?: continue
            val a = LocalDate.ofInstant(Date(points[i].timestamp).toInstant(), zone)
            val b = LocalDate.ofInstant(Date(next.timestamp).toInstant(), zone)
            val gap = ChronoUnit.DAYS.between(a, b).toInt()
            if (gap <= 1) continue
            for (step in 1 until gap) {
                val day = a.plusDays(step.toLong())
                val ts = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                val t = step.toFloat() / gap.toFloat()
                out += BodyTrendPoint(
                    timestamp = ts,
                    deltaFat = 0f,
                    deltaMuscle = 0f,
                    absoluteFat = lerp(points[i].absoluteFat, next.absoluteFat, t),
                    absoluteMuscle = lerp(points[i].absoluteMuscle, next.absoluteMuscle, t),
                    isInterpolated = true,
                )
            }
        }
        return out
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun applyBaselineDeltas(points: List<BodyTrendPoint>): List<BodyTrendPoint> {
        val baseline = points.firstOrNull() ?: return points
        return points.map {
            it.copy(
                deltaFat = it.absoluteFat - baseline.absoluteFat,
                deltaMuscle = it.absoluteMuscle - baseline.absoluteMuscle,
            )
        }
    }

    private fun formatRangeLabel(startMillis: Long, endMillis: Long): String {
        val labelFormatter = SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH, Locale.getDefault())
        val rangeFormatter = SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH_YEAR, Locale.getDefault())
        return "${labelFormatter.format(Date(startMillis))} – ${rangeFormatter.format(Date(endMillis))}"
    }

    private fun buildComposition(latest: ScanRecord, previous: ScanRecord?): BodyComposition {
        val fatPct = latest.fatPercentage
        val musclePct = latest.muscleMassPercentage()
        val bis = latest.bisScore()

        val prevFatPct = previous?.fatPercentage
        val prevMusclePct = previous?.muscleMassPercentage()
        val prevBis = previous?.bisScore()

        return BodyComposition(
            timestamp = latest.timestamp,
            weightKg = latest.weightKg,
            bmi = latest.bmi,
            fatPercentage = fatPct,
            muscleMassPercentage = musclePct,
            fatMassKg = latest.fatBodyMassKg,
            muscleMassKg = latest.leanBodyMassKg,
            bisScore = bis,
            deltaWeightKg = delta(latest.weightKg, previous?.weightKg),
            deltaBmi = delta(latest.bmi, previous?.bmi),
            deltaFatPercentage = delta(fatPct, prevFatPct),
            deltaMuscleMassPercentage = delta(musclePct, prevMusclePct),
            deltaFatMassKg = delta(latest.fatBodyMassKg, previous?.fatBodyMassKg),
            deltaMuscleMassKg = delta(latest.leanBodyMassKg, previous?.leanBodyMassKg),
            deltaBisScore = delta(bis, prevBis),
        )
    }

    private fun delta(current: Float?, previous: Float?): Float? {
        if (current == null || previous == null) return null
        return current - previous
    }
}

private fun ScanRecord.muscleMassPercentage(): Float? {
    val lean = leanBodyMassKg ?: return null
    val weight = weightKg ?: return null
    if (weight <= 0f) return null
    return (lean / weight) * 100f
}

private fun ScanRecord.derivedFatKg(): Float? {
    fatBodyMassKg?.let { return it }
    val pct = fatPercentage ?: return null
    val weight = weightKg ?: return null
    return weight * pct / 100f
}

private fun ScanRecord.bisScore(): Float? {
    val musclePct = muscleMassPercentage() ?: return null
    val fatPct = fatPercentage ?: return null
    val bmi = bmi ?: return null
    val bmiPenalty = kotlin.math.abs(bmi - 22f) * 1.5f
    return (musclePct - fatPct + 25f - bmiPenalty).coerceIn(0f, 100f)
}
