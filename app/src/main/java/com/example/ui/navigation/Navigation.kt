package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CHATS("chats", "Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    CALLS("calls", "Calls", Icons.Filled.Call, Icons.Outlined.Call),
    CONTACTS("contacts", "Contacts", Icons.Filled.Contacts, Icons.Outlined.Contacts),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Auth : Screen("auth")
    data object ProfileSetup : Screen("profile_setup")
    data object ChatDetail : Screen("chat_detail/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_detail/$conversationId"
    }
    data object NewGroup : Screen("new_group")
    data object Status : Screen("status")
    data object ActiveCall : Screen("active_call/{callId}/{name}/{type}") {
        fun createRoute(callId: String, name: String, type: String) = "active_call/$callId/$name/$type"
    }
    data object PrivacySettings : Screen("privacy_settings")
    data object AppLockSettings : Screen("app_lock_settings")
    data object StorageSettings : Screen("storage_settings")
    data object BackupSettings : Screen("backup_settings")
    data object LinkedDevices : Screen("linked_devices")
    data object HelpSupport : Screen("help_support")
}
