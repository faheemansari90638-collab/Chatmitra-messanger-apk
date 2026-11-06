package com.example.ui.screens.ai

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HamzaAiConfig
import com.example.data.repository.AiRepository
import com.example.data.repository.SettingsRepository
import com.example.ui.components.Modern3DCard
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HamzaAiProfileScreen(
    aiRepository: AiRepository,
    settingsRepository: SettingsRepository,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedConfig by settingsRepository.hamzaAiConfigFlow.collectAsState(initial = HamzaAiConfig())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Configuration editable states initialized from savedConfig
    var currentTone by remember(savedConfig.tone) { mutableStateOf(savedConfig.tone) }
    var currentLength by remember(savedConfig.responseLength) { mutableStateOf(savedConfig.responseLength) }
    var currentEmoji by remember(savedConfig.emojiLevel) { mutableStateOf(savedConfig.emojiLevel) }
    var currentLanguage by remember(savedConfig.defaultLanguage) { mutableStateOf(savedConfig.defaultLanguage) }
    var currentPolishingStyle by remember(savedConfig.smartPolishingStyle) { mutableStateOf(savedConfig.smartPolishingStyle) }
    var customPrompt by remember(savedConfig.customInstructions) { mutableStateOf(savedConfig.customInstructions) }
    var explainGrammar by remember(savedConfig.grammarExplanation) { mutableStateOf(savedConfig.grammarExplanation) }

    // Live Tone Playground states
    var testPromptInput by remember { mutableStateOf("Say hello and introduce yourself in your active tone style!") }
    var testOutputReply by remember { mutableStateOf<String?>(null) }
    var isTestingTone by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_avatar")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Hamza AI Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Assistant",
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("ai_profile_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("reset_ai_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Preferences",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                .navigationBarsPadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Modern Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PurpleAccent
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "AI Profile & Specs",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "Tone & Style Config",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                if (selectedTabIndex == 0) {
                    // TAB 0: PROFILE & SPECS
                    item {
                        // 3D Avatar Hero Card
                        Modern3DCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_profile_hero_card"),
                            elevation = 6.dp
                        ) {
                            val heroGradient = Brush.linearGradient(
                                colors = listOf(
                                    PurpleAccent.copy(alpha = 0.15f),
                                    EmeraldPrimary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(heroGradient)
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    // Outer decorative glowing ring
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .scale(avatarScale)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        PurpleAccent.copy(alpha = 0.45f),
                                                        EmeraldPrimary.copy(alpha = 0.2f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                    // Main Avatar Orb
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .shadow(8.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(PurpleAccent, EmeraldDark)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Hamza AI Logo",
                                            tint = Color.White,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }
                                    // Active Status Dot
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(OnlineGreen)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "Hamza AI",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ChatMitra Messaging Intelligence Companion",
                                    fontSize = 13.sp,
                                    color = PurpleAccent,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(Modifier.height(10.dp))

                                // Active Persona Badge
                                Surface(
                                    color = EmeraldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Active Tone: $currentTone",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldDark
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Engine Specifications
                    item {
                        Text(
                            text = "Engine Architecture & Telemetry",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )

                        Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SpecItem(
                                    icon = Icons.Default.AutoAwesome,
                                    iconTint = PurpleAccent,
                                    label = "Core Engine",
                                    value = "Gemini 3.8 Flash (Firebase AI Logic)"
                                )
                                Spacer(Modifier.height(12.dp))
                                SpecItem(
                                    icon = Icons.Default.Translate,
                                    iconTint = EmeraldPrimary,
                                    label = "Fluency",
                                    value = "English, हिन्दी (Hindi), اردو (Urdu)"
                                )
                                Spacer(Modifier.height(12.dp))
                                SpecItem(
                                    icon = Icons.Default.Speed,
                                    iconTint = Color(0xFF339AF0),
                                    label = "Streaming Speed",
                                    value = "Realtime First-Token (<200ms)"
                                )
                                Spacer(Modifier.height(12.dp))
                                SpecItem(
                                    icon = Icons.Default.Security,
                                    iconTint = Color(0xFFE67700),
                                    label = "Chat Privacy",
                                    value = "Zero training retention • On-demand invocation only"
                                )
                            }
                        }
                    }

                    // Capability Highlights
                    item {
                        Text(
                            text = "What Hamza AI Does in ChatMitra",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CapabilityCard(
                                title = "Smart Message Polisher",
                                subtitle = "Refines rough ideas into polite, persuasive, or executive message drafts.",
                                icon = Icons.Default.FormatQuote,
                                iconBg = PurpleAccent
                            )
                            CapabilityCard(
                                title = "Cultural Multilingual Translation",
                                subtitle = "Translates between English, Hindi, and Urdu while preserving slang and emojis.",
                                icon = Icons.Default.Translate,
                                iconBg = EmeraldPrimary
                            )
                            CapabilityCard(
                                title = "Chat Summaries & Task Extraction",
                                subtitle = "Instantly condenses unread messages and surfaces dates, decisions, and todos.",
                                icon = Icons.Default.Lightbulb,
                                iconBg = Color(0xFF339AF0)
                            )
                        }
                    }

                    item {
                        // Quick Configure Shortcut Button
                        Button(
                            onClick = { selectedTabIndex = 1 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("open_tone_config_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Configure Tone & Response Style", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // TAB 1: TONE & RESPONSE CONFIGURATION
                    item {
                        Text(
                            text = "Select Tone Persona",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                        Text(
                            text = "Choose how Hamza AI communicates, drafts, and replies in your chats.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        val tones = listOf(
                            ToneOption(
                                name = "Friendly & Warm",
                                tag = "🌟 Recommended for Daily Chats",
                                description = "Casual, polite, warm, and approachable with natural conversational expressions.",
                                color = EmeraldPrimary
                            ),
                            ToneOption(
                                name = "Professional & Formal",
                                tag = "💼 Corporate & Executive",
                                description = "Polished, courteous, crisp sentences with proper workplace and business etiquette.",
                                color = PurpleAccent
                            ),
                            ToneOption(
                                name = "Concise & Direct",
                                tag = "⚡ Bullet Points & Fast",
                                description = "Short, to-the-point answers without small talk or pleasantries.",
                                color = Color(0xFF339AF0)
                            ),
                            ToneOption(
                                name = "Creative & Witty",
                                tag = "🎨 Playful & Engaging",
                                description = "Expressive, witty, engaging wordplay, and charming analogies.",
                                color = Color(0xFFF06595)
                            ),
                            ToneOption(
                                name = "Empathetic & Gentle",
                                tag = "🕊️ Soothing & Supportive",
                                description = "Patient, gentle, reassuring, thoughtful, and deeply considerate.",
                                color = Color(0xFF20C997)
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            tones.forEach { tone ->
                                val isSelected = currentTone == tone.name
                                ToneCard(
                                    tone = tone,
                                    isSelected = isSelected,
                                    onSelect = { currentTone = tone.name }
                                )
                            }
                        }
                    }

                    // Formatting & Length Preferences
                    item {
                        Text(
                            text = "Response Formatting",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp)
                        )

                        Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Response Length",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Concise", "Balanced", "Detailed").forEach { length ->
                                        val isSel = currentLength == length
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { currentLength = length },
                                            color = if (isSel) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = length,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    text = "Emoji Usage",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("None", "Moderate", "Expressive").forEach { emoji ->
                                        val isSel = currentEmoji == emoji
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { currentEmoji = emoji },
                                            color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = emoji,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // Grammar explanation toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Explain Grammar Rules",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Include gentle educational notes explaining why text was revised",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = explainGrammar,
                                        onCheckedChange = { explainGrammar = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = EmeraldPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Custom Persona Instructions
                    item {
                        Text(
                            text = "Custom Persona Instructions (Optional)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                        Text(
                            text = "Add custom instructions that Hamza AI should always follow for you.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Modern3DCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = customPrompt,
                                    onValueChange = { customPrompt = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_instructions_input"),
                                    placeholder = {
                                        Text(
                                            "e.g. Always address me as Boss, use conversational Hinglish where fitting, keep replies under 3 sentences...",
                                            fontSize = 12.sp
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 5,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PurpleAccent,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "Quick Presets:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(6.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val presets = listOf(
                                        "+ Address me respectfully",
                                        "+ Keep under 2 sentences",
                                        "+ Simple everyday words",
                                        "+ Add motivating quote",
                                        "+ Suggest a follow-up"
                                    )
                                    presets.forEach { preset ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val clean = preset.removePrefix("+ ")
                                                    customPrompt = if (customPrompt.isBlank()) clean else "$customPrompt. $clean"
                                                },
                                            color = PurpleAccent.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, PurpleAccent.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = preset,
                                                fontSize = 11.sp,
                                                color = PurpleAccent,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Live Tone Playground
                    item {
                        Text(
                            text = "Live Tone Sandbox & Preview",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                        Text(
                            text = "Test your selected tone live before saving.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Modern3DCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tone_playground_card"),
                            elevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = testPromptInput,
                                    onValueChange = { testPromptInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("tone_test_input"),
                                    label = { Text("Test Prompt", fontSize = 12.sp) },
                                    maxLines = 3,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tone: $currentTone • $currentLength",
                                        fontSize = 11.sp,
                                        color = PurpleAccent,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Button(
                                        onClick = {
                                            if (testPromptInput.isNotBlank()) {
                                                isTestingTone = true
                                                testOutputReply = null
                                                val previewConfig = HamzaAiConfig(
                                                    tone = currentTone,
                                                    responseLength = currentLength,
                                                    emojiLevel = currentEmoji,
                                                    defaultLanguage = currentLanguage,
                                                    smartPolishingStyle = currentPolishingStyle,
                                                    customInstructions = customPrompt,
                                                    grammarExplanation = explainGrammar
                                                )
                                                scope.launch {
                                                    val res = aiRepository.testToneReply(testPromptInput, previewConfig)
                                                    res.onSuccess { reply ->
                                                        testOutputReply = reply
                                                    }.onFailure { err ->
                                                        testOutputReply = "Test preview error: ${err.message}"
                                                    }
                                                    isTestingTone = false
                                                }
                                            }
                                        },
                                        enabled = !isTestingTone && testPromptInput.isNotBlank(),
                                        modifier = Modifier.testTag("run_tone_test_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        if (isTestingTone) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Generating...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Test Tone", fontSize = 12.sp)
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = testOutputReply != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PurpleAccent.copy(alpha = 0.08f))
                                            .border(1.dp, PurpleAccent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PurpleAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Hamza AI ($currentTone preview):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PurpleAccent
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = testOutputReply.orEmpty(),
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Save Button
                    item {
                        Button(
                            onClick = {
                                val updated = HamzaAiConfig(
                                    tone = currentTone,
                                    responseLength = currentLength,
                                    emojiLevel = currentEmoji,
                                    defaultLanguage = currentLanguage,
                                    smartPolishingStyle = currentPolishingStyle,
                                    customInstructions = customPrompt,
                                    grammarExplanation = explainGrammar
                                )
                                scope.launch {
                                    settingsRepository.updateHamzaAiConfig(updated)
                                    aiRepository.activeConfig = updated
                                    Toast.makeText(context, "Hamza AI Tone preferences saved! ✨", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_ai_tone_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Save AI Tone & Preferences",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Reset Defaults Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset AI Tone Preferences?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will restore Hamza AI's tone to Friendly & Warm with default response styling.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            val def = HamzaAiConfig()
                            settingsRepository.resetHamzaAiConfig()
                            aiRepository.activeConfig = def
                            currentTone = def.tone
                            currentLength = def.responseLength
                            currentEmoji = def.emojiLevel
                            currentLanguage = def.defaultLanguage
                            currentPolishingStyle = def.smartPolishingStyle
                            customPrompt = def.customInstructions
                            explainGrammar = def.grammarExplanation
                            Toast.makeText(context, "Hamza AI preferences reset to defaults", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class ToneOption(
    val name: String,
    val tag: String,
    val description: String,
    val color: Color
)

@Composable
private fun ToneCard(
    tone: ToneOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Modern3DCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tone_option_${tone.name.lowercase().replace(" ", "_")}"),
        elevation = if (isSelected) 5.dp else 2.dp,
        backgroundColor = if (isSelected) tone.color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) tone.color else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tone.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tone.tag,
                        fontSize = 11.sp,
                        color = tone.color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tone.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SpecItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CapabilityCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color
) {
    Modern3DCard(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBg,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
