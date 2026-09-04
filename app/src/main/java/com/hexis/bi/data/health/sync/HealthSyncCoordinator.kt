package com.hexis.bi.data.health.sync

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.remote.HealthRemoteDataSource
import com.hexis.bi.data.intelligence.IntelligenceConfigRepository
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import com.hexis.bi.utils.constants.HealthAnalysisConstants
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
import com.hexis.bi.utils.constants.TerraCacheConstants
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class HealthSyncCoordinator(
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val local: HealthLocalDataSource,
    private val remote: HealthRemoteDataSource,
    private val auth: FirebaseAuth,
    private val configRepository: IntelligenceConfigRepository,
) {

    private suspend fun requiredAnalysisDays(): Int {
        val windows = configRepository.config().getOrNull()?.windows
        if (windows == null) {
            Timber.w("Engine config unreadable; prioritising %d days", HealthAnalysisConstants.FALLBACK_REQUIRED_DAYS)
            return HealthAnalysisConstants.FALLBACK_REQUIRED_DAYS
        }
        return windows.observationDays
    }

    suspend fun syncRecentWindow(today: LocalDate = LocalDate.now()): List<Throwable> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        local.pruneExpiredDaysOnStartup(uid)
        val start = today.minusDays(CanonicalCacheConstants.FORCED_REFRESH_DAYS - 1)
        return coroutineScope {
            val activity = async { activityRepository.sync(start, today).logFailure("activity") }
            val sleep = async { sleepRepository.sync(start, today).logFailure("sleep") }
            listOfNotNull(activity.await(), sleep.await())
        }
    }

    suspend fun prewarmScans(): Result<Unit> {
        if (auth.currentUser == null) return Result.success(Unit)
        return scanHistoryRepository.sync()
            .onFailure { Timber.w(it, "Scan snapshot prewarm failed; cached scans still served") }
    }

    suspend fun fillGaps(
        today: LocalDate = LocalDate.now(),
        budget: Duration = HealthSyncWorkConstants.BACKFILL_BUDGET,
        elapsed: () -> Duration,
        isActive: () -> Boolean,
    ): BackfillOutcome {
        val uid = auth.currentUser?.uid ?: return BackfillOutcome.Skipped
        local.pruneExpiredDaysOnStartup(uid)

        val identities = remote.identities().getOrElse { error ->
            Timber.w(error, "Gap fill skipped: identity lookup failed")
            return BackfillOutcome.Failed
        }
        if (identities.fetchable.isEmpty()) {
            Timber.i("Gap fill skipped: no queryable source")
            return BackfillOutcome.Skipped
        }

        val stillReadable = { isActive() && remote.fetchableIdentities(identities.all).isNotEmpty() }
        val identityIds = identities.fetchable.map { it.terraUserId }
        val retention = retentionWindow(local.retentionFloor(), today)

        val requiredDays = requiredAnalysisDays()
        val analysis = analysisWindow(today, retention, requiredDays)
        val analysisOutcome = fillWindow(
            GapFillArgs(uid, identityIds, analysis, elapsed, stillReadable),
            budget = domainDeadline(budget, elapsed(), 2),
            label = "analysis window",
        )
        Timber.i(
            "Gap fill analysis window (%d days): %s | readiness %s",
            analysis.size, analysisOutcome, readiness(uid, identityIds, today, requiredDays),
        )

        val historyOutcome = fillWindow(
            GapFillArgs(uid, identityIds, retention, elapsed, stillReadable),
            budget = budget,
            label = "history",
        )
        return worstOf(listOf(analysisOutcome, historyOutcome))
    }

    private suspend fun readiness(
        uid: String,
        identityIds: List<String>,
        today: LocalDate,
        requiredDays: Int,
    ): HealthAnalysisReadiness {
        val window = analysisWindow(today, retentionWindow(local.retentionFloor(), today), requiredDays)
        if (identityIds.isEmpty() || window.isEmpty()) return HealthAnalysisReadiness.notReady(requiredDays)
        return HealthAnalysisReadiness(
            daily = local.coverage(uid, identityIds, HealthLocalDataSource.SOURCE_DAILY, window),
            sleep = local.coverage(uid, identityIds, HealthLocalDataSource.SOURCE_SLEEP, window),
        )
    }

    private suspend fun fillWindow(
        args: GapFillArgs,
        budget: Duration,
        label: String,
    ): BackfillOutcome {
        val sleep = fillDomain(
            args,
            HealthLocalDataSource.SOURCE_SLEEP,
            domainDeadline(budget, args.elapsed(), 2),
        ) { start, end ->
            sleepRepository.sync(start, end)
        }
        val daily = fillDomain(args, HealthLocalDataSource.SOURCE_DAILY, budget) { start, end ->
            activityRepository.sync(start, end)
        }
        return worstOf(listOf(sleep, daily)).also { Timber.i("Gap fill %s: %s", label, it) }
    }

    private data class GapFillArgs(
        val uid: String,
        val identityIds: List<String>,
        val window: List<LocalDate>,
        val elapsed: () -> Duration,
        val isActive: () -> Boolean,
    )

    private suspend fun fillDomain(
        args: GapFillArgs,
        source: String,
        budget: Duration,
        sync: suspend (LocalDate, LocalDate) -> Result<Unit>,
    ): BackfillOutcome {
        val total = args.window.size
        val missing = local.staleDays(args.uid, args.identityIds, source, args.window, RANGE_TTL)
        val alreadySynced = total - missing.size
        if (missing.isEmpty()) return BackfillOutcome.Complete
        Timber.i("Gap fill %s: %d of %d days missing", source, missing.size, total)

        val outcome = fillMissingDays(
            missing = missing,
            budget = budget,
            elapsed = args.elapsed,
            isActive = args.isActive,
            onBatchFilled = { filled ->
                val synced = (alreadySynced + filled).coerceIn(0, total)
                Timber.i("Gap fill %s %d/%d days (%d%%)", source, synced, total, synced * 100 / total)
            },
            sync = sync,
        )
        Timber.i("Gap fill %s: %s", source, outcome)
        return outcome
    }

    private fun Result<Unit>.logFailure(source: String): Throwable? =
        exceptionOrNull()?.also {
            Timber.w(it, "Foreground %s sync failed; cached data still served", source)
        }

    private companion object {
        val RANGE_TTL: Duration = Duration.ofMillis(TerraCacheConstants.RANGE_CACHE_TTL_MS)
    }
}

internal fun domainDeadline(budget: Duration, elapsed: Duration, remainingDomains: Int): Duration {
    if (remainingDomains <= 1) return budget
    val remaining = budget - elapsed
    if (remaining.isNegative || remaining.isZero) return elapsed
    return elapsed + remaining.dividedBy(remainingDomains.toLong())
}

internal fun retentionWindow(floor: LocalDate, today: LocalDate): List<LocalDate> {
    if (floor.isAfter(today)) return emptyList()
    return generateSequence(floor) { it.plusDays(1).takeIf { next -> !next.isAfter(today) } }.toList()
}

internal fun analysisWindow(
    today: LocalDate,
    retention: List<LocalDate>,
    requiredDays: Int,
): List<LocalDate> {
    if (retention.isEmpty() || requiredDays <= 0) return emptyList()
    val earliest = today.minusDays(requiredDays - 1L)
    return retention.filter { !it.isBefore(earliest) && !it.isAfter(today) }
}

internal fun worstOf(outcomes: List<BackfillOutcome>): BackfillOutcome = when {
    outcomes.isEmpty() -> BackfillOutcome.Skipped
    BackfillOutcome.Failed in outcomes -> BackfillOutcome.Failed
    BackfillOutcome.Unreachable in outcomes -> BackfillOutcome.Unreachable
    BackfillOutcome.Incomplete in outcomes -> BackfillOutcome.Incomplete
    BackfillOutcome.Complete in outcomes -> BackfillOutcome.Complete
    else -> BackfillOutcome.Skipped
}

internal suspend fun fillMissingDays(
    missing: List<LocalDate>,
    budget: Duration,
    elapsed: () -> Duration,
    isActive: () -> Boolean,
    batchDays: Int = HealthSyncWorkConstants.BACKFILL_CHUNK_DAYS.toInt(),
    onBatchFilled: suspend (filled: Int) -> Unit,
    sync: suspend (LocalDate, LocalDate) -> Result<Unit>,
): BackfillOutcome {
    var filled = 0
    var failedBatches = 0
    var consecutiveFailures = 0
    var stoppedEarly = false
    var unreachable = false

    for (batch in missing.sortedDescending().chunked(batchDays)) {
        if (!isActive() || elapsed() >= budget) {
            stoppedEarly = true
            break
        }
        val error = sync(batch.min(), batch.max()).exceptionOrNull()
        if (error != null) {
            failedBatches++
            if (error is HealthSourceUnavailable) {
                unreachable = true
                break
            }
            consecutiveFailures++
            if (consecutiveFailures >= HealthAnalysisConstants.MAX_CONSECUTIVE_FAILURES) break
            continue
        }
        consecutiveFailures = 0
        filled += batch.size
        onBatchFilled(filled)
    }

    return when {
        unreachable && filled == 0 -> BackfillOutcome.Unreachable
        failedBatches > 0 && filled == 0 -> BackfillOutcome.Failed
        failedBatches > 0 || stoppedEarly -> BackfillOutcome.Incomplete
        else -> BackfillOutcome.Complete
    }
}

internal enum class BackfillOutcome {
    Complete,

    Incomplete,

    Unreachable,

    Failed,

    Skipped,
}
