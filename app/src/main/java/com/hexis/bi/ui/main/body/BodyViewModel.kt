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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

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
    private var heightCm: Float? = null
    private var gender: String? = null
    private var visibleRegions: Set<BodyMeasurementRegion> =
        BodyMeasurementRegion.measurableRegions.toSet()
    private val loadingVisualColorPairs = mutableSetOf<Pair<String, String>>()
    private val loadingCompareColorPairs = mutableSetOf<Pair<String, String>>()
    private var requestedVisualColorPair: Pair<String, String>? = null
    private var requestedLeftColorPair: Pair<String, String>? = null
    private var requestedRightColorPair: Pair<String, String>? = null

    init {
        observeColorMode()
        loadData()
    }

    fun selectTab(tab: BodyTab) {
        _state.update { it.copy(selectedTab = tab) }

        if (_state.value.visual.mode != BodyVisualMode.Color) return
        when (tab) {
            BodyTab.Visual -> {
                if (_state.value.visual.colorModel !is BodyVisualColorModel.Ready) {
                    loadVisualColorMesh()
                }
            }

            BodyTab.Compare -> loadCompareColorMeshesIfNeeded()
            else -> Unit
        }
    }

    fun selectMassUnit(unit: BodyMassUnit) {
        _state.update { it.copy(massUnit = unit) }
    }

    fun selectTimeRange(range: BodyTimeRange) {
        _state.update {
            it.copy(
                timeRange = range,
                periodPhysiqueDrift = computePeriodDrift(range)
            )
        }
        rebuildChart()
    }

    fun showBisInfo() = _state.update { it.copy(showBisInfo = true) }
    fun dismissBisInfo() = _state.update { it.copy(showBisInfo = false) }

    fun showBodyProportionInfo() = _state.update { it.copy(showBodyProportionInfo = true) }
    fun dismissBodyProportionInfo() = _state.update { it.copy(showBodyProportionInfo = false) }

    fun selectBodyPart(region: BodyMeasurementRegion) {
        _state.update { it.copy(visual = it.visual.copy(selectedBodyPart = region)) }
    }

    fun selectCompareBodyPart(region: BodyMeasurementRegion) {
        _state.update { it.copy(compare = it.compare.copy(selectedBodyPart = region)) }
    }


    fun selectMode(mode: BodyVisualMode) {
        applyColorMode(mode)
        viewModelScope.launch {
            preferencesRepository.setBodyVisualMode(mode.name)
        }
    }

    fun selectVisualScan(timestamp: Long) {
        updateVisualScan(selectedTimestamp = timestamp)
    }

    fun selectCompareLeftScan(timestamp: Long) {
        updateCompareScans(leftTimestamp = timestamp)
    }

    fun selectCompareRightScan(timestamp: Long) {
        updateCompareScans(rightTimestamp = timestamp)
    }

    fun retry() = loadData()

    private fun observeColorMode() {
        viewModelScope.launch {
            preferencesRepository.bodyVisualMode.collect { storedMode ->
                val mode = BodyVisualMode.entries.firstOrNull { it.name == storedMode }
                    ?: BodyVisualMode.Base
                applyColorMode(mode)
            }
        }
    }

    private fun applyColorMode(mode: BodyVisualMode) {
        _state.update {
            it.copy(
                visual = it.visual.copy(mode = mode),
                compare = it.compare.copy(mode = mode),
            )
        }
        if (mode != BodyVisualMode.Color) return
        when (_state.value.selectedTab) {
            BodyTab.Visual -> {
                if (_state.value.visual.colorModel !is BodyVisualColorModel.Ready) {
                    loadVisualColorMesh()
                }
            }

            BodyTab.Compare -> loadCompareColorMeshesIfNeeded()
            else -> Unit
        }
    }

    private fun loadData() {
        _state.update { it.copy(loadState = BodyLoadState.Loading) }
        viewModelScope.launch {
            val profile = userRepository.getUser().getOrNull()
            val isMetric = profile
                ?.unitSystem
                .isMetricUnitSystem()
            visibleRegions = BodyMeasurementRegion.visibleRegionsOrDefault(
                userRepository.getUserSettings().getOrNull()?.measurementZones,
            )

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
            heightCm = profile?.heightCm?.toFloat()
            gender = profile?.gender

            val latest = allScans.lastOrNull()
            val previous = allScans.dropLast(1).lastOrNull()

            val composition = if (latest == null) BodyComposition.empty()
            else buildComposition(
                latest = latest,
                previous = previous,
                heightCm = heightCm,
            )

            _state.update {
                it.copy(
                    loadState = BodyLoadState.Ready,
                    isMetric = isMetric,
                    composition = composition,
                    bodyProportion = buildBodyProportion(latest, heightCm, gender),
                    periodPhysiqueDrift = computePeriodDrift(it.timeRange),
                )
            }
            updateVisualScan(selectedTimestamp = _state.value.visual.latestScanTimestamp)
            updateCompareScans(
                leftTimestamp = _state.value.compare.leftScanTimestamp ?: latest?.timestamp,
                rightTimestamp = _state.value.compare.rightScanTimestamp ?: previous?.timestamp,
            )
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
                    visibleRegions = visibleRegions,
                ),
            )
        }
        if (_state.value.visual.mode == BodyVisualMode.Color &&
            _state.value.selectedTab == BodyTab.Visual
        ) {
            loadVisualColorMesh()
        }
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

    private fun updateCompareScans(
        leftTimestamp: Long? = _state.value.compare.leftScanTimestamp,
        rightTimestamp: Long? = _state.value.compare.rightScanTimestamp,
    ) {
        val latest = allScans.lastOrNull()
        val previous = allScans.dropLast(1).lastOrNull()
        val (left, leftPrevious) = scanAndPrevious(leftTimestamp ?: latest?.timestamp)
        val (right, rightPrevious) = scanAndPrevious(rightTimestamp ?: previous?.timestamp)
        val options = allScans
            .takeLast(BodyConstants.VISUAL_SCAN_OPTION_LIMIT)
            .asReversed()
            .map { VisualScanOption(timestamp = it.timestamp) }

        // Prevent a previous request from populating Color after the selection changes.
        requestedLeftColorPair = null
        requestedRightColorPair = null
        _state.update {
            it.copy(
                compare = it.compare.copy(
                    hasData = latest != null,
                    scanOptions = options,
                    leftScanTimestamp = left?.timestamp ?: latest?.timestamp,
                    rightScanTimestamp = right?.timestamp ?: previous?.timestamp,
                    leftModel3dUrl = left?.model3dUrl?.takeUnless { url -> url.isBlank() },
                    rightModel3dUrl = right?.model3dUrl?.takeUnless { url -> url.isBlank() },
                    leftMeasurements = left?.measurements.orEmpty(),
                    leftPreviousMeasurements = leftPrevious?.measurements.orEmpty(),
                    rightMeasurements = right?.measurements.orEmpty(),
                    rightPreviousMeasurements = rightPrevious?.measurements.orEmpty(),
                    leftColorModel = BodyVisualColorModel.Idle,
                    rightColorModel = BodyVisualColorModel.Idle,
                    visibleRegions = visibleRegions,
                ),
            )
        }
        if (_state.value.compare.mode == BodyVisualMode.Color &&
            _state.value.selectedTab == BodyTab.Compare
        ) {
            loadCompareColorMeshesIfNeeded()
        }
    }

    private fun scanAndPrevious(timestamp: Long?): Pair<ScanRecord?, ScanRecord?> {
        if (timestamp == null) return null to null
        val index = allScans.indexOfLast { it.timestamp == timestamp }
        if (index < 0) return null to null
        return allScans.getOrNull(index) to allScans.getOrNull(index - 1)
    }

    private fun loadCompareColorMeshesIfNeeded() {
        when (_state.value.compare.leftColorModel) {
            BodyVisualColorModel.Idle, BodyVisualColorModel.Error ->
                loadCompareColorMesh(isLeft = true)

            else -> Unit
        }
        when (_state.value.compare.rightColorModel) {
            BodyVisualColorModel.Idle, BodyVisualColorModel.Error ->
                loadCompareColorMesh(isLeft = false)

            else -> Unit
        }
    }

    private fun loadCompareColorMesh(isLeft: Boolean) {
        val compare = _state.value.compare
        val timestamp = if (isLeft) compare.leftScanTimestamp else compare.rightScanTimestamp
        val (selected, previous) = scanAndPrevious(timestamp)
        val beforeId = previous?.measurementId?.takeUnless { it.isBlank() }
        val afterId = selected?.measurementId?.takeUnless { it.isBlank() }
        val pair = if (beforeId == null || afterId == null) null else beforeId to afterId

        if (isLeft) requestedLeftColorPair = pair else requestedRightColorPair = pair
        if (pair == null) {
            updateCompareColorModel(isLeft, BodyVisualColorModel.Unavailable)
            return
        }

        updateCompareColorModel(isLeft, BodyVisualColorModel.Loading)
        if (!loadingCompareColorPairs.add(pair)) return

        viewModelScope.launch {
            threeDLookRepository.loadColorAnalysisMeshUrl(
                beforeMeasurementId = pair.first,
                afterMeasurementId = pair.second,
            ).onSuccess { meshUrl ->
                applyCompareColorResult(pair, BodyVisualColorModel.Ready(meshUrl))
            }.onFailure {
                applyCompareColorResult(pair, BodyVisualColorModel.Error)
            }
            loadingCompareColorPairs.remove(pair)
        }
    }

    private fun applyCompareColorResult(
        pair: Pair<String, String>,
        model: BodyVisualColorModel,
    ) {
        val leftMatches = requestedLeftColorPair == pair
        val rightMatches = requestedRightColorPair == pair
        if (!leftMatches && !rightMatches) return
        _state.update {
            it.copy(
                compare = it.compare.copy(
                    leftColorModel = if (leftMatches) model else it.compare.leftColorModel,
                    rightColorModel = if (rightMatches) model else it.compare.rightColorModel,
                ),
            )
        }
    }

    private fun updateCompareColorModel(isLeft: Boolean, model: BodyVisualColorModel) {
        _state.update {
            it.copy(
                compare = if (isLeft) it.compare.copy(leftColorModel = model)
                else it.compare.copy(rightColorModel = model),
            )
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

        val series = buildSeries(scans, zone, rangeStart, rangeEnd, anchorPreviousAtStart = true)

        val labelFormatter =
            SimpleDateFormat(DateFormatConstants.DAY_MONTH_NUMERIC, Locale.getDefault())
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

        val series = buildSeries(scans, zone, rangeStart, rangeEnd, anchorPreviousAtStart = true)

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
        val driftFraction = BodyConstants.PREDICTED_DRIFT_FRACTION

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

        // Sparse forecast: a few nodes split evenly across the predicted-drift and future-estimate
        // phases (not one per day). Drift is realized on an easeIn curve (see predictedNodes), so the
        // fan starts gently near the origin and accelerates outward to its max at the final point.
        val out = points.toMutableList()
        val seenOffsets = LinkedHashSet<Int>()
        for (node in predictedNodes(driftFraction)) {
            val dayOffset = (daysAhead * node.timeFraction).roundToInt().coerceIn(1, daysAhead)
            if (!seenOffsets.add(dayOffset)) continue
            val timeFraction = dayOffset.toFloat() / daysAhead
            val day = lastDay.plusDays(dayOffset.toLong())
            val ts = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            out += BodyTrendPoint(
                timestamp = ts,
                deltaFat = 0f,
                deltaMuscle = 0f,
                absoluteFat = (last.absoluteFat + fatDrift * node.driftRealized).coerceIn(0f, 100f),
                absoluteMuscle = (last.absoluteMuscle + muscleDrift * node.driftRealized)
                    .coerceIn(0f, 100f),
                isInterpolated = true,
                phase = if (timeFraction <= driftFraction) PredictedDrift else FutureEstimate,
            )
        }
        return out
    }

    /** A forecast tail node: where it sits in the window and how much of the drift it has realized. */
    private data class PredictedNode(val timeFraction: Float, val driftRealized: Float)

    /**
     * Forecast tail nodes, split evenly between the predicted-drift and future-estimate phases (2
     * each). Drift is realized on an easeIn (quadratic) curve, so the fan starts gently near the
     * origin — a flat-looking plateau — and accelerates outward, reaching the full projected drift
     * at the final point (widest at the right edge), matching the design rather than a linear ramp.
     */
    private fun predictedNodes(driftFraction: Float): List<PredictedNode> {
        val futureSpan = 1f - driftFraction
        val timeFractions = listOf(
            driftFraction * 0.5f,               // predicted drift
            driftFraction,                      // predicted drift
            driftFraction + futureSpan * 0.5f,  // future estimate
            driftFraction + futureSpan,         // future estimate
        )
        return timeFractions.map { t -> PredictedNode(t, t * t) }
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
        val startFormatter =
            SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH, Locale.getDefault())
        val endFormatter =
            SimpleDateFormat(DateFormatConstants.DATE_RANGE_DAY_MONTH_YEAR, Locale.getDefault())
        return "${startFormatter.format(Date(startMillis))} – ${endFormatter.format(Date(endMillis))}"
    }

    private fun buildComposition(
        latest: ScanRecord,
        previous: ScanRecord?,
        heightCm: Float?
    ): BodyComposition {
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

    private fun computePeriodDrift(range: BodyTimeRange): Float? {
        val latest = allScans.lastOrNull() ?: return null
        val baseline = periodDriftBaseline(latest, range) ?: return null
        return comparablePhysiqueScoreDelta(latest, baseline, heightCm)
    }

    private fun periodDriftBaseline(latest: ScanRecord, range: BodyTimeRange): ScanRecord? {
        val zone = ZoneId.systemDefault()
        val latestDay = LocalDate.ofInstant(Date(latest.timestamp).toInstant(), zone)
        val periodStartMillis = when (range) {
            BodyTimeRange.Week -> latestDay.minusWeeks(1)
            BodyTimeRange.Month -> latestDay.minusMonths(1)
            BodyTimeRange.Year -> latestDay.minusYears(1)
        }.atStartOfDay(zone).toInstant().toEpochMilli()
        return allScans.lastOrNull { it.timestamp <= periodStartMillis }
            ?: allScans.firstOrNull { it.timestamp < latest.timestamp }
    }
}
