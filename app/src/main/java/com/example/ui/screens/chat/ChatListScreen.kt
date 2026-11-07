package com.example.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.repository.ChatRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    chatRepository: ChatRepository,
    currentUserId: String,
    onConversationClick: (String, String) -> Unit,
    onNewGroupClick: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenStatus: () -> Unit,
    onOpenPersonalSpace: () -> Unit = {},
    onOpenQrCode: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val conversations by chatRepository.observeConversations().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }

    val folders = listOf("All", "Unread", "Friends", "Family", "Work", "Groups", "Important")

    // Long press conversation options dialog
    var selectedConversationForOptions by remember { mutableStateOf<Conversation?>(null) }
    var showFolderSelectDialog by remember { mutableStateOf(false) }

    // Pin unlock dialog for locked chats
    var pendingLockedConversation by remember { mutableStateOf<Conversation?>(null) }
    var pinUnlockInput by remember { mutableStateOf("") }
    var pinUnlockError by remember { mutableStateOf(false) }

    val filteredList = remember(conversations, searchQuery, selectedFolder) {
        conversations.filter { conv ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else (conv.groupTitle ?: "").contains(searchQuery, ignoreCase = true) ||
                 (conv.lastMessageText ?: "").contains(searchQuery, ignoreCase = true)

            val matchesFolder = when (selectedFolder) {
                "All" -> true
                "Unread" -> conv.unreadCount > 0
                "Groups" -> conv.type == "group"
                else -> conv.chatFolder.equals(selectedFolder, ignoreCase = true)
            }

            matchesSearch && matchesFolder
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Personal Space Quick Access FAB
                FloatingActionButton(
                    onClick = onOpenPersonalSpace,
                    containerColor = Color(0xFF339AF0),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).testTag("personal_space_fab")
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Personal Space", modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(10.dp))

                FloatingActionButton(
                    onClick = onNewGroupClick,
                    containerColor = PurpleAccent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("new_group_fab")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "New Group", modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(10.dp))

                FloatingActionButton(
                    onClick = onOpenContacts,
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .testTag("new_chat_fab")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "New Chat")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Search & QR Code Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chats & messages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_search_input")
                )

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onOpenQrCode,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = EmeraldPrimary)
                }
            }

            // Chat Folders Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    val isSelected = selectedFolder == folder
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Status Stories Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "My Status" add button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenStatus() }
                        .padding(end = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, EmeraldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add status",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = "My Status",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Recent contacts status row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(listOf("Aarav", "Priya", "Farhan", "Zaid", "Rohan")) { contactName ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onOpenStatus() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .border(2.dp, EmeraldPrimary, CircleShape)
                                    .padding(3.dp)
                            ) {
                                AvatarView(
                                    name = contactName,
                                    size = 48.dp,
                                    isOnline = true,
                                    showOnlineIndicator = false
                                )
                            }
                            Text(
                                text = contactName,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Conversations List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            tint = EmeraldPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No conversations match '$searchQuery'"
                            else if (selectedFolder != "All") "No chats in '$selectedFolder' folder"
                            else "No chats yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap the floating action button to start messaging friends or create a group.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            currentUserId = currentUserId,
                            onClick = {
                                if (conversation.isLocked) {
                                    pendingLockedConversation = conversation
                                    pinUnlockInput = ""
                                    pinUnlockError = false
                                } else {
                                    val title = conversation.groupTitle ?: "Chat"
                                    onConversationClick(conversation.id, title)
                                }
                            },
                            onLongClick = {
                                selectedConversationForOptions = conversation
                            }
                        )
                    }
                }
            }
        }
    }

    // Long press options dialog (Folders, Lock, Disappearing)
    selectedConversationForOptions?.let { conv ->
        AlertDialog(
            onDismissRequest = { selectedConversationForOptions = null },
            title = { Text(conv.groupTitle ?: "Chat Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showFolderSelectDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Move to Folder (${conv.chatFolder})", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                chatRepository.updateChatLock(conv.id, !conv.isLocked)
                                selectedConversationForOptions = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PurpleAccent)
                            Spacer(Modifier.width(10.dp))
                            Text(if (conv.isLocked) "Unlock Chat" else "Lock Chat with PIN", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedConversationForOptions = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move to folder dialog
    if (showFolderSelectDialog && selectedConversationForOptions != null) {
        val conv = selectedConversationForOptions!!
        AlertDialog(
            onDismissRequest = { showFolderSelectDialog = false },
            title = { Text("Select Folder") },
            text = {
                Column {
                    listOf("All", "Friends", "Family", "Work", "Groups", "Important").forEach { folder ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    scope.launch {
                                        chatRepository.updateChatFolder(conv.id, folder)
                                        showFolderSelectDialog = false
                                        selectedConversationForOptions = null
                                    }
                                }
                        ) {
                            Text(folder, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFolderSelectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Locked Chat PIN Prompt Dialog
    pendingLockedConversation?.let { conv ->
        AlertDialog(
            onDismissRequest = { pendingLockedConversation = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PurpleAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Chat Locked")
                }
            },
            text = {
                Column {
                    Text("Enter your 4-digit ChatMitra PIN to open this confidential chat.", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinUnlockInput,
                        onValueChange = { if (it.length <= 6) pinUnlockInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Security PIN") },
                        isError = pinUnlockError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinUnlockError) {
                        Text("Incorrect PIN. Please try again.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinUnlockInput.length >= 4) {
                            val title = conv.groupTitle ?: "Chat"
                            pendingLockedConversation = null
                            onConversationClick(conv.id, title)
                        } else {
                            pinUnlockError = true
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLockedConversation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val title = conversation.groupTitle ?: if (conversation.type == "group") "Group Chat" else "Direct Chat"
    val lastMsg = conversation.lastMessageText ?: "No messages yet"

    val timeFormatted = conversation.lastMessageTimestamp?.let {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(it.seconds * 1000))
    } ?: ""

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                name = title,
                avatarUrl = conversation.groupAvatarUrl,
                size = 50.dp,
                isOnline = true
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isLocked) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = PurpleAccent, modifier = Modifier.size(14.dp))
                        }
                        if (conversation.disappearingDurationSeconds != null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.HourglassEmpty, contentDescription = "Disappearing", tint = Color.Gray, modifier = Modifier.size(13.dp))
                        }
                    }

                    if (timeFormatted.isNotBlank()) {
                        Text(
                            text = timeFormatted,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (conversation.isLocked) "🔒 Confidential chat locked" else lastMsg,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${conversation.unreadCount}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
