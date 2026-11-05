package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CallRecord(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val callType: String = "audio", // "audio" or "video"
    val status: String = "ringing", // "ringing", "accepted", "rejected", "ended", "missed"
    val participantUids: List<String> = emptyList(),
    val durationSeconds: Int? = null,
    val isScreenSharing: Boolean = false,
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
