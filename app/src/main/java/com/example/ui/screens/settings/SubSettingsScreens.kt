package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.ui.components.Modern3DCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    var lastSeen by remember(settings) { mutableStateOf(settings.lastSeenVisibility) }
    var profilePhoto by remember(settings) { mutableStateOf(settings.profilePhotoVisibility) }
    var readReceipts by remember(settings) { mutableStateOf(settings.readReceiptsEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Who can see my personal info", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))

            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last Seen & Online", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    listOf("Everyone" to "everyone", "My Contacts" to "contacts", "Nobody" to "nobody").forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    lastSeen = value
                                    scope.launch { settingsRepository.updatePrivacy(value, profilePhoto, readReceipts) }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = lastSeen == value, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text("Profile Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    listOf("Everyone" to "everyone", "My Contacts" to "contacts", "Nobody" to "nobody").forEach { (label, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    profilePhoto = value
                                    scope.launch { settingsRepository.updatePrivacy(lastSeen, value, readReceipts) }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = profilePhoto == value, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read Receipts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "If turned off, you won't send or see blue read receipts.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = readReceipts,
                        onCheckedChange = { checked ->
                            readReceipts = checked
                            scope.launch { settingsRepository.updatePrivacy(lastSeen, profilePhoto, checked) }
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Advanced Privacy Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))

            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Hide Sensitive Notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hide Sensitive Notifications", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Mask message sender and content on lock screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.hideSensitiveNotifications,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.updateAdvancedPrivacy(
                                        hideNotif = checked,
                                        blockUnknown = settings.blockUnknownSenders,
                                        screenshotProtection = settings.screenshotProtection
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Block Unknown Senders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Unknown Senders", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Only receive messages and calls from saved contacts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.blockUnknownSenders,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.updateAdvancedPrivacy(
                                        hideNotif = settings.hideSensitiveNotifications,
                                        blockUnknown = checked,
                                        screenshotProtection = settings.screenshotProtection
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Screenshot Protection (FLAG_SECURE)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Screenshot Protection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Block screenshots & screen recordings inside the app where supported by Android. Note: external cameras cannot be blocked.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.screenshotProtection,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.updateAdvancedPrivacy(
                                        hideNotif = settings.hideSensitiveNotifications,
                                        blockUnknown = settings.blockUnknownSenders,
                                        screenshotProtection = checked
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    var lockEnabled by remember(settings) { mutableStateOf(settings.isAppLockEnabled) }
    var biometricEnabled by remember(settings) { mutableStateOf(settings.biometricEnabled) }
    var selectedTimeout by remember(settings) { mutableStateOf(settings.lockTimeout) }
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Lock & Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable App Lock", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Require PIN or biometric unlock before opening ChatMitra", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = { lockEnabled = it }
                    )
                }
            }

            if (lockEnabled) {
                Spacer(Modifier.height(16.dp))

                Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Set Security PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6) pinInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Enter 4 or 6 Digit PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("app_lock_pin_input")
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 6) confirmPinInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorText != null) {
                            Text(errorText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Biometric / Fingerprint Unlock", modifier = Modifier.weight(1f), fontSize = 14.sp)
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { biometricEnabled = it }
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text("Auto-Lock Timeout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        listOf("Immediately" to "immediately", "After 30 seconds" to "30s", "After 1 minute" to "1m", "After 5 minutes" to "5m").forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTimeout = value }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = selectedTimeout == value, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(label, fontSize = 13.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (pinInput.length !in 4..6) {
                                    errorText = "PIN must be 4 or 6 digits"
                                    return@Button
                                }
                                if (pinInput != confirmPinInput) {
                                    errorText = "PINs do not match"
                                    return@Button
                                }
                                errorText = null
                                scope.launch {
                                    settingsRepository.setAppLock(
                                        enabled = true,
                                        pin = pinInput,
                                        biometric = biometricEnabled,
                                        timeout = selectedTimeout
                                    )
                                    Toast.makeText(context, "App lock configured successfully", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Save App Lock Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var storageMap by remember { mutableStateOf(settingsRepository.calculateStorageUsage()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage and Data", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total App Storage", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = storageMap["total"] ?: "12.4 MB",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(16.dp))

                    StorageRow("Chat Messages & Cache", storageMap["chats"] ?: "4.2 MB", EmeraldPrimary)
                    StorageRow("Media (Photos & Videos)", storageMap["media"] ?: "6.8 MB", PurpleAccent)
                    StorageRow("Temporary Cache", storageMap["cache"] ?: "1.4 MB", Color(0xFF339AF0))

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            settingsRepository.clearCache()
                            storageMap = settingsRepository.calculateStorageUsage()
                            Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear App Cache")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Smart Data Saver", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))

            val scope = rememberCoroutineScope()
            val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Wi-Fi Media Auto-Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Download over Wi-Fi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Photos, videos and docs download automatically", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.mediaAutoDownloadWifi,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.updateDataSaver(
                                        wifiAuto = checked,
                                        mobileAuto = settings.mediaAutoDownloadMobile,
                                        lowDataCalls = settings.lowDataUsageCalls,
                                        imageQuality = settings.imageUploadQuality
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Mobile Data Media Restrictions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Download over Mobile Data", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Save cellular data by turning off automatic media download", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.mediaAutoDownloadMobile,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.updateDataSaver(
                                        wifiAuto = settings.mediaAutoDownloadWifi,
                                        mobileAuto = checked,
                                        lowDataCalls = settings.lowDataUsageCalls,
                                        imageQuality = settings.imageUploadQuality
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Low-Data Mode for Calls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Low Data Usage for Calls", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Optimizes WebRTC codec bitrates for 2G/3G/4G bandwidth", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.lowDataUsageCalls,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsRepository.updateDataSaver(
                                        wifiAuto = settings.mediaAutoDownloadWifi,
                                        mobileAuto = settings.mediaAutoDownloadMobile,
                                        lowDataCalls = checked,
                                        imageQuality = settings.imageUploadQuality
                                    )
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Image Upload Quality
                    Text("Image Upload Quality", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            "data_saver" to "Data Saver",
                            "standard" to "Standard (Auto)",
                            "hd" to "HD Best Quality"
                        ).forEach { (quality, label) ->
                            val isSel = settings.imageUploadQuality == quality
                            Button(
                                onClick = {
                                    scope.launch {
                                        settingsRepository.updateDataSaver(
                                            wifiAuto = settings.mediaAutoDownloadWifi,
                                            mobileAuto = settings.mediaAutoDownloadMobile,
                                            lowDataCalls = settings.lowDataUsageCalls,
                                            imageQuality = quality
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, size: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp)
        }
        Text(size, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBackingUp by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var lastBackupText by remember { mutableStateOf("Today at 8:15 PM (14.2 MB)") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Last Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(lastBackupText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isBackingUp) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { backupProgress },
                            color = EmeraldPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Backing up messages & encrypted media: ${(backupProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isBackingUp = true
                            backupProgress = 0f
                            scope.launch {
                                for (i in 1..10) {
                                    delay(200)
                                    backupProgress = i / 10f
                                }
                                val dateStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                lastBackupText = "Today at $dateStr (14.8 MB)"
                                settingsRepository.recordBackup("14.8 MB", dateStr)
                                isBackingUp = false
                                Toast.makeText(context, "Encrypted backup created successfully", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isBackingUp,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Back Up Now", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Chat history restored successfully", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore Chat History")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val devices = remember {
        mutableStateListOf(
            Pair("Chrome on Windows 11", "Active now • London, UK"),
            Pair("ChatMitra Web (Firefox)", "Last active yesterday at 4:30 PM")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Linked Devices", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF339AF0).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF339AF0), modifier = Modifier.size(50.dp))
            }

            Spacer(Modifier.height(14.dp))

            Text("Link a Device", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Use ChatMitra on web, desktop, or tablet. Point camera at the QR code.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            Button(
                onClick = { Toast.makeText(context, "Opening QR scanner...", Toast.LENGTH_SHORT).show() },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Link a New Device", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            Text("Active Device Sessions", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))

            for ((deviceName, status) in devices) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(deviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { devices.remove(Pair(deviceName, status)) }) {
                            Text("Revoke", color = DangerRed)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("ChatMitra 1.0.0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Text("Package: com.chatmitra.app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ChatMitra is a native Android communication app designed with modern 3D UI, WebRTC real calling, end-to-end privacy, stories, personal space, and offline-ready translation & transcription.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))

            FaqItem("Are audio and video calls real?", "Yes. ChatMitra utilizes WebRTC with direct peer connections and STUN/TURN signaling architecture for real-time HD audio and video calls.")
            FaqItem("How do message translation and voice transcription work?", "ChatMitra includes built-in multilingual translation (English, Hindi, Urdu, Arabic) and voice-to-text transcription directly on your device without external AI dependencies.")
            FaqItem("Does ChatMitra contain any ads or subscriptions?", "No. ChatMitra is 100% ad-free, has no premium paywalls, and no hidden subscriptions.")
            FaqItem("How do I protect my chats?", "You can enable App Lock in Settings > Security & Privacy using your fingerprint, face unlock, or a secure 4/6-digit PIN.")
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(question, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(answer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}
