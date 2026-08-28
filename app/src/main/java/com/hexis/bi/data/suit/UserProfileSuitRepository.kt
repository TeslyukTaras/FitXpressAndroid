package com.hexis.bi.data.suit

import com.hexis.bi.data.user.FirestoreSchema.UserFields
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.suit.SuitConnectionInfo
import com.hexis.bi.domain.suit.SuitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileSuitRepository(
    private val userRepository: UserRepository,
) : SuitRepository {

    override val connectionState: Flow<SuitConnectionInfo?> =
        userRepository.observeUser().map { profile ->
            profile.suitId?.trim()?.takeIf { it.isNotEmpty() }?.let { SuitConnectionInfo(suitId = it) }
        }

    override suspend fun connect(suitId: String) {
        userRepository.updateFields(mapOf(UserFields.SUIT_ID to suitId))
    }

    override suspend fun disconnect() {
        userRepository.updateFields(mapOf(UserFields.SUIT_ID to null))
    }
}
