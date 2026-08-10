package com.hexis.bi.ui.main.home.intelligence

import com.hexis.bi.BuildConfig
import com.hexis.bi.domain.intelligence.RunIntelligenceUseCase
import com.hexis.bi.intelligence.engine.EngineReport
import java.util.Locale

data class EngineFindingRow(
    val finding: String,
    val weight: String,
    val confidence: String,
)

sealed interface EngineFindingsState {
    data object Hidden : EngineFindingsState

    data class Ready(val rows: List<EngineFindingRow>) : EngineFindingsState
}

data class AreaFindings(
    private val byWindow: Map<Int, EngineFindingsState> = emptyMap(),
    private val primaryWindowDays: Int = 0,
) {
    fun forWindow(windowDays: Int): EngineFindingsState =
        byWindow[windowDays] ?: EngineFindingsState.Hidden

    val primary: EngineFindingsState get() = forWindow(primaryWindowDays)
}

object EngineFindingsMapper {

    fun forArea(report: EngineReport, area: String): AreaFindings = AreaFindings(
        byWindow = report.availableWindows.associateWith { window ->
            rowsFor(report, area, window)
        },
        primaryWindowDays = report.primaryWindowDays,
    )

    private fun rowsFor(report: EngineReport, area: String, windowDays: Int): EngineFindingsState {
        val rows = report.forWindow(windowDays)?.findingsByArea?.get(area).orEmpty().map { finding ->
            EngineFindingRow(
                finding = finding.insightId,
                weight = formatWeight(report.weightOf(finding)),
                confidence = "${finding.confidence} ${formatScore(finding.confidenceScore)}",
            )
        }
        return if (rows.isEmpty()) EngineFindingsState.Hidden else EngineFindingsState.Ready(rows)
    }

    private fun formatWeight(weight: Double): String =
        if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()

    private fun formatScore(score: Double): String = String.format(Locale.US, "%.3f", score)
}

internal suspend fun RunIntelligenceUseCase.findingsFor(area: String): AreaFindings {
    if (!BuildConfig.INTELLIGENCE_ENGINE_ENABLED) return AreaFindings()
    return invoke().fold(
        onSuccess = { EngineFindingsMapper.forArea(it, area) },
        onFailure = { AreaFindings() },
    )
}
