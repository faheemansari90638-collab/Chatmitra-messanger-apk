package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.StatusUpdate
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StatusRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to manage statuses.")
    }

    /**
     * Observes real-time active statuses created in the last 24 hours.
     */
    fun observeActiveStatuses(): Flow<List<StatusUpdate>> = callbackFlow {
        val path = "statuses"
        val now = System.currentTimeMillis()
        val twentyFourHoursAgo = now - 86400000L

        val query = db.collection("statuses")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(StatusUpdate::class.java)
                    .filter { it.timestamp >= twentyFourHoursAgo }
                    .sortedByDescending { it.timestamp }
                trySend(list)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun postStatus(
        userName: String,
        userAvatarUrl: String?,
        text: String,
        mediaUrl: String? = null,
        type: String = "text",
        backgroundColorHex: String = "#12B886",
        privacy: String = "everyone"
    ): Result<String> {
        val uid = requireUserId()
        val statusId = "st_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "statuses/$statusId"

        return try {
            val payload = mapOf(
                "id" to statusId,
                "userId" to uid,
                "userName" to userName,
                "userAvatarUrl" to userAvatarUrl,
                "text" to text.trim(),
                "mediaUrl" to mediaUrl,
                "type" to type,
                "backgroundColorHex" to backgroundColorHex,
                "privacy" to privacy,
                "viewers" to emptyList<String>(),
                "viewersCount" to 0,
                "timestamp" to System.currentTimeMillis(),
                "expiresAt" to (System.currentTimeMillis() + 86400000L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("statuses").document(statusId).set(payload).await()
            Result.success(statusId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(statusId: String) {
        val uid = auth.currentUser?.uid ?: return
        val path = "statuses/$statusId"
        try {
            db.collection("statuses").document(statusId).update(
                mapOf(
                    "viewers" to FieldValue.arrayUnion(uid),
                    "viewersCount" to FieldValue.increment(1)
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        val path = "statuses/$statusId"
        return try {
            db.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
