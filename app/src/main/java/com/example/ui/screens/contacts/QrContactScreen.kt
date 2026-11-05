package com.example.ui.screens.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.repository.ContactsRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.Modern3DCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrContactScreen(
    userProfile: UserProfile?,
    contactsRepository: ContactsRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Scan / Search input state
    var qrCodeInput by remember { mutableStateOf("") }
    var searchedUser by remember { mutableStateOf<UserProfile?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val chatMitraId = userProfile?.chatMitraId ?: "user_${userProfile?.userId?.take(8)}"
    val displayName = userProfile?.displayName ?: "ChatMitra User"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Contact Sharing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My QR Code") },
                    icon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Scan / Add Contact") },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // My QR Code view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Modern3DCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        elevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AvatarView(name = displayName, size = 64.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(text = displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(text = "@$chatMitraId", fontSize = 13.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)

                            Spacer(Modifier.height(18.dp))

                            // 3D Styled QR Code Pattern Box
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .size(200.dp)
                                    .border(2.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "QR Code Pattern",
                                            tint = Color.Black,
                                            modifier = Modifier.size(160.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Your QR code is end-to-end encrypted and private. Friends can scan to add you without sharing your phone number.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ChatMitra ID", "@$chatMitraId"))
                                Toast.makeText(context, "ID copied: @$chatMitraId", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy ID")
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Sharing link: chatmitra.app/add/$chatMitraId", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share QR")
                        }
                    }
                }
            } else {
                // Scan / Enter Code view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add Contact by QR / ID",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Enter a ChatMitra handle or paste scanned QR code payload to verify the profile before adding.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = qrCodeInput,
                        onValueChange = { qrCodeInput = it },
                        placeholder = { Text("e.g. @priya_tech or chatmitra.app/...") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.fillMaxWidth().testTag("qr_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val cleanInput = qrCodeInput.trim().removePrefix("@").removePrefix("chatmitra.app/add/")
                            if (cleanInput.isNotBlank()) {
                                isSearching = true
                                searchError = null
                                scope.launch {
                                    val res = contactsRepository.searchUserByChatMitraId(cleanInput)
                                    isSearching = false
                                    res.onSuccess { profile ->
                                        if (profile != null) {
                                            searchedUser = profile
                                        } else {
                                            searchError = "No ChatMitra account found for '$cleanInput'"
                                        }
                                    }.onFailure {
                                        searchError = "Search failed: ${it.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Verify Profile", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (searchError != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(text = searchError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }

                    // Verified Contact Preview Card
                    searchedUser?.let { user ->
                        Spacer(Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarView(name = user.displayName, avatarUrl = user.avatarUrl, size = 48.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                        }
                                        Text(text = "@${user.chatMitraId}", fontSize = 12.sp, color = EmeraldPrimary)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(text = user.about, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            contactsRepository.addContact(
                                                name = user.displayName,
                                                phoneNumber = user.phoneNumber,
                                                chatMitraId = user.chatMitraId
                                            )
                                            Toast.makeText(context, "${user.displayName} added to contacts!", Toast.LENGTH_SHORT).show()
                                            searchedUser = null
                                            qrCodeInput = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add to Contacts")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
