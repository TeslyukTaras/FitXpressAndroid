package com.hexis.bi.data.health.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

interface HealthSyncScheduler {
    fun enqueueHistoryBackfill(reason: String)

    fun backfillInFlight(): Flow<Boolean>
}

internal class WorkManagerHealthSyncScheduler(
    private val workManager: WorkManager,
) : HealthSyncScheduler {

    override fun enqueueHistoryBackfill(reason: String) {
        val request = OneTimeWorkRequestBuilder<HealthSyncWorker>()
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
            ExistingWorkPolicy.KEEP,
            request,
        )
        Timber.d("Health history backfill enqueued (%s)", reason)
    }

    override fun backfillInFlight(): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(HealthSyncWorkConstants.UNIQUE_WORK_NAME)
            .map { infos -> infos.any { !it.state.isFinished } }
            .distinctUntilChanged()
}
