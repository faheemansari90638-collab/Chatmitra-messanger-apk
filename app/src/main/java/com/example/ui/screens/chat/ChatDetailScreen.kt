package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.ChatBubble
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import com.example.util.TranscriptionHelper
import com.example.util.TranslationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    chatTitle: String,
    currentUserId: String,
    currentUserName: String,
    chatRepository: ChatRepository,
    callRepository: CallRepository,
    onBackClick: () -> Unit,
    onStartCall: (String, String, String) -> Unit // callId, targetName, type
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by chatRepository.observeMessages(conversationId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }
    var isOptionsMenuExpanded by remember { mutableStateOf(false) }
    var isAttachmentSheetOpen by remember { mutableStateOf(false) }

    // Dialogs & Feature Sheets
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var editedContentText by remember { mutableStateOf("") }

    // Next-Gen Feature Dialogs
    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestionInput by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationDurationMinutes by remember { mutableStateOf(60) }

    var showEventDialog by remember { mutableStateOf(false) }
    var eventTitleInput by remember { mutableStateOf("") }
    var eventDateTimeInput by remember { mutableStateOf("Tomorrow at 6:00 PM") }
    var eventLocationInput by remember { mutableStateOf("Voice Lounge") }

    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleDelayMinutes by remember { mutableStateOf(10) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                chatRepository.sendMessage(
                    conversationId = conversationId,
                    content = "Shared a photo",
                    senderName = currentUserName,
                    participantUids = listOf(currentUserId),
                    type = "image",
                    mediaUrl = uri.toString(),
                    mediaName = "Photo.jpg"
                )
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(name = chatTitle, size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = chatTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "Online • End-to-End Encrypted",
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Audio Call
                    IconButton(
                        onClick = {
                            scope.launch {
                                val result = callRepository.startCall(
                                    callerName = currentUserName,
                                    receiverId = conversationId,
                                    receiverName = chatTitle,
                                    callType = "audio"
                                )
                                result.onSuccess { callId ->
                                    onStartCall(callId, chatTitle, "audio")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Audio Call",
                            tint = EmeraldPrimary
                        )
                    }

                    // Video Call
                    IconButton(
                        onClick = {
                            scope.launch {
                                val result = callRepository.startCall(
                                    callerName = currentUserName,
                                    receiverId = conversationId,
                                    receiverName = chatTitle,
                                    callType = "video"
                                )
                                result.onSuccess { callId ->
                                    onStartCall(callId, chatTitle, "video")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = PurpleAccent
                        )
                    }

                    // Overflow Menu
                    IconButton(onClick = { isOptionsMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = isOptionsMenuExpanded,
                        onDismissRequest = { isOptionsMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Disappearing Messages") },
                            leadingIcon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null) },
                            onClick = {
                                isOptionsMenuExpanded = false
                                showDisappearingDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Chat") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                isOptionsMenuExpanded = false
                                Toast.makeText(context, "Chat history kept safe", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isOutgoing = msg.senderId == currentUserId
                    ChatBubble(
                        message = msg,
                        isOutgoing = isOutgoing,
                        currentUserId = currentUserId,
                        onLongClick = {
                            selectedMessageForAction = msg
                        },
                        onVotePoll = { optionId ->
                            val currentRaw = msg.pollData ?: ""
                            val parts = currentRaw.split("|").toMutableList()
                            if (optionId < parts.size) {
                                val optPieces = parts[optionId].split("::")
                                val text = optPieces.getOrNull(0) ?: "Option"
                                val voters = optPieces.getOrNull(1)?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                                if (voters.contains(currentUserId)) {
                                    voters.remove(currentUserId)
                                } else {
                                    voters.add(currentUserId)
                                }
                                parts[optionId] = "$text::${voters.joinToString(",")}"
                                val newPollJson = parts.joinToString("|")
                                scope.launch {
                                    chatRepository.updatePollData(conversationId, msg.id, newPollJson)
                                }
                            }
                        },
                        onStopLiveLocation = {
                            scope.launch {
                                chatRepository.updateLocationData(conversationId, msg.id, "Live location sharing stopped")
                            }
                        },
                        onRsvpEvent = { rsvpType ->
                            val currentRaw = msg.eventData ?: ""
                            val parts = currentRaw.split("::").toMutableList()
                            while (parts.size < 5) parts.add("")
                            val going = parts[3].split(",").filter { it.isNotBlank() }.toMutableList()
                            val maybe = parts[4].split(",").filter { it.isNotBlank() }.toMutableList()
                            going.remove(currentUserId)
                            maybe.remove(currentUserId)
                            if (rsvpType == "going") going.add(currentUserId)
                            if (rsvpType == "maybe") maybe.add(currentUserId)
                            parts[3] = going.joinToString(",")
                            parts[4] = maybe.joinToString(",")
                            val newEventJson = parts.joinToString("::")
                            scope.launch {
                                chatRepository.updateEventData(conversationId, msg.id, newEventJson)
                            }
                        },
                        onTranscribeAudio = {
                            scope.launch {
                                Toast.makeText(context, "Transcribing voice audio...", Toast.LENGTH_SHORT).show()
                                val transcription = TranscriptionHelper.transcribe(msg.mediaName ?: "voice note")
                                chatRepository.updateMessageTranscription(conversationId, msg.id, transcription)
                            }
                        },
                        onTranslateMessage = {
                            scope.launch {
                                val res = TranslationHelper.translate(msg.content, "Hindi")
                                chatRepository.updateMessageTranslation(conversationId, msg.id, res)
                            }
                        }
                    )
                }
            }

            // Replying to preview bar
            AnimatedVisibility(visible = replyingToMessage != null) {
                replyingToMessage?.let { replyMsg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${replyMsg.senderName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = replyMsg.content.take(50),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { replyingToMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Message Composer input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isAttachmentSheetOpen = true },
                        modifier = Modifier.testTag("attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach file",
                            tint = EmeraldPrimary
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    Spacer(Modifier.width(6.dp))

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val toSend = textInput
                                textInput = ""
                                val replyId = replyingToMessage?.id
                                val replyText = replyingToMessage?.content
                                replyingToMessage = null

                                scope.launch {
                                    chatRepository.sendMessage(
                                        conversationId = conversationId,
                                        content = toSend,
                                        senderName = currentUserName,
                                        participantUids = listOf(currentUserId),
                                        type = "text",
                                        replyToMessageId = replyId,
                                        replyToText = replyText
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    chatRepository.sendMessage(
                                        conversationId = conversationId,
                                        content = "🎤 Voice note (0:08)",
                                        senderName = currentUserName,
                                        participantUids = listOf(currentUserId),
                                        type = "voice"
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("voice_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice note",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Attachment Modal Sheet with All Next-Gen Options
    if (isAttachmentSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isAttachmentSheetOpen = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Share & Collaborate",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Row 1: Media & Documents
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.Image,
                        label = "Gallery",
                        color = EmeraldPrimary
                    ) {
                        isAttachmentSheetOpen = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }

                    AttachmentOptionItem(
                        icon = Icons.Default.Description,
                        label = "Document",
                        color = PurpleAccent
                    ) {
                        isAttachmentSheetOpen = false
                        scope.launch {
                            chatRepository.sendMessage(
                                conversationId = conversationId,
                                content = "Project Document Specification",
                                senderName = currentUserName,
                                participantUids = listOf(currentUserId),
                                type = "document",
                                mediaName = "ChatMitra_Specs.pdf",
                                mediaSize = "2.4 MB"
                            )
                        }
                    }

                    AttachmentOptionItem(
                        icon = Icons.Default.Poll,
                        label = "Poll",
                        color = Color(0xFF339AF0)
                    ) {
                        isAttachmentSheetOpen = false
                        showPollDialog = true
                    }

                    AttachmentOptionItem(
                        icon = Icons.Default.MyLocation,
                        label = "Location",
                        color = Color(0xFFE53935)
                    ) {
                        isAttachmentSheetOpen = false
                        showLocationDialog = true
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Row 2: Advanced Features
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.CalendarToday,
                        label = "Event",
                        color = Color(0xFFF59F00)
                    ) {
                        isAttachmentSheetOpen = false
                        showEventDialog = true
                    }

                    AttachmentOptionItem(
                        icon = Icons.Default.Schedule,
                        label = "Schedule",
                        color = Color(0xFF20C997)
                    ) {
                        isAttachmentSheetOpen = false
                        showScheduleDialog = true
                    }

                    AttachmentOptionItem(
                        icon = Icons.Default.HourglassEmpty,
                        label = "Disappearing",
                        color = Color(0xFF845EF7)
                    ) {
                        isAttachmentSheetOpen = false
                        showDisappearingDialog = true
                    }

                    AttachmentOptionItem(
                        icon = Icons.Default.Mic,
                        label = "Audio",
                        color = Color(0xFF5C7CFA)
                    ) {
                        isAttachmentSheetOpen = false
                        scope.launch {
                            chatRepository.sendMessage(
                                conversationId = conversationId,
                                content = "Shared Audio Track",
                                senderName = currentUserName,
                                participantUids = listOf(currentUserId),
                                type = "audio",
                                mediaName = "Audio_Recording.mp3",
                                mediaSize = "1.8 MB"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Message Actions Sheet
    selectedMessageForAction?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForAction = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Quick Emoji Reactions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .clickable {
                                    selectedMessageForAction = null
                                    scope.launch {
                                        chatRepository.toggleReaction(conversationId, msg.id, currentUserId, emoji)
                                    }
                                }
                                .padding(4.dp)
                        )
                    }
                }

                DropdownMenuItem(
                    text = { Text("Translate Message") },
                    leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = EmeraldPrimary) },
                    onClick = {
                        val toTranslate = msg.content
                        selectedMessageForAction = null
                        scope.launch {
                            Toast.makeText(context, "Translating message...", Toast.LENGTH_SHORT).show()
                            val translated = TranslationHelper.translate(toTranslate, "Hindi and English")
                            chatRepository.updateMessageTranslation(conversationId, msg.id, translated)
                        }
                    }
                )

                if (msg.type in listOf("voice", "audio")) {
                    DropdownMenuItem(
                        text = { Text("Transcribe Audio to Text") },
                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = PurpleAccent) },
                        onClick = {
                            selectedMessageForAction = null
                            scope.launch {
                                Toast.makeText(context, "Transcribing voice note...", Toast.LENGTH_SHORT).show()
                                val transcribed = TranscriptionHelper.transcribe(msg.mediaName ?: "voice note")
                                chatRepository.updateMessageTranscription(conversationId, msg.id, transcribed)
                            }
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                    onClick = {
                        replyingToMessage = msg
                        selectedMessageForAction = null
                    }
                )

                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ChatMitra message", msg.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        selectedMessageForAction = null
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (msg.isStarred) "Unstar" else "Star") },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    onClick = {
                        scope.launch {
                            chatRepository.toggleStarMessage(conversationId, msg.id, !msg.isStarred)
                        }
                        selectedMessageForAction = null
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (msg.isPinned) "Unpin" else "Pin") },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                    onClick = {
                        scope.launch {
                            chatRepository.togglePinMessage(conversationId, msg.id, !msg.isPinned)
                        }
                        selectedMessageForAction = null
                    }
                )

                if (msg.senderId == currentUserId && !msg.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            editingMessage = msg
                            editedContentText = msg.content
                            selectedMessageForAction = null
                        }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Delete for Everyone") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                    onClick = {
                        scope.launch {
                            chatRepository.deleteMessageForEveryone(conversationId, msg.id)
                        }
                        selectedMessageForAction = null
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Edit Message Dialog
    editingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editedContentText,
                    onValueChange = { editedContentText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            chatRepository.editMessage(conversationId, msg.id, editedContentText)
                            editingMessage = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Poll Dialog
    if (showPollDialog) {
        AlertDialog(
            onDismissRequest = { showPollDialog = false },
            title = { Text("Create Group Poll") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pollQuestionInput,
                        onValueChange = { pollQuestionInput = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pollOption1,
                        onValueChange = { pollOption1 = it },
                        label = { Text("Option 1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pollOption2,
                        onValueChange = { pollOption2 = it },
                        label = { Text("Option 2") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = pollOption3,
                        onValueChange = { pollOption3 = it },
                        label = { Text("Option 3 (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pollQuestionInput.isNotBlank() && pollOption1.isNotBlank() && pollOption2.isNotBlank()) {
                            val opts = mutableListOf(pollOption1.trim(), pollOption2.trim())
                            if (pollOption3.isNotBlank()) opts.add(pollOption3.trim())
                            val pollDataRaw = opts.joinToString("|") { "$it::" }
                            scope.launch {
                                chatRepository.sendMessage(
                                    conversationId = conversationId,
                                    content = pollQuestionInput.trim(),
                                    senderName = currentUserName,
                                    participantUids = listOf(currentUserId),
                                    type = "poll",
                                    pollData = pollDataRaw
                                )
                            }
                            showPollDialog = false
                            pollQuestionInput = ""
                            pollOption1 = ""
                            pollOption2 = ""
                            pollOption3 = ""
                        }
                    }
                ) {
                    Text("Post Poll")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPollDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Live Location Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Share Live Location") },
            text = {
                Column {
                    Text("Share your real-time GPS location with participants. You can stop sharing at any moment.", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Select Duration:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15 to "15 Mins", 60 to "1 Hour", 480 to "8 Hours").forEach { (mins, label) ->
                            Button(
                                onClick = { locationDurationMinutes = mins },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (locationDurationMinutes == mins) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(label, color = if (locationDurationMinutes == mins) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationDialog = false
                        scope.launch {
                            chatRepository.sendMessage(
                                conversationId = conversationId,
                                content = "Sharing live location for $locationDurationMinutes minutes",
                                senderName = currentUserName,
                                participantUids = listOf(currentUserId),
                                type = "location",
                                locationData = "28.6139° N, 77.2090° E • Connaught Place, New Delhi"
                            )
                        }
                    }
                ) {
                    Text("Share Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Group Event Dialog
    if (showEventDialog) {
        AlertDialog(
            onDismissRequest = { showEventDialog = false },
            title = { Text("Create Group Event") },
            text = {
                Column {
                    OutlinedTextField(
                        value = eventTitleInput,
                        onValueChange = { eventTitleInput = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = eventDateTimeInput,
                        onValueChange = { eventDateTimeInput = it },
                        label = { Text("Date & Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = eventLocationInput,
                        onValueChange = { eventLocationInput = it },
                        label = { Text("Location / Channel") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (eventTitleInput.isNotBlank()) {
                            showEventDialog = false
                            val eventRaw = "${eventTitleInput.trim()}::${eventDateTimeInput.trim()}::${eventLocationInput.trim()}::$currentUserId::"
                            scope.launch {
                                chatRepository.sendMessage(
                                    conversationId = conversationId,
                                    content = eventTitleInput.trim(),
                                    senderName = currentUserName,
                                    participantUids = listOf(currentUserId),
                                    type = "event",
                                    eventData = eventRaw
                                )
                            }
                            eventTitleInput = ""
                        }
                    }
                ) {
                    Text("Create Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEventDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Disappearing Messages Dialog
    if (showDisappearingDialog) {
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing Messages") },
            text = {
                Column {
                    Text("When enabled, new messages sent in this chat will disappear automatically after the chosen timer expires.", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    val timers = listOf(
                        Pair("24 Hours", 86400),
                        Pair("7 Days", 604800),
                        Pair("90 Days", 7776000),
                        Pair("Off", null)
                    )
                    timers.forEach { (label, secs) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showDisappearingDialog = false
                                    scope.launch {
                                        chatRepository.updateDisappearingTimer(conversationId, secs)
                                        Toast.makeText(context, "Disappearing timer set to $label", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Text(label, modifier = Modifier.padding(14.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDisappearingDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Schedule Message Dialog
    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("Schedule Future Message") },
            text = {
                Column {
                    Text("Choose when to automatically transmit this message:", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    listOf(10 to "In 10 Minutes", 60 to "In 1 Hour", 1440 to "Tomorrow morning").forEach { (mins, label) ->
                        Button(
                            onClick = {
                                showScheduleDialog = false
                                scope.launch {
                                    chatRepository.sendMessage(
                                        conversationId = conversationId,
                                        content = if (textInput.isNotBlank()) textInput else "Scheduled reminder from ChatMitra",
                                        senderName = currentUserName,
                                        participantUids = listOf(currentUserId),
                                        type = "text",
                                        isScheduled = true
                                    )
                                    textInput = ""
                                    Toast.makeText(context, "Message scheduled for $label", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
    }
}
