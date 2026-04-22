package com.hexis.bi.data.user

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(): Flow<UserProfile>
    suspend fun getUser(): Result<UserProfile>
    suspend fun createUser(profile: UserProfile): Result<Unit>
    suspend fun createUserIfAbsent(profile: UserProfile): Result<Unit>
    suspend fun updateUser(profile: UserProfile): Result<Unit>
    suspend fun updateFields(fields: Map<String, Any?>): Result<Unit>
    suspend fun updateAvatarUrl(url: String): Result<Unit>
    suspend fun deleteUser(): Result<Unit>

    suspend fun getUserSettings(): Result<UserSettings>
    suspend fun updateUserSettings(fields: Map<String, Any?>): Result<Unit>
}
