package com.example.ui.screens.calls

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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecord
import com.example.data.repository.CallRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PurpleAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallsListScreen(
    callRepository: CallRepository,
    currentUserId: String,
    onStartCall: (String, String) -> Unit, // targetName, callType
    onOpenContacts: () -> Unit
) {
    val calls by callRepository.observeCalls().collectAsState(initial = emptyList())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenContacts,
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .testTag("new_call_fab")
            ) {
                Icon(Icons.Default.AddIcCall, contentDescription = "New Call")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "Recent Calls",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            if (calls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = EmeraldPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "No calls yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "To start an audio or HD video call with friends, tap the call button below.",
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
                    items(calls, key = { it.id }) { call ->
                        CallItem(
                            call = call,
                            currentUserId = currentUserId,
                            onCallback = {
                                val target = if (call.callerId == currentUserId) call.receiverName else call.callerName
                                onStartCall(target, call.callType)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallItem(
    call: CallRecord,
    currentUserId: String,
    onCallback: () -> Unit
) {
    val isOutgoing = call.callerId == currentUserId
    val displayName = if (isOutgoing) call.receiverName else call.callerName
    val isMissed = call.status == "missed" || call.status == "rejected"

    val timeFormatted = call.createdAt?.let {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(it.seconds * 1000))
    } ?: "Recent"

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
                name = displayName,
                size = 46.dp,
                isOnline = false
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMissed) DangerRed else MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isMissed -> Icons.AutoMirrored.Filled.CallMissed
                            isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                            else -> Icons.AutoMirrored.Filled.CallReceived
                        },
                        contentDescription = null,
                        tint = when {
                            isMissed -> DangerRed
                            isOutgoing -> OnlineGreen
                            else -> EmeraldPrimary
                        },
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (call.durationSeconds != null && call.durationSeconds > 0) {
                        Text(
                            text = " • ${call.durationSeconds / 60}m ${call.durationSeconds % 60}s",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onCallback,
                modifier = Modifier.testTag("call_again_button")
            ) {
                Icon(
                    imageVector = if (call.callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Call back",
                    tint = if (call.callType == "video") PurpleAccent else EmeraldPrimary
                )
            }
        }
    }
}
