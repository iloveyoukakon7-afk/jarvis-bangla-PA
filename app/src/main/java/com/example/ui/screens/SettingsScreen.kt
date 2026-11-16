package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AssistantSettings
import com.example.data.OrbStatus
import com.example.ui.components.ArcReactorOrb
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.VelvetElevated
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.WarmCream

/**
 * 100% Voice-Driven Settings screen:
 * Chhotu automatically asks "বস, কোনটা চেঞ্জ করমু?" and applies changes by voice.
 * ১. আওয়াজ
 * ২. ভাষা (ঢাকাইয়া / গ্রাম্য)
 * ৩. মনে রাখা (১০ দিন / ৫ দিন)
 */
@Composable
fun SettingsScreen(
    settings: AssistantSettings,
    orbStatus: OrbStatus,
    audioLevel: Float,
    partialTranscript: String,
    pendingVoicePrompt: Boolean,
    onEnterVoiceSettings: () -> Unit,
    onLeaveVoiceSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onEnterVoiceSettings()
    }

    DisposableEffect(Unit) {
        onDispose {
            onLeaveVoiceSettings()
        }
    }

    val voiceSpeedLabel = when {
        settings.speechSpeed >= 1.15f -> "দ্রুত"
        settings.speechSpeed <= 0.90f -> "আস্তে"
        else -> "স্বাভাবিক"
    }
    val retentionLabel = if (settings.memoryRetentionDays == 5) "৫ দিন" else "১০ দিন"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianBlack, VelvetSurface, ObsidianBlack)
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ভয়েস সেটিংস",
                    style = MaterialTheme.typography.displaySmall,
                    color = BrightGold
                )
                Text(
                    text = "বস, কিছু ক্লিক করা লাগবে না — মুখে বললেই বদলে যাবে!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedSand
                )
            }

            // Active Voice Prompt Orb
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_settings_prompt_card"),
                colors = CardDefaults.cardColors(containerColor = VelvetElevated),
                border = BorderStroke(1.5.dp, RoyalGold),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArcReactorOrb(
                        status = orbStatus,
                        audioLevel = audioLevel,
                        wakeName = settings.wakeName,
                        onClick = onEnterVoiceSettings,
                        orbSize = 132.dp
                    )
                    Text(
                        text = if (pendingVoicePrompt) {
                            "\"বস, কোনটা চেঞ্জ করমু?\""
                        } else {
                            "ভয়েস দিয়া সেটিংস বলুন বস"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    if (partialTranscript.isNotBlank() && orbStatus == OrbStatus.LISTENING) {
                        Surface(
                            color = CharcoalPanel,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrightGold)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = BrightGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "\"$partialTranscript\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarmCream
                                )
                            }
                        }
                    }
                }
            }

            // ১. আওয়াজ (Read-only voice-controlled status card)
            VoiceSettingStatusCard(
                icon = Icons.Filled.VolumeUp,
                title = "১. আওয়াজ",
                currentValue = voiceSpeedLabel,
                voiceCommandHint = "মুখে কন: \"আওয়াজ দ্রুত কর\" • \"আওয়াজ আস্তে কর\" • \"স্বাভাবিক কর\"",
                testTag = "settings_voice_status_card"
            )

            // ২. ভাষা (ঢাকাইয়া / গ্রাম্য)
            VoiceSettingStatusCard(
                icon = Icons.Filled.RecordVoiceOver,
                title = "২. ভাষা",
                currentValue = settings.dialectMode.labelBangla,
                voiceCommandHint = "মুখে কন: \"ঢাকাইয়া কর\" অথবা \"গ্রাম্য কর\"",
                testTag = "settings_dialect_status_card"
            )

            // ৩. মনে রাখা (১০ দিন / ৫ দিন)
            VoiceSettingStatusCard(
                icon = Icons.Filled.History,
                title = "৩. মনে রাখা",
                currentValue = retentionLabel,
                voiceCommandHint = "মুখে কন: \"১০ দিন মনে রাখ\" অথবা \"৫ দিন মনে রাখ\"",
                testTag = "settings_memory_status_card"
            )
        }
    }
}

@Composable
private fun VoiceSettingStatusCard(
    icon: ImageVector,
    title: String,
    currentValue: String,
    voiceCommandHint: String,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
        border = BorderStroke(1.dp, BronzeBorder),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = RoyalGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = WarmCream
                    )
                }
                Surface(
                    color = RoyalGold,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.titleMedium,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                text = voiceCommandHint,
                style = MaterialTheme.typography.bodyMedium,
                color = ChampagneGold
            )
        }
    }
}
