package com.example.ui.screens.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiMessage
import com.example.data.repository.AiRepository
import com.example.ui.components.Modern3DCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@Composable
fun HamzaAiScreen(
    aiRepository: AiRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by aiRepository.observeAiMessages().collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var streamingReply by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    val languages = listOf("English", "हिन्दी (Hindi)", "اردو (Urdu)")

    val quickPrompts = listOf(
        "Grammar Check" to "Please check and correct the grammar of this message: ",
        "Translate to Hindi" to "Please translate this message into natural Hindi: ",
        "Translate to Urdu" to "Please translate this message into natural Urdu: ",
        "Draft Polite Reply" to "Draft a polite and professional message acknowledging: ",
        "Summarize Notes" to "Please summarize the key points of the following text: "
    )

    LaunchedEffect(messages.size, streamingReply) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Hamza AI 3D Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val aiGradient = Brush.linearGradient(
                        colors = listOf(PurpleAccent, EmeraldPrimary)
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(aiGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Hamza AI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Multilingual Assistant (EN • HI • UR)",
                            fontSize = 11.sp,
                            color = PurpleAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            aiRepository.clearAiHistory()
                            Toast.makeText(context, "AI chat cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("clear_ai_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Language Selector Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages) { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { selectedLanguage = lang },
                    label = { Text(lang, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurpleAccent,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { (title, prefix) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { inputPrompt = prefix }
                        .border(0.5.dp, PurpleAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (messages.isEmpty() && streamingReply == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = PurpleAccent.copy(alpha = 0.7f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Namaste & Welcome to Hamza AI!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Ask me to translate, draft messages, check English grammar, or answer questions in English, Hindi, or Urdu.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                AiMessageBubble(
                    content = msg.content,
                    isUser = isUser,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Hamza AI", msg.content))
                        Toast.makeText(context, "Copied AI response", Toast.LENGTH_SHORT).show()
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, msg.content)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Hamza AI Response"))
                    }
                )
            }

            // Realtime streaming bubble
            if (streamingReply != null) {
                item {
                    AiMessageBubble(
                        content = streamingReply ?: "",
                        isUser = false,
                        isStreaming = true,
                        onCopy = {},
                        onShare = {}
                    )
                }
            }
        }

        // Input Area
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it },
                    placeholder = { Text("Ask Hamza AI anything...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input")
                )

                Spacer(Modifier.width(8.dp))

                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.5.dp,
                        color = PurpleAccent
                    )
                } else {
                    IconButton(
                        onClick = {
                            val promptToSend = inputPrompt.trim()
                            if (promptToSend.isNotBlank()) {
                                inputPrompt = ""
                                isGenerating = true
                                streamingReply = ""

                                scope.launch {
                                    val langCode = when {
                                        selectedLanguage.contains("Hindi") -> "hi"
                                        selectedLanguage.contains("Urdu") -> "ur"
                                        else -> "en"
                                    }

                                    aiRepository.sendPrompt(
                                        prompt = promptToSend,
                                        language = langCode,
                                        onChunkReceived = { chunk ->
                                            streamingReply = chunk
                                        }
                                    )
                                    streamingReply = null
                                    isGenerating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PurpleAccent)
                            .testTag("send_ai_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send to AI",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean = false,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Surface(
                shape = bubbleShape,
                color = if (isUser) PurpleAccent else MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.border(
                    width = 0.5.dp,
                    color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = bubbleShape
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!isUser) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Hamza AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Text(
                        text = content,
                        fontSize = 14.sp,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    if (isStreaming) {
                        Text(
                            text = "● typing...",
                            fontSize = 11.sp,
                            color = PurpleAccent,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (!isUser && !isStreaming && content.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = Color.Gray,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.Gray,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
