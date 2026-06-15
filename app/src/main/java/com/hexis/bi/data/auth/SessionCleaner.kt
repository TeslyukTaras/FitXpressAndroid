package com.hexis.bi.data.auth

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
) {

    suspend fun signOut() {
        clearLocalData()
        authRepository.signOut()
    }

    suspend fun clearLocalData() {
        terraManagerHolder.clearLocalManager()
        TerraSdkSync.reset()
        orderDraftHolder.clear()
        scanResultRepository.clear()
        preferencesRepository.clearAccountData()
    }
}
