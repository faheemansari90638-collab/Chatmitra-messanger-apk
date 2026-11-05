package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class Contact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val isChatMitraUser: Boolean = false,
    val chatMitraId: String? = null,
    val avatarUrl: String? = null,
    val about: String? = null,
    val isBlocked: Boolean = false
)

@IgnoreExtraProperties
data class StatusUpdate(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val text: String = "",
    val mediaUrl: String? = null,
    val type: String = "text", // "text", "photo", "video"
    val backgroundColorHex: String = "#12B886",
    val privacy: String = "everyone", // "everyone", "contacts", "private"
    val viewers: List<String> = emptyList(), // list of viewer UIDs
    val viewersCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L, // 24 hours
    @ServerTimestamp val createdAt: Timestamp? = null
)

// Group Poll Models
data class PollOption(
    val id: Int = 0,
    val text: String = "",
    val voterUids: List<String> = emptyList()
)

data class PollData(
    val question: String = "",
    val options: List<PollOption> = emptyList(),
    val allowMultiple: Boolean = false,
    val isClosed: Boolean = false,
    val closedAt: Long? = null
)

// Live Location Models
data class LiveLocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val durationMinutes: Int = 60, // 15, 60, 480
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 3600000L,
    val isSharing: Boolean = true
)

// Group Event Models
data class GroupEventData(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val eventTimestamp: Long = System.currentTimeMillis() + 86400000L,
    val location: String = "",
    val goingUids: List<String> = emptyList(),
    val maybeUids: List<String> = emptyList(),
    val declineUids: List<String> = emptyList()
)

// Personal Space (Saved Messages & Notes)
@IgnoreExtraProperties
data class SavedItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "note", // "note", "image", "document", "link", "voice"
    val url: String? = null,
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AppSettings(
    val isDarkMode: Boolean = false,
    val useSystemTheme: Boolean = true,
    val isAppLockEnabled: Boolean = false,
    val lockTimeout: String = "immediately", // "immediately", "30s", "1m", "5m"
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
    val lastSeenVisibility: String = "everyone", // "everyone", "contacts", "nobody"
    val profilePhotoVisibility: String = "everyone",
    val readReceiptsEnabled: Boolean = true,
    val fontSize: String = "medium", // "small", "medium", "large"
    val chatWallpaper: String = "default",
    // Smart Data Saver settings
    val mediaAutoDownloadWifi: Boolean = true,
    val mediaAutoDownloadMobile: Boolean = false,
    val lowDataUsageCalls: Boolean = false,
    val imageUploadQuality: String = "standard", // "data_saver", "standard", "hd"
    // Advanced Privacy
    val hideSensitiveNotifications: Boolean = false,
    val blockUnknownSenders: Boolean = false,
    val screenshotProtection: Boolean = false,
    val customChatFolders: List<String> = listOf("All", "Friends", "Family", "Work", "Groups", "Important")
)
