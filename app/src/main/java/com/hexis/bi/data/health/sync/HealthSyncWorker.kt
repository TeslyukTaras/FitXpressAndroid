package com.hexis.bi.data.health.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.telemetry.SyncOutcome
import com.hexis.bi.data.telemetry.SyncStage
import com.hexis.bi.data.telemetry.SyncTrigger
import com.hexis.bi.data.telemetry.Telemetry
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
import com.hexis.bi.utils.constants.TelemetryWorkData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Duration

internal class HealthSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val coordinator: HealthSyncCoordinator by inject()
    private val auth: FirebaseAuth by inject()
    private val local: HealthLocalDataSource by inject()
    private val telemetry: Telemetry by inject()

    private val trigger: SyncTrigger
        get() = SyncTrigger.ofWire(inputData.getString(TelemetryWorkData.KEY_TRIGGER))

    override suspend fun doWork(): Result {
        if (auth.currentUser == null) {
            Timber.d("Health sync worker: signed out, nothing to do")
            return Result.success()
        }
        if (!syncGate.tryLock()) {
            Timber.d("Health sync worker: a sync is already running, deferring this run")
            return giveUpOrRetry()
        }
        return try {
            sync()
        } finally {
            syncGate.unlock()
        }
    }

    private suspend fun sync(): Result {
        val startedAt = System.nanoTime()
        val elapsed = { Duration.ofNanos(System.nanoTime() - startedAt) }
        val syncTrigger = trigger
        var failedOperations = 0

        val quickSyncFailures = runCatching { coordinator.syncRecentWindow() }
            .getOrElse { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Health sync worker: recent window failed")
                telemetry.healthSyncFailed(syncTrigger, SyncStage.QuickSyncSource, error)
                report(syncTrigger, elapsed(), SyncOutcome.Failed, QUICK_SYNC_SOURCES)
                return giveUpOrRetry()
            }
        quickSyncFailures.forEach { telemetry.healthSyncFailed(syncTrigger, SyncStage.QuickSyncSource, it) }
        failedOperations += quickSyncFailures.size

        coordinator.prewarmScans().onFailure { error ->
            telemetry.healthSyncFailed(syncTrigger, SyncStage.ScanSync, error)
            failedOperations++
        }

        val outcome = runCatching {
            coordinator.fillGaps(elapsed = elapsed, isActive = { !isStopped })
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            Timber.w(error, "Health sync worker: gap fill failed")
            telemetry.healthSyncFailed(syncTrigger, SyncStage.HistoricalBackfill, error)
            report(syncTrigger, elapsed(), SyncOutcome.Failed, failedOperations + 1)
            return giveUpOrRetry()
        }

        if (outcome == BackfillOutcome.Failed || outcome == BackfillOutcome.Unreachable) {
            telemetry.healthSyncFailed(syncTrigger, SyncStage.HistoricalBackfill, backfillFailure(outcome))
            failedOperations++
        }
        report(syncTrigger, elapsed(), outcomeOf(outcome, failedOperations), failedOperations)

        auth.currentUser?.uid?.let { local.logStats(it) }
        Timber.i("Health sync finished: %s after %ds", outcome, elapsed().seconds)
        return when (outcome) {
            BackfillOutcome.Unreachable -> {
                Timber.i("Health sync: source unreachable; resumes on next app open")
                Result.success()
            }
            BackfillOutcome.Failed, BackfillOutcome.Incomplete -> giveUpOrRetry(outcome)
            BackfillOutcome.Complete, BackfillOutcome.Skipped -> Result.success()
        }
    }

    private fun report(
        trigger: SyncTrigger,
        elapsed: Duration,
        outcome: SyncOutcome,
        failedOperations: Int,
    ) = telemetry.healthSyncCompleted(trigger, elapsed.toMillis(), outcome, failedOperations)

    private fun outcomeOf(backfill: BackfillOutcome, failedOperations: Int): SyncOutcome = when {
        failedOperations >= TOTAL_OPERATIONS -> SyncOutcome.Failed
        failedOperations > 0 -> SyncOutcome.Partial
        backfill == BackfillOutcome.Incomplete -> SyncOutcome.Partial
        else -> SyncOutcome.Complete
    }

    private fun backfillFailure(outcome: BackfillOutcome): Throwable =
        if (outcome == BackfillOutcome.Unreachable) {
            HealthSourceUnavailable(HealthBackfillFailed())
        } else {
            HealthBackfillFailed()
        }

    private fun giveUpOrRetry(outcome: BackfillOutcome? = null): Result {
        val attempts = runAttemptCount + 1
        if (attempts >= HealthSyncWorkConstants.MAX_RUN_ATTEMPTS) {
            Timber.w(
                "Health sync stopping after %d runs (%s); resumes on next app open",
                attempts, outcome ?: "error",
            )
            return Result.success()
        }
        return Result.retry()
    }

    private companion object {
        val syncGate = Mutex()

        const val QUICK_SYNC_SOURCES = 2

        const val TOTAL_OPERATIONS = QUICK_SYNC_SOURCES + 2
    }
}
