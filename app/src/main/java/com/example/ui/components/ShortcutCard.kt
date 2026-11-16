package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.device.ShortcutHelper
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.DeepGold
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.VelvetElevated
import com.example.ui.theme.WarmCream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceShortcutCard(
    wakeName: String,
    onShortcutResult: (success: Boolean, messageBangla: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customTitle by rememberSaveable { mutableStateOf("শামীমরে ক") }
    var customCommand by rememberSaveable { mutableStateOf("ছোটু শামীমরে ক") }
    var feedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun handlePinResult(requested: Boolean, label: String) {
        val msg = if (requested) {
            "জি বস, \"$label\" শর্টকাট আপনের ফোনের হোম স্ক্রিনে পাঠাইয়া দিছি।"
        } else {
            "বস, অ্যাপের আইকনে চাইপা ধইরা রাখলেও শর্টকাট হোম স্ক্রিনে নিতে পারবেন।"
        }
        feedbackMessage = msg
        onShortcutResult(requested, msg)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
        border = BorderStroke(1.dp, BronzeBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AddToHomeScreen,
                    contentDescription = "হোম স্ক্রিন শর্টকাট",
                    tint = RoyalGold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ফোনের হোম স্ক্রিনে শর্টকাট রাখুন",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrightGold
                )
            }

            Text(
                text = "নিচের যেকোনো বড় বাটনে চাপ দিলেই আপনার ফোনের স্ক্রিনে সরাসরি ছোটুর শর্টকাট বসে যাবে, বস:",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSand
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val ok = ShortcutHelper.pinVoiceShortcut(context, wakeName)
                        handlePinResult(ok, "$wakeName ভয়েস")
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("pin_voice_shortcut_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianBlack
                    )
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ভয়েস শর্টকাট", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val ok = ShortcutHelper.pinCallAgentShortcut(context)
                        handlePinResult(ok, "দারোয়ান")
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("pin_call_agent_shortcut_button"),
                    border = BorderStroke(1.dp, RoyalGold)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = RoyalGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("দারোয়ান", color = RoyalGold)
                }

                OutlinedButton(
                    onClick = {
                        val ok = ShortcutHelper.pinVoiceUnlockShortcut(context)
                        handlePinResult(ok, "ভয়েস আনলক")
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("pin_unlock_shortcut_button"),
                    border = BorderStroke(1.dp, BronzeBorder)
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = WarmCream)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ভয়েস আনলক", color = WarmCream)
                }
            }

            Text(
                text = "নিজের পছন্দমতো কমান্ড শর্টকাট:",
                style = MaterialTheme.typography.labelLarge,
                color = WarmCream
            )

            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = { Text("শর্টকাটের নাম") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_shortcut_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalGold,
                    unfocusedBorderColor = BronzeBorder,
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream
                )
            )

            OutlinedTextField(
                value = customCommand,
                onValueChange = { customCommand = it },
                label = { Text("ট্যাপ করলে ছোটু যা করবে") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_shortcut_command_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalGold,
                    unfocusedBorderColor = BronzeBorder,
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream
                )
            )

            Button(
                onClick = {
                    if (customCommand.isNotBlank()) {
                        val ok = ShortcutHelper.pinCustomCommandShortcut(
                            context = context,
                            shortcutTitle = customTitle,
                            banglaCommand = customCommand
                        )
                        handlePinResult(ok, customTitle.ifBlank { "বসের হুকুম" })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("pin_custom_shortcut_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGold,
                    contentColor = WarmCream
                )
            ) {
                Icon(Icons.Filled.AddToHomeScreen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("হোম স্ক্রিনে রাখুন", fontWeight = FontWeight.Bold)
            }

            if (!feedbackMessage.isNullOrBlank()) {
                Text(
                    text = feedbackMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldSafe
                )
            }
        }
    }
}

@Composable
fun HomeShortcutDialog(
    wakeName: String,
    onShortcutResult: (success: Boolean, messageBangla: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetElevated,
        titleContentColor = BrightGold,
        textContentColor = WarmCream,
        title = {
            Text(
                text = "হোম স্ক্রিনে শর্টকাট রাখুন",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DeviceShortcutCard(
                    wakeName = wakeName,
                    onShortcutResult = onShortcutResult
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ কর", color = RoyalGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}
