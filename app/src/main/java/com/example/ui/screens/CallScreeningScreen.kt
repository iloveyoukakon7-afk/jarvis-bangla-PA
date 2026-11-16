package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BossScheduleStatus
import com.example.data.CallChannel
import com.example.data.CallRecordingEntity
import com.example.data.CallScreenLogEntry
import com.example.data.CallScreenPhase
import com.example.data.CallScreenSession
import com.example.service.CallScreeningCoordinator
import com.example.ui.theme.AmberAlert
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
fun CallScreeningScreen(
    callScreeningEnabled: Boolean,
    autoRecordCalls: Boolean,
    bossSchedule: BossScheduleStatus,
    session: CallScreenSession,
    logs: List<CallScreenLogEntry>,
    recordings: List<CallRecordingEntity>,
    playingRecordingPath: String?,
    onToggleCallScreening: (Boolean) -> Unit,
    onToggleAutoRecord: (Boolean) -> Unit,
    onSelectSchedule: (BossScheduleStatus) -> Unit,
    onStartLiveTest: (callerNumber: String, callerName: String, channel: CallChannel, isVideo: Boolean) -> Unit,
    onListenCallerVoice: () -> Unit,
    onListenBossVoiceDecision: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onFinishCallKeepOrDelete: (keepRecording: Boolean) -> Unit,
    onResetSession: () -> Unit,
    onPlayRecording: (CallRecordingEntity) -> Unit,
    onKeepRecording: (Long) -> Unit,
    onDeleteRecording: (CallRecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var roleCheckNonce by remember { mutableStateOf(0) }
    val isScreeningRoleHeld = remember(roleCheckNonce) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        roleCheckNonce++
    }

    var selectedCallerPreset by rememberSaveable { mutableStateOf("শামীম ভাই") }
    var selectedChannel by rememberSaveable { mutableStateOf(CallChannel.WHATSAPP) }
    var isVideoCall by rememberSaveable { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "দারোয়ান ও কল রেকর্ড",
                style = MaterialTheme.typography.headlineMedium,
                color = BrightGold
            )
            Text(
                text = "ফোন কল, হোয়াটসঅ্যাপ, ইমো, মেসেঞ্জার, ব্রিলিয়ান্ট — সব কল আগে ছোটু ধরবে!",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand
            )

            // Darowan Controls + Auto Record + Boss Schedule
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "দারোয়ান অটো কল রিসিভ",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = if (callScreeningEnabled) "চালু — ছোটু আগে কথা কইব" else "বন্ধ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (callScreeningEnabled) EmeraldSafe else MutedSand
                            )
                        }
                        Switch(
                            checked = callScreeningEnabled,
                            onCheckedChange = onToggleCallScreening,
                            modifier = Modifier.testTag("call_screening_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = RoyalGold
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সব কল অটো রেকর্ড কর",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = "রেকর্ডের আগে কলাররে জানাইয়া দিবে 'কলটি রেকর্ড হচ্ছে'",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (autoRecordCalls) CrimsonShield else MutedSand
                            )
                        }
                        Switch(
                            checked = autoRecordCalls,
                            onCheckedChange = onToggleAutoRecord,
                            modifier = Modifier.testTag("auto_record_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WarmCream,
                                checkedTrackColor = CrimsonShield
                            )
                        )
                    }

                    Text(
                        text = "বসের এখনকার শিডিউল:",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrightGold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BossScheduleStatus.entries.forEach { st ->
                            val selected = bossSchedule == st
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectSchedule(st) },
                                label = { Text(st.labelBangla) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RoyalGold,
                                    selectedLabelColor = ObsidianBlack,
                                    containerColor = VelvetSurface,
                                    labelColor = WarmCream
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isScreeningRoleHeld) {
                            OutlinedButton(
                                onClick = {
                                    val rm =
                                        context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                                        val intent: Intent =
                                            rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                        roleLauncher.launch(intent)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, RoyalGold)
                            ) {
                                Text("ফোন রোল চালু", color = RoyalGold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            border = BorderStroke(1.dp, BronzeBorder)
                        ) {
                            Text("ইমো/হোয়াটসঅ্যাপ কল", color = WarmCream)
                        }
                    }
                }
            }

            // Active Screening Session OR Multi-Platform Darowan Tester
            if (session.phase == CallScreenPhase.IDLE) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "দারোয়ান কল পরীক্ষা (সব অ্যাপ)",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrightGold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CallChannel.entries.forEach { ch ->
                                FilterChip(
                                    selected = selectedChannel == ch,
                                    onClick = { selectedChannel = ch },
                                    label = { Text(ch.labelBangla) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalGold,
                                        selectedLabelColor = ObsidianBlack,
                                        containerColor = VelvetSurface,
                                        labelColor = WarmCream
                                    )
                                )
                            }
                            FilterChip(
                                selected = isVideoCall,
                                onClick = { isVideoCall = !isVideoCall },
                                label = { Text(if (isVideoCall) "ভিডিও কল" else "অডিও কল") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberAlert,
                                    selectedLabelColor = ObsidianBlack,
                                    containerColor = VelvetSurface,
                                    labelColor = WarmCream
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("শামীম ভাই", "আব্বু", "অজানা নম্বর").forEach { preset ->
                                FilterChip(
                                    selected = selectedCallerPreset == preset,
                                    onClick = { selectedCallerPreset = preset },
                                    label = { Text(preset) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrightGold,
                                        selectedLabelColor = ObsidianBlack,
                                        containerColor = VelvetSurface,
                                        labelColor = WarmCream
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val number = when (selectedCallerPreset) {
                                    "শামীম ভাই" -> "+8801711-223344"
                                    "আব্বু" -> "+8801811-556677"
                                    else -> "+8801900-000000"
                                }
                                onStartLiveTest(
                                    number,
                                    if (selectedCallerPreset == "অজানা নম্বর") "" else selectedCallerPreset,
                                    selectedChannel,
                                    isVideoCall
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_call_screen_test_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "দারোয়ান দিয়া ভয়েস কল ধরো",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                ActiveDarowanSessionCard(
                    session = session,
                    onListenCallerVoice = onListenCallerVoice,
                    onListenBossVoiceDecision = onListenBossVoiceDecision,
                    onAcceptCall = onAcceptCall,
                    onRejectCall = onRejectCall,
                    onFinishCallKeepOrDelete = onFinishCallKeepOrDelete,
                    onResetSession = onResetSession
                )
            }

            // Call Recordings List (Keep or Auto-Delete)
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "কল রেকর্ড স্টোরেজ",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrightGold
                    )
                    if (recordings.isEmpty()) {
                        Text(
                            text = "কোনো সংরক্ষিত কল রেকর্ড নাই বস।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSand
                        )
                    } else {
                        recordings.forEach { rec ->
                            val isPlaying = playingRecordingPath == rec.filePath
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VelvetElevated),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onPlayRecording(rec) }) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                            contentDescription = "শুনুন",
                                            tint = RoyalGold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${rec.callerDisplay} (${rec.channelLabel})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WarmCream
                                        )
                                        Text(
                                            text = "${rec.durationSec} সেকেন্ড • ${if (rec.keepForever) "স্থায়ী রাখা" else "অস্থায়ী"}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (rec.keepForever) EmeraldSafe else MutedSand
                                        )
                                    }
                                    if (!rec.keepForever) {
                                        IconButton(onClick = { onKeepRecording(rec.id) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Bookmark,
                                                contentDescription = "রেখে দাও",
                                                tint = BrightGold
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteRecording(rec) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "ডিলিট",
                                            tint = CrimsonShield
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Screened Call Logs
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "দারোয়ানের কল তালিকা",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrightGold
                    )
                    if (logs.isEmpty()) {
                        Text(
                            text = "এখনো কোনো কল আসে নাই বস।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSand
                        )
                    } else {
                        logs.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VelvetElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.callerDisplay} • ${item.channelLabel}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = WarmCream
                                        )
                                        Text(
                                            text = item.callerReason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedSand
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = item.decisionLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (item.decisionLabel.contains("সংযুক্ত")) {
                                                EmeraldSafe
                                            } else {
                                                CrimsonShield
                                            }
                                        )
                                        Text(
                                            text = item.timestampLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedSand
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDarowanSessionCard(
    session: CallScreenSession,
    onListenCallerVoice: () -> Unit,
    onListenBossVoiceDecision: () -> Unit,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onFinishCallKeepOrDelete: (keepRecording: Boolean) -> Unit,
    onResetSession: () -> Unit
) {
    val channelTitle = if (session.isVideoCall) {
        "${session.channel.labelBangla} ভিডিও কল"
    } else {
        session.channel.labelBangla
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = VelvetElevated),
        border = BorderStroke(2.dp, if (session.isEmergency) CrimsonShield else RoyalGold)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "কল আসছে ($channelTitle): ${session.knownContactName.ifBlank { session.callerNumber }}",
                style = MaterialTheme.typography.titleLarge,
                color = if (session.isEmergency) CrimsonShield else BrightGold
            )

            Text(
                text = "ছোটু কলাররে কইতাছে:",
                style = MaterialTheme.typography.labelLarge,
                color = RoyalGold
            )
            Text(
                text = "\"${CallScreeningCoordinator.INITIAL_CALLER_GREETING}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = WarmCream
            )

            if (session.phase == CallScreenPhase.GREETING_CALLER ||
                session.phase == CallScreenPhase.LISTENING_CALLER ||
                session.phase == CallScreenPhase.ANALYZING_CALLER
            ) {
                Button(
                    onClick = onListenCallerVoice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (session.phase == CallScreenPhase.ANALYZING_CALLER) {
                            "কলারের কথা বুঝতেছি..."
                        } else {
                            "কলারের কথা শুনতেছে ছোটু (মাইক চালু)"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (session.callerTranscript.isNotBlank()) {
                Text(
                    text = "কলার কইছে: \"${session.callerTranscript}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSand
                )
            }

            if (session.bossBriefing.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                    border = BorderStroke(1.dp, BrightGold)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "আলাদা করে বসের কাছে ছোটুর খবর (কলার শুনবে না):",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = session.bossBriefing,
                            style = MaterialTheme.typography.titleLarge,
                            color = ChampagneGold
                        )
                    }
                }
            }

            if (session.phase == CallScreenPhase.ASKING_BOSS) {
                Button(
                    onClick = onListenBossVoiceDecision,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelvetSurface,
                        contentColor = BrightGold
                    ),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("মুখে কন (\"হ\" বা \"না\")")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onAcceptCall,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("boss_accept_call_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSafe,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("হ (দেন)", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onRejectCall,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .testTag("boss_reject_call_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonShield,
                            contentColor = WarmCream
                        )
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("না (কাট)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (session.phase == CallScreenPhase.CONNECTED_TO_BOSS) {
                if (session.isRecordingActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = CrimsonShield,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "কলটি রেকর্ড হচ্ছে... (কলারকে জানানো হইছে)",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrimsonShield
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onFinishCallKeepOrDelete(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalGold,
                                contentColor = ObsidianBlack
                            )
                        ) {
                            Text("শেষ + রেখে দে", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onFinishCallKeepOrDelete(false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            border = BorderStroke(1.dp, CrimsonShield)
                        ) {
                            Text("শেষ + অটো ডিলিট", color = CrimsonShield)
                        }
                    }
                } else {
                    Text(
                        text = "জি বস, কল কানেক্ট হইছে!",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldSafe
                    )
                    OutlinedButton(
                        onClick = onResetSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("কল শেষ", color = RoyalGold)
                    }
                }
            }

            if (session.phase == CallScreenPhase.REJECTED_POLITELY) {
                Text(
                    text = "ছোটু কলাররে কইয়া দিছে: \"${session.callerClosingLine}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CrimsonShield
                )
                OutlinedButton(
                    onClick = onResetSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("ঠিক আছে", color = RoyalGold)
                }
            }
        }
    }
}
