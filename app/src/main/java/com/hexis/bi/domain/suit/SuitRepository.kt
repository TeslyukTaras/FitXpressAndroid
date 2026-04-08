package com.hexis.bi.domain.suit

import kotlinx.coroutines.flow.Flow

interface SuitRepository {
    val connectionState: Flow<SuitConnectionInfo?>
    suspend fun connect(suitId: String)
    suspend fun disconnect()
}
