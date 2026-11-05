package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.SettingsRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.Modern3DCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    authRepository: AuthRepository,
    settingsRepository: SettingsRepository,
    onEditProfile: () -> Unit,
    onNavigatePrivacy: () -> Unit,
    onNavigateAppLock: () -> Unit,
    onNavigateStorage: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateLinkedDevices: () -> Unit,
    onNavigateHelp: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsState(initial = com.example.data.model.AppSettings())
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) }
        )

        // User Profile Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onEditProfile() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarView(
                    name = userProfile?.displayName ?: "User",
                    avatarUrl = userProfile?.avatarUrl,
                    size = 62.dp,
                    isOnline = true
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userProfile?.displayName ?: "ChatMitra User",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@${userProfile?.chatMitraId ?: "id"}",
                        fontSize = 13.sp,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = userProfile?.about ?: "Available on ChatMitra",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onEditProfile) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = EmeraldPrimary)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Settings Groups
        SettingsCategoryTitle("SECURITY & PRIVACY")
        SettingsRowItem(
            icon = Icons.Default.Security,
            iconTint = EmeraldPrimary,
            title = "Privacy",
            subtitle = "Last seen, profile photo, read receipts",
            onClick = onNavigatePrivacy
        )
        SettingsRowItem(
            icon = Icons.Default.Lock,
            iconTint = PurpleAccent,
            title = "App Lock & Biometrics",
            subtitle = if (settings.isAppLockEnabled) "Enabled (${settings.lockTimeout})" else "Disabled",
            onClick = onNavigateAppLock
        )
        SettingsRowItem(
            icon = Icons.Default.Devices,
            iconTint = Color(0xFF339AF0),
            title = "Linked Devices",
            subtitle = "Pair another phone, tablet or browser",
            onClick = onNavigateLinkedDevices
        )

        Spacer(Modifier.height(14.dp))

        SettingsCategoryTitle("CHATS & STORAGE")
        SettingsRowItem(
            icon = Icons.Default.Palette,
            iconTint = PurpleAccent,
            title = "Theme & Appearance",
            subtitle = if (settings.useSystemTheme) "System Default" else if (settings.isDarkMode) "Dark Theme" else "Light Theme",
            trailing = {
                Switch(
                    checked = settings.isDarkMode,
                    onCheckedChange = { isDark ->
                        scope.launch { settingsRepository.setTheme(false, isDark) }
                    }
                )
            }
        )
        SettingsRowItem(
            icon = Icons.Default.Storage,
            iconTint = EmeraldPrimary,
            title = "Storage and Data",
            subtitle = "Network usage, auto-download, clear cache",
            onClick = onNavigateStorage
        )
        SettingsRowItem(
            icon = Icons.Default.Backup,
            iconTint = Color(0xFF20C997),
            title = "Chat Backup & Restore",
            subtitle = "Encrypted local & cloud chat archives",
            onClick = onNavigateBackup
        )

        Spacer(Modifier.height(14.dp))

        SettingsCategoryTitle("SUPPORT & ABOUT")
        SettingsRowItem(
            icon = Icons.Default.Help,
            iconTint = Color(0xFF339AF0),
            title = "Help and Support",
            subtitle = "FAQs, contact support, report problem",
            onClick = onNavigateHelp
        )
        SettingsRowItem(
            icon = Icons.Default.Info,
            iconTint = EmeraldPrimary,
            title = "About ChatMitra",
            subtitle = "Version 1.0.0 • No Ads • Free forever",
            onClick = onNavigateHelp
        )

        Spacer(Modifier.height(14.dp))

        SettingsCategoryTitle("ACCOUNT ACTIONS")
        SettingsRowItem(
            icon = Icons.Default.Logout,
            iconTint = Color.Gray,
            title = "Log Out",
            subtitle = "Sign out of this session",
            onClick = { showLogoutDialog = true }
        )
        SettingsRowItem(
            icon = Icons.Default.DeleteForever,
            iconTint = DangerRed,
            title = "Delete Account",
            subtitle = "Permanently remove profile and messages",
            titleColor = DangerRed,
            onClick = { showDeleteAccountDialog = true }
        )

        Spacer(Modifier.height(30.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to log out of ChatMitra?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authRepository.signOut(scope) {
                            onLoggedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account Permanently?") },
            text = { Text("This action cannot be undone. All your messages, profile info, and data will be erased forever.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        scope.launch {
                            authRepository.deleteAccount()
                            onLoggedOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else Modifier

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
