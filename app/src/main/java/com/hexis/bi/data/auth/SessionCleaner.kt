package com.hexis.bi.data.auth

import android.content.Context
import coil3.SingletonImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.CanonicalUserCacheCleaner
import com.hexis.bi.data.health.sync.HealthSyncScheduler
import com.hexis.bi.data.order.OrderDraftHolder
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.store.PendingFirestoreCacheWipe
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.ui.avatar.ObjDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SessionCleaner(
    private val preferencesRepository: UserPreferencesRepository,
    private val orderDraftHolder: OrderDraftHolder,
    private val scanResultRepository: ScanResultRepository,
    private val terraManagerHolder: TerraManagerHolder,
    private val authRepository: AuthRepository,
    private val canonicalAggregateRepository: CanonicalUserCacheCleaner,
    private val healthSyncScheduler: HealthSyncScheduler,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context,
) {

    suspend fun signOut() {
        val outgoingUserId = firebaseAuth.currentUser?.uid
        authRepository.signOut()
        clearLocalDataFor(outgoingUserId)
    }

    suspend fun deleteAccount(delete: suspend () -> Result<Unit>): Result<Unit> {
        val outgoingUserId = firebaseAuth.currentUser?.uid
        return delete().onSuccess {
            authRepository.signOut()
            clearLocalDataFor(outgoingUserId)
            withContext(Dispatchers.IO) { PendingFirestoreCacheWipe.arm(context) }
        }
    }

    private suspend fun clearLocalDataFor(userId: String?) {
        healthSyncScheduler.cancelBackfill()
        healthSyncScheduler.cancelPeriodicSync()
        userId?.let { canonicalAggregateRepository.clearUser(it) }
        terraManagerHolder.clearSdkIdentity(context)
        TerraSdkSync.reset()
        orderDraftHolder.clear()
        scanResultRepository.clear()
        preferencesRepository.clearAccountData()
        clearCachedFiles()
        Timber.i("Local account data cleared (user=%s)", userId ?: "none")
    }

    private suspend fun clearCachedFiles() = withContext(Dispatchers.IO) {
        ObjDiskCache.clear(context.cacheDir)
        SingletonImageLoader.get(context).run {
            memoryCache?.clear()
            diskCache?.clear()
        }
    }
}
