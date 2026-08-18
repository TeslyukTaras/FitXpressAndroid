package com.hexis.bi.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.data.health.local.CanonicalUserCacheCleaner
import com.hexis.bi.data.order.OrderDraftHolder
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.scan.ScanResultRepository
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.terra.TerraSdkSync

class SessionCleaner(
    private val preferencesRepository: UserPreferencesRepository,
    private val orderDraftHolder: OrderDraftHolder,
    private val scanResultRepository: ScanResultRepository,
    private val terraManagerHolder: TerraManagerHolder,
    private val authRepository: AuthRepository,
    private val canonicalAggregateRepository: CanonicalUserCacheCleaner,
    private val firebaseAuth: FirebaseAuth,
) {

    suspend fun signOut() {
        val outgoingUserId = firebaseAuth.currentUser?.uid
        authRepository.signOut()
        clearLocalDataFor(outgoingUserId)
    }

    suspend fun deleteAccount(delete: suspend () -> Result<Unit>): Result<Unit> {
        val outgoingUserId = firebaseAuth.currentUser?.uid
        return delete().onSuccess { clearLocalDataFor(outgoingUserId) }
    }

    private suspend fun clearLocalDataFor(userId: String?) {
        userId?.let { canonicalAggregateRepository.clearUser(it) }
        terraManagerHolder.clearLocalManager()
        TerraSdkSync.reset()
        orderDraftHolder.clear()
        scanResultRepository.clear()
        preferencesRepository.clearAccountData()
    }
}
