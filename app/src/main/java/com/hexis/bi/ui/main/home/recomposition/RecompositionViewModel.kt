package com.hexis.bi.ui.main.home.recomposition

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanFetchProjection
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.domain.recomposition.CompositionState
import com.hexis.bi.domain.recomposition.RecompositionCalculator
import com.hexis.bi.domain.recomposition.RecompositionResult
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.constants.RecompositionConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

class RecompositionViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(RecompositionState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    private fun load() {
        viewModelScope.launch {
            setLoading(true)
            val today = LocalDate.now()
            val oldestWindowStart = windowStart(RecompositionWindow.OneYear, today)
            val oldestWindowStartMillis = oldestWindowStart
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            scanHistoryRepository
                .getScansSavedSince(oldestWindowStartMillis, ScanFetchProjection.LIST_SUMMARY)
                .onSuccess { scans ->
                    _state.update {
                        it.copy(cards = RecompositionWindow.entries.map { window -> card(window, scans, today) })
                    }
                }
                .onFailure { error ->
                    setError(error.message ?: string(R.string.recomposition_error_load))
                }
            setLoading(false)
        }
    }

    private fun card(
        window: RecompositionWindow,
        scans: List<ScanRecord>,
        today: LocalDate,
    ): RecompositionCardUi {
        val result = RecompositionCalculator.buildWindow(
            scans = scans,
            windowStart = windowStart(window, today),
            windowEnd = today,
        )

        return when (result.state) {
            CompositionState.NotEnoughScans, CompositionState.NeedsAnotherScan -> emptyCard(window)
            else -> dataCard(window, result)
        }
    }

    private fun windowStart(window: RecompositionWindow, today: LocalDate): LocalDate = when (window) {
        RecompositionWindow.FourWeeks -> today.minusWeeks(RecompositionConstants.WINDOW_WEEKS_SHORT)
        RecompositionWindow.SixMonths -> today.minusMonths(RecompositionConstants.WINDOW_MONTHS_MEDIUM)
        RecompositionWindow.OneYear -> today.minusYears(RecompositionConstants.WINDOW_YEARS_LONG)
    }

    private fun emptyCard(window: RecompositionWindow): RecompositionCardUi {
        val dash = string(R.string.stat_unknown)
        return RecompositionCardUi(
            window = window,
            isRecomposition = false,
            recomposedValue = dash,
            weightChangeText = dash,
            weightSubtitle = "",
            fat = RecompositionMetricUi(dash),
            lean = RecompositionMetricUi(dash),
        )
    }

    private fun dataCard(window: RecompositionWindow, result: RecompositionResult): RecompositionCardUi {
        val isRecomposition = result.state == CompositionState.Recomposition
        return RecompositionCardUi(
            window = window,
            isRecomposition = isRecomposition,
            recomposedValue = result.recomposedKg?.takeIf { it > 0f }?.let { formatMagnitude(it) }
                ?: string(R.string.stat_unknown),
            weightChangeText = signedValue(result.weightChangeKg),
            weightSubtitle = string(weightSubtitleRes(result.state)),
            fat = metric(result.fatChangeKg, favorableWhenNegative = true),
            lean = metric(result.leanChangeKg, favorableWhenNegative = false),
        )
    }

    /**
     * "Improving" whenever fat mass trended down (recomposition or fat loss); "Stable" otherwise. Keyed
     * off the observed fat direction rather than the strict recomposition flag so it reflects the trend.
     */
    private fun weightSubtitleRes(state: CompositionState): Int = when (state) {
        CompositionState.Recomposition, CompositionState.WeightLoss -> R.string.recomposition_trend_improving
        else -> R.string.recomposition_trend_stable
    }

    private fun metric(changeKg: Float?, favorableWhenNegative: Boolean): RecompositionMetricUi {
        changeKg ?: return RecompositionMetricUi(string(R.string.stat_unknown))
        val favorable = when {
            changeKg == 0f -> null
            changeKg < 0f -> favorableWhenNegative
            else -> !favorableWhenNegative
        }
        val favorableAmount = if (favorableWhenNegative) -changeKg else changeKg
        val fraction = RecompositionConstants.TREND_BAR_CENTER_FRACTION +
            favorableAmount / (2f * RecompositionConstants.TREND_BAR_NORMALIZATION_KG)
        return RecompositionMetricUi(
            valueText = signedValue(changeKg),
            favorable = favorable,
            markerFraction = fraction.coerceIn(0f, 1f),
        )
    }

    private fun signedValue(value: Float?): String {
        value ?: return string(R.string.stat_unknown)
        val sign = if (value >= 0f) "+" else "-"
        return string(R.string.recomposition_signed_value, sign, abs(value))
    }

    private fun formatMagnitude(value: Float): String =
        String.format(Locale.US, MAGNITUDE_FORMAT, abs(value))

    private companion object {
        const val MAGNITUDE_FORMAT = "%.1f"
    }
}
