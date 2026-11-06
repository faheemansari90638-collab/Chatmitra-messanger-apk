package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AppSettings
import com.example.data.model.HamzaAiConfig
import com.example.data.repository.AiRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ContactsRepository
import com.example.data.repository.PersonalSpaceRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StatusRepository
import com.example.ui.screens.MainScreen
import com.example.ui.screens.ai.HamzaAiProfileScreen
import com.example.ui.screens.ai.HamzaAiScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.ProfileSetupScreen
import com.example.ui.screens.calls.ActiveCallScreen
import com.example.ui.screens.chat.ChatDetailScreen
import com.example.ui.screens.chat.GroupCreateScreen
import com.example.ui.screens.contacts.QrContactScreen
import com.example.ui.screens.lock.PinLockScreen
import com.example.ui.screens.personal.PersonalSpaceScreen
import com.example.ui.screens.settings.AppLockScreen
import com.example.ui.screens.settings.BackupScreen
import com.example.ui.screens.settings.HelpSupportScreen
import com.example.ui.screens.settings.LinkedDevicesScreen
import com.example.ui.screens.settings.PrivacySettingsScreen
import com.example.ui.screens.settings.StorageScreen
import com.example.ui.screens.status.StatusScreen
import com.example.ui.theme.ChatMitraTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check initialization with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val authRepository = AuthRepository(this)
        val chatRepository = ChatRepository(this)
        val callRepository = CallRepository(this)
        val contactsRepository = ContactsRepository(this)
        val aiRepository = AiRepository(this)
        val settingsRepository = SettingsRepository(this)
        val personalSpaceRepository = PersonalSpaceRepository(this)
        val statusRepository = StatusRepository(this)

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (settings.useSystemTheme) systemDark else settings.isDarkMode

            // Screenshot Protection (FLAG_SECURE) handling
            LaunchedEffect(settings.screenshotProtection) {
                if (settings.screenshotProtection) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            ChatMitraTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatMitraApp(
                        authRepository = authRepository,
                        chatRepository = chatRepository,
                        callRepository = callRepository,
                        contactsRepository = contactsRepository,
                        aiRepository = aiRepository,
                        settingsRepository = settingsRepository,
                        personalSpaceRepository = personalSpaceRepository,
                        statusRepository = statusRepository,
                        settings = settings
                    )
                }
            }
        }
    }
}

sealed class CurrentDestination {
    data object Main : CurrentDestination()
    data object ProfileSetup : CurrentDestination()
    data class ChatDetail(val conversationId: String, val title: String) : CurrentDestination()
    data object NewGroup : CurrentDestination()
    data object Status : CurrentDestination()
    data class ActiveCall(val callId: String, val name: String, val type: String) : CurrentDestination()
    data object PersonalSpace : CurrentDestination()
    data object QrContact : CurrentDestination()
    data object PrivacySettings : CurrentDestination()
    data object AppLockSettings : CurrentDestination()
    data object StorageSettings : CurrentDestination()
    data object BackupSettings : CurrentDestination()
    data object LinkedDevices : CurrentDestination()
    data object HelpSupport : CurrentDestination()
    data object HamzaAiProfile : CurrentDestination()
}

@Composable
fun ChatMitraApp(
    authRepository: AuthRepository,
    chatRepository: ChatRepository,
    callRepository: CallRepository,
    contactsRepository: ContactsRepository,
    aiRepository: AiRepository,
    settingsRepository: SettingsRepository,
    personalSpaceRepository: PersonalSpaceRepository,
    statusRepository: StatusRepository,
    settings: AppSettings
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val userProfile by authRepository.userProfile.collectAsState()
    val aiConfig by settingsRepository.hamzaAiConfigFlow.collectAsState(initial = HamzaAiConfig())

    LaunchedEffect(aiConfig) {
        aiRepository.activeConfig = aiConfig
    }

    var isAppUnlocked by remember(settings.isAppLockEnabled) {
        mutableStateOf(!settings.isAppLockEnabled)
    }

    var destination by remember { mutableStateOf<CurrentDestination>(CurrentDestination.Main) }

    // Silent auto sign-in attempt on startup
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            authRepository.attemptAutoSignIn()
        }
    }

    // App Lock Gate
    if (settings.isAppLockEnabled && !isAppUnlocked) {
        PinLockScreen(
            settingsRepository = settingsRepository,
            onUnlocked = { isAppUnlocked = true }
        )
        return
    }

    // Auth Gate
    val user = currentUser
    if (user == null) {
        AuthScreen(
            authRepository = authRepository,
            onAuthSuccess = {
                // If profile is missing displayName, navigate to ProfileSetup
                if (authRepository.userProfile.value?.displayName.isNullOrBlank()) {
                    destination = CurrentDestination.ProfileSetup
                } else {
                    destination = CurrentDestination.Main
                }
            }
        )
        return
    }

    // If profile is not set up yet
    if (userProfile == null || userProfile?.displayName.isNullOrBlank()) {
        ProfileSetupScreen(
            authRepository = authRepository,
            onProfileSaved = {
                destination = CurrentDestination.Main
            }
        )
        return
    }

    // Main App Navigation Handler
    when (val dest = destination) {
        is CurrentDestination.Main -> {
            MainScreen(
                currentUserId = user.uid,
                userProfile = userProfile,
                chatRepository = chatRepository,
                callRepository = callRepository,
                contactsRepository = contactsRepository,
                aiRepository = aiRepository,
                settingsRepository = settingsRepository,
                authRepository = authRepository,
                onNavigateChatDetail = { convId, title ->
                    destination = CurrentDestination.ChatDetail(convId, title)
                },
                onNavigateNewGroup = {
                    destination = CurrentDestination.NewGroup
                },
                onNavigateStatus = {
                    destination = CurrentDestination.Status
                },
                onNavigateCall = { callId, name, type ->
                    destination = CurrentDestination.ActiveCall(callId, name, type)
                },
                onOpenPersonalSpace = {
                    destination = CurrentDestination.PersonalSpace
                },
                onOpenQrCode = {
                    destination = CurrentDestination.QrContact
                },
                onOpenAiProfile = {
                    destination = CurrentDestination.HamzaAiProfile
                },
                onNavigatePrivacy = { destination = CurrentDestination.PrivacySettings },
                onNavigateAppLock = { destination = CurrentDestination.AppLockSettings },
                onNavigateStorage = { destination = CurrentDestination.StorageSettings },
                onNavigateBackup = { destination = CurrentDestination.BackupSettings },
                onNavigateLinkedDevices = { destination = CurrentDestination.LinkedDevices },
                onNavigateHelp = { destination = CurrentDestination.HelpSupport },
                onEditProfile = { destination = CurrentDestination.ProfileSetup },
                onLoggedOut = {
                    destination = CurrentDestination.Main
                }
            )
        }
        is CurrentDestination.ProfileSetup -> {
            BackHandler { destination = CurrentDestination.Main }
            ProfileSetupScreen(
                authRepository = authRepository,
                onProfileSaved = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.ChatDetail -> {
            BackHandler { destination = CurrentDestination.Main }
            ChatDetailScreen(
                conversationId = dest.conversationId,
                chatTitle = dest.title,
                currentUserId = user.uid,
                currentUserName = userProfile?.displayName ?: "User",
                chatRepository = chatRepository,
                callRepository = callRepository,
                aiRepository = aiRepository,
                onBackClick = { destination = CurrentDestination.Main },
                onStartCall = { callId, name, type ->
                    destination = CurrentDestination.ActiveCall(callId, name, type)
                }
            )
        }
        is CurrentDestination.NewGroup -> {
            BackHandler { destination = CurrentDestination.Main }
            GroupCreateScreen(
                currentUserId = user.uid,
                chatRepository = chatRepository,
                contactsRepository = contactsRepository,
                onBackClick = { destination = CurrentDestination.Main },
                onGroupCreated = { convId, title ->
                    destination = CurrentDestination.ChatDetail(convId, title)
                }
            )
        }
        is CurrentDestination.Status -> {
            BackHandler { destination = CurrentDestination.Main }
            StatusScreen(
                currentUserId = user.uid,
                currentUserName = userProfile?.displayName ?: "User",
                statusRepository = statusRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.ActiveCall -> {
            BackHandler { destination = CurrentDestination.Main }
            ActiveCallScreen(
                callId = dest.callId,
                targetName = dest.name,
                callType = dest.type,
                callRepository = callRepository,
                onCallEnded = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.PersonalSpace -> {
            BackHandler { destination = CurrentDestination.Main }
            PersonalSpaceScreen(
                personalSpaceRepository = personalSpaceRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.QrContact -> {
            BackHandler { destination = CurrentDestination.Main }
            QrContactScreen(
                userProfile = userProfile,
                contactsRepository = contactsRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.PrivacySettings -> {
            BackHandler { destination = CurrentDestination.Main }
            PrivacySettingsScreen(
                settingsRepository = settingsRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.AppLockSettings -> {
            BackHandler { destination = CurrentDestination.Main }
            AppLockScreen(
                settingsRepository = settingsRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.StorageSettings -> {
            BackHandler { destination = CurrentDestination.Main }
            StorageScreen(
                settingsRepository = settingsRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.BackupSettings -> {
            BackHandler { destination = CurrentDestination.Main }
            BackupScreen(
                settingsRepository = settingsRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
        is CurrentDestination.LinkedDevices -> {
            BackHandler { destination = CurrentDestination.Main }
            LinkedDevicesScreen(onBackClick = { destination = CurrentDestination.Main })
        }
        is CurrentDestination.HelpSupport -> {
            BackHandler { destination = CurrentDestination.Main }
            HelpSupportScreen(onBackClick = { destination = CurrentDestination.Main })
        }
        is CurrentDestination.HamzaAiProfile -> {
            BackHandler { destination = CurrentDestination.Main }
            HamzaAiProfileScreen(
                aiRepository = aiRepository,
                settingsRepository = settingsRepository,
                onBackClick = { destination = CurrentDestination.Main }
            )
        }
    }
}
