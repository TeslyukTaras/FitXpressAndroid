package com.hexis.bi.data.health.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import com.hexis.bi.data.telemetry.SyncTrigger
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
import com.hexis.bi.utils.constants.TelemetryWorkData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

enum class HealthSyncTrigger(
    internal val reason: String,
    internal val policy: ExistingWorkPolicy,
    internal val telemetry: SyncTrigger,
) {
    Launch("launch", ExistingWorkPolicy.KEEP, SyncTrigger.Launch),
    Foreground("foreground", ExistingWorkPolicy.KEEP, SyncTrigger.Foreground),
    SignIn("sign_in", ExistingWorkPolicy.KEEP, SyncTrigger.Launch),
    SourceConnected("source_connected", ExistingWorkPolicy.APPEND_OR_REPLACE, SyncTrigger.NewConnection),
    HealthConnectReadable(
        "health_connect_readable",
        ExistingWorkPolicy.APPEND_OR_REPLACE,
        SyncTrigger.NewConnection,
    ),
}

interface HealthSyncScheduler {
    fun enqueueHistoryBackfill(trigger: HealthSyncTrigger)

    fun cancelBackfill()

    fun schedulePeriodicSync()

    fun cancelPeriodicSync()

    fun backfillInFlight(): Flow<Boolean>
}

internal class WorkManagerHealthSyncScheduler(
    private val workManager: WorkManager,
) : HealthSyncScheduler {

    override fun enqueueHistoryBackfill(trigger: HealthSyncTrigger) {
        val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
            .setInputData(triggerData(trigger.telemetry))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                HealthSyncWorkConstants.RETRY_BACKOFF.toMillis(),
                TimeUnit.MILLISECONDS,
            )
            .addTag(HealthSyncWorkConstants.UNIQUE_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            HealthSyncWorkConstants.UNIQUE_WORK_NAME,
            trigger.policy,
            request,
        )
        Timber.d("Health history backfill enqueued (%s)", trigger.reason)
    }

    override fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            HealthSyncWorkConstants.PERIODIC_INTERVAL.toMinutes(),
            TimeUnit.MINUTES,
        )
            .setInputData(triggerData(SyncTrigger.Background))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                HealthSyncWorkConstants.RETRY_BACKOFF.toMillis(),
                TimeUnit.MILLISECONDS,
            )
            .setInitialDelay(
                HealthSyncWorkConstants.PERIODIC_INTERVAL.toMinutes(),
                TimeUnit.MINUTES,
            )
            .addTag(HealthSyncWorkConstants.PERIODIC_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HealthSyncWorkConstants.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Timber.d(
            "Health periodic sync scheduled every %d minute(s)",
            HealthSyncWorkConstants.PERIODIC_INTERVAL.toMinutes(),
        )
    }

    override fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(HealthSyncWorkConstants.PERIODIC_WORK_NAME)
        Timber.d("Health periodic sync cancelled")
    }

    override fun cancelBackfill() {
        workManager.cancelUniqueWork(HealthSyncWorkConstants.UNIQUE_WORK_NAME)
        Timber.d("Health history backfill cancelled")
    }

    private fun triggerData(trigger: SyncTrigger) =
        workDataOf(TelemetryWorkData.KEY_TRIGGER to trigger.wire)

    private fun isBackfillActive(info: WorkInfo): Boolean = when (info.state) {
        WorkInfo.State.RUNNING -> true
        WorkInfo.State.ENQUEUED -> info.runAttemptCount > 0
        else -> false
    }

    override fun backfillInFlight(): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(HealthSyncWorkConstants.UNIQUE_WORK_NAME)
            .map { infos -> infos.any(::isBackfillActive) }
            .distinctUntilChanged()
}
