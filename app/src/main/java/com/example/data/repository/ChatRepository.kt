package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.ChatDao
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "ChatRepository"

class ChatRepository(
    private val db: FirebaseFirestore,
    private val chatDao: ChatDao? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        chatDao = null
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing chats.")
    }

    /**
     * Observes all active conversations the current user participates in.
     */
    fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        val uid = requireUserId()
        val path = "conversations"

        val query = db.collection(path)
            .whereArrayContains("participantUids", uid)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(Conversation::class.java)
                    .sortedByDescending { it.lastMessageTimestamp?.seconds ?: it.createdAt?.seconds ?: 0L }
                trySend(list)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Finds an existing direct conversation between two users or creates a new one.
     */
    suspend fun getOrCreateDirectConversation(
        currentUid: String,
        otherUid: String,
        otherDisplayName: String
    ): Result<String> {
        val path = "conversations"
        return try {
            val querySnapshot = db.collection(path)
                .whereArrayContains("participantUids", currentUid)
                .get()
                .await()

            val existing = querySnapshot.toObjects(Conversation::class.java).firstOrNull { conv ->
                conv.type == "direct" && conv.participantUids.contains(otherUid)
            }

            if (existing != null) {
                Result.success(existing.id)
            } else {
                val newId = "conv_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val newConv = hashMapOf(
                    "id" to newId,
                    "type" to "direct",
                    "participantUids" to listOf(currentUid, otherUid).sorted(),
                    "groupTitle" to otherDisplayName,
                    "lastMessageText" to "Conversation started",
                    "lastMessageSenderId" to currentUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection(path).document(newId).set(newConv).await()
                Result.success(newId)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Creates a new group conversation.
     */
    suspend fun createGroupConversation(
        adminUid: String,
        groupTitle: String,
        groupDescription: String,
        memberUids: List<String>
    ): Result<String> {
        val path = "conversations"
        val newId = "group_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val allParticipants = (memberUids + adminUid).distinct()

        val payload = hashMapOf(
            "id" to newId,
            "type" to "group",
            "participantUids" to allParticipants,
            "groupTitle" to groupTitle.trim(),
            "groupDescription" to groupDescription.trim(),
            "groupAdminUid" to adminUid,
            "lastMessageText" to "Group created",
            "lastMessageSenderId" to adminUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.collection(path).document(newId).set(payload).await()
            Result.success(newId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Realtime observation of messages in a conversation.
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val path = "conversations/$conversationId/messages"
        val query = db.collection("conversations")
            .document(conversationId)
            .collection("messages")

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(Message::class.java)
                    .sortedBy { it.createdAt?.seconds ?: 0L }
                trySend(list)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Sends a new message in a conversation.
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        senderName: String,
        participantUids: List<String>,
        type: String = "text",
        mediaUrl: String? = null,
        mediaName: String? = null,
        mediaSize: String? = null,
        replyToMessageId: String? = null,
        replyToText: String? = null,
        pollData: String? = null,
        locationData: String? = null,
        eventData: String? = null,
        scheduledTime: Timestamp? = null,
        isScheduled: Boolean = false,
        disappearingDurationSeconds: Int? = null
    ): Result<String> {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "conversations/$conversationId/messages/$messageId"

        val messageData = mutableMapOf<String, Any>(
            "id" to messageId,
            "conversationId" to conversationId,
            "senderId" to uid,
            "senderName" to senderName,
            "content" to content.trim(),
            "type" to type,
            "status" to if (isScheduled) "scheduled" else "sent",
            "participantUids" to participantUids,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (mediaUrl != null) messageData["mediaUrl"] = mediaUrl
        if (mediaName != null) messageData["mediaName"] = mediaName
        if (mediaSize != null) messageData["mediaSize"] = mediaSize
        if (replyToMessageId != null) messageData["replyToMessageId"] = replyToMessageId
        if (replyToText != null) messageData["replyToText"] = replyToText
        if (pollData != null) messageData["pollData"] = pollData
        if (locationData != null) messageData["locationData"] = locationData
        if (eventData != null) messageData["eventData"] = eventData
        if (scheduledTime != null) messageData["scheduledTime"] = scheduledTime
        if (isScheduled) messageData["isScheduled"] = true
        if (disappearingDurationSeconds != null) {
            messageData["disappearingDurationSeconds"] = disappearingDurationSeconds
            val expiresAtSeconds = (System.currentTimeMillis() / 1000) + disappearingDurationSeconds
            messageData["expiresAt"] = Timestamp(expiresAtSeconds, 0)
        }

        return try {
            val batch = db.batch()
            val msgRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
            batch.set(msgRef, messageData)

            val convRef = db.collection("conversations").document(conversationId)
            val previewText = when (type) {
                "image" -> "📷 Photo"
                "video" -> "📹 Video"
                "document" -> "📄 Document"
                "audio", "voice" -> "🎤 Voice message"
                "poll" -> "📊 Poll: ${content.take(40)}"
                "location" -> "📍 Live Location"
                "event" -> "📅 Event: ${content.take(40)}"
                else -> content.take(60)
            }

            batch.update(
                convRef,
                mapOf(
                    "lastMessageText" to previewText,
                    "lastMessageSenderId" to uid,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing message content (Edit).
     */
    suspend fun editMessage(conversationId: String, messageId: String, newContent: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "content" to newContent.trim(),
                        "isEdited" to true,
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
     * Delete message for everyone.
     */
    suspend fun deleteMessageForEveryone(conversationId: String, messageId: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "content" to "This message was deleted",
                        "isDeleted" to true,
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
     * Toggle starred message.
     */
    suspend fun toggleStarMessage(conversationId: String, messageId: String, isStarred: Boolean): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "isStarred" to isStarred,
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
     * Toggle pinned message.
     */
    suspend fun togglePinMessage(conversationId: String, messageId: String, isPinned: Boolean): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "isPinned" to isPinned,
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
     * Mark message as read.
     */
    suspend fun markMessageRead(conversationId: String, messageId: String) {
        val path = "conversations/$conversationId/messages/$messageId"
        try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "status" to "read",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark message read: ${e.message}")
        }
    }

    suspend fun updatePollData(conversationId: String, messageId: String, pollDataJson: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "pollData" to pollDataJson,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateLocationData(conversationId: String, messageId: String, locationDataJson: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "locationData" to locationDataJson,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateEventData(conversationId: String, messageId: String, eventDataJson: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "eventData" to eventDataJson,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDisappearingTimer(conversationId: String, durationSeconds: Int?): Result<Unit> {
        val path = "conversations/$conversationId"
        return try {
            val updatePayload = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (durationSeconds != null) {
                updatePayload["disappearingDurationSeconds"] = durationSeconds
            } else {
                updatePayload["disappearingDurationSeconds"] = FieldValue.delete()
            }
            db.collection("conversations").document(conversationId).update(updatePayload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChatFolder(conversationId: String, folder: String): Result<Unit> {
        val path = "conversations/$conversationId"
        return try {
            db.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "chatFolder" to folder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChatLock(conversationId: String, isLocked: Boolean): Result<Unit> {
        val path = "conversations/$conversationId"
        return try {
            db.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "isLocked" to isLocked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMessageTranslation(conversationId: String, messageId: String, translatedText: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "translatedText" to translatedText,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMessageTranscription(conversationId: String, messageId: String, transcribedText: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            db.collection("conversations").document(conversationId).collection("messages").document(messageId)
                .update(
                    mapOf(
                        "transcribedText" to transcribedText,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(conversationId: String, messageId: String, userId: String, emoji: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            val docRef = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
            val snapshot = docRef.get().await()
            val message = snapshot.toObject(Message::class.java)
            val currentEmoji = message?.reactions?.get(userId)
            if (currentEmoji == emoji) {
                docRef.update(
                    mapOf(
                        "reactions.$userId" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "reactions.$userId" to emoji,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
