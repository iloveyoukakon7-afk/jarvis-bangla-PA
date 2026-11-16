package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AssistantSettings
import com.example.data.ConversationEntry
import com.example.data.OrbStatus
import com.example.data.PendingBrowserUpdatePrompt
import com.example.data.PendingPermissionPrompt
import com.example.ui.components.ArcReactorOrb
import com.example.ui.components.HomeShortcutDialog
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.CrimsonShield
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.VelvetElevated
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.WarmCream

@Composable
fun AssistantHudScreen(
    settings: AssistantSettings,
    learningDay: Int,
    orbStatus: OrbStatus,
    audioLevel: Float,
    partialTranscript: String,
    conversations: List<ConversationEntry>,
    pendingPermission: PendingPermissionPrompt?,
    pendingBrowserUpdate: PendingBrowserUpdatePrompt?,
    errorBanner: String?,
    onOrbClick: () -> Unit,
    onShortcutNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showShortcutDialog by rememberSaveable { mutableStateOf(false) }

    val latestBossEntry = conversations.lastOrNull { it.isBoss }
    val latestServantEntry = conversations.lastOrNull { !it.isBoss }

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
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Minimal Voice Status Strip (no typing, no clutter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = CharcoalPanel,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Text(
                        text = "চেনার ${learningDay}ম দিন • ${settings.dialectMode.labelBangla}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                Surface(
                    color = if (settings.wakeWordActive) {
                        RoyalGold.copy(alpha = 0.2f)
                    } else {
                        CharcoalPanel
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (settings.wakeWordActive) BrightGold else BronzeBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Hearing,
                            contentDescription = "ওই ছোটু চালু",
                            tint = if (settings.wakeWordActive) BrightGold else MutedSand,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (settings.wakeWordActive) "\"ওই ছোটু\" চালু" else "ভয়েস বন্ধ",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmCream
                        )
                    }
                }

                Surface(
                    color = CharcoalPanel,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BronzeBorder)
                ) {
                    Text(
                        text = settings.bossSchedule.labelBangla,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settings.bossSchedule.name == "FREE") EmeraldSafe else BrightGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                if (settings.autoRecordCalls) {
                    Surface(
                        color = CrimsonShield.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CrimsonShield)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FiberManualRecord,
                                contentDescription = null,
                                tint = CrimsonShield,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "অটো রেকর্ড",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmCream
                            )
                        }
                    }
                }

                AssistChip(
                    onClick = { showShortcutDialog = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AddToHomeScreen,
                            contentDescription = "হোম শর্টকাট",
                            tint = RoyalGold,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("শর্টকাট") },
                    modifier = Modifier.testTag("hud_shortcut_chip"),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = CharcoalPanel,
                        labelColor = WarmCream
                    ),
                    border = BorderStroke(1.dp, BronzeBorder)
                )
            }

            // Centerpiece Golden Voice Orb
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ArcReactorOrb(
                    status = orbStatus,
                    audioLevel = audioLevel,
                    wakeName = settings.wakeName,
                    onClick = onOrbClick,
                    orbSize = 216.dp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Live Boss Voice Hearing Preview
                if (partialTranscript.isNotBlank() &&
                    (orbStatus == OrbStatus.LISTENING || orbStatus == OrbStatus.WAKE_STANDBY)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_voice_transcript_card"),
                        colors = CardDefaults.cardColors(containerColor = VelvetElevated),
                        border = BorderStroke(1.dp, BrightGold),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = BrightGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "\"$partialTranscript\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ChampagneGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Voice-Only "হ" / "না" Approval Status Card
                if (pendingPermission != null) {
                    VoiceQuestionBanner(
                        icon = Icons.Filled.GraphicEq,
                        title = "অনুমতি দরকার বস (মুখে কন)",
                        subtitle = pendingPermission.questionBangla,
                        hint = "মাইক চালু আছে — মুখে \"হ\" অথবা \"না\" কন বস"
                    )
                } else if (pendingBrowserUpdate != null) {
                    VoiceQuestionBanner(
                        icon = Icons.Filled.Public,
                        title = "এআই স্টুডিও আপডেট (মুখে কন)",
                        subtitle = pendingBrowserUpdate.questionBangla,
                        hint = "মাইক চালু আছে — মুখে \"হ\" অথবা \"না\" কন বস"
                    )
                } else {
                    // Spoken Audio Subtitle Card (Voice-first — Boss hears aloud, minimal visual sync)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spoken_voice_status_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (latestServantEntry?.isBlocked == true) {
                                CrimsonShield.copy(alpha = 0.16f)
                            } else {
                                CharcoalPanel
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (latestServantEntry?.isBlocked == true) CrimsonShield else BronzeBorder
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (latestBossEntry != null) {
                                Text(
                                    text = "বস কইছেন: \"${latestBossEntry.text}\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedSand
                                )
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "ছোটুর ভয়েস উত্তর",
                                    tint = if (latestServantEntry?.isBlocked == true) {
                                        CrimsonShield
                                    } else {
                                        BrightGold
                                    },
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = latestServantEntry?.text
                                        ?: "সালাম বস! \"ওই ছোটু\" বইলা মুখে যা ইচ্ছা কন।",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmCream
                                )
                            }
                            if (!latestServantEntry?.actionBadge.isNullOrBlank()) {
                                Surface(
                                    color = RoyalGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = latestServantEntry?.actionBadge.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrightGold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!errorBanner.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = CrimsonShield.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CrimsonShield)
                    ) {
                        Text(
                            text = errorBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmCream,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Bottom Pure Voice Prompt Footer (NO Text Field, NO Typing, NO Click Required)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_only_footer"),
                color = CharcoalPanel.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "পুরোপুরি ভয়েস সিস্টেম — কোনো টাইপিং বা ক্লিক লাগবে না",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrightGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "কন: \"ওই ছোটু শামীমরে ক\" • \"সব কল রেকর্ড কর\" • \"সেটিংস চেঞ্জ কর\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSand,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showShortcutDialog) {
        HomeShortcutDialog(
            wakeName = settings.wakeName,
            onShortcutResult = { _, msg -> onShortcutNotice(msg) },
            onDismiss = { showShortcutDialog = false }
        )
    }
}

@Composable
private fun VoiceQuestionBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    hint: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VelvetElevated),
        border = BorderStroke(1.5.dp, BrightGold),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BrightGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrightGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = WarmCream
            )
            Surface(
                color = RoyalGold.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneGold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
