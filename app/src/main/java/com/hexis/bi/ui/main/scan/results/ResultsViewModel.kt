package com.hexis.bi.ui.main.scan.results

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.scan.ThreeDLookRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.avatar.prefetchMetricAvatarModel
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.body.BodyVisualColorModel
import com.hexis.bi.ui.main.body.BodyVisualMode
import com.hexis.bi.ui.main.body.CompareState
import com.hexis.bi.ui.main.body.VisualScanOption
import com.hexis.bi.ui.main.body.VisualState
import com.hexis.bi.ui.main.body.buildBodyProportion
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

/**
 * Drives the Results screen, which reuses the My Body Visual/Compare presentation. It loads the
 * scan being shown (the just-finished scan, or one opened from history) plus its two predecessors,
 * and maps them into [VisualState] / [CompareState]. Color analysis (the Base/Color toggle) is
 * ported from the body flow.
 */
class ResultsViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val scanResultRepository: ScanResultRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val threeDLookRepository: ThreeDLookRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    private var current: ScanRecord? = null
    private var previous: ScanRecord? = null
    private var beforePrevious: ScanRecord? = null
    private var heightCm: Float? = null
    private var gender: String? = null

    private val loadingColorPairs = mutableSetOf<Pair<String, String>>()
    private var requestedVisualColorPair: Pair<String, String>? = null
    private var requestedLeftColorPair: Pair<String, String>? = null
    private var requestedRightColorPair: Pair<String, String>? = null
    private var personalizeHintEvaluated = false

    fun onResultsShown() {
        if (personalizeHintEvaluated) return
        personalizeHintEvaluated = true
        viewModelScope.launch {
            if (!preferencesRepository.isPersonalizeResultsHintShown()) {
                preferencesRepository.setPersonalizeResultsHintShown()
                _state.update { it.copy(showPersonalizeResultsHint = true) }
            }
        }
    }

    fun onPersonalizeResultsHintDismissed() {
        _state.update { it.copy(showPersonalizeResultsHint = false) }
    }

    fun showBodyProportionInfo() = _state.update { it.copy(showBodyProportionInfo = true) }
    fun dismissBodyProportionInfo() = _state.update { it.copy(showBodyProportionInfo = false) }

    init {
        observeColorMode()
        loadUnitSystem()
        load()
    }

    private fun loadUnitSystem() = launch(showLoading = false) {
        userRepository.getUser().onSuccess { profile ->
            heightCm = profile.heightCm?.toFloat()
            gender = profile.gender
            _state.update {
                it.copy(
                    isMetric = profile.unitSystem.isMetricUnitSystem(),
                    bodyProportion = buildBodyProportion(current, heightCm, gender),
                )
            }
        }
    }

    private fun load() = launch(showLoading = false) {
        val selectedScanId = scanResultRepository.selectedScanId
        val (cur, prev, beforePrev) =
            if (selectedScanId != null) loadHistoryTriple(selectedScanId)
            else loadFreshTriple()

        if (cur == null) {
            _state.update { it.copy(isLoading = false) }
            return@launch
        }
        current = cur
        previous = prev
        beforePrevious = beforePrev
        prewarmCurrentModelInBackground(cur)
        _state.update {
            it.copy(
                isLoading = false,
                visual = buildVisual(it.visual),
                compare = buildCompare(it.compare),
                bodyProportion = buildBodyProportion(cur, heightCm, gender),
            )
        }
        // The persisted mode may already be Color; now that measurement ids exist, load the mesh.
        if (_state.value.visual.mode == BodyVisualMode.Color &&
            _state.value.selectedTab == ResultsTab.Visual
        ) {
            loadVisualColorMesh()
        }
    }

    private suspend fun loadHistoryTriple(scanId: String): Triple<ScanRecord?, ScanRecord?, ScanRecord?> {
        val cur = freshScanRecord(scanId)
            ?: scanHistoryRepository.getScanRecordById(scanId).getOrNull()
            ?: return Triple(null, null, null)
        val older = scanHistoryRepository
            .getOlderScanRecordsBefore(Timestamp(Date(cur.timestamp)), limit = 2)
            .getOrElse { emptyList() }
        return Triple(
            cur,
            older.getOrNull(0)?.withMeasurements(),
            older.getOrNull(1)?.withMeasurements()
        )
    }

    private suspend fun loadFreshTriple(): Triple<ScanRecord?, ScanRecord?, ScanRecord?> {
        val cur = scanResultRepository.latestResult
            ?.takeIf { it.scanId != null && it.savedAtMillis != null }
            ?.let { latest ->
                scanHistoryRepository.buildScanRecordFromResponse(
                    response = latest.response,
                    scanId = latest.scanId.orEmpty(),
                    savedAtMillis = latest.savedAtMillis ?: 0L,
                )
            }
            ?: scanHistoryRepository.getLatestScan().getOrNull()
            ?: return Triple(null, null, null)
        val (prev, beforePrev) = scanHistoryRepository.getPreviousTwoScans()
            .getOrElse { null to null }
        return Triple(cur, prev?.withMeasurements(), beforePrev?.withMeasurements())
    }

    private fun freshScanRecord(scanId: String): ScanRecord? {
        val latest = scanResultRepository.latestResult ?: return null
        val latestScanId = latest.scanId ?: return null
        val savedAtMillis = latest.savedAtMillis ?: return null
        if (latestScanId != scanId) return null
        return scanHistoryRepository.buildScanRecordFromResponse(
            response = latest.response,
            scanId = latestScanId,
            savedAtMillis = savedAtMillis,
        )
    }

    private fun ScanRecord.withMeasurements(): ScanRecord? = takeIf { it.measurements.isNotEmpty() }

    private fun prewarmCurrentModelInBackground(scan: ScanRecord) {
        val modelUrl = scan.model3dUrl?.takeUnless { it.isBlank() } ?: return
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                prefetchMetricAvatarModel(appContext, modelUrl)
            }.onFailure {
                Timber.w(it, "Unable to prewarm scan result model")
            }
        }
    }

    private fun buildVisual(previousVisual: VisualState): VisualState {
        val cur = current ?: return VisualState()
        return VisualState(
            selectedBodyPart = previousVisual.selectedBodyPart,
            mode = previousVisual.mode,
            colorModel = BodyVisualColorModel.Idle,
            hasData = true,
            isLatestScanSelected = true,
            scanOptions = emptyList(),
            latestScanTimestamp = cur.timestamp,
            previousScanTimestamp = previous?.timestamp,
            beforePreviousScanTimestamp = beforePrevious?.timestamp,
            latestModel3dUrl = cur.model3dUrl?.takeUnless { it.isBlank() },
            previousModel3dUrl = previous?.model3dUrl?.takeUnless { it.isBlank() },
            latestMeasurements = cur.measurements,
            previousMeasurements = previous?.measurements.orEmpty(),
            beforePreviousMeasurements = beforePrevious?.measurements.orEmpty(),
        )
    }

    private fun buildCompare(previousCompare: CompareState): CompareState {
        val cur = current ?: return CompareState()
        return CompareState(
            hasData = true,
            mode = previousCompare.mode,
            selectedBodyPart = previousCompare.selectedBodyPart,
            // Locked to the two latest scans; populated only so the summary reads "Latest / Previous"
            // (the picker dropdowns are hidden via showScanSelector = false).
            scanOptions = listOfNotNull(
                VisualScanOption(cur.timestamp),
                previous?.let { VisualScanOption(it.timestamp) },
            ),
            leftScanTimestamp = cur.timestamp,
            rightScanTimestamp = previous?.timestamp,
            leftModel3dUrl = cur.model3dUrl?.takeUnless { it.isBlank() },
            rightModel3dUrl = previous?.model3dUrl?.takeUnless { it.isBlank() },
            leftMeasurements = cur.measurements,
            leftPreviousMeasurements = previous?.measurements.orEmpty(),
            rightMeasurements = previous?.measurements.orEmpty(),
            rightPreviousMeasurements = beforePrevious?.measurements.orEmpty(),
            leftColorModel = BodyVisualColorModel.Idle,
            rightColorModel = BodyVisualColorModel.Idle,
        )
    }

    fun selectTab(tab: ResultsTab) {
        _state.update { it.copy(selectedTab = tab) }
        if (_state.value.visual.mode != BodyVisualMode.Color) return
        when (tab) {
            ResultsTab.Visual ->
                if (_state.value.visual.colorModel !is BodyVisualColorModel.Ready) loadVisualColorMesh()

            ResultsTab.Compare -> loadCompareColorMeshesIfNeeded()
            ResultsTab.MyBody -> Unit
        }
    }

    fun selectBodyPart(region: BodyMeasurementRegion) {
        _state.update { it.copy(visual = it.visual.copy(selectedBodyPart = region)) }
    }

    fun selectCompareBodyPart(region: BodyMeasurementRegion) {
        _state.update { it.copy(compare = it.compare.copy(selectedBodyPart = region)) }
    }


    /** Base/Color is shared with My Body via [UserPreferencesRepository] so the choice persists. */
    fun selectMode(mode: BodyVisualMode) {
        applyColorMode(mode)
        viewModelScope.launch { preferencesRepository.setBodyVisualMode(mode.name) }
    }

    private fun observeColorMode() {
        viewModelScope.launch {
            preferencesRepository.bodyVisualMode.collect { stored ->
                val mode =
                    BodyVisualMode.entries.firstOrNull { it.name == stored } ?: BodyVisualMode.Base
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
            ResultsTab.Visual ->
                if (_state.value.visual.colorModel !is BodyVisualColorModel.Ready) loadVisualColorMesh()

            ResultsTab.Compare -> loadCompareColorMeshesIfNeeded()
            ResultsTab.MyBody -> Unit
        }
    }

    /** Results is locked to the current scan, so scan switching is a no-op. */
    fun selectVisualScan(timestamp: Long) = Unit
    fun selectCompareLeftScan(timestamp: Long) = Unit
    fun selectCompareRightScan(timestamp: Long) = Unit

    private fun loadVisualColorMesh() {
        val beforeId = previous?.measurementId?.takeUnless { it.isBlank() }
        val afterId = current?.measurementId?.takeUnless { it.isBlank() }
        val pair = if (beforeId == null || afterId == null) null else beforeId to afterId

        requestedVisualColorPair = pair
        if (pair == null) {
            setVisualColorModel(BodyVisualColorModel.Unavailable)
            return
        }
        setVisualColorModel(BodyVisualColorModel.Loading)
        if (!loadingColorPairs.add(pair)) return

        launch(showLoading = false) {
            threeDLookRepository.loadColorAnalysisMeshUrl(
                beforeMeasurementId = pair.first,
                afterMeasurementId = pair.second,
            ).onSuccess { meshUrl ->
                if (requestedVisualColorPair == pair) setVisualColorModel(
                    BodyVisualColorModel.Ready(
                        meshUrl
                    )
                )
            }.onFailure {
                if (requestedVisualColorPair == pair) setVisualColorModel(BodyVisualColorModel.Error)
            }
            loadingColorPairs.remove(pair)
        }
    }

    private fun setVisualColorModel(model: BodyVisualColorModel) {
        _state.update { it.copy(visual = it.visual.copy(colorModel = model)) }
    }

    private fun loadCompareColorMeshesIfNeeded() {
        when (_state.value.compare.leftColorModel) {
            BodyVisualColorModel.Idle, BodyVisualColorModel.Error -> loadCompareColorMesh(isLeft = true)
            else -> Unit
        }
        when (_state.value.compare.rightColorModel) {
            BodyVisualColorModel.Idle, BodyVisualColorModel.Error -> loadCompareColorMesh(isLeft = false)
            else -> Unit
        }
    }

    private fun loadCompareColorMesh(isLeft: Boolean) {
        // Left = current vs previous; right = previous vs before-previous.
        val selected = if (isLeft) current else previous
        val prior = if (isLeft) previous else beforePrevious
        val beforeId = prior?.measurementId?.takeUnless { it.isBlank() }
        val afterId = selected?.measurementId?.takeUnless { it.isBlank() }
        val pair = if (beforeId == null || afterId == null) null else beforeId to afterId

        if (isLeft) requestedLeftColorPair = pair else requestedRightColorPair = pair
        if (pair == null) {
            updateCompareColorModel(isLeft, BodyVisualColorModel.Unavailable)
            return
        }
        updateCompareColorModel(isLeft, BodyVisualColorModel.Loading)
        if (!loadingColorPairs.add(pair)) return

        launch(showLoading = false) {
            threeDLookRepository.loadColorAnalysisMeshUrl(
                beforeMeasurementId = pair.first,
                afterMeasurementId = pair.second,
            ).onSuccess { meshUrl ->
                applyCompareColorResult(pair, BodyVisualColorModel.Ready(meshUrl))
            }.onFailure {
                applyCompareColorResult(pair, BodyVisualColorModel.Error)
            }
            loadingColorPairs.remove(pair)
        }
    }

    private fun applyCompareColorResult(pair: Pair<String, String>, model: BodyVisualColorModel) {
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

    override fun onCleared() {
        scanResultRepository.selectedScanId = null
        super.onCleared()
    }
}
