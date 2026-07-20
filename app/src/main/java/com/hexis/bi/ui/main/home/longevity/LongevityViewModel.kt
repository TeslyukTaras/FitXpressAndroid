package com.hexis.bi.ui.main.home.longevity

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.longevity.FoundationStatus
import com.hexis.bi.domain.longevity.FunctionalFoundation
import com.hexis.bi.domain.longevity.LongevityCalculator
import com.hexis.bi.domain.longevity.LongevityCalculator.withFunctional
import com.hexis.bi.domain.longevity.LongevityResult
import com.hexis.bi.domain.longevity.MetabolicFoundation
import com.hexis.bi.domain.longevity.PhysicalFoundation
import com.hexis.bi.domain.longevity.RecompositionFoundation
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.LongevityFoundationConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Drives the body-led Longevity screen.
 *
 * Loading is split in two because the two data sources cost wildly different amounts. Scans are a
 * single Firestore query, so all three windows' body foundations are computed from one read and
 * shown immediately. Wearable data goes through Terra, which chunks any range into ~30-day requests
 * — a year costs dozens of sequential round trips — so it is fetched only for the window actually
 * being viewed, in the background, and cached per window. Functional Capacity fills in when it
 * arrives; the other three foundations never wait on it.
 */
class LongevityViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val recoveryRepository: RecoveryRepository,
    private val sleepRepository: SleepRepository,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(LongevityState())
    val state: StateFlow<LongevityState> = _state.asStateFlow()

    private var bodyResults: Map<LongevityWindow, LongevityResult> = emptyMap()
    private val functionalCache = mutableMapOf<LongevityWindow, FunctionalFoundation>()
    private var functionalJob: Job? = null

    init {
        load()
    }

    fun selectWindow(window: LongevityWindow) {
        if (window == _state.value.selectedWindow) return
        _state.update { it.copy(selectedWindow = window) }
        render(window)
        loadFunctional(window)
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    private fun load() {
        viewModelScope.launch {
            setLoading(true)
            loadBody()
            setLoading(false)
            loadFunctional(_state.value.selectedWindow)
        }
    }

    private suspend fun loadBody() = coroutineScope {
        val today = LocalDate.now()
        val earliestStart = windowStart(LongevityWindow.OneYear, today)
        val earliestStartMillis = earliestStart
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val heightDef = async { userRepository.getUser().getOrNull()?.heightCm?.toFloat() }
        val scans: List<ScanRecord> = scanHistoryRepository
            .getScansSavedSince(earliestStartMillis, ScanFetchProjection.LIST_SUMMARY)
            .getOrElse { error ->
                setError(error.message ?: string(R.string.longevity_error_load))
                return@coroutineScope
            }
        val heightCm = heightDef.await()

        bodyResults = LongevityWindow.entries.associateWith { window ->
            LongevityCalculator.evaluateBody(
                scans = scans,
                heightCm = heightCm,
                windowStart = windowStart(window, today),
                windowEnd = today,
            )
        }
        render(_state.value.selectedWindow)
    }

    private fun loadFunctional(window: LongevityWindow) {
        if (functionalCache.containsKey(window)) {
            render(window)
            return
        }
        functionalJob?.cancel()
        functionalJob = viewModelScope.launch {
            val today = LocalDate.now()
            val start = windowStart(window, today)
            val functional = coroutineScope {
                val activityDef = async {
                    // Default detail: the model reads daily steps and VO2 max, no intraday samples.
                    activityRepository.getSummariesForRange(start, today).getOrNull().orEmpty()
                }
                val recoveryDef = async {
                    recoveryRepository.getSnapshotsForRange(start, today).getOrNull().orEmpty()
                }
                val sleepDef = async {
                    sleepRepository.getSessionsForRange(start, today).getOrNull().orEmpty()
                }
                LongevityCalculator.evaluateFunctional(
                    allActivity = activityDef.await(),
                    allRecovery = recoveryDef.await(),
                    allSleep = sleepDef.await(),
                    windowStart = start,
                    windowEnd = today,
                )
            }
            functionalCache[window] = functional
            if (_state.value.selectedWindow == window) render(window)
        }
    }

    private fun render(window: LongevityWindow) {
        val body = bodyResults[window] ?: return
        val result = functionalCache[window]?.let { body.withFunctional(it) } ?: body
        _state.update {
            it.copy(
                direction = result.direction,
                foundations = listOf(
                    metabolicUi(result.metabolic),
                    recompositionUi(result.recomposition),
                    physicalUi(result.physical),
                    functionalUi(result.functional),
                ),
            )
        }
    }

    private fun windowStart(window: LongevityWindow, today: LocalDate): LocalDate = when (window) {
        LongevityWindow.FourWeeks -> today.minusWeeks(LongevityFoundationConstants.WINDOW_WEEKS_SHORT)
        LongevityWindow.SixMonths -> today.minusMonths(LongevityFoundationConstants.WINDOW_MONTHS_MEDIUM)
        LongevityWindow.OneYear -> today.minusYears(LongevityFoundationConstants.WINDOW_YEARS_LONG)
    }

    // ---- Evidence rows -------------------------------------------------------------------------

    private fun metabolicUi(foundation: MetabolicFoundation) = LongevityFoundationUi(
        titleRes = R.string.longevity_foundation_metabolic,
        status = foundation.status,
        evidence = listOf(
            transition(
                label = string(R.string.longevity_evidence_waist_to_height),
                start = foundation.waistToHeightStart?.let { format(it, RATIO_DECIMALS) },
                end = foundation.waistToHeightEnd?.let { format(it, RATIO_DECIMALS) },
            )
        ),
    )

    private fun recompositionUi(foundation: RecompositionFoundation) = LongevityFoundationUi(
        titleRes = R.string.longevity_foundation_recomposition,
        status = foundation.status,
        evidence = listOf(
            signedEvidence(
                label = string(R.string.longevity_evidence_fat),
                change = foundation.fatChangeKg,
                unit = string(R.string.unit_kg),
            ),
            signedEvidence(
                label = string(R.string.longevity_evidence_lean),
                change = foundation.leanChangeKg,
                unit = string(R.string.unit_kg),
            ),
        ),
    )

    private fun physicalUi(foundation: PhysicalFoundation) = LongevityFoundationUi(
        titleRes = R.string.longevity_foundation_physical,
        status = foundation.status,
        evidence = listOf(
            signedEvidence(
                label = string(R.string.longevity_evidence_thigh),
                change = foundation.thighChangeCm,
                unit = string(R.string.unit_cm),
            ),
            signedEvidence(
                label = string(R.string.longevity_evidence_calf),
                change = foundation.calfChangeCm,
                unit = string(R.string.unit_cm),
            ),
        ),
    )

    private fun functionalUi(foundation: FunctionalFoundation) = LongevityFoundationUi(
        titleRes = R.string.longevity_foundation_functional,
        status = foundation.status,
        evidence = listOf(
            transition(
                label = string(R.string.longevity_evidence_vo2_max),
                start = foundation.vo2Start?.roundToInt()?.toString(),
                end = foundation.vo2End?.roundToInt()?.toString(),
            ),
            transition(
                label = string(R.string.longevity_evidence_rhr),
                start = foundation.restingHeartRateStart?.toString(),
                end = foundation.restingHeartRateEnd?.toString(),
                unit = string(R.string.unit_bpm),
            ),
        ),
    )

    private fun transition(
        label: String,
        start: String?,
        end: String?,
        unit: String = "",
    ): LongevityEvidenceUi {
        if (start == null || end == null) return missing(label)
        return LongevityEvidenceUi(
            label = label,
            value = string(R.string.longevity_evidence_transition, start, end),
            unit = unit,
        )
    }

    private fun signedEvidence(label: String, change: Float?, unit: String): LongevityEvidenceUi {
        change ?: return missing(label)
        val sign = if (change < 0f) MINUS_SIGN else PLUS_SIGN
        return LongevityEvidenceUi(
            label = label,
            value = sign + format(abs(change), CHANGE_DECIMALS),
            unit = unit,
        )
    }

    private fun missing(label: String) =
        LongevityEvidenceUi(label = label, value = string(R.string.stat_unknown))

    private fun format(value: Float, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)

    private companion object {
        const val RATIO_DECIMALS = 2
        const val CHANGE_DECIMALS = 1
        const val MINUS_SIGN = "−"
        const val PLUS_SIGN = "+"
    }
}
