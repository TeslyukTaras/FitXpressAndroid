package com.hexis.bi.ui.main.home.physiquedrift

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.scan.ScanRecord
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.body.PhysiqueScoreBreakdown
import com.hexis.bi.domain.body.muscleMassPercentage
import com.hexis.bi.domain.body.physiqueScoreBreakdown
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.hexis.bi.utils.millisToShortMonthDayYear
import java.util.Locale
import kotlin.math.roundToInt

class PhysiqueDriftViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val userRepository: UserRepository,
) : BaseViewModel(application, initialLoading = true) {

    private val _state = MutableStateFlow(PhysiqueDriftState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun showInfoSheet() = _state.update { it.copy(showInfoSheet = true) }

    fun dismissInfoSheet() = _state.update { it.copy(showInfoSheet = false) }

    private fun load() {
        viewModelScope.launch {
            setLoading(true)
            renderFromRepositories()
            setLoading(false)
        }
    }

    private suspend fun renderFromRepositories() = coroutineScope {
        val scansDef = async { scanHistoryRepository.getRecentScans(limit = 2).getOrNull().orEmpty() }
        val heightDef = async { userRepository.getUser().getOrNull()?.heightCm?.toFloat() }

        val scans = scansDef.await()
        val heightCm = heightDef.await()
        val latest = scans.getOrNull(0)
        val previous = scans.getOrNull(1)
        val breakdown = latest?.physiqueScoreBreakdown(heightCm)

        if (latest == null || breakdown == null) {
            _state.update {
                it.copy(
                    hasData = false,
                    score = string(R.string.stat_unknown),
                    level = PhysiqueDriftLevel.NoData,
                    description = string(R.string.physique_drift_no_data),
                    scanDate = "",
                    bodyFat = PhysiqueMetric(string(R.string.stat_unknown)),
                    leanBody = PhysiqueMetric(string(R.string.stat_unknown)),
                    waistProfile = string(R.string.stat_unknown),
                    proportions = string(R.string.stat_unknown),
                    symmetry = string(R.string.stat_unknown),
                    shoulderToWaistRatio = string(R.string.stat_unknown),
                    insight = string(R.string.physique_drift_insight_no_data),
                )
            }
            return@coroutineScope
        }

        val previousBreakdown = previous?.physiqueScoreBreakdown(heightCm)
        val level = levelFor(breakdown.score)
        _state.update {
            it.copy(
                hasData = true,
                score = String.format(Locale.US, SCORE_FORMAT, breakdown.score),
                level = level,
                description = string(descriptionFor(level)),
                scanDate = formatScanDate(latest),
                bodyFat = bodyFatMetric(breakdown, previousBreakdown),
                leanBody = leanBodyMetric(latest, previous),
                waistProfile = waistProfile(breakdown.waistShapeScore),
                proportions = proportions(breakdown.proportionScore),
                symmetry = symmetry(breakdown.proportionScore),
                shoulderToWaistRatio = breakdown.shoulderToWaistRatio
                    ?.let { ratio -> String.format(Locale.US, RATIO_FORMAT, ratio) }
                    ?: string(R.string.stat_unknown),
                insight = insightFor(level),
            )
        }
    }

    private fun bodyFatMetric(
        latest: PhysiqueScoreBreakdown,
        previous: PhysiqueScoreBreakdown?,
    ): PhysiqueMetric {
        val value = latest.bodyFatPercent?.let { String.format(Locale.US, PERCENT_FORMAT, it) }
            ?: string(R.string.stat_unknown)
        val delta = deltaPercent(latest.bodyFatPercent, previous?.bodyFatPercent)
        return PhysiqueMetric(
            value = value,
            delta = delta,
        )
    }

    private fun leanBodyMetric(latest: ScanRecord, previous: ScanRecord?): PhysiqueMetric {
        val lean = latest.muscleMassPercentage()
        val value = lean?.let { String.format(Locale.US, PERCENT_FORMAT, it) }
            ?: string(R.string.stat_unknown)
        val delta = deltaPercent(lean, previous?.muscleMassPercentage())
        return PhysiqueMetric(
            value = value,
            delta = delta,
        )
    }

    private fun waistProfile(score: Float?): String = when {
        score == null -> string(R.string.stat_unknown)
        score >= HIGH_COMPONENT_SCORE -> string(R.string.physique_drift_improving)
        score >= MID_COMPONENT_SCORE -> string(R.string.physique_drift_balanced)
        else -> string(R.string.physique_drift_needs_work)
    }

    private fun proportions(score: Float?): String = when {
        score == null -> string(R.string.stat_unknown)
        score >= HIGH_COMPONENT_SCORE -> string(R.string.physique_drift_well_balanced)
        score >= MID_COMPONENT_SCORE -> string(R.string.physique_drift_balanced)
        else -> string(R.string.physique_drift_developing)
    }

    private fun symmetry(score: Float?): String =
        score?.let { "${(it / MAX_SCORE * 100f).roundToInt()}%" } ?: string(R.string.stat_unknown)

    private fun levelFor(score: Float): PhysiqueDriftLevel = when {
        score >= 8f -> PhysiqueDriftLevel.Athletic
        score >= 5f -> PhysiqueDriftLevel.Improving
        else -> PhysiqueDriftLevel.NeedsWork
    }

    private fun descriptionFor(level: PhysiqueDriftLevel): Int = when (level) {
        PhysiqueDriftLevel.Athletic -> R.string.physique_drift_level_athletic_desc
        PhysiqueDriftLevel.Improving -> R.string.physique_drift_level_improving_desc
        PhysiqueDriftLevel.NeedsWork -> R.string.physique_drift_level_needs_work_desc
        PhysiqueDriftLevel.NoData -> R.string.physique_drift_no_data
    }

    private fun insightFor(level: PhysiqueDriftLevel): String = string(
        when (level) {
            PhysiqueDriftLevel.Athletic -> R.string.physique_drift_insight_athletic
            PhysiqueDriftLevel.Improving -> R.string.physique_drift_insight_improving
            PhysiqueDriftLevel.NeedsWork -> R.string.physique_drift_insight_needs_work
            PhysiqueDriftLevel.NoData -> R.string.physique_drift_insight_no_data
        }
    )

    private fun formatScanDate(scan: ScanRecord): String =
        scan.timestamp.millisToShortMonthDayYear()

    private fun deltaPercent(latest: Float?, previous: Float?): Float? {
        latest ?: return null
        previous ?: return null
        return latest - previous
    }

    private companion object {
        const val SCORE_FORMAT = "%.1f"
        const val PERCENT_FORMAT = "%.1f%%"
        const val RATIO_FORMAT = "%.2f"
        const val HIGH_COMPONENT_SCORE = 7.5f
        const val MID_COMPONENT_SCORE = 5f
        const val MAX_SCORE = 10f
    }
}
