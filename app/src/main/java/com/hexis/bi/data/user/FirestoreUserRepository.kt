package com.hexis.bi.data.user

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.hexis.bi.R
import com.hexis.bi.data.user.FirestoreSchema.UserFields
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context,
) : UserRepository {

    private val collection get() = firestore.collection(FirestoreSchema.USERS_COLLECTION)

    override fun observeUser(): Flow<UserProfile> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid
            ?: error(context.getString(R.string.error_session_expired))
        val listener = collection.document(uid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            snap?.toObject<UserProfile>()?.let { trySend(it) }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getUser(): Result<UserProfile> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: error(context.getString(R.string.error_session_expired))
        collection.document(uid).get().await().toObject<UserProfile>()
            ?: error(context.getString(R.string.error_profile_not_found))
    }

    override suspend fun createUser(profile: UserProfile): Result<Unit> = runCatching {
        collection.document(profile.uid).set(profile).await()
    }

    override suspend fun createUserIfAbsent(profile: UserProfile): Result<Unit> = runCatching {
        val docRef = collection.document(profile.uid)
        if (!docRef.get().await().exists()) {
            docRef.set(profile).await()
        }
    }

    override suspend fun updateUser(profile: UserProfile): Result<Unit> = runCatching {
        collection.document(profile.uid).set(profile, SetOptions.merge()).await()
    }

    override suspend fun updateAvatarUrl(url: String): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: error(context.getString(R.string.error_session_expired))
        collection.document(uid).update(UserFields.AVATAR_URL, url).await()
    }

    override suspend fun deleteUser(): Result<Unit> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: error(context.getString(R.string.error_session_expired))
        collection.document(uid).delete().await()
    }
}
