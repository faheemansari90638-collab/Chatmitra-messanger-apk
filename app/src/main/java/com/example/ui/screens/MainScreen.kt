package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.AiRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ContactsRepository
import com.example.data.repository.SettingsRepository
import com.example.ui.navigation.MainTab
import com.example.ui.screens.ai.HamzaAiScreen
import com.example.ui.screens.calls.CallsListScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.chat.ChatListScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent

@Composable
fun MainScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    chatRepository: ChatRepository,
    callRepository: CallRepository,
    contactsRepository: ContactsRepository,
    aiRepository: AiRepository,
    settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    onNavigateChatDetail: (String, String) -> Unit,
    onNavigateNewGroup: () -> Unit,
    onNavigateStatus: () -> Unit,
    onNavigateCall: (String, String, String) -> Unit,
    onNavigatePrivacy: () -> Unit,
    onNavigateAppLock: () -> Unit,
    onNavigateStorage: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateLinkedDevices: () -> Unit,
    onNavigateHelp: () -> Unit,
    onOpenPersonalSpace: () -> Unit = {},
    onOpenQrCode: () -> Unit = {},
    onOpenAiProfile: () -> Unit = {},
    onEditProfile: () -> Unit,
    onLoggedOut: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.CHATS) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) {
                                    if (tab == MainTab.HAMZA_AI) PurpleAccent else EmeraldPrimary
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    if (tab == MainTab.HAMZA_AI) PurpleAccent else EmeraldPrimary
                                } else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = if (tab == MainTab.HAMZA_AI) PurpleAccent.copy(alpha = 0.15f)
                            else EmeraldPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_${tab.route}")
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    MainTab.CHATS -> {
                        ChatListScreen(
                            chatRepository = chatRepository,
                            currentUserId = currentUserId,
                            onConversationClick = onNavigateChatDetail,
                            onNewGroupClick = onNavigateNewGroup,
                            onOpenContacts = { selectedTab = MainTab.CONTACTS },
                            onOpenStatus = onNavigateStatus,
                            onOpenPersonalSpace = onOpenPersonalSpace,
                            onOpenQrCode = onOpenQrCode
                        )
                    }
                    MainTab.CALLS -> {
                        CallsListScreen(
                            callRepository = callRepository,
                            currentUserId = currentUserId,
                            onStartCall = { targetName, type ->
                                onNavigateCall("new_call", targetName, type)
                            },
                            onOpenContacts = { selectedTab = MainTab.CONTACTS }
                        )
                    }
                    MainTab.CONTACTS -> {
                        ContactsScreen(
                            currentUserId = currentUserId,
                            contactsRepository = contactsRepository,
                            chatRepository = chatRepository,
                            onStartChatWithUser = onNavigateChatDetail
                        )
                    }
                    MainTab.HAMZA_AI -> {
                        HamzaAiScreen(
                            aiRepository = aiRepository,
                            settingsRepository = settingsRepository,
                            onOpenAiProfile = onOpenAiProfile
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            userProfile = userProfile,
                            authRepository = authRepository,
                            settingsRepository = settingsRepository,
                            onEditProfile = onEditProfile,
                            onNavigatePrivacy = onNavigatePrivacy,
                            onNavigateAppLock = onNavigateAppLock,
                            onNavigateStorage = onNavigateStorage,
                            onNavigateBackup = onNavigateBackup,
                            onNavigateLinkedDevices = onNavigateLinkedDevices,
                            onNavigateHelp = onNavigateHelp,
                            onNavigateAiProfile = onOpenAiProfile,
                            onLoggedOut = onLoggedOut
                        )
                    }
                }
            }
        }
    }
}
