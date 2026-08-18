package com.hexis.bi.data.health.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.hexis.bi.utils.constants.HealthSyncWorkConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

enum class HealthSyncTrigger(internal val reason: String, internal val policy: ExistingWorkPolicy) {
    AppOpen("app_open", ExistingWorkPolicy.KEEP),
    SignIn("sign_in", ExistingWorkPolicy.KEEP),
    SourceConnected("source_connected", ExistingWorkPolicy.APPEND_OR_REPLACE),
    HealthConnectReadable("health_connect_readable", ExistingWorkPolicy.APPEND_OR_REPLACE),
}

interface HealthSyncScheduler {
    fun enqueueHistoryBackfill(trigger: HealthSyncTrigger)

    fun backfillInFlight(): Flow<Boolean>
}

internal class WorkManagerHealthSyncScheduler(
    private val workManager: WorkManager,
) : HealthSyncScheduler {

    override fun enqueueHistoryBackfill(trigger: HealthSyncTrigger) {
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
            trigger.policy,
            request,
        )
        Timber.d("Health history backfill enqueued (%s)", trigger.reason)
    }

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
