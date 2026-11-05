package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Conversation(
    val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val participantUids: List<String> = emptyList(),
    val groupTitle: String? = null,
    val groupDescription: String? = null,
    val groupAdminUid: String? = null,
    val groupAvatarUrl: String? = null,
    val lastMessageText: String? = null,
    val lastMessageSenderId: String? = null,
    val lastMessageTimestamp: Timestamp? = null,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val unreadCount: Int = 0,
    val disappearingDurationSeconds: Int? = null, // e.g. 86400 (24h), 604800 (7d), 7776000 (90d)
    val chatFolder: String = "All",
    val isLocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
