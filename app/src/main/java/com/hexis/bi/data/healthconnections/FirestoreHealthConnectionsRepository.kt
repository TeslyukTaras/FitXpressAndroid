package com.hexis.bi.data.healthconnections

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import com.hexis.bi.R
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.FirestoreSchema.HealthConnectionFields
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreHealthConnectionsRepository(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context,
) : HealthConnectionsRepository {

    private fun collection() = run {
        val uid = firebaseAuth.currentUser?.uid
            ?: error(context.getString(R.string.error_session_expired))
        firestore
            .collection(FirestoreSchema.USERS_COLLECTION)
            .document(uid)
            .collection(FirestoreSchema.SETTINGS_COLLECTION)
            .document(FirestoreSchema.USER_SETTINGS_DOC)
            .collection(FirestoreSchema.HEALTH_CONNECTIONS_COLLECTION)
    }

    override fun observeConnections(): Flow<List<HealthConnection>> = callbackFlow {
        val registration = collection().addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val items = snap?.documents
                ?.mapNotNull { it.toObject<HealthConnection>() }
                ?: emptyList()
            trySend(items)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getConnections(): Result<List<HealthConnection>> = runCatching {
        collection().get().await().documents.mapNotNull { it.toObject<HealthConnection>() }
    }

    override suspend fun upsertConnection(connection: HealthConnection): Result<Unit> = runCatching {
        val payload = mapOf(
            HealthConnectionFields.TERRA_USER_ID to connection.terraUserId,
            HealthConnectionFields.PROVIDER to connection.provider,
            HealthConnectionFields.SOURCE to connection.source,
            HealthConnectionFields.ENVIRONMENT to connection.environment,
            HealthConnectionFields.CONNECTED_AT to (connection.connectedAt ?: Timestamp.now()),
            HealthConnectionFields.ACTIVE to connection.active,
        )
        collection().document(connection.terraUserId).set(payload, SetOptions.merge()).await()
    }

    override suspend fun deactivateConnection(terraUserId: String): Result<Unit> = runCatching {
        collection().document(terraUserId)
            .update(HealthConnectionFields.ACTIVE, false)
            .await()
    }
}
