package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.SavedItem
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

class PersonalSpaceRepository(
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
            ?: throw IllegalStateException("User must be signed in to access Personal Space.")
    }

    fun observeSavedItems(): Flow<List<SavedItem>> = callbackFlow {
        val uid = requireUserId()
        val path = "users/$uid/saved_items"
        val query = db.collection("users").document(uid).collection("saved_items")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(SavedItem::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: (it.timestamp / 1000) }
                trySend(items)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun addSavedItem(
        title: String,
        content: String,
        type: String, // "note", "image", "document", "link", "voice"
        url: String? = null,
        tags: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val itemId = "item_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/$uid/saved_items/$itemId"

        return try {
            val payload = mapOf(
                "id" to itemId,
                "userId" to uid,
                "title" to title.trim(),
                "content" to content.trim(),
                "type" to type,
                "url" to url,
                "tags" to tags,
                "timestamp" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("saved_items").document(itemId).set(payload).await()
            Result.success(itemId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedItem(itemId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/saved_items/$itemId"
        return try {
            db.collection("users").document(uid).collection("saved_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
