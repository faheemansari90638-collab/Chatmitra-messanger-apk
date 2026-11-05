package com.example.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.ContactsRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreateScreen(
    currentUserId: String,
    chatRepository: ChatRepository,
    contactsRepository: ContactsRepository,
    onBackClick: () -> Unit,
    onGroupCreated: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var groupTitle by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var registeredUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val selectedUids = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = contactsRepository.getRegisteredChatMitraUsers()
        if (result.isSuccess) {
            registeredUsers = result.getOrNull() ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Group", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = groupTitle,
                onValueChange = { groupTitle = it },
                label = { Text("Group Title") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("group_title_input")
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = groupDescription,
                onValueChange = { groupDescription = it },
                label = { Text("Description (Optional)") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Select Members (${selectedUids.size} selected)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(registeredUsers, key = { it.userId }) { user ->
                    val isChecked = selectedUids.contains(user.userId)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isChecked) selectedUids.remove(user.userId)
                                else selectedUids.add(user.userId)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                name = user.displayName,
                                avatarUrl = user.avatarUrl,
                                size = 42.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${user.chatMitraId}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedUids.add(user.userId)
                                    else selectedUids.remove(user.userId)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    if (groupTitle.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            val res = chatRepository.createGroupConversation(
                                adminUid = currentUserId,
                                groupTitle = groupTitle,
                                groupDescription = groupDescription,
                                memberUids = selectedUids.toList()
                            )
                            isLoading = false
                            if (res.isSuccess) {
                                onGroupCreated(res.getOrThrow(), groupTitle)
                            }
                        }
                    }
                },
                enabled = groupTitle.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_group_submit_button")
            ) {
                Text("Create Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
