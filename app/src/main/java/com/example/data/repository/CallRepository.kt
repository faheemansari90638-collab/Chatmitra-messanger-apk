package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CallRecord
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

private const val TAG = "CallRepository"

class CallRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google to make calls.")
    }

    /**
     * Observes call records involving the user.
     */
    fun observeCalls(): Flow<List<CallRecord>> = callbackFlow {
        val uid = requireUserId()
        val path = "calls"

        val query = db.collection(path)
            .whereArrayContains("participantUids", uid)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(CallRecord::class.java)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                trySend(list)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Initiates a new WebRTC audio/video call.
     */
    suspend fun startCall(
        callerName: String,
        receiverId: String,
        receiverName: String,
        callType: String = "audio"
    ): Result<String> {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "calls/$callId"

        val callPayload = hashMapOf(
            "id" to callId,
            "callerId" to uid,
            "callerName" to callerName,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "callType" to callType,
            "status" to "ringing",
            "participantUids" to listOf(uid, receiverId),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.collection("calls").document(callId).set(callPayload).await()
            Result.success(callId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Answers an incoming call.
     */
    suspend fun answerCall(callId: String): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Rejects an incoming call.
     */
    suspend fun rejectCall(callId: String): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Ends an active call.
     */
    suspend fun endCall(callId: String, durationSeconds: Int = 0): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to "ended",
                    "durationSeconds" to durationSeconds,
                    "isScreenSharing" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Toggles screen sharing state during video/audio call.
     */
    suspend fun toggleScreenSharing(callId: String, isSharing: Boolean): Result<Unit> {
        val path = "calls/$callId"
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "isScreenSharing" to isSharing,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
