package com.hexis.bi.data.health.sync

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.health.local.HealthLocalDataSource
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.utils.constants.CanonicalCacheConstants
import java.time.LocalDate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class HealthSyncCoordinator(
    private val activityRepository: ActivityRepository,
    private val sleepRepository: SleepRepository,
    private val local: HealthLocalDataSource,
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

    private fun Result<Unit>.logFailure(source: String) {
        exceptionOrNull()?.let { Timber.w(it, "Foreground %s sync failed; cached data still served", source) }
    }
}
