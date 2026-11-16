package com.example.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.example.data.DialectMode
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.CrimsonShield
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.VelvetSurface
import com.example.ui.theme.WarmCream

fun buildRequiredJarvisPermissions(): Array<String> {
    val perms = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        perms.add(Manifest.permission.ANSWER_PHONE_CALLS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        perms.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return perms.toTypedArray()
}

@Composable
fun OnboardingScreen(
    initialDialect: DialectMode,
    onPermissionsGranted: () -> Unit,
    onVoiceDialectSetup: ((DialectMode) -> Unit) -> Unit,
    onFinishOnboarding: (DialectMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDialect by rememberSaveable { mutableStateOf(initialDialect) }
    var permRefresh by rememberSaveable { mutableStateOf(0) }
    var voicePromptedOnce by rememberSaveable { mutableStateOf(false) }

    val requiredPerms = remember { buildRequiredJarvisPermissions() }
    val grantedCount = remember(permRefresh) {
        requiredPerms.count {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    val hasMicPerm = remember(permRefresh) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasMicPerm) {
        if (hasMicPerm && !voicePromptedOnce) {
            voicePromptedOnce = true
            onVoiceDialectSetup { spokenDialect ->
                selectedDialect = spokenDialect
                onFinishOnboarding(spokenDialect)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permRefresh++
        onPermissionsGranted()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        permRefresh++
    }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "সালাম সুমন বস! আমি ছোটু",
                style = MaterialTheme.typography.displaySmall,
                color = BrightGold
            )
            Text(
                text = "আপনার পার্সোনাল অ্যাসিস্ট্যান্ট ও কল দারোয়ান। আপনি \"হ\" বললে করুম, \"না\" বললে করুম না বস!",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedSand
            )

            // 1. Dialect Choice Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = RoyalGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ছোটু কোন ভাষায় কথা কইব?",
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmCream
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DialectMode.entries.forEach { mode ->
                            val selected = selectedDialect == mode
                            Button(
                                onClick = { selectedDialect = mode },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("onboarding_dialect_${mode.name}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) RoyalGold else VelvetSurface,
                                    contentColor = if (selected) ObsidianBlack else WarmCream
                                ),
                                border = if (selected) null else BorderStroke(1.dp, BronzeBorder)
                            ) {
                                Text(mode.labelBangla, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. One-Time Permissions Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalPanel),
                border = BorderStroke(1.dp, BronzeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = RoyalGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "এককালীন অনুমতি (ব্যাংক বাদে)",
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmCream
                        )
                    }
                    Text(
                        text = "কনট্যাক্ট, কল, হোয়াটসঅ্যাপ, ইমো, মেসেঞ্জার, ব্রাউজার, স্টোরেজ ও নোটিফিকেশনের জন্য একবার অনুমতি দিন। এরপর প্রতিটি নতুন কাজেও ছোটু আগে 'হ/না' জিজ্ঞেস করে নিবে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSand
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonShield.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, CrimsonShield.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = CrimsonShield)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ব্যাংক, বিকাশ, নগদ, ওটিপি ও পিন সম্পূর্ণ নিষিদ্ধ।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmCream
                            )
                        }
                    }

                    Text(
                        text = "অনুমতি দেওয়া হয়েছে: $grantedCount / ${requiredPerms.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (grantedCount > 0) EmeraldSafe else RoyalGold
                    )

                    Button(
                        onClick = { permissionLauncher.launch(requiredPerms) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("grant_all_permissions_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("সব অনুমতি দিন (হ)", fontWeight = FontWeight.Bold)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        OutlinedButton(
                            onClick = {
                                val rm =
                                    context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                                if (rm != null &&
                                    rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                                    !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                                ) {
                                    val intent: Intent = rm.createRequestRoleIntent(
                                        RoleManager.ROLE_CALL_SCREENING
                                    )
                                    roleLauncher.launch(intent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            border = BorderStroke(1.dp, RoyalGold)
                        ) {
                            Text("দারোয়ান কল রিসিভ চালু করুন", color = RoyalGold)
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
                            .fillMaxWidth()
                            .height(52.dp),
                        border = BorderStroke(1.dp, BronzeBorder)
                    ) {
                        Text("ইমো / হোয়াটসঅ্যাপ / মেসেঞ্জার কল নোটিফিকেশন", color = WarmCream)
                    }
                }
            }

            Button(
                onClick = { onFinishOnboarding(selectedDialect) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("finish_onboarding_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrightGold,
                    contentColor = ObsidianBlack
                )
            ) {
                Text(
                    text = "শুরু কর, ছোটু!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
