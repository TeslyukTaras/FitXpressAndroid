package com.hexis.bi.domain.intelligence

import com.hexis.bi.data.intelligence.IntelligenceConfigRepository
import com.hexis.bi.data.intelligence.IntelligenceInputProvider
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.engine.EngineReport
import com.hexis.bi.intelligence.engine.IntelligenceEngine
import com.hexis.bi.intelligence.model.EngineInput
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class RunIntelligenceUseCase(
    private val inputProvider: IntelligenceInputProvider,
    private val configRepository: IntelligenceConfigRepository,
    private val computation: CoroutineDispatcher = Dispatchers.Default,
) {

    private data class Memo(val input: EngineInput, val configVersion: String, val report: EngineReport)

    private val memo = AtomicReference<Memo>()

    suspend operator fun invoke(): Result<EngineReport> {
        val config = configRepository.config().getOrElse { return Result.failure(it) }
        val input = inputProvider.load(config.windows.analysisDays, config.windows.baselineDays)
            .getOrElse { return Result.failure(it) }
        return runReport(input, config)
    }

    private suspend fun runReport(input: EngineInput, config: EngineConfig): Result<EngineReport> {
        memo.get()?.let { cached ->
            if (cached.input == input && cached.configVersion == config.configVersion) {
                return Result.success(cached.report)
            }
        }
        val startedAt = System.currentTimeMillis()
        return runCatching {
            withContext(computation) { IntelligenceEngine.run(input, config) }
        }.onSuccess { report ->
            memo.set(Memo(input, config.configVersion, report))
            log(report, input, System.currentTimeMillis() - startedAt)
        }.onFailure { Timber.w(it, "Intelligence run failed") }
    }

    private fun log(report: EngineReport, input: EngineInput, elapsedMs: Long) {
        Timber.d(
            "Intelligence run %s in %dms: window=%dd points=%d | %d finding(s), %d suppressed | " +
                "%d/%d metrics ok, still_learning=%s",
            report.runDate, elapsedMs, report.primaryWindowDays, input.points.size,
            report.findings.size, report.suppressed.size,
            report.metricsOk, report.metricsTotal, report.stillLearning,
        )
        Timber.d(
            "  windows: %s",
            report.availableWindows.joinToString { "${it}d=${report.forWindow(it)?.findings?.size ?: 0}" },
        )
        report.verdicts.filterNot { it.ok }.forEach { verdict ->
            Timber.d("  gated %-16s %-20s %s", verdict.metric, verdict.status, verdict.reasons)
        }
        report.findings.forEach { finding ->
            Timber.d(
                "  finding #%d %-28s area=%-9s %s%s",
                finding.priorityRank, finding.insightId, finding.area, finding.direction,
                if (finding.featured) " featured" else "",
            )
        }
        report.suppressed.forEach { suppressed ->
            Timber.d("  suppressed %-28s %s", suppressed.insightId, suppressed.reason)
        }
    }
}
