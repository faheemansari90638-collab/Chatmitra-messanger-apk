package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Message
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: Message,
    isOutgoing: Boolean,
    currentUserId: String = "",
    onLongClick: () -> Unit,
    onClick: () -> Unit = {},
    onVotePoll: (Int) -> Unit = {},
    onStopLiveLocation: () -> Unit = {},
    onRsvpEvent: (String) -> Unit = {}, // "going", "maybe", "decline"
    onTranscribeAudio: () -> Unit = {},
    onTranslateMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    val outgoingGradient = Brush.horizontalGradient(
        colors = listOf(EmeraldPrimary, EmeraldDark)
    )

    val timeString = if (message.createdAt != null) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.createdAt.seconds * 1000))
    } else "Just now"

    var isVoicePlaying by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = bubbleShape,
                shadowElevation = if (isOutgoing) 3.dp else 1.5.dp,
                color = if (isOutgoing) Color.Transparent else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .border(
                        width = 0.5.dp,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = bubbleShape
                    )
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .testTag("chat_bubble_${message.id}")
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (isOutgoing) Modifier.background(outgoingGradient)
                            else Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        // Sender name in group conversations for incoming messages
                        if (!isOutgoing && message.senderName.isNotBlank()) {
                            Text(
                                text = message.senderName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleAccent,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Reply preview block
                        if (!message.replyToText.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isOutgoing) Color.Black.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isOutgoing) Color.White.copy(alpha = 0.3f) else EmeraldPrimary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = message.replyToText,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    color = if (isOutgoing) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Scheduled message banner
                        if (message.isScheduled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF339AF0).copy(alpha = 0.2f))
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF339AF0), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Scheduled Message", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        // Media / Specialized Message Types
                        when (message.type) {
                            "image" -> {
                                if (!message.mediaUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = "Message media",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .padding(bottom = 6.dp)
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = if (isOutgoing) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = message.mediaName ?: "Photo",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            "document" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isOutgoing) Color.Black.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (isOutgoing) Color.White else EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = message.mediaName ?: "Document",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (message.mediaSize != null) {
                                            Text(
                                                text = message.mediaSize,
                                                fontSize = 10.sp,
                                                color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                            "audio", "voice" -> {
                                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { isVoicePlaying = !isVoicePlaying },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isOutgoing) Color.White.copy(alpha = 0.25f) else EmeraldPrimary.copy(alpha = 0.15f))
                                        ) {
                                            Icon(
                                                imageVector = if (isVoicePlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "Play voice",
                                                tint = if (isOutgoing) Color.White else EmeraldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (isVoicePlaying) "Playing..." else "Voice note (0:15)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (message.transcribedText.isNullOrBlank()) {
                                                    Text(
                                                        text = "Transcribe",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isOutgoing) Color.White else EmeraldPrimary,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .clickable { onTranscribeAudio() }
                                                            .padding(2.dp)
                                                    )
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { if (isVoicePlaying) 0.6f else 0.0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .height(3.dp),
                                                color = if (isOutgoing) Color.White else EmeraldPrimary,
                                                trackColor = if (isOutgoing) Color.White.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    // Voice-to-Text Transcription Banner
                                    if (!message.transcribedText.isNullOrBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isOutgoing) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                .padding(6.dp)
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isOutgoing) Color.White else EmeraldPrimary)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Transcribed Text", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary)
                                                }
                                                Text(
                                                    text = message.transcribedText,
                                                    fontSize = 12.sp,
                                                    color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "poll" -> {
                                // Group Poll Component
                                PollView(
                                    pollQuestion = message.content,
                                    pollDataRaw = message.pollData,
                                    currentUserId = currentUserId,
                                    isOutgoing = isOutgoing,
                                    onVote = onVotePoll
                                )
                            }
                            "location" -> {
                                // Live Location Component
                                LiveLocationView(
                                    locationDataRaw = message.locationData,
                                    isOutgoing = isOutgoing,
                                    onStopSharing = onStopLiveLocation
                                )
                            }
                            "event" -> {
                                // Group Event Component
                                GroupEventView(
                                    eventDataRaw = message.eventData,
                                    currentUserId = currentUserId,
                                    isOutgoing = isOutgoing,
                                    onRsvp = onRsvpEvent
                                )
                            }
                        }

                        // Message text (for standard text or caption)
                        if (message.content.isNotBlank() && message.type !in listOf("poll", "location", "event")) {
                            Text(
                                text = message.content,
                                fontSize = 14.sp,
                                fontStyle = if (message.isDeleted) FontStyle.Italic else FontStyle.Normal,
                                color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Translation Banner
                        if (!message.translatedText.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOutgoing) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isOutgoing) Color.White else EmeraldPrimary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Translation", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = message.translatedText,
                                        fontSize = 13.sp,
                                        color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        // Metadata row (Timestamp, edit flag, star, pin, read receipt, disappearing icon)
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        ) {
                            if (message.disappearingDurationSeconds != null) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Disappearing",
                                    tint = if (isOutgoing) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            if (message.isStarred) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Starred",
                                    tint = if (isOutgoing) Color.White.copy(alpha = 0.8f) else Color(0xFFFFC107),
                                    modifier = Modifier
                                        .size(12.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            if (message.isPinned) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = if (isOutgoing) Color.White.copy(alpha = 0.8f) else PurpleAccent,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            if (message.isEdited) {
                                Text(
                                    text = "edited ",
                                    fontSize = 10.sp,
                                    color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else Color.Gray
                                )
                            }

                            Text(
                                text = timeString,
                                fontSize = 10.sp,
                                color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else Color.Gray
                            )

                            if (isOutgoing) {
                                Spacer(Modifier.width(4.dp))
                                MessageStatusTick(status = message.status)
                            }
                        }
                    }
                }
            }

            // Reactions pill attached beneath bubble
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    message.reactions.values.distinct().take(4).forEach { emoji ->
                        Text(text = emoji, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PollView(
    pollQuestion: String,
    pollDataRaw: String?,
    currentUserId: String,
    isOutgoing: Boolean,
    onVote: (Int) -> Unit
) {
    // Parse options from raw string
    // Format: "opt1:uid1,uid2|opt2:uid3" or fallback
    val optionsList = remember(pollDataRaw) {
        if (!pollDataRaw.isNullOrBlank()) {
            pollDataRaw.split("|").mapIndexed { idx, part ->
                val pieces = part.split("::")
                val text = pieces.getOrNull(0) ?: "Option ${idx + 1}"
                val voters = pieces.getOrNull(1)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                Triple(idx, text, voters)
            }
        } else {
            listOf(
                Triple(0, "Yes", emptyList()),
                Triple(1, "No", emptyList())
            )
        }
    }

    val totalVotes = optionsList.sumOf { it.third.size }

    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Poll, contentDescription = null, tint = if (isOutgoing) Color.White else EmeraldPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = pollQuestion.ifBlank { "Group Poll" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))

        optionsList.forEach { (optId, optText, voters) ->
            val hasVoted = voters.contains(currentUserId)
            val votePercent = if (totalVotes > 0) voters.size.toFloat() / totalVotes else 0f

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (hasVoted) (if (isOutgoing) Color.White.copy(alpha = 0.25f) else EmeraldPrimary.copy(alpha = 0.15f))
                        else (if (isOutgoing) Color.Black.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onVote(optId) }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (hasVoted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (isOutgoing) Color.White else EmeraldPrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = optText,
                                fontSize = 12.sp,
                                fontWeight = if (hasVoted) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${voters.size} votes",
                            fontSize = 11.sp,
                            color = if (isOutgoing) Color.White.copy(alpha = 0.8f) else Color.Gray
                        )
                    }
                    LinearProgressIndicator(
                        progress = { votePercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isOutgoing) Color.White else EmeraldPrimary,
                        trackColor = if (isOutgoing) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveLocationView(
    locationDataRaw: String?,
    isOutgoing: Boolean,
    onStopSharing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Live Location Sharing",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isOutgoing) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = if (isOutgoing) Color.White else EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = locationDataRaw ?: "Active GPS location broadcast",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Live for 1 hour • Updated real-time",
                            fontSize = 10.sp,
                            color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                }

                if (isOutgoing) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onStopSharing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop Sharing Location", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupEventView(
    eventDataRaw: String?,
    currentUserId: String,
    isOutgoing: Boolean,
    onRsvp: (String) -> Unit
) {
    // Format: "Title::Date::Location::goingUid1,goingUid2::maybeUid::declineUid"
    val parts = eventDataRaw?.split("::") ?: emptyList()
    val eventTitle = parts.getOrNull(0) ?: "Group Event"
    val eventDate = parts.getOrNull(1) ?: "Tomorrow, 6:00 PM"
    val eventLocation = parts.getOrNull(2) ?: "Online / Voice Channel"
    val goingList = parts.getOrNull(3)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val maybeList = parts.getOrNull(4)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    val isGoing = goingList.contains(currentUserId)
    val isMaybe = maybeList.contains(currentUserId)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isOutgoing) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (isOutgoing) Color.White else PurpleAccent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = eventTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            Text(text = "🕒 $eventDate", fontSize = 11.sp, color = if (isOutgoing) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "📍 $eventLocation", fontSize = 11.sp, color = if (isOutgoing) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onRsvp("going") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGoing) EmeraldPrimary else (if (isOutgoing) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text("Going (${goingList.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isGoing) Color.White else (if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface))
                }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = { onRsvp("maybe") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMaybe) PurpleAccent else (if (isOutgoing) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Text("Maybe (${maybeList.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isMaybe) Color.White else (if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface))
                }
            }
        }
    }
}
