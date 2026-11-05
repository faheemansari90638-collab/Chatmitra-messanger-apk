package com.example.ui.screens.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Contact
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.ContactsRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@Composable
fun ContactsScreen(
    currentUserId: String,
    contactsRepository: ContactsRepository,
    chatRepository: ChatRepository,
    onStartChatWithUser: (String, String) -> Unit // conversationId, targetName
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isAddContactDialogOpen by remember { mutableStateOf(false) }

    var registeredUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val deviceContacts by contactsRepository.loadPhoneContacts().collectAsState(initial = emptyList())
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    LaunchedEffect(Unit) {
        val result = contactsRepository.getRegisteredChatMitraUsers()
        if (result.isSuccess) {
            registeredUsers = result.getOrNull() ?: emptyList()
        }
    }

    val filteredRegistered = remember(registeredUsers, searchQuery) {
        if (searchQuery.isBlank()) registeredUsers
        else registeredUsers.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.chatMitraId.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredDevice = remember(deviceContacts, searchQuery) {
        if (searchQuery.isBlank()) deviceContacts
        else deviceContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddContactDialogOpen = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or @handle...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("ChatMitra Users (${filteredRegistered.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Phone Contacts", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // ChatMitra Users
                if (filteredRegistered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No other registered users found yet. Invite your friends!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredRegistered, key = { it.userId }) { user ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarView(
                                        name = user.displayName,
                                        avatarUrl = user.avatarUrl,
                                        size = 46.dp,
                                        isOnline = user.isOnline
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "@${user.chatMitraId}",
                                            fontSize = 12.sp,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = user.about,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val convResult = chatRepository.getOrCreateDirectConversation(
                                                    currentUid = currentUserId,
                                                    otherUid = user.userId,
                                                    otherDisplayName = user.displayName
                                                )
                                                if (convResult.isSuccess) {
                                                    onStartChatWithUser(convResult.getOrThrow(), user.displayName)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.testTag("message_user_${user.userId}")
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Chat", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Phone Contacts
                if (!hasContactsPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Access Phone Contacts",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "ChatMitra allows you to discover friends who already use the app and easily invite others.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Grant Permission", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredDevice, key = { it.id + it.phoneNumber }) { contact ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarView(
                                        name = contact.name,
                                        size = 44.dp,
                                        isOnline = false,
                                        showOnlineIndicator = false
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = contact.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(text = contact.phoneNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Hey! Let's chat securely on ChatMitra. Join me with Hamza AI assistant!"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Invite via"))
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Invite", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (isAddContactDialogOpen) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isAddContactDialogOpen = false },
            title = { Text("Add New Contact") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number (+91 / +966)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isAddContactDialogOpen = false
                        // Start conversation
                        scope.launch {
                            val dummyUid = "user_${newPhone.filter { it.isDigit() }.takeLast(8)}"
                            val convRes = chatRepository.getOrCreateDirectConversation(
                                currentUid = currentUserId,
                                otherUid = dummyUid,
                                otherDisplayName = newName.ifBlank { newPhone }
                            )
                            if (convRes.isSuccess) {
                                onStartChatWithUser(convRes.getOrThrow(), newName.ifBlank { newPhone })
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Add & Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddContactDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
