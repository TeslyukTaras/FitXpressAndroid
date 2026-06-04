package com.hexis.bi.ui.main.home.paceofaging

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.recovery.RecoveryRepository
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.longevity.AgingLevel
import com.hexis.bi.domain.longevity.AgingSignal
import com.hexis.bi.domain.longevity.PaceOfAgingInputs
import com.hexis.bi.domain.longevity.PaceOfAgingResult
import com.hexis.bi.domain.longevity.agingLevel
import com.hexis.bi.domain.longevity.computePaceOfAging
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.home.longevity.LongevityTrend
import com.hexis.bi.ui.main.home.longevity.waistToHeightRatio
import com.hexis.bi.utils.constants.LongevityConstants
import com.hexis.bi.utils.constants.PaceOfAgingConstants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class PaceOfAgingViewModel(
    application: Application,
    private val recoveryRepository: RecoveryRepository,
    private val activityRepository: ActivityRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
    private val terraManagerHolder: TerraManagerHolder,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(PaceOfAgingState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    private fun load() {
        setLoading(true)
        viewModelScope.launch {
            renderFromRepositories()
            setLoading(false)
            val synced = TerraSdkSync.syncLinkedConnections(
                terraManagerHolder.current,
                reason = "pace_of_aging",
            )
            if (synced) renderFromRepositories()
        }
    }

    private suspend fun renderFromRepositories() = coroutineScope {
        val today = LocalDate.now()
        val recoveryDef = async { recoveryRepository.getSnapshotForDate(today).getOrNull() }
        val activityDef = async { activityRepository.getSummaryForDate(today).getOrNull() }
        val scansDef = async { scanHistoryRepository.getRecentScans(limit = 2).getOrNull().orEmpty() }
        val heightDef = async { userRepository.getUser().getOrNull()?.heightCm?.toFloat() }

        val recovery = recoveryDef.await()
        val activity = activityDef.await()
        val scans = scansDef.await()
        val heightCm = heightDef.await()
        val latestScan = scans.getOrNull(0)
        val previousScan = scans.getOrNull(1)

        val result = computePaceOfAging(
            PaceOfAgingInputs(
                hrvMs = recovery?.hrvMs,
                restingHeartRateBpm = recovery?.restingHeartRateBpm,
                sleepScore = recovery?.sleepScore,
                recoveryScore = recovery?.score,
                steps = activity?.steps,
                bodyFatPercent = latestScan?.fatPercentage,
                waistToHeightRatio = waistToHeightRatio(latestScan, heightCm),
                vo2Max = activity?.vo2MaxMlPerMinPerKg,
                stressLevel = recovery?.stressLevel,
            )
        )

        val waistTrend = waistTrend(latestScan, previousScan, heightCm)
        val bodyFat = latestScan?.fatPercentage?.takeIf { it > 0f }
            ?.let { String.format(Locale.US, "%.1f%%", it) }
        val syncedDate = today.format(SYNCED_FMT)

        if (result == null) {
            _state.update {
                it.copy(
                    hasData = false,
                    paceText = "",
                    percentText = "",
                    meterFraction = NEUTRAL_FRACTION,
                    description = string(R.string.pace_of_aging_no_data),
                    syncedDate = syncedDate,
                    waistTrend = waistTrend,
                    bodyFat = bodyFat,
                    insight = string(R.string.pace_of_aging_insight_generic),
                )
            }
            return@coroutineScope
        }

        val pace = result.pace
        val level = agingLevel(pace)
        _state.update {
            it.copy(
                hasData = true,
                level = level,
                paceText = String.format(Locale.US, PACE_FORMAT, pace),
                meterFraction = meterFraction(pace),
                percentText = percentText(pace, level),
                description = description(level),
                syncedDate = syncedDate,
                waistTrend = waistTrend,
                bodyFat = bodyFat,
                insight = insight(level, result),
            )
        }
    }

    /** Bar fill for the pace: lower pace (slower aging) fills more of the red→green track. */
    private fun meterFraction(pace: Float): Float =
        ((PaceOfAgingConstants.MAX - pace) / (PaceOfAgingConstants.MAX - PaceOfAgingConstants.MIN))
            .coerceIn(0f, 1f)

    private fun percentText(pace: Float, level: AgingLevel): String {
        val percent = (abs(PaceOfAgingConstants.BASELINE - pace) * 100f).roundToInt()
        return when (level) {
            AgingLevel.Slower -> string(R.string.pace_of_aging_slower_format, percent)
            AgingLevel.Faster -> string(R.string.pace_of_aging_faster_format, percent)
            AgingLevel.Normal -> string(R.string.pace_of_aging_on_pace)
        }
    }

    private fun description(level: AgingLevel): String = string(
        when (level) {
            AgingLevel.Slower -> R.string.pace_of_aging_desc_slower
            AgingLevel.Normal -> R.string.pace_of_aging_desc_normal
            AgingLevel.Faster -> R.string.pace_of_aging_desc_faster
        }
    )

    private fun insight(level: AgingLevel, result: PaceOfAgingResult): String {
        val driver = if (level == AgingLevel.Faster) {
            result.contributions.filter { it.effect < 0f }.minByOrNull { it.effect }
        } else {
            result.contributions
                .filter { it.effect > 0f && it.signal != AgingSignal.Stress }
                .maxByOrNull { it.effect }
        } ?: return string(R.string.pace_of_aging_insight_generic)

        val signal = string(signalLabel(driver.signal)).replaceFirstChar { it.uppercase() }
        return string(
            when (level) {
                AgingLevel.Slower -> R.string.pace_of_aging_insight_slower
                AgingLevel.Normal -> R.string.pace_of_aging_insight_normal
                AgingLevel.Faster -> R.string.pace_of_aging_insight_faster
            },
            signal,
        )
    }

    @StringRes
    private fun signalLabel(signal: AgingSignal): Int = when (signal) {
        AgingSignal.Hrv -> R.string.pace_of_aging_signal_hrv
        AgingSignal.RestingHeartRate -> R.string.pace_of_aging_signal_rhr
        AgingSignal.Sleep -> R.string.pace_of_aging_signal_sleep
        AgingSignal.Recovery -> R.string.pace_of_aging_signal_recovery
        AgingSignal.Activity -> R.string.pace_of_aging_signal_activity
        AgingSignal.BodyFat -> R.string.pace_of_aging_signal_body_fat
        AgingSignal.Waist -> R.string.pace_of_aging_signal_waist
        AgingSignal.Vo2Max -> R.string.pace_of_aging_signal_vo2
        AgingSignal.Stress -> R.string.pace_of_aging_signal_stress
    }

    private fun waistTrend(latest: ScanRecord?, previous: ScanRecord?, heightCm: Float?): LongevityTrend? {
        val latestRatio = waistToHeightRatio(latest, heightCm) ?: return null
        val previousRatio = waistToHeightRatio(previous, heightCm)?.takeIf { it > 0f } ?: return null
        val deltaPercent = (latestRatio - previousRatio) / previousRatio * 100f
        return when {
            deltaPercent < -LongevityConstants.TREND_FLAT_THRESHOLD -> LongevityTrend.Improving
            deltaPercent > LongevityConstants.TREND_FLAT_THRESHOLD -> LongevityTrend.Decreasing
            else -> LongevityTrend.Stable
        }
    }

    private companion object {
        const val PACE_FORMAT = "%.2fx"
        const val NEUTRAL_FRACTION = 0.5f
        val SYNCED_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    }
}
