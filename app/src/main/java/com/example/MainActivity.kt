package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.CallScreenPhase
import com.example.device.ShortcutHelper
import com.example.security.VoiceBiometricManager
import com.example.ui.JarvisUiEvent
import com.example.ui.JarvisViewModel
import com.example.ui.screens.AssistantHudScreen
import com.example.ui.screens.CallScreeningScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.JarvisBanglaTheme
import com.example.ui.theme.MutedSand
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.WarmCream

private enum class JarvisTab(val routeLabel: String) {
    HUD("হোম"),
    CALL_AGENT("দারোয়ান"),
    MEMORY("মেমরি"),
    SETTINGS("সেটিংস")
}

class MainActivity : FragmentActivity() {

    private var pendingShortcutIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        pendingShortcutIntent = intent

        val initialTab = if (intent?.getStringExtra("open_tab") == "call_agent" ||
            intent?.getStringExtra(ShortcutHelper.EXTRA_SHORTCUT_ACTION) ==
            ShortcutHelper.ACTION_OPEN_CALL_AGENT
        ) {
            JarvisTab.CALL_AGENT
        } else {
            JarvisTab.HUD
        }

        setContent {
            JarvisBanglaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBlack
                ) {
                    JarvisAppRoot(
                        activity = this@MainActivity,
                        initialTab = initialTab,
                        pendingShortcutIntent = pendingShortcutIntent,
                        onShortcutIntentConsumed = { pendingShortcutIntent = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutIntent = intent
    }
}

@Composable
private fun JarvisAppRoot(
    activity: FragmentActivity,
    initialTab: JarvisTab,
    pendingShortcutIntent: Intent?,
    onShortcutIntentConsumed: () -> Unit,
    viewModel: JarvisViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val orbStatus by viewModel.orbStatus.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val pendingPermission by viewModel.pendingPermission.collectAsStateWithLifecycle()
    val pendingBrowserUpdate by viewModel.pendingBrowserUpdate.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val callSession by viewModel.callSession.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val learnedHabits by viewModel.learnedHabits.collectAsStateWithLifecycle()
    val forbiddenActions by viewModel.forbiddenActions.collectAsStateWithLifecycle()
    val callRecordings by viewModel.callRecordings.collectAsStateWithLifecycle()
    val playingRecordingPath by viewModel.playingRecordingPath.collectAsStateWithLifecycle()

    val pendingSettingsVoicePrompt by viewModel.pendingSettingsVoicePrompt.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(initialTab) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(activity, "জি বস, ছবি নেওয়া হইছে।", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(activity, "জি বস, ফাইল খোলা হইছে।", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                JarvisUiEvent.LaunchPhotoPicker -> {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                JarvisUiEvent.LaunchFilePicker -> {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
                JarvisUiEvent.LaunchBiometricUnlock -> {
                    VoiceBiometricManager.authenticateWithOfficialPrompt(
                        activity = activity,
                        onSuccess = { viewModel.onBiometricUnlockSuccess() },
                        onError = { msg -> viewModel.onBiometricUnlockError(msg) }
                    )
                }
                JarvisUiEvent.OpenSettingsTab -> {
                    currentTab = JarvisTab.SETTINGS
                }
            }
        }
    }

    LaunchedEffect(settings.onboardingCompleted, settings.wakeWordActive) {
        if (settings.onboardingCompleted && settings.wakeWordActive) {
            viewModel.ensureWakeStandbyIfPermitted()
        }
    }

    LaunchedEffect(pendingShortcutIntent, settings.onboardingCompleted) {
        val shortcutIntent = pendingShortcutIntent ?: return@LaunchedEffect
        val action = shortcutIntent.getStringExtra(ShortcutHelper.EXTRA_SHORTCUT_ACTION)
        if (!action.isNullOrBlank() && settings.onboardingCompleted) {
            when (action) {
                ShortcutHelper.ACTION_START_LISTENING -> {
                    currentTab = JarvisTab.HUD
                    viewModel.startSingleCommandListening()
                }
                ShortcutHelper.ACTION_OPEN_CALL_AGENT -> {
                    currentTab = JarvisTab.CALL_AGENT
                }
                ShortcutHelper.ACTION_VOICE_UNLOCK -> {
                    currentTab = JarvisTab.MEMORY
                }
                ShortcutHelper.ACTION_RUN_COMMAND -> {
                    currentTab = JarvisTab.HUD
                    val cmd = shortcutIntent.getStringExtra(ShortcutHelper.EXTRA_PRESET_COMMAND)
                    if (!cmd.isNullOrBlank()) {
                        viewModel.processBossInput(cmd)
                    }
                }
            }
            onShortcutIntentConsumed()
        }
    }

    LaunchedEffect(callSession.phase) {
        if (callSession.phase == CallScreenPhase.GREETING_CALLER && callSession.isLiveSystemCall) {
            currentTab = JarvisTab.CALL_AGENT
        }
    }

    if (!settings.onboardingCompleted) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBlack
        ) { innerPadding ->
            OnboardingScreen(
                initialDialect = settings.dialectMode,
                onPermissionsGranted = { viewModel.markPermissionsGranted() },
                onVoiceDialectSetup = { onChosen ->
                    viewModel.speakOnboardingDialectPrompt(onChosen)
                },
                onFinishOnboarding = { dialect ->
                    viewModel.completeOnboarding(dialect)
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    if (currentTab != JarvisTab.HUD) {
        BackHandler {
            currentTab = JarvisTab.HUD
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = ObsidianBlack,
        bottomBar = {
            NavigationBar(
                containerColor = CharcoalPanel,
                contentColor = WarmCream
            ) {
                NavigationBarItem(
                    selected = currentTab == JarvisTab.HUD,
                    onClick = { currentTab = JarvisTab.HUD },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisTab.HUD) {
                                Icons.Filled.GraphicEq
                            } else {
                                Icons.Outlined.GraphicEq
                            },
                            contentDescription = "হোম"
                        )
                    },
                    label = { Text(JarvisTab.HUD.routeLabel) },
                    modifier = Modifier.testTag("nav_tab_hud"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = BrightGold,
                        indicatorColor = RoyalGold,
                        unselectedIconColor = MutedSand,
                        unselectedTextColor = MutedSand
                    )
                )
                NavigationBarItem(
                    selected = currentTab == JarvisTab.CALL_AGENT,
                    onClick = { currentTab = JarvisTab.CALL_AGENT },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisTab.CALL_AGENT) {
                                Icons.Filled.Call
                            } else {
                                Icons.Outlined.Call
                            },
                            contentDescription = "দারোয়ান"
                        )
                    },
                    label = { Text(JarvisTab.CALL_AGENT.routeLabel) },
                    modifier = Modifier.testTag("nav_tab_call_agent"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = BrightGold,
                        indicatorColor = RoyalGold,
                        unselectedIconColor = MutedSand,
                        unselectedTextColor = MutedSand
                    )
                )
                NavigationBarItem(
                    selected = currentTab == JarvisTab.MEMORY,
                    onClick = { currentTab = JarvisTab.MEMORY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisTab.MEMORY) {
                                Icons.Filled.History
                            } else {
                                Icons.Outlined.History
                            },
                            contentDescription = "মেমরি"
                        )
                    },
                    label = { Text(JarvisTab.MEMORY.routeLabel) },
                    modifier = Modifier.testTag("nav_tab_memory"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = BrightGold,
                        indicatorColor = RoyalGold,
                        unselectedIconColor = MutedSand,
                        unselectedTextColor = MutedSand
                    )
                )
                NavigationBarItem(
                    selected = currentTab == JarvisTab.SETTINGS,
                    onClick = { currentTab = JarvisTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == JarvisTab.SETTINGS) {
                                Icons.Filled.Settings
                            } else {
                                Icons.Outlined.Settings
                            },
                            contentDescription = "সেটিংস"
                        )
                    },
                    label = { Text(JarvisTab.SETTINGS.routeLabel) },
                    modifier = Modifier.testTag("nav_tab_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = BrightGold,
                        indicatorColor = RoyalGold,
                        unselectedIconColor = MutedSand,
                        unselectedTextColor = MutedSand
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            JarvisTab.HUD -> AssistantHudScreen(
                settings = settings,
                learningDay = viewModel.learningDayCount(),
                orbStatus = orbStatus,
                audioLevel = audioLevel,
                partialTranscript = partialTranscript,
                conversations = conversations,
                pendingPermission = pendingPermission,
                pendingBrowserUpdate = pendingBrowserUpdate,
                errorBanner = errorBanner,
                onOrbClick = { viewModel.toggleListeningOrWake() },
                onShortcutNotice = { msg -> viewModel.speakServantNotice(msg, "হোম শর্টকাট") },
                modifier = Modifier.padding(innerPadding)
            )
            JarvisTab.CALL_AGENT -> CallScreeningScreen(
                callScreeningEnabled = settings.callScreeningEnabled,
                autoRecordCalls = settings.autoRecordCalls,
                bossSchedule = settings.bossSchedule,
                session = callSession,
                logs = callLogs,
                recordings = callRecordings,
                playingRecordingPath = playingRecordingPath,
                onToggleCallScreening = { viewModel.setCallScreeningEnabled(it) },
                onToggleAutoRecord = { viewModel.setAutoRecordCalls(it) },
                onSelectSchedule = { viewModel.setBossSchedule(it) },
                onStartLiveTest = { number, name, channel, isVideo ->
                    viewModel.startIncomingCallScreeningFlow(number, name, channel, isVideo)
                },
                onListenCallerVoice = { viewModel.listenToCallerVoiceForScreening() },
                onListenBossVoiceDecision = { viewModel.listenBossCallDecision() },
                onAcceptCall = { viewModel.acceptScreenedCallDirectly() },
                onRejectCall = { viewModel.rejectScreenedCallDirectly() },
                onFinishCallKeepOrDelete = { keep -> viewModel.finishActiveCallAndMaybeKeep(keep) },
                onResetSession = { viewModel.resetCallScreeningSession() },
                onPlayRecording = { rec -> viewModel.togglePlayRecording(rec) },
                onKeepRecording = { id -> viewModel.keepRecordingForever(id) },
                onDeleteRecording = { rec -> viewModel.deleteRecording(rec) },
                modifier = Modifier.padding(innerPadding)
            )
            JarvisTab.MEMORY -> MemoryScreen(
                learningDay = viewModel.learningDayCount(),
                retentionDays = settings.memoryRetentionDays,
                memories = memories,
                habits = learnedHabits,
                forbiddenActions = forbiddenActions,
                onPinMemory = { id -> viewModel.pinMemoryForever(id) },
                onDeleteMemory = { id -> viewModel.deleteMemory(id) },
                onClearUnpinned = { viewModel.clearAllUnpinnedMemories() },
                onUnbanAction = { key -> viewModel.unbanAction(key) },
                modifier = Modifier.padding(innerPadding)
            )
            JarvisTab.SETTINGS -> SettingsScreen(
                settings = settings,
                orbStatus = orbStatus,
                audioLevel = audioLevel,
                partialTranscript = partialTranscript,
                pendingVoicePrompt = pendingSettingsVoicePrompt,
                onEnterVoiceSettings = { viewModel.promptVoiceSettings() },
                onLeaveVoiceSettings = { viewModel.cancelVoiceSettingsPrompt() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
