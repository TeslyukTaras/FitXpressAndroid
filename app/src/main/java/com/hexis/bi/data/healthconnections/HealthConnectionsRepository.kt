package com.hexis.bi.data.healthconnections

import kotlinx.coroutines.flow.Flow

interface HealthConnectionsRepository {
    fun observeConnections(): Flow<List<HealthConnection>>
    suspend fun getConnections(): Result<List<HealthConnection>>
    suspend fun upsertConnection(connection: HealthConnection): Result<Unit>
    suspend fun deactivateConnection(terraUserId: String): Result<Unit>
}
