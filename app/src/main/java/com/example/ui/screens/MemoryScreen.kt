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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ForbiddenActionEntity
import com.example.data.LearnedHabitEntity
import com.example.data.MemoryEntryEntity
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
import java.util.concurrent.TimeUnit

@Composable
fun MemoryScreen(
    learningDay: Int,
    retentionDays: Int,
    memories: List<MemoryEntryEntity>,
    habits: List<LearnedHabitEntity>,
    forbiddenActions: List<ForbiddenActionEntity>,
    onPinMemory: (Long) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearUnpinned: () -> Unit,
    onUnbanAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "ছোটুর মেমরি ও শেখা",
                style = MaterialTheme.typography.headlineMedium,
                color = BrightGold
            )

            // Learning Progress Card (৩-৪ দিনে সুমন বসকে চিনে নেওয়া)
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, RoyalGold)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = RoyalGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সুমন বসকে চেনার ${learningDay}ম দিন",
                            style = MaterialTheme.typography.titleLarge,
                            color = BrightGold
                        )
                    }
                    Text(
                        text = if (learningDay <= 4) {
                            "প্রথম ৩-৪ দিন ছোটু আপনার পছন্দ-অপছন্দ ও কল করার ধরন চিনতাছে বস।"
                        } else {
                            "ছোটু আপনার স্টাইল চিনে নিছে — আপনার মতো কইরাই কথা কইব বস!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand
                    )

                    if (habits.isNotEmpty()) {
                        Text(
                            text = "ছোটু যা শিখছে:",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        habits.take(6).forEach { habit ->
                            Text(
                                text = "• ${habit.descriptionBangla} (${habit.frequencyCount} বার)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmCream
                            )
                        }
                    }
                }
            }

            // Never-Repeat ("না") Forbidden Rules Card
            if (forbiddenActions.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                    border = BorderStroke(1.dp, CrimsonShield)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Block, contentDescription = null, tint = CrimsonShield)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "যেসব কাজে বস 'না' বলছেন (আর করব না)",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                        }
                        forbiddenActions.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.reasonBangla,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSand,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { onUnbanAction(item.actionKey) },
                                    border = BorderStroke(1.dp, BronzeBorder)
                                ) {
                                    Text("আবার চলুক", color = RoyalGold)
                                }
                            }
                        }
                    }
                }
            }

            // 10-Day / 5-Day Auto-Expiring Memories
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "কথার মেমরি ($retentionDays দিন)",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarmCream
                            )
                            Text(
                                text = "$retentionDays দিন পর অটো ডিলিট হয়; 'রেখে দাও' চাপলে চিরদিন থাকবে।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSand
                            )
                        }
                    }

                    if (memories.isNotEmpty()) {
                        Button(
                            onClick = onClearUnpinned,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("clear_unpinned_memories_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelvetElevated,
                                contentColor = CrimsonShield
                            ),
                            border = BorderStroke(1.dp, CrimsonShield)
                        ) {
                            Text("অস্থায়ী সব ডিলিট কর", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (memories.isEmpty()) {
                        Text(
                            text = "এখনো কোনো মেমরি নাই বস।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSand
                        )
                    } else {
                        memories.forEach { mem ->
                            val daysLeft = TimeUnit.MILLISECONDS.toDays(
                                (mem.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
                            ) + 1
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VelvetElevated),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (mem.isPinnedForever) RoyalGold else BronzeBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "বস: ${mem.bossText}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = BrightGold
                                        )
                                        Text(
                                            text = "ছোটু: ${mem.chhotuReply}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = WarmCream
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (mem.isPinnedForever) {
                                                "চিরস্থায়ী রাখা হইছে"
                                            } else {
                                                "$daysLeft দিন পর অটো ডিলিট"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (mem.isPinnedForever) EmeraldSafe else MutedSand
                                        )
                                    }
                                    if (!mem.isPinnedForever) {
                                        IconButton(onClick = { onPinMemory(mem.id) }) {
                                            Icon(
                                                imageVector = Icons.Filled.Bookmark,
                                                contentDescription = "রেখে দাও",
                                                tint = BrightGold
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "ডিলিট কর",
                                            tint = CrimsonShield
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
