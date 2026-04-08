package com.hexis.bi.data.suit

import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.domain.suit.SuitConnectionInfo
import com.hexis.bi.domain.suit.SuitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MockSuitRepository(
    private val preferencesRepository: UserPreferencesRepository,
) : SuitRepository {

    override val connectionState: Flow<SuitConnectionInfo?> =
        preferencesRepository.connectedSuitId.map { id ->
            if (id.isNotEmpty()) SuitConnectionInfo(suitId = id) else null
        }

    override suspend fun connect(suitId: String) {
        preferencesRepository.setConnectedSuitId(suitId)
    }

    override suspend fun disconnect() {
        preferencesRepository.clearConnectedSuitId()
    }
}
