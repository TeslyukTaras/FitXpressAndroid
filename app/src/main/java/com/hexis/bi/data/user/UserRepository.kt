package com.hexis.bi.data.user

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeUser(): Flow<UserProfile>

    /** Same Firestore session as profile; listens to `settings/userSettings` in parallel (no extra manual fetch). */
    fun observeUserSettings(): Flow<UserSettings>
    suspend fun getUser(): Result<UserProfile>
    suspend fun createUser(profile: UserProfile): Result<Unit>
    suspend fun createUserIfAbsent(profile: UserProfile): Result<Unit>
    suspend fun updateUser(profile: UserProfile): Result<Unit>
    suspend fun updateFields(fields: Map<String, Any?>): Result<Unit>
    suspend fun updateImageUrl(url: String): Result<Unit>
    suspend fun deleteUser(): Result<Unit>

    suspend fun getUserSettings(): Result<UserSettings>
    suspend fun updateUserSettings(fields: Map<String, Any?>): Result<Unit>
}
