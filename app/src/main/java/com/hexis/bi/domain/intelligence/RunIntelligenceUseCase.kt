package com.hexis.bi.domain.intelligence

import com.hexis.bi.BuildConfig
import com.hexis.bi.data.intelligence.IntelligenceConfigRepository
import com.hexis.bi.data.intelligence.IntelligenceWordingRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.intelligence.config.CopyConfig
import com.hexis.bi.data.intelligence.IntelligenceInputProvider
import com.hexis.bi.intelligence.config.EngineConfig
import com.hexis.bi.intelligence.engine.Domains
import com.hexis.bi.intelligence.engine.EngineReport
import com.hexis.bi.intelligence.engine.IntelligenceEngine
import com.hexis.bi.intelligence.model.EngineInput
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

internal data class IntelligenceRun(
    val report: EngineReport,
    val config: EngineConfig,
    val copy: CopyConfig,
    val isMetric: Boolean = true,
)

internal data class IntelligenceRunUpdate(
    val userId: String,
    val run: IntelligenceRun,
)

internal class RunIntelligenceUseCase(
    private val inputProvider: IntelligenceInputProvider,
    private val userRepository: UserRepository,
    private val configRepository: IntelligenceConfigRepository,
    private val wordingRepository: IntelligenceWordingRepository,
    private val computation: CoroutineDispatcher = Dispatchers.Default,
) {

    private data class Memo(val input: EngineInput, val config: EngineConfig, val report: EngineReport)

    private val memo = AtomicReference<Memo>()

    private val runLock = Mutex()
    private val _updates = MutableSharedFlow<IntelligenceRunUpdate>(extraBufferCapacity = 1)
    val updates = _updates.asSharedFlow()

    suspend operator fun invoke(): Result<IntelligenceRun> {
        val loadedConfig = configRepository.config().getOrElse { return Result.failure(it) }
        val config = loadedConfig.withLocalActivityPersistenceOverride()
        val historyDays = config.windows.trendDays.maxOrNull() ?: config.windows.analysisDays
        val input = inputProvider.load(
            analysisDays = config.windows.analysisDays,
            baselineDays = config.windows.baselineDays,
            historyDays = historyDays,
        )
            .getOrElse { return Result.failure(it) }
        val copy = wordingRepository.config().map { it.copy }.getOrElse {
            Timber.w(it, "Insight wording unavailable; findings render without sentences")
            CopyConfig()
        }
        val isMetric = userRepository.getUser().getOrNull()?.unitSystem?.isMetricUnitSystem() ?: true
        val result = runReport(input, config.withHeight(input.heightCm), copy, isMetric)
        result.getOrNull()?.let { run ->
            userRepository.getUser().getOrNull()?.uid?.takeIf { it.isNotEmpty() }?.let { uid ->
                _updates.tryEmit(IntelligenceRunUpdate(uid, run))
            }
        }
        return result
    }

    private fun EngineConfig.withHeight(heightCm: Double?): EngineConfig {
        if (heightCm == null) {
            Timber.w("User profile has no height; height-dependent signals are omitted")
            return this
        }
        return copy(composites = composites.copy(heightCm = heightCm))
    }

    private suspend fun runReport(
        input: EngineInput,
        config: EngineConfig,
        copy: CopyConfig,
        isMetric: Boolean,
    ): Result<IntelligenceRun> = runLock.withLock {
        memo.get()?.let { cached ->
            if (cached.input == input && cached.config == config) {
                return Result.success(IntelligenceRun(cached.report, cached.config, copy, isMetric))
            }
        }
        val startedAt = System.currentTimeMillis()
        runCatching {
            withContext(computation) {
                IntelligenceEngine.run(input, config)
                    .also { log(it, input, System.currentTimeMillis() - startedAt) }
            }
        }.onSuccess { report -> memo.set(Memo(input, config, report)) }
            .onFailure { Timber.w(it, "Intelligence run failed") }
            .map { IntelligenceRun(it, config, copy, isMetric) }
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
        if (BuildConfig.DEBUG) {
            report.availableWindows.forEach { windowDays ->
                report.forWindow(windowDays)?.findings.orEmpty().forEach { finding ->
                    Timber.d(
                        "  %3dd #%d %-28s %-8s %.3f",
                        windowDays,
                        finding.priorityRank,
                        finding.insightId,
                        finding.confidence,
                        finding.confidenceScore,
                    )
                }
            }
        }
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

/**
 * Temporary Android-side parity override. Remove this when the shared engine config changes
 * activity's min_persist_days from 14 to 7.
 *
 * Keep this after config parsing/validation so the bundled and Remote Config documents remain
 * untouched, and pass the resulting config through the entire run so execution, memoization, and
 * debug UI all describe the same effective rules.
 */
internal fun EngineConfig.withLocalActivityPersistenceOverride(): EngineConfig {
    val configuredDays = trend.minPersistDaysFor(Domains.ACTIVITY)
    if (configuredDays == LOCAL_ACTIVITY_MIN_PERSIST_DAYS) return this

    Timber.i(
        "Applying local activity min_persist_days override: %d -> %d",
        configuredDays,
        LOCAL_ACTIVITY_MIN_PERSIST_DAYS,
    )
    return copy(
        trend = trend.copy(
            minPersistDays = trend.minPersistDays +
                (Domains.ACTIVITY to LOCAL_ACTIVITY_MIN_PERSIST_DAYS),
        ),
    )
}

private const val LOCAL_ACTIVITY_MIN_PERSIST_DAYS = 7
