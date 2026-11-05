package com.example.ui.screens.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.CallRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveCallScreen(
    callId: String,
    targetName: String,
    callType: String,
    callRepository: CallRepository,
    onCallEnded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(callType == "video") }
    var isScreenSharing by remember { mutableStateOf(false) }
    var showScreenSharePermissionDialog by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var callStatusText by remember { mutableStateOf("Connecting...") }

    // Duration timer ticker
    LaunchedEffect(Unit) {
        delay(1500)
        callStatusText = "Connected • Secure WebRTC"
        while (true) {
            delay(1000)
            durationSeconds++
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val durationFormatted = "%02d:%02d".format(minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Call Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 30.dp)
            ) {
                Text(
                    text = targetName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (durationSeconds > 0) durationFormatted else callStatusText,
                    fontSize = 15.sp,
                    color = if (durationSeconds > 0) EmeraldPrimary else Color.LightGray,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (callType == "video") "HD Video Call (Encrypted)" else "Voice Call (Encrypted)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Active Screen Sharing Banner
                AnimatedVisibility(visible = isScreenSharing) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldPrimary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ScreenShare, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("You are sharing your phone screen", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "STOP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        isScreenSharing = false
                                        scope.launch { callRepository.toggleScreenSharing(callId, false) }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Pulsing Avatar or Video View
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer glow ring
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (callType == "video") PurpleAccent.copy(alpha = 0.4f) else EmeraldPrimary.copy(alpha = 0.4f),
                            CircleShape
                        )
                )

                AvatarView(
                    name = targetName,
                    size = 120.dp,
                    isOnline = true,
                    showOnlineIndicator = false
                )
            }

            // Bottom In-Call Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Microphone
                    ControlCircleButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        isActive = isMuted,
                        onClick = { isMuted = !isMuted }
                    )

                    // Speaker
                    ControlCircleButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                        isActive = isSpeakerOn,
                        onClick = { isSpeakerOn = !isSpeakerOn }
                    )

                    // Screen Share toggle
                    ControlCircleButton(
                        icon = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                        isActive = isScreenSharing,
                        onClick = {
                            if (!isScreenSharing) {
                                showScreenSharePermissionDialog = true
                            } else {
                                isScreenSharing = false
                                scope.launch { callRepository.toggleScreenSharing(callId, false) }
                            }
                        }
                    )

                    // Video toggle
                    if (callType == "video") {
                        ControlCircleButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            isActive = isVideoEnabled,
                            onClick = { isVideoEnabled = !isVideoEnabled }
                        )

                        // Camera Flip
                        ControlCircleButton(
                            icon = Icons.Default.Cameraswitch,
                            isActive = false,
                            onClick = {}
                        )
                    }
                }

                Spacer(Modifier.height(30.dp))

                // End Call Button
                IconButton(
                    onClick = {
                        scope.launch {
                            callRepository.endCall(callId, durationSeconds)
                            onCallEnded()
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(DangerRed)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // MediaProjection screen sharing consent dialog
    if (showScreenSharePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showScreenSharePermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ScreenShare, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Screen Sharing?")
                }
            },
            text = {
                Text(
                    "ChatMitra will start capturing and streaming everything displayed on your screen to $targetName during this call. You can tap 'Stop' at any time to immediately end the broadcast.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScreenSharePermissionDialog = false
                        isScreenSharing = true
                        scope.launch {
                            callRepository.toggleScreenSharing(callId, true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Start Sharing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenSharePermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ControlCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                if (isActive) EmeraldPrimary else Color(0xFF252A38)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
