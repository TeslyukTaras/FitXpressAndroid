package com.hexis.bi.ui.main.body

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.domain.body.comparablePhysiqueScoreDelta
import com.hexis.bi.domain.body.muscleMassPercentage
import com.hexis.bi.domain.body.physiqueScore
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.body.BodyTrendPhase.ConfirmedScan
import com.hexis.bi.ui.main.body.BodyTrendPhase.FutureEstimate
import com.hexis.bi.ui.main.body.BodyTrendPhase.PredictedDrift
import com.hexis.bi.utils.constants.BodyConstants
import com.hexis.bi.utils.constants.DateFormatConstants
import com.hexis.bi.utils.isMetricUnitSystem
import java.time.temporal.ChronoUnit
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class BodyViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
    private val threeDLookRepository: ThreeDLookRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(BodyState())
    val state: StateFlow<BodyState> = _state.asStateFlow()

    private var allScans: List<ScanRecord> = emptyList()
    private val loadingVisualColorPairs = mutableSetOf<Pair<String, String>>()
    private var requestedVisualColorPair: Pair<String, String>? = null

    init {
        observeVisualMode()
        loadData()
    }

    fun selectTab(tab: BodyTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun selectMassUnit(unit: BodyMassUnit) {
        _state.update { it.copy(massUnit = unit) }
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

    fun selectVisualMode(mode: BodyVisualMode) {
        applyVisualMode(mode)
        viewModelScope.launch {
            preferencesRepository.setBodyVisualMode(mode.name)
        }
    }

    fun selectVisualScan(timestamp: Long) {
        updateVisualScan(selectedTimestamp = timestamp)
    }

    fun retry() = loadData()

    private fun observeVisualMode() {
        viewModelScope.launch {
            preferencesRepository.bodyVisualMode.collect { storedMode ->
                val mode = BodyVisualMode.entries.firstOrNull { it.name == storedMode }
                    ?: BodyVisualMode.Base
                applyVisualMode(mode)
            }
        }
    }

    private fun applyVisualMode(mode: BodyVisualMode) {
        if (_state.value.visual.mode != mode) {
            _state.update { it.copy(visual = it.visual.copy(mode = mode)) }
        }
        if (mode == BodyVisualMode.Color &&
            _state.value.visual.colorModel !is BodyVisualColorModel.Ready
        ) {
            loadVisualColorMesh()
        }
    }

    private fun loadData() {
        _state.update { it.copy(loadState = BodyLoadState.Loading) }
        viewModelScope.launch {
            val profile = userRepository.getUser().getOrNull()
            val isMetric = profile
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
            else buildComposition(
                latest = latest,
                previous = previous,
                heightCm = profile?.heightCm?.toFloat(),
            )

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
            .takeLast(BodyConstants.VISUAL_SCAN_OPTION_LIMIT)
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
                    colorModel = BodyVisualColorModel.Idle,
                    previousModel3dUrl = previous?.model3dUrl?.takeUnless { url -> url.isBlank() },
                    latestMeasurements = selected?.measurements.orEmpty(),
                    previousMeasurements = previous?.measurements.orEmpty(),
                    beforePreviousMeasurements = beforePrevious?.measurements.orEmpty(),
                ),
            )
        }
        if (_state.value.visual.mode == BodyVisualMode.Color) loadVisualColorMesh()
    }

    private fun loadVisualColorMesh() {
        val selectedTimestamp = _state.value.visual.latestScanTimestamp
        val selectedIndex = allScans.indexOfLast { it.timestamp == selectedTimestamp }
        val selected = allScans.getOrNull(selectedIndex)
        val previous = allScans.getOrNull(selectedIndex - 1)
        val beforeId = previous?.measurementId?.takeUnless { it.isBlank() }
        val afterId = selected?.measurementId?.takeUnless { it.isBlank() }
        val pair = if (beforeId == null || afterId == null) null else beforeId to afterId

        requestedVisualColorPair = pair
        if (pair == null) {
            _state.update {
                it.copy(
                    visual = it.visual.copy(
                        colorModel = BodyVisualColorModel.Unavailable,
                    )
                )
            }
            return
        }

        _state.update {
            it.copy(visual = it.visual.copy(colorModel = BodyVisualColorModel.Loading))
        }
        if (!loadingVisualColorPairs.add(pair)) return

        viewModelScope.launch {
            threeDLookRepository.loadColorAnalysisMeshUrl(
                beforeMeasurementId = pair.first,
                afterMeasurementId = pair.second,
            ).onSuccess { meshUrl ->
                if (requestedVisualColorPair == pair) {
                    _state.update {
                        it.copy(
                            visual = it.visual.copy(
                                colorModel = BodyVisualColorModel.Ready(meshUrl),
                            )
                        )
                    }
                }
            }.onFailure {
                if (requestedVisualColorPair == pair) {
                    _state.update {
                        it.copy(visual = it.visual.copy(colorModel = BodyVisualColorModel.Error))
                    }
                }
            }
            loadingVisualColorPairs.remove(pair)
        }
    }

    private fun rebuildChart() {
        val s = _state.value
        val now = System.currentTimeMillis()
        val chart = when (s.timeRange) {
            BodyTimeRange.Week -> buildWeekChart(allScans, now)
            BodyTimeRange.Month -> buildMonthChart(allScans, now)
            BodyTimeRange.Year -> buildYearChart(allScans, now)
        }
        _state.update { it.copy(chart = chart) }
    }

    private fun buildWeekChart(
        scans: List<ScanRecord>,
        nowMillis: Long,
    ): BodyChartData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(Date(nowMillis).toInstant(), zone)
        val startDay = today.minusDays(BodyConstants.WEEK_HALF_DAYS)
        val endDay = today.plusDays(BodyConstants.WEEK_HALF_DAYS)
        val rangeStart = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = endDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val series = buildSeries(
            scans = scans,
            zone = zone,
            rangeStartMillis = rangeStart,
            rangeEndMillis = rangeEnd,
            anchorPreviousAtStart = true,
        )

        val dayFormatter = SimpleDateFormat(DateFormatConstants.DAY_NAME_SHORT, Locale.getDefault())
        val totalDays = ChronoUnit.DAYS.between(startDay, endDay).toInt()
        val labels = centeredTickTimestamps(rangeStart, rangeEnd, totalDays + 1).map { ts ->
            BodyChartAxisLabel(
                timestamp = ts,
                text = dayFormatter.format(Date(ts)).take(2)
                    .lowercase(Locale.getDefault())
                    .replaceFirstChar { it.titlecase(Locale.getDefault()) },
            )
        }

        return BodyChartData(
            rangeStartMillis = rangeStart,
            rangeEndMillis = rangeEnd,
            points = series.points,
            axisLabels = labels,
            rangeLabel = formatRangeLabel(rangeStart, rangeEnd),
            yAxisBound = series.yAxisBound,
            gridLines = series.gridLines,
        )
    }

    private fun buildMonthChart(
        scans: List<ScanRecord>,
        nowMillis: Long,
    ): BodyChartData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(Date(nowMillis).toInstant(), zone)
        val startDay = today.minusDays(BodyConstants.MONTH_HALF_DAYS)
        val endDay = today.plusDays(BodyConstants.MONTH_HALF_DAYS)
        val rangeStart = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = endDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val series = buildSeries(scans, zone, rangeStart, rangeEnd)

        val labelFormatter = SimpleDateFormat(DateFormatConstants.DAY_MONTH_NUMERIC, Locale.getDefault())
        val labels = centeredTickTimestamps(
            rangeStart,
            rangeEnd,
            BodyConstants.MONTH_LABEL_COUNT,
        ).map { ts ->
            BodyChartAxisLabel(timestamp = ts, text = labelFormatter.format(Date(ts)))
        }.distinctBy { it.text }

        return BodyChartData(
            rangeStartMillis = rangeStart,
            rangeEndMillis = rangeEnd,
            points = series.points,
            axisLabels = labels,
            rangeLabel = formatRangeLabel(rangeStart, rangeEnd),
            yAxisBound = series.yAxisBound,
            gridLines = series.gridLines,
        )
    }

    private fun buildYearChart(
        scans: List<ScanRecord>,
        nowMillis: Long,
    ): BodyChartData {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.ofInstant(Date(nowMillis).toInstant(), zone)
        val startDay = today.minusMonths(BodyConstants.YEAR_HALF_MONTHS)
        val endDay = today.plusMonths(BodyConstants.YEAR_HALF_MONTHS)
        val rangeStart = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEnd = endDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val series = buildSeries(scans, zone, rangeStart, rangeEnd)

        val totalMonths = ChronoUnit.MONTHS.between(
            YearMonth.from(startDay),
            YearMonth.from(endDay),
        ).toInt().coerceAtLeast(1)
        val monthFormatter = SimpleDateFormat(DateFormatConstants.MONTH_SHORT, Locale.getDefault())
        val labels = centeredTickTimestamps(rangeStart, rangeEnd, totalMonths).mapIndexed { i, ts ->
            val showLabel = totalMonths <= BodyConstants.YEAR_LABEL_ALL_BELOW_MONTHS ||
                i % BodyConstants.YEAR_LABEL_STEP == 0
            BodyChartAxisLabel(
                timestamp = ts,
                text = if (showLabel) monthFormatter.format(Date(ts)) else "",
            )
        }

        return BodyChartData(
            rangeStartMillis = rangeStart,
            rangeEndMillis = rangeEnd,
            points = series.points,
            axisLabels = labels,
            rangeLabel = formatRangeLabel(rangeStart, rangeEnd),
            yAxisBound = series.yAxisBound,
            gridLines = series.gridLines,
        )
    }

    private data class ChartSeries(
        val points: List<BodyTrendPoint>,
        val yAxisBound: Float,
        val gridLines: List<Float>,
    )

    private fun centeredTickTimestamps(
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        count: Int,
    ): List<Long> {
        val safeCount = count.coerceAtLeast(1)
        val span = (rangeEndMillis - rangeStartMillis + 1L).coerceAtLeast(1L)
        return List(safeCount) { index ->
            rangeStartMillis + span * (2L * index + 1L) / (2L * safeCount)
        }
    }

    private fun buildSeries(
        scans: List<ScanRecord>,
        zone: ZoneId,
        rangeStartMillis: Long,
        rangeEndMillis: Long,
        anchorPreviousAtStart: Boolean = false,
    ): ChartSeries {
        val confirmed = bucketByDay(scans, zone)
        val rangeStartDay = LocalDate.ofInstant(Date(rangeStartMillis).toInstant(), zone)
        val anchorTimestamp = rangeStartDay.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val priorToRange = if (anchorPreviousAtStart) {
            confirmed.lastOrNull { it.timestamp <= anchorTimestamp }
        } else {
            null
        }
        val anchor = priorToRange?.copy(timestamp = anchorTimestamp, isInterpolated = true)
        val hasConfirmedInRange = confirmed.any { it.timestamp in rangeStartMillis..rangeEndMillis }
        val anchoredProjection = anchor != null && !hasConfirmedInRange
        val projectionPoints = if (anchoredProjection) listOfNotNull(anchor) else confirmed
        val sourceSlope = if (anchoredProjection) {
            val priorIndex = confirmed.indexOf(priorToRange)
            confirmed.getOrNull(priorIndex - 1)?.let { it to priorToRange }
        } else {
            null
        }
        val withTail = fillPredictedTail(projectionPoints, zone, rangeEndMillis, sourceSlope)
        val visible = withTail.filter { it.timestamp in rangeStartMillis..rangeEndMillis }
        val displayed = if (anchorPreviousAtStart) {
            anchorPreviousScanAtStart(confirmed, visible, rangeStartMillis, zone)
        } else {
            visible
        }
        val points = applyBaselineDeltas(displayed)
        val (yAxisBound, gridLines) = dynamicBounds(points)
        return ChartSeries(points, yAxisBound, gridLines)
    }

    private fun anchorPreviousScanAtStart(
        confirmed: List<BodyTrendPoint>,
        visible: List<BodyTrendPoint>,
        rangeStartMillis: Long,
        zone: ZoneId,
    ): List<BodyTrendPoint> {
        val startDay = LocalDate.ofInstant(Date(rangeStartMillis).toInstant(), zone)
        val anchorTimestamp = startDay.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        if (visible.any { it.timestamp == anchorTimestamp && it.phase == ConfirmedScan }) return visible
        val previous = confirmed.lastOrNull { it.timestamp <= anchorTimestamp } ?: return visible
        val anchor = previous.copy(timestamp = anchorTimestamp, isInterpolated = true)
        return (listOf(anchor) + visible.filter { it.timestamp > anchorTimestamp }).sortedBy { it.timestamp }
    }

    private fun dynamicBounds(points: List<BodyTrendPoint>): Pair<Float, List<Float>> {
        val maxAbs = points.maxOfOrNull { max(abs(it.deltaFat), abs(it.deltaMuscle)) } ?: 0f
        val halfRange = BodyConstants.niceYHalfRange(maxAbs)
        return halfRange to BodyConstants.gridLinesFor(halfRange)
    }

    private fun bucketByDay(scans: List<ScanRecord>, zone: ZoneId): List<BodyTrendPoint> {
        val byDay = scans.groupBy { LocalDate.ofInstant(Date(it.timestamp).toInstant(), zone) }
        return byDay.entries.sortedBy { it.key }.mapNotNull { (day, dayScans) ->
            val tsMidday = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            averagePoint(tsMidday, dayScans)
        }
    }

    private fun averagePoint(timestamp: Long, scans: List<ScanRecord>): BodyTrendPoint? {
        val muscle = scans.mapNotNull { it.muscleMassPercentage() }.averageOrNull()
        val fat = scans.mapNotNull { it.fatPercentage }.averageOrNull()
        if (muscle == null || fat == null) return null
        return BodyTrendPoint(timestamp, 0f, 0f, fat, muscle, phase = ConfirmedScan)
    }

    private fun List<Float>.averageOrNull(): Float? = if (isEmpty()) null else sum() / size

    private fun fillPredictedTail(
        points: List<BodyTrendPoint>,
        zone: ZoneId,
        rangeEndMillis: Long,
        sourceSlope: Pair<BodyTrendPoint, BodyTrendPoint>? = null,
    ): List<BodyTrendPoint> {
        val last = points.lastOrNull() ?: return points
        val lastDay = LocalDate.ofInstant(Date(last.timestamp).toInstant(), zone)
        val endDay = LocalDate.ofInstant(Date(rangeEndMillis).toInstant(), zone)
        val daysAhead = ChronoUnit.DAYS.between(lastDay, endDay).toInt()
        if (daysAhead <= 0) return points
        val predictedDriftDays = ceil(daysAhead * BodyConstants.PREDICTED_DRIFT_FRACTION)
            .toInt()
            .coerceAtLeast(1)

        val recent = sourceSlope ?: points.getOrNull(points.lastIndex - 1)?.let { it to last }
        val (muscleStep, fatStep) = recent?.let { (previous, latest) ->
            val previousDay = LocalDate.ofInstant(Date(previous.timestamp).toInstant(), zone)
            val latestDay = LocalDate.ofInstant(Date(latest.timestamp).toInstant(), zone)
            val span = ChronoUnit.DAYS.between(previousDay, latestDay).coerceAtLeast(1).toFloat()
            ((latest.absoluteMuscle - previous.absoluteMuscle) / span) to
                ((latest.absoluteFat - previous.absoluteFat) / span)
        } ?: (0f to 0f)

        val muscleDrift = muscleStep * daysAhead
        val fatDrift = fatStep * daysAhead
        val out = points.toMutableList()
        for (step in 1..daysAhead) {
            val day = lastDay.plusDays(step.toLong())
            val ts = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            val ease = easeOut(step.toFloat() / daysAhead)
            out += BodyTrendPoint(
                timestamp = ts,
                deltaFat = 0f,
                deltaMuscle = 0f,
                absoluteFat = (last.absoluteFat + fatDrift * ease).coerceIn(0f, 100f),
                absoluteMuscle = (last.absoluteMuscle + muscleDrift * ease).coerceIn(0f, 100f),
                isInterpolated = true,
                phase = if (step <= predictedDriftDays) PredictedDrift else FutureEstimate,
            )
        }
        return out
    }

    private fun easeOut(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return 1f - (1f - x) * (1f - x)
    }

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
        val startFormatter = SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH, Locale.getDefault())
        val endFormatter = SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH_YEAR, Locale.getDefault())
        return "${startFormatter.format(Date(startMillis))} – ${endFormatter.format(Date(endMillis))}"
    }

    private fun buildComposition(latest: ScanRecord, previous: ScanRecord?, heightCm: Float?): BodyComposition {
        val fatPct = latest.fatPercentage
        val musclePct = latest.muscleMassPercentage()
        val bis = latest.physiqueScore(heightCm)

        val prevFatPct = previous?.fatPercentage
        val prevMusclePct = previous?.muscleMassPercentage()

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
            deltaBisScore = comparablePhysiqueScoreDelta(latest, previous, heightCm),
        )
    }

    private fun delta(current: Float?, previous: Float?): Float? {
        if (current == null || previous == null) return null
        return current - previous
    }
}
