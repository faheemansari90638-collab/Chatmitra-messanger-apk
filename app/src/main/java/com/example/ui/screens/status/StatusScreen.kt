package com.example.ui.screens.status

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StatusUpdate
import com.example.data.repository.StatusRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    currentUserId: String,
    currentUserName: String,
    statusRepository: StatusRepository,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allStatuses by statusRepository.observeActiveStatuses().collectAsState(initial = emptyList())

    val myStatuses = remember(allStatuses, currentUserId) {
        allStatuses.filter { it.userId == currentUserId }
    }

    val recentStatuses = remember(allStatuses, currentUserId) {
        allStatuses.filter { it.userId != currentUserId }
    }

    var viewingStatus by remember { mutableStateOf<StatusUpdate?>(null) }
    var showAddTextStatusDialog by remember { mutableStateOf(false) }
    var statusTextInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#12B886") }
    var statusPrivacy by remember { mutableStateOf("everyone") } // "everyone", "contacts", "private"
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Viewers list bottom sheet
    var viewingStatusViewers by remember { mutableStateOf<StatusUpdate?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                statusRepository.postStatus(
                    userName = currentUserName,
                    userAvatarUrl = null,
                    text = "Photo Status",
                    mediaUrl = uri.toString(),
                    type = "photo",
                    privacy = statusPrivacy
                )
            }
        }
    }

    val colorOptions = listOf("#12B886", "#7950F2", "#339AF0", "#F59F00", "#E53935", "#212529")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stories & Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPrivacyDialog = true }) {
                        Icon(Icons.Default.Security, contentDescription = "Privacy Settings", tint = EmeraldPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showAddTextStatusDialog = true },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = EmeraldPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp).testTag("add_text_status_fab")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Text status", modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(12.dp))

                FloatingActionButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_media_status_fab")
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Media status")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // My Status Section
            item {
                Text(
                    text = "My Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (myStatuses.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddTextStatusDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.5.dp, EmeraldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary)
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Add to My Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Disappears automatically in 24 hours",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    myStatuses.forEach { status ->
                        val hoursRemaining = ((status.expiresAt - System.currentTimeMillis()) / 3600000L).coerceAtLeast(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingStatus = status }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .border(2.dp, EmeraldPrimary, CircleShape)
                                    .padding(3.dp)
                            ) {
                                AvatarView(name = currentUserName, size = 48.dp)
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Active Status",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Expires in $hoursRemaining hours • ${status.viewers.size} views",
                                    fontSize = 12.sp,
                                    color = EmeraldPrimary
                                )
                            }

                            IconButton(onClick = { viewingStatusViewers = status }) {
                                Icon(Icons.Default.Visibility, contentDescription = "Viewers", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        statusRepository.deleteStatus(status.id)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Recent Updates (${recentStatuses.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (recentStatuses.isEmpty()) {
                item {
                    Text(
                        text = "No recent status updates from your contacts.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(recentStatuses) { status ->
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val timeFormatted = sdf.format(Date(status.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewingStatus = status
                                scope.launch {
                                    statusRepository.markStatusViewed(status.id)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .border(2.dp, PurpleAccent, CircleShape)
                                .padding(3.dp)
                        ) {
                            AvatarView(name = status.userName, avatarUrl = status.userAvatarUrl, size = 48.dp)
                        }

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                text = status.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Today, $timeFormatted • Tap to view",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Story Progress Viewer Dialog (WhatsApp / Instagram style 24h viewer)
    viewingStatus?.let { status ->
        Dialog(
            onDismissRequest = { viewingStatus = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var progress by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(Unit) {
                while (progress < 1f) {
                    delay(50)
                    progress += 0.01f
                }
                viewingStatus = null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(android.graphics.Color.parseColor(status.backgroundColorHex)))
                    .clickable { viewingStatus = null }
            ) {
                // Top Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .height(3.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                // Top user info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 36.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(name = status.userName, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.userName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Status update (24h)",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewingStatus = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Story Content (Media or text)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!status.mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = status.mediaUrl,
                            contentDescription = "Status media",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    } else {
                        Text(
                            text = status.text,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }
            }
        }
    }

    // Add Text Status Dialog
    if (showAddTextStatusDialog) {
        AlertDialog(
            onDismissRequest = { showAddTextStatusDialog = false },
            title = { Text("New Status Update") },
            text = {
                Column {
                    OutlinedTextField(
                        value = statusTextInput,
                        onValueChange = { statusTextInput = it },
                        placeholder = { Text("What's on your mind?") },
                        modifier = Modifier.fillMaxWidth().height(110.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Select Background Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorOptions.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                        color = if (selectedColorHex == hex) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (statusTextInput.isNotBlank()) {
                            scope.launch {
                                statusRepository.postStatus(
                                    userName = currentUserName,
                                    userAvatarUrl = null,
                                    text = statusTextInput.trim(),
                                    type = "text",
                                    backgroundColorHex = selectedColorHex,
                                    privacy = statusPrivacy
                                )
                            }
                            statusTextInput = ""
                            showAddTextStatusDialog = false
                        }
                    }
                ) {
                    Text("Post Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Viewers List Bottom Sheet
    viewingStatusViewers?.let { status ->
        ModalBottomSheet(
            onDismissRequest = { viewingStatusViewers = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Viewed by ${status.viewers.size} contacts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (status.viewers.isEmpty()) {
                    Text("No one has viewed this status update yet.", fontSize = 13.sp, color = Color.Gray)
                } else {
                    status.viewers.forEach { viewerUid ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            AvatarView(name = "Contact", size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(text = "Contact ($viewerUid)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Status Privacy Settings Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Status Privacy") },
            text = {
                Column {
                    Text("Choose who can view your 24-hour status stories:", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "everyone" to "My Contacts (Recommended)",
                        "contacts" to "My Contacts Except...",
                        "private" to "Only Share With Specific Friends"
                    ).forEach { (priv, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (statusPrivacy == priv) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { statusPrivacy = priv }
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = if (statusPrivacy == priv) FontWeight.Bold else FontWeight.Medium,
                                color = if (statusPrivacy == priv) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
