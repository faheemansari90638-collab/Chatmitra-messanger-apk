package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val type: String = "text", // "text", "image", "video", "document", "audio", "voice"
    val mediaUrl: String? = null,
    val mediaName: String? = null,
    val mediaSize: String? = null,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val status: String = "sent", // "sent", "delivered", "read"
    val isPinned: Boolean = false,
    val isStarred: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val reactions: Map<String, String> = emptyMap(), // uid -> emoji
    val participantUids: List<String> = emptyList(),
    val pollData: String? = null, // JSON string for Poll
    val locationData: String? = null, // JSON string for Live Location
    val eventData: String? = null, // JSON string for Group Event
    val scheduledTime: Timestamp? = null,
    val isScheduled: Boolean = false,
    val disappearingDurationSeconds: Int? = null,
    val expiresAt: Timestamp? = null,
    val translatedText: String? = null,
    val transcribedText: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
