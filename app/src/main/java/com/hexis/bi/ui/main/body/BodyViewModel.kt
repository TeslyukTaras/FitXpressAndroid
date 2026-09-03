package com.hexis.bi.ui.main.body

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.body.PhysiquePredictionRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.domain.body.comparablePhysiqueScoreDelta
import com.hexis.bi.domain.body.muscleMassPercentage
import com.hexis.bi.domain.body.predictionDays
import com.hexis.bi.data.intelligence.IntelligenceConfigRepository
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.prediction.PredictionConstants
import com.hexis.bi.intelligence.prediction.PredictionSeries
import com.hexis.bi.intelligence.prediction.ScanDay
import com.hexis.bi.intelligence.prediction.predictWeekly
import com.hexis.bi.domain.body.physiqueScore
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.BodyConstants
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class BodyViewModel internal constructor(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
    private val threeDLookRepository: ThreeDLookRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val intelligenceConfigRepository: IntelligenceConfigRepository,
    private val physiquePredictionRepository: PhysiquePredictionRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(BodyState())
    val state: StateFlow<BodyState> = _state.asStateFlow()

    private var allScans: List<ScanRecord> = emptyList()
    private var heightCm: Float? = null
    private var engineConfig: EngineConfig? = null
    private var gender: String? = null
    private var predictionGains: Map<PredictionSeries, Double> = emptyMap()
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
            engineConfig = intelligenceConfigRepository.config().getOrNull()
            gender = profile?.gender

            val latest = allScans.lastOrNull()
            val previous = allScans.dropLast(1).lastOrNull()

            val composition = if (latest == null) BodyComposition.empty()
            else buildComposition(
                latest = latest,
                previous = previous,
                config = engineConfig,
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
            calibratePrediction()
        }
    }

    private fun calibratePrediction() {
        val days = predictionDays()
        if (days.isEmpty()) return
        viewModelScope.launch {
            val gains = physiquePredictionRepository.calibrateAndStore(days).getOrNull().orEmpty()
            if (gains != predictionGains) {
                predictionGains = gains
                rebuildChart()
            }
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
        val range = _state.value.timeRange
        val days = predictionDays()
        val now = System.currentTimeMillis()
        val chart = PhysiqueTrendChart.build(days, range, now, predictionGains)
        val estimateChart = if (range == BodyTimeRange.FourWeeks) {
            chart
        } else {
            PhysiqueTrendChart.build(days, BodyTimeRange.FourWeeks, now, predictionGains)
        }
        val prediction = predictionState(days, estimateChart, now)
        _state.update { it.copy(chart = chart, prediction = prediction) }
    }

    private fun predictionDays(): List<ScanDay> {
        val config = engineConfig ?: return emptyList()
        return allScans.predictionDays(config, heightCm)
    }

    private fun predictionState(
        days: List<ScanDay>,
        chart: BodyChartData,
        nowMillis: Long,
    ): PhysiquePredictionState {
        val zone = ZoneId.systemDefault()
        val lastDay = days.lastOrNull()?.date?.let(LocalDate::parse)
            ?: return PhysiquePredictionState.None
        val today = LocalDate.ofInstant(Date(nowMillis).toInstant(), zone)
        val dueDay = lastDay.plusDays(BodyConstants.SCAN_CADENCE_DAYS)
        val daysToNextScan = ChronoUnit.DAYS.between(today, dueDay).coerceAtLeast(0L).toInt()

        if (days.size < PredictionConstants.MIN_BUCKETS) {
            return PhysiquePredictionState.AwaitingSecondScan(daysToNextScan)
        }
        if (today.isAfter(dueDay)) return PhysiquePredictionState.Overdue
        val prediction = predictWeekly(days, predictionGains)
            ?: return PhysiquePredictionState.AwaitingSecondScan(daysToNextScan)
        val predictedPoint = chart.points.lastOrNull()
            ?.takeIf { it.phase == BodyTrendPhase.WeeklyPrediction }
        return PhysiquePredictionState.Active(
            daysToNextScan = daysToNextScan,
            leanAdvantage = predictedPoint?.deltaMuscle,
            fatAdvantage = predictedPoint?.deltaFat,
            fit = prediction.predicted[PredictionSeries.COMPARABLE_SCORE]?.toFloat(),
        )
    }

    private fun buildComposition(
        latest: ScanRecord,
        previous: ScanRecord?,
        config: EngineConfig?,
        heightCm: Float?
    ): BodyComposition {
        val fatPct = latest.fatPercentage
        val musclePct = latest.muscleMassPercentage()
        val bis = config?.let { latest.physiqueScore(it, heightCm) }

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
            deltaBisScore = config?.let {
                comparablePhysiqueScoreDelta(latest, previous, it, heightCm)
            },
        )
    }

    private fun delta(current: Float?, previous: Float?): Float? {
        if (current == null || previous == null) return null
        return current - previous
    }

    private fun computePeriodDrift(range: BodyTimeRange): Float? {
        val latest = allScans.lastOrNull() ?: return null
        val config = engineConfig ?: return null
        val baseline = periodDriftBaseline(latest, range) ?: return null
        return comparablePhysiqueScoreDelta(latest, baseline, config, heightCm)
    }

    // Closest scan to the period start in either direction; ties resolve to the older scan.
    private fun periodDriftBaseline(latest: ScanRecord, range: BodyTimeRange): ScanRecord? {
        val zone = ZoneId.systemDefault()
        val latestDay = LocalDate.ofInstant(Date(latest.timestamp).toInstant(), zone)
        val periodStart = when (range) {
            BodyTimeRange.FourWeeks -> latestDay.minusWeeks(1)
            BodyTimeRange.SixMonths -> latestDay.minusMonths(BodyConstants.SIX_MONTH_SPAN)
            BodyTimeRange.OneYear -> latestDay.minusMonths(BodyConstants.ONE_YEAR_SPAN_MONTHS)
        }.atStartOfDay(zone).toInstant().toEpochMilli()
        return allScans
            .filter { it.timestamp < latest.timestamp }
            .minWithOrNull(compareBy({ abs(it.timestamp - periodStart) }, { it.timestamp }))
    }
}
