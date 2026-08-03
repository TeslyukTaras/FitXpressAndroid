package com.hexis.bi.data.health.sync

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.health.remote.HealthRemoteDataSource
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
import com.hexis.bi.utils.constants.TerraCacheConstants
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class HealthSyncCoordinator(
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val local: HealthLocalDataSource,
    private val remote: HealthRemoteDataSource,
    private val auth: FirebaseAuth,
) {

    suspend fun syncRecentWindow(today: LocalDate = LocalDate.now()) {
        val uid = auth.currentUser?.uid ?: return
        local.pruneExpiredDaysOnStartup(uid)
        val start = today.minusDays(CanonicalCacheConstants.FORCED_REFRESH_DAYS - 1)
        coroutineScope {
            launch { activityRepository.sync(start, today).logFailure("activity") }
            launch { sleepRepository.sync(start, today).logFailure("sleep") }
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
        val window = retentionWindow(local.retentionFloor(), today)
        val args = GapFillArgs(uid, identities.fetchable.map { it.terraUserId }, window, elapsed, stillReadable)

        val sleep = fillDomain(args, HealthLocalDataSource.SOURCE_SLEEP, domainDeadline(budget, elapsed(), 2)) {
            start, end -> sleepRepository.sync(start, end)
        }
        val daily = fillDomain(args, HealthLocalDataSource.SOURCE_DAILY, domainDeadline(budget, elapsed(), 1)) {
            start, end -> activityRepository.sync(start, end)
        }

        val outcomes = listOf(sleep, daily)
        return when {
            BackfillOutcome.Failed in outcomes -> BackfillOutcome.Failed
            BackfillOutcome.Incomplete in outcomes -> BackfillOutcome.Incomplete
            else -> BackfillOutcome.Complete
        }
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

    private fun Result<Unit>.logFailure(source: String) {
        exceptionOrNull()?.let { Timber.w(it, "Foreground %s sync failed; cached data still served", source) }
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
    for (batch in missing.sortedDescending().chunked(batchDays)) {
        if (!isActive() || elapsed() >= budget) return BackfillOutcome.Incomplete
        if (sync(batch.min(), batch.max()).isFailure) return BackfillOutcome.Failed
        filled += batch.size
        onBatchFilled(filled)
    }
    return BackfillOutcome.Complete
}

internal enum class BackfillOutcome {
    Complete,

    Incomplete,

    Failed,

    Skipped,
}
