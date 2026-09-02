package com.hexis.bi.data.health.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
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

        runCatching { coordinator.syncRecentWindow() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Timber.w(error, "Health sync worker: recent window failed")
                return giveUpOrRetry()
            }

        coordinator.prewarmScans()

        val outcome = runCatching {
            coordinator.fillGaps(elapsed = elapsed, isActive = { !isStopped })
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            Timber.w(error, "Health sync worker: gap fill failed")
            return giveUpOrRetry()
        }

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
    }
}
