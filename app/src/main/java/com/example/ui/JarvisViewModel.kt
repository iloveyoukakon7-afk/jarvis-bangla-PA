package com.example.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JarvisAiRepository
import com.example.data.AssistantSettings
import com.example.data.BossMood
import com.example.data.BossScheduleStatus
import com.example.data.CallChannel
import com.example.data.CallRecordingEntity
import com.example.data.CallScreenLogEntry
import com.example.data.CallScreenPhase
import com.example.data.CallScreenSession
import com.example.data.ChhotuDatabase
import com.example.data.ConversationEntry
import com.example.data.DialectMode
import com.example.data.ForbiddenActionEntity
import com.example.data.JarvisIntentType
import com.example.data.LearnedHabitEntity
import com.example.data.MemoryEntryEntity
import com.example.data.OrbStatus
import com.example.data.ParsedJarvisCommand
import com.example.data.PendingBrowserUpdatePrompt
import com.example.data.PendingPermissionPrompt
import com.example.data.PermissionCategory
import com.example.data.SettingsRepository
import com.example.device.BanglaSpeechEngine
import com.example.device.CallRecorderManager
import com.example.device.ContactsHelper
import com.example.device.DeviceActionExecutor
import com.example.device.ShortcutHelper
import com.example.security.BankGuardrail
import com.example.security.VoiceBiometricManager
import com.example.service.CallScreeningCoordinator
import com.example.service.JarvisWakeWordService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "ChhotuViewModel"

sealed interface JarvisUiEvent {
    data object LaunchPhotoPicker : JarvisUiEvent
    data object LaunchFilePicker : JarvisUiEvent
    data object LaunchBiometricUnlock : JarvisUiEvent
    data object OpenSettingsTab : JarvisUiEvent
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepo = SettingsRepository(appContext)
    private val aiRepo = JarvisAiRepository()
    private val speechEngine = BanglaSpeechEngine(appContext)
    private val actionExecutor = DeviceActionExecutor(appContext)
    private val callRecorder = CallRecorderManager(appContext)
    private val memoryDao = ChhotuDatabase.getInstance(appContext).memoryDao()

    private val _uiEvents = Channel<JarvisUiEvent>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    val settings: StateFlow<AssistantSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AssistantSettings()
    )

    val memories: StateFlow<List<MemoryEntryEntity>> = memoryDao.observeMemories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val learnedHabits: StateFlow<List<LearnedHabitEntity>> = memoryDao.observeHabits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val forbiddenActions: StateFlow<List<ForbiddenActionEntity>> =
        memoryDao.observeForbiddenActions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val callRecordings: StateFlow<List<CallRecordingEntity>> =
        memoryDao.observeRecordings().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val callSession: StateFlow<CallScreenSession> = CallScreeningCoordinator.session
    val callLogs: StateFlow<List<CallScreenLogEntry>> = CallScreeningCoordinator.logs

    private val _isThinking = MutableStateFlow(false)
    private val _isBlockedShield = MutableStateFlow(false)
    private val _pendingPermission = MutableStateFlow<PendingPermissionPrompt?>(null)
    val pendingPermission: StateFlow<PendingPermissionPrompt?> = _pendingPermission.asStateFlow()

    private val _pendingBrowserUpdate = MutableStateFlow<PendingBrowserUpdatePrompt?>(null)
    val pendingBrowserUpdate: StateFlow<PendingBrowserUpdatePrompt?> =
        _pendingBrowserUpdate.asStateFlow()

    private val _pendingSettingsVoicePrompt = MutableStateFlow(false)
    val pendingSettingsVoicePrompt: StateFlow<Boolean> = _pendingSettingsVoicePrompt.asStateFlow()

    private val _playingRecordingPath = MutableStateFlow<String?>(null)
    val playingRecordingPath: StateFlow<String?> = _playingRecordingPath.asStateFlow()

    val audioLevel: StateFlow<Float> = speechEngine.audioLevel
    val partialTranscript: StateFlow<String> = speechEngine.partialTranscript

    val orbStatus: StateFlow<OrbStatus> = combine(
        speechEngine.isListening,
        speechEngine.isSpeaking,
        speechEngine.isWakeStandby,
        _isThinking,
        _isBlockedShield,
        CallScreeningCoordinator.session
    ) { values ->
        val listening = values[0] as Boolean
        val speaking = values[1] as Boolean
        val wakeStandby = values[2] as Boolean
        val thinking = values[3] as Boolean
        val blocked = values[4] as Boolean
        val call = values[5] as CallScreenSession

        when {
            blocked -> OrbStatus.BLOCKED_SHIELD
            call.isRecordingActive -> OrbStatus.RECORDING_CALL
            call.phase != CallScreenPhase.IDLE &&
                call.phase != CallScreenPhase.CONNECTED_TO_BOSS &&
                call.phase != CallScreenPhase.REJECTED_POLITELY -> OrbStatus.CALL_SCREENING
            listening -> OrbStatus.LISTENING
            thinking -> OrbStatus.THINKING
            speaking -> OrbStatus.SPEAKING
            wakeStandby -> OrbStatus.WAKE_STANDBY
            else -> OrbStatus.IDLE
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrbStatus.IDLE)

    private val _conversations = MutableStateFlow(
        listOf(
            ConversationEntry(
                isBoss = false,
                text = "সালাম সুমন বস! আমি ছোটু, আপনার নিজের লোক। হুকুম করেন বস, কী করতে হইব?",
                actionBadge = "ছোটু প্রস্তুত"
            )
        )
    )
    val conversations: StateFlow<List<ConversationEntry>> = _conversations.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.purgeExpired(System.currentTimeMillis())
        }
    }

    fun learningDayCount(): Int {
        val elapsedMillis = (System.currentTimeMillis() - settings.value.firstLaunchMillis)
            .coerceAtLeast(0L)
        return TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt() + 1
    }

    fun dismissError() {
        _errorBanner.value = null
    }

    fun markPermissionsGranted() {
        viewModelScope.launch {
            settingsRepo.setPermissionsGrantedOnce(true)
        }
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureWakeStandbyIfPermitted() {
        val current = settings.value
        if (!current.onboardingCompleted || !current.wakeWordActive) return
        if (!hasMicPermission()) return
        if (orbStatus.value == OrbStatus.LISTENING ||
            orbStatus.value == OrbStatus.SPEAKING ||
            orbStatus.value == OrbStatus.WAKE_STANDBY ||
            orbStatus.value == OrbStatus.THINKING
        ) {
            return
        }
        runCatching { JarvisWakeWordService.start(appContext, current.wakeName) }
        startWakeStandbyCycle()
    }

    fun speakOnboardingDialectPrompt(onVoiceChosen: (DialectMode) -> Unit) {
        if (!hasMicPermission()) return
        val prompt = "সালাম বস! আমি ছোটু। ঢাকাইয়া না গ্রাম্য — কোন ভাষায় কথা কমু বস?"
        speechEngine.speakBangla(prompt, settings.value.speechSpeed) {
            if (hasMicPermission()) {
                speechEngine.startSingleCommandListening(
                    onResult = { spoken ->
                        val lower = spoken.lowercase()
                        val mode = if (lower.contains("গ্রাম") || lower.contains("gram")) {
                            DialectMode.GRAMYO
                        } else {
                            DialectMode.DHAKAIYA
                        }
                        onVoiceChosen(mode)
                    },
                    onError = { /* user can still tap finish or speak again */ }
                )
            }
        }
    }

    fun completeOnboarding(dialectMode: DialectMode) {
        viewModelScope.launch {
            settingsRepo.completeOnboarding(dialectMode)
            ShortcutHelper.refreshDynamicShortcuts(appContext, "ছোটু")
            val greeting = if (dialectMode == DialectMode.DHAKAIYA) {
                "জি বস, সব রেডি! এখন থাইকা আমি ছোটু আপনার সাথে আছি। খালি কইবেন 'ওই ছোটু', তাইলেই হাজির!"
            } else {
                "আইচ্ছা বস, সব ঠিকঠাক হইয়া গেছে! এখন থাইকা 'ওই ছোটু' কইলেই আমি হাজির হইয়া যামু!"
            }
            appendServantMessage(greeting, "ওই ছোটু চালু")
            if (hasMicPermission()) {
                runCatching { JarvisWakeWordService.start(appContext, "ওই ছোটু") }
            }
            speechEngine.speakBangla(greeting, settings.value.speechSpeed) {
                resumeWakeStandbyIfActive()
            }
        }
    }

    fun promptVoiceSettings() {
        _pendingSettingsVoicePrompt.value = true
        val prompt = "বস, কোনটা চেঞ্জ করমু? আওয়াজ, ভাষা ঢাকাইয়া না গ্রাম্য, নাকি মনে রাখার দিন?"
        appendServantMessage(prompt, "ভয়েস সেটিংস")
        speechEngine.speakBangla(prompt, settings.value.speechSpeed) {
            if (hasMicPermission()) {
                startSingleCommandListening()
            }
        }
    }

    fun cancelVoiceSettingsPrompt() {
        _pendingSettingsVoicePrompt.value = false
        resumeWakeStandbyIfActive()
    }

    fun setDialectMode(mode: DialectMode) {
        viewModelScope.launch {
            _pendingSettingsVoicePrompt.value = false
            settingsRepo.setDialectMode(mode)
            val msg = if (mode == DialectMode.DHAKAIYA) {
                "জি বস, এখন থাইকা পুরা ঢাকাইয়া ভাষায় কথা কমু!"
            } else {
                "আইচ্ছা বস, এখন থাইকা আপনের লগে গ্রাম্য বাংলায় কথা কমু!"
            }
            appendServantMessage(msg, "ভাষা: ${mode.labelBangla}")
            speechEngine.speakBangla(msg, settings.value.speechSpeed) {
                resumeWakeStandbyIfActive()
            }
        }
    }

    fun setMemoryRetentionDays(days: Int) {
        viewModelScope.launch {
            _pendingSettingsVoicePrompt.value = false
            settingsRepo.setMemoryRetentionDays(days)
            val msg = "জি বস, আপনার কথা $days দিন মনে রাখুম, তারপর অটো মুইছা দিমু!"
            appendServantMessage(msg, "মনে রাখা: $days দিন")
            speechEngine.speakBangla(msg, settings.value.speechSpeed) {
                resumeWakeStandbyIfActive()
            }
        }
    }

    fun setSpeechSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepo.setSpeechSpeed(speed)
        }
    }

    private fun applyVoiceSpeedWithSpokenReply(speed: Float, reply: String, badge: String) {
        viewModelScope.launch {
            _pendingSettingsVoicePrompt.value = false
            settingsRepo.setSpeechSpeed(speed)
            appendServantMessage(reply, badge)
            speechEngine.speakBangla(reply, speed) {
                resumeWakeStandbyIfActive()
            }
        }
    }

    fun previewServantVoice() {
        val sample = if (settings.value.dialectMode == DialectMode.DHAKAIYA) {
            "জি বস, ছোটু হাজির! আপনে খালি কইবেন 'ওই ছোটু' আর কাম হইয়া যাইব বস!"
        } else {
            "আইচ্ছা বস, ছোটু আপনার পাশেই আছে! যা কইবেন তাই করুম বস!"
        }
        speechEngine.speakBangla(sample, settings.value.speechSpeed) {
            resumeWakeStandbyIfActive()
        }
    }

    fun setCallScreeningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setCallScreeningEnabled(enabled)
        }
    }

    fun setAutoRecordCalls(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAutoRecordCalls(enabled)
            val msg = if (enabled) {
                "জি বস, এখন থাইকা সব কল অটো রেকর্ড করুম আর বইলা দিমু 'কলটি রেকর্ড হচ্ছে'!"
            } else {
                "জি বস, অটো কল রেকর্ড বন্ধ কইরা দিছি।"
            }
            appendServantMessage(msg, if (enabled) "কল রেকর্ড চালু" else "কল রেকর্ড বন্ধ")
            speechEngine.speakBangla(msg, settings.value.speechSpeed) {
                resumeWakeStandbyIfActive()
            }
        }
    }

    fun setBossSchedule(status: BossScheduleStatus) {
        viewModelScope.launch {
            settingsRepo.setBossSchedule(status)
            val msg = when (status) {
                BossScheduleStatus.FREE -> "জি বস, এখন কেউ কল করলে আপনেরে জিজ্ঞেস কইরা দিমু।"
                BossScheduleStatus.SLEEPING -> "আইচ্ছা বস, ঘুমাইয়া পড়েন। ইমারজেন্সি ছাড়া কাউরে দিমু না।"
                BossScheduleStatus.WORKING -> "জি বস, কামের সময় ইমারজেন্সি ছাড়া কল আটকায় দিমু।"
            }
            appendServantMessage(msg, status.labelBangla)
            speechEngine.speakBangla(msg, settings.value.speechSpeed) {
                resumeWakeStandbyIfActive()
            }
        }
    }

    fun speakServantNotice(message: String, badge: String? = null) {
        appendServantMessage(message, badge)
        speechEngine.speakBangla(message, settings.value.speechSpeed) {
            resumeWakeStandbyIfActive()
        }
    }

    fun toggleListeningOrWake() {
        if (orbStatus.value == OrbStatus.LISTENING) {
            speechEngine.stopAllListening()
            resumeWakeStandbyIfActive()
            return
        }
        if (orbStatus.value == OrbStatus.SPEAKING) {
            speechEngine.stopSpeaking()
        }
        startSingleCommandListening()
    }

    fun startSingleCommandListening() {
        _errorBanner.value = null
        _isBlockedShield.value = false
        speechEngine.startSingleCommandListening(
            onResult = { spokenText ->
                processBossInput(spokenText)
            },
            onError = { err ->
                _pendingSettingsVoicePrompt.value = false
                if (err.contains("অনুমতি") || err.contains("পাওয়া যাচ্ছে না")) {
                    _errorBanner.value = err
                }
                resumeWakeStandbyIfActive()
            }
        )
    }

    fun toggleWakeWordStandby(enable: Boolean) {
        viewModelScope.launch {
            settingsRepo.setWakeWordActive(enable)
            val currentWakeName = settings.value.wakeName
            if (enable) {
                if (hasMicPermission()) {
                    runCatching { JarvisWakeWordService.start(appContext, currentWakeName) }
                    startWakeStandbyCycle()
                }
                val msg = "জি বস, '$currentWakeName' বইলা ডাক দিলেই ছোটু সাড়া দিব!"
                appendServantMessage(msg, "ওই ছোটু চালু")
                speechEngine.speakBangla(msg, settings.value.speechSpeed) {
                    resumeWakeStandbyIfActive()
                }
            } else {
                speechEngine.stopAllListening()
                runCatching { JarvisWakeWordService.stop(appContext) }
            }
        }
    }

    private fun resumeWakeStandbyIfActive() {
        val current = settings.value
        if (current.onboardingCompleted && current.wakeWordActive && hasMicPermission()) {
            startWakeStandbyCycle()
        }
    }

    private fun startWakeStandbyCycle() {
        if (!hasMicPermission()) return
        speechEngine.startWakeWordStandby(
            wakeNameProvider = { settings.value.wakeName },
            onWakeCommand = { strippedCommand ->
                processBossInput(strippedCommand)
            },
            onWakeOnly = {
                speechEngine.speakBangla("জি বস, কন বস!", settings.value.speechSpeed) {
                    startSingleCommandListening()
                }
            },
            onStatusError = { err ->
                _errorBanner.value = err
            }
        )
    }

    // --- Boss "হ" / "না" Approval Handlers ---

    fun answerPendingPermission(allow: Boolean) {
        val pending = _pendingPermission.value ?: return
        _pendingPermission.value = null

        viewModelScope.launch {
            if (allow) {
                settingsRepo.grantPermissionCategory(pending.category)
                val contacts = withContext(Dispatchers.IO) {
                    ContactsHelper.loadContacts(appContext)
                }
                executeAndRespond(pending.command, contacts, pending.originalUtterance)
            } else {
                // Rule #4: Once Boss says "না", never do it again!
                withContext(Dispatchers.IO) {
                    memoryDao.insertForbidden(
                        ForbiddenActionEntity(
                            actionKey = pending.command.intentType.name,
                            reasonBangla = "${pending.category.labelBangla} কাজে বস 'না' কইছে"
                        )
                    )
                }
                val reply = "জি বস, আপনে 'না' কইছেন। এই কাম আর কোনোদিন করুম না বস!"
                appendServantMessage(reply, "স্থায়ী নিষেধ")
                speechEngine.speakBangla(reply, settings.value.speechSpeed) {
                    resumeWakeStandbyIfActive()
                }
            }
        }
    }

    fun answerBrowserUpdate(allowUpdate: Boolean) {
        _pendingBrowserUpdate.value = null
        val msg = if (allowUpdate) {
            "জি বস, আপনে 'হ' কইছেন! আপডেটের জন্য এআই স্টুডিও রেডি করছি।"
        } else {
            "জি বস, আপনে 'না' কইছেন তাই কোনো আপডেট করি নাই।"
        }
        appendServantMessage(msg, if (allowUpdate) "আপডেট অনুমোদিত" else "আপডেট বাতিল")
        speechEngine.speakBangla(msg, settings.value.speechSpeed) {
            resumeWakeStandbyIfActive()
        }
    }

    fun processBossInput(rawInput: String) {
        val text = rawInput.trim()
        if (text.isBlank()) return

        _errorBanner.value = null
        _isBlockedShield.value = false
        appendBossMessage(text)

        // Check if Boss is answering a pending permission ("হ" / "না") by voice
        val pendingPerm = _pendingPermission.value
        if (pendingPerm != null) {
            if (isAffirmativeYes(text)) {
                answerPendingPermission(true)
                return
            } else if (isNegativeNo(text)) {
                answerPendingPermission(false)
                return
            }
        }

        // Check if Boss is answering Chrome AI Studio update ("হ" / "না") by voice
        if (_pendingBrowserUpdate.value != null) {
            if (isAffirmativeYes(text)) {
                answerBrowserUpdate(true)
                return
            } else if (isNegativeNo(text)) {
                answerBrowserUpdate(false)
                return
            }
        }

        val currentSettings = settings.value
        val cleanedCommand = BanglaSpeechEngine.stripWakeName(text, currentSettings.wakeName)
            .ifBlank { text }

        if (BankGuardrail.isBlockedRequest(text)) {
            _pendingSettingsVoicePrompt.value = false
            _isBlockedShield.value = true
            appendServantMessage(
                text = BankGuardrail.REFUSAL_MESSAGE_BANGLA,
                badge = "ব্যাংক সুরক্ষা",
                isBlocked = true
            )
            speechEngine.speakBangla(BankGuardrail.REFUSAL_MESSAGE_BANGLA, currentSettings.speechSpeed) {
                resumeWakeStandbyIfActive()
            }
            return
        }

        // Handle Voice-Driven Settings Prompt follow-up directly & naturally
        if (_pendingSettingsVoicePrompt.value) {
            if (tryHandleVoiceSettingsFollowUp(cleanedCommand)) {
                return
            }
        }

        if (VoiceBiometricManager.matchesUnlockPhrase(text, currentSettings.unlockPhrase)) {
            val reply = "জি বস, আপনার আওয়াজ চিনছি! ভয়েস আনলক চালু করতাছি।"
            appendServantMessage(reply, "ভয়েস আনলক")
            speechEngine.speakBangla(reply, currentSettings.speechSpeed) {
                _uiEvents.trySend(JarvisUiEvent.LaunchBiometricUnlock)
            }
            return
        }

        viewModelScope.launch {
            _isThinking.value = true
            try {
                val contacts = withContext(Dispatchers.IO) {
                    ContactsHelper.loadContacts(appContext)
                }
                val topHabits = withContext(Dispatchers.IO) {
                    memoryDao.getTopHabits().map { "${it.descriptionBangla} (${it.frequencyCount}বার)" }
                }
                val forbidden = withContext(Dispatchers.IO) {
                    memoryDao.getAllForbiddenActions()
                }
                val recentMems = withContext(Dispatchers.IO) {
                    memoryDao.getRecentMemories().map { "${it.bossText} -> ${it.chhotuReply}" }
                }

                val parsed = aiRepo.parseBossCommand(
                    utterance = cleanedCommand,
                    dialectMode = currentSettings.dialectMode,
                    learningDayNumber = learningDayCount(),
                    contactNames = contacts.map { it.name },
                    topHabits = topHabits,
                    forbiddenList = forbidden.map { it.reasonBangla },
                    recentMemories = recentMems
                )

                // Rule #4: Never repeat an action Boss previously forbade
                val alreadyForbidden = forbidden.any { it.actionKey == parsed.intentType.name }
                if (alreadyForbidden) {
                    val refuse = "বস, আগে একবার আপনে এইটায় 'না' কইছিলেন, তাই ছোটু এইটা আর করব না বস!"
                    appendServantMessage(refuse, "নিষিদ্ধ কাজ")
                    speechEngine.speakBangla(refuse, currentSettings.speechSpeed) {
                        resumeWakeStandbyIfActive()
                    }
                    return@launch
                }

                // Rule #1: Ask Boss first if this permission category isn't consented yet
                val neededCategory = actionExecutor.requiredPermissionCategory(parsed.intentType)
                if (neededCategory != null &&
                    !currentSettings.grantedCategories.contains(neededCategory.name)
                ) {
                    val askQuestion =
                        "বস, ${neededCategory.labelBangla} ব্যবহার কইরা কামটা করমু? অনুমতি দিলে 'হ' কন, না হইলে 'না' কন বস।"
                    _pendingPermission.value = PendingPermissionPrompt(
                        category = neededCategory,
                        questionBangla = askQuestion,
                        command = parsed,
                        originalUtterance = cleanedCommand
                    )
                    appendServantMessage(askQuestion, "অনুমতি চাওয়া")
                    speechEngine.speakBangla(askQuestion, currentSettings.speechSpeed) {
                        if (hasMicPermission()) {
                            startSingleCommandListening()
                        }
                    }
                    return@launch
                }

                executeAndRespond(parsed, contacts, cleanedCommand)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Boss command", e)
                val errMsg = e.localizedMessage
                    ?: "মাফ করবেন বস, এখন একটু নেটওয়ার্ক সমস্যা হইতাছে।"
                _errorBanner.value = errMsg
                appendServantMessage(errMsg, "সমস্যা")
                speechEngine.speakBangla(errMsg, currentSettings.speechSpeed) {
                    resumeWakeStandbyIfActive()
                }
            } finally {
                _isThinking.value = false
            }
        }
    }

    private fun tryHandleVoiceSettingsFollowUp(utterance: String): Boolean {
        val t = utterance.trim().lowercase()
        return when {
            t.contains("ঢাকা") || t.contains("dhaka") -> {
                setDialectMode(DialectMode.DHAKAIYA)
                true
            }
            t.contains("গ্রাম") || t.contains("gram") -> {
                setDialectMode(DialectMode.GRAMYO)
                true
            }
            t.contains("১০") || t.contains("10") || t.contains("দশ") -> {
                setMemoryRetentionDays(10)
                true
            }
            t.contains("৫") || t.contains("5") || t.contains("পাঁচ") -> {
                setMemoryRetentionDays(5)
                true
            }
            t.contains("দ্রুত") || t.contains("জোরে") || t.contains("বাড়া") ||
                t.contains("তাড়াতাড়ি") || t.contains("fast") -> {
                applyVoiceSpeedWithSpokenReply(
                    1.22f,
                    "জি বস, ছোটুর আওয়াজ একটু দ্রুত কইরা দিছি!",
                    "আওয়াজ: দ্রুত"
                )
                true
            }
            t.contains("আস্তে") || t.contains("ধীরে") || t.contains("কমা") ||
                t.contains("কম") || t.contains("slow") -> {
                applyVoiceSpeedWithSpokenReply(
                    0.82f,
                    "জি বস, ছোটুর আওয়াজ আরামদায়ক আস্তে কইরা দিছি!",
                    "আওয়াজ: আস্তে"
                )
                true
            }
            t.contains("স্বাভাবিক") || t.contains("নরমাল") || t.contains("মাঝারি") ||
                t.contains("সাধারণ") -> {
                applyVoiceSpeedWithSpokenReply(
                    1.0f,
                    "জি বস, আওয়াজ স্বাভাবিক কইরা দিছি!",
                    "আওয়াজ: স্বাভাবিক"
                )
                true
            }
            t == "আওয়াজ" || t == "আওয়াজ" -> {
                val followUp = "বস, আওয়াজ দ্রুত করমু না আস্তে করমু?"
                appendServantMessage(followUp, "ভয়েস সেটিংস")
                speechEngine.speakBangla(followUp, settings.value.speechSpeed) {
                    if (hasMicPermission()) startSingleCommandListening()
                }
                true
            }
            t == "ভাষা" -> {
                val followUp = "বস, ঢাকাইয়া করমু না গ্রাম্য বাংলা করমু?"
                appendServantMessage(followUp, "ভয়েস সেটিংস")
                speechEngine.speakBangla(followUp, settings.value.speechSpeed) {
                    if (hasMicPermission()) startSingleCommandListening()
                }
                true
            }
            t.contains("মনে") && !t.contains("১০") && !t.contains("৫") && !t.contains("দশ") && !t.contains("পাঁচ") -> {
                val followUp = "বস, ১০ দিন মনে রাখুম না ৫ দিন?"
                appendServantMessage(followUp, "ভয়েস সেটিংস")
                speechEngine.speakBangla(followUp, settings.value.speechSpeed) {
                    if (hasMicPermission()) startSingleCommandListening()
                }
                true
            }
            isNegativeNo(t) || t.contains("থাক") || t.contains("কিছু না") -> {
                _pendingSettingsVoicePrompt.value = false
                val msg = "আইচ্ছা বস, কোনো সেটিংস বদলাই নাই!"
                appendServantMessage(msg, "সেটিংস অপরিবর্তিত")
                speechEngine.speakBangla(msg, settings.value.speechSpeed) {
                    resumeWakeStandbyIfActive()
                }
                true
            }
            else -> false
        }
    }

    private suspend fun executeAndRespond(
        parsed: ParsedJarvisCommand,
        contacts: List<com.example.data.DeviceContact>,
        originalUtterance: String
    ) {
        val currentSettings = settings.value

        // Handle special Chhotu stateful & voice-settings commands first
        _pendingSettingsVoicePrompt.value = false
        var effectiveSpeechSpeed = currentSettings.speechSpeed
        when (parsed.intentType) {
            JarvisIntentType.ENABLE_CALL_RECORDING -> {
                settingsRepo.setAutoRecordCalls(true)
            }
            JarvisIntentType.KEEP_LAST_RECORDING -> {
                callRecorder.keepLatestRecording()
            }
            JarvisIntentType.PIN_MEMORY -> {
                withContext(Dispatchers.IO) { memoryDao.pinLatestMemory() }
            }
            JarvisIntentType.DELETE_MEMORY -> {
                withContext(Dispatchers.IO) { memoryDao.deleteAllUnpinned() }
            }
            JarvisIntentType.SET_SCHEDULE_SLEEPING -> {
                settingsRepo.setBossSchedule(BossScheduleStatus.SLEEPING)
            }
            JarvisIntentType.SET_SCHEDULE_WORKING -> {
                settingsRepo.setBossSchedule(BossScheduleStatus.WORKING)
            }
            JarvisIntentType.SET_SCHEDULE_FREE -> {
                settingsRepo.setBossSchedule(BossScheduleStatus.FREE)
            }
            JarvisIntentType.FORBID_ACTION -> {
                withContext(Dispatchers.IO) {
                    memoryDao.insertForbidden(
                        ForbiddenActionEntity(
                            actionKey = parsed.messageOrTopic.ifBlank { originalUtterance },
                            reasonBangla = originalUtterance
                        )
                    )
                }
            }
            JarvisIntentType.OPEN_SETTINGS_VOICE_PROMPT -> {
                _uiEvents.send(JarvisUiEvent.OpenSettingsTab)
                return
            }
            JarvisIntentType.CHANGE_VOICE_SPEED_FAST -> {
                effectiveSpeechSpeed = 1.22f
                settingsRepo.setSpeechSpeed(effectiveSpeechSpeed)
            }
            JarvisIntentType.CHANGE_VOICE_SPEED_SLOW -> {
                effectiveSpeechSpeed = 0.82f
                settingsRepo.setSpeechSpeed(effectiveSpeechSpeed)
            }
            JarvisIntentType.CHANGE_VOICE_SPEED_NORMAL -> {
                effectiveSpeechSpeed = 1.0f
                settingsRepo.setSpeechSpeed(effectiveSpeechSpeed)
            }
            JarvisIntentType.CHANGE_DIALECT_DHAKAIYA -> {
                settingsRepo.setDialectMode(DialectMode.DHAKAIYA)
            }
            JarvisIntentType.CHANGE_DIALECT_GRAMYO -> {
                settingsRepo.setDialectMode(DialectMode.GRAMYO)
            }
            JarvisIntentType.CHANGE_MEMORY_10_DAYS -> {
                settingsRepo.setMemoryRetentionDays(10)
            }
            JarvisIntentType.CHANGE_MEMORY_5_DAYS -> {
                settingsRepo.setMemoryRetentionDays(5)
            }
            else -> Unit
        }

        val execResult = actionExecutor.execute(parsed, contacts)
        if (execResult.statusBadgeBangla == "ব্যাংক সুরক্ষা") {
            _isBlockedShield.value = true
        }
        if (execResult.openedAiStudioInBrowser) {
            _pendingBrowserUpdate.value = PendingBrowserUpdatePrompt(
                url = "https://aistudio.google.com",
                questionBangla = "বস, ক্রোমে এআই স্টুডিও দেখেন। আপডেট করুম? 'হ' বা 'না' কন বস।"
            )
        }

        val finalReply = execResult.followUpReplyBangla ?: parsed.spokenReply
        val badge = when {
            parsed.detectedMood == BossMood.SAD -> "মন ভালো করার চেষ্টা"
            parsed.confidenceNote.isNotBlank() -> parsed.confidenceNote
            else -> execResult.statusBadgeBangla
        }

        appendServantMessage(
            text = finalReply,
            badge = badge,
            isBlocked = execResult.statusBadgeBangla == "ব্যাংক সুরক্ষা"
        )

        // Persist Memory (auto-expires after 10 or 5 days) + Learn Habits
        persistMemoryAndLearn(
            bossText = originalUtterance,
            chhotuReply = finalReply,
            parsed = parsed,
            retentionDays = settings.value.memoryRetentionDays
        )

        speechEngine.speakBangla(finalReply, effectiveSpeechSpeed) {
            if (execResult.openedAiStudioInBrowser && hasMicPermission()) {
                startSingleCommandListening()
            } else {
                resumeWakeStandbyIfActive()
            }
        }

        if (execResult.requestPhotoPicker) {
            _uiEvents.send(JarvisUiEvent.LaunchPhotoPicker)
        }
        if (execResult.requestFilePicker) {
            _uiEvents.send(JarvisUiEvent.LaunchFilePicker)
        }
        if (execResult.requestVoiceUnlock) {
            _uiEvents.send(JarvisUiEvent.LaunchBiometricUnlock)
        }
    }

    private suspend fun persistMemoryAndLearn(
        bossText: String,
        chhotuReply: String,
        parsed: ParsedJarvisCommand,
        retentionDays: Int
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expiresAt = now + TimeUnit.DAYS.toMillis(retentionDays.toLong())
        memoryDao.insertMemory(
            MemoryEntryEntity(
                bossText = bossText,
                chhotuReply = chhotuReply,
                category = parsed.intentType.name,
                moodTag = parsed.detectedMood.name,
                createdAt = now,
                expiresAt = expiresAt,
                isPinnedForever = parsed.intentType == JarvisIntentType.PIN_MEMORY
            )
        )

        // Learn frequent contact or habit
        val habitKey = when {
            parsed.targetContactOrApp.isNotBlank() -> "target_${parsed.targetContactOrApp.lowercase()}"
            parsed.learnedHabitNote.isNotBlank() -> "habit_${parsed.learnedHabitNote.hashCode()}"
            else -> null
        }
        if (habitKey != null) {
            val existing = memoryDao.findHabit(habitKey)
            val desc = parsed.learnedHabitNote.ifBlank {
                "বেশি যোগাযোগ/কাজ: ${parsed.targetContactOrApp}"
            }
            memoryDao.upsertHabit(
                LearnedHabitEntity(
                    habitKey = habitKey,
                    descriptionBangla = desc,
                    frequencyCount = (existing?.frequencyCount ?: 0) + 1,
                    lastUpdated = now
                )
            )
        }
    }

    // --- Memory Vault Actions ---

    fun pinMemoryForever(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.pinMemory(id)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemoryById(id)
        }
    }

    fun clearAllUnpinnedMemories() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { memoryDao.deleteAllUnpinned() }
            val msg = "জি বস, অপ্রয়োজনীয় মেমরি মুইছা দিছি, দরকারি গুলা রাইখা দিছি!"
            speechEngine.speakBangla(msg, settings.value.speechSpeed)
        }
    }

    fun unbanAction(actionKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.removeForbidden(actionKey)
        }
    }

    // --- Voice Unlock ---

    fun requestVoiceUnlockTest() {
        val reply = "জি বস, অফিশিয়াল ভয়েস ও বায়োমেট্রিক যাচাই চালু করতাছি।"
        speechEngine.speakBangla(reply, settings.value.speechSpeed) {
            _uiEvents.trySend(JarvisUiEvent.LaunchBiometricUnlock)
        }
    }

    fun onBiometricUnlockSuccess() {
        _isVaultUnlocked.value = true
        val msg = "জি বস, যাচাই সম্পন্ন! সিস্টেম খুইলা দিছি বস।"
        appendServantMessage(msg, "আনলক সফল")
        speechEngine.speakBangla(msg, settings.value.speechSpeed)
    }

    fun onBiometricUnlockError(errorMessage: String) {
        _errorBanner.value = errorMessage
        speechEngine.speakBangla(errorMessage, settings.value.speechSpeed)
    }

    // --- Darowan Call Screening & Auto-Record System ---

    fun startIncomingCallScreeningFlow(
        callerNumber: String,
        knownContactName: String,
        channel: CallChannel = CallChannel.PHONE,
        isVideoCall: Boolean = false
    ) {
        val resolvedName = knownContactName.ifBlank {
            ContactsHelper.lookupContactNameByNumber(appContext, callerNumber)
        }
        CallScreeningCoordinator.beginIncomingScreening(
            callerNumber = callerNumber.ifBlank { "+8801711-000000" },
            knownContactName = resolvedName,
            channel = channel,
            isVideoCall = isVideoCall,
            isLiveSystemCall = false
        )
        speechEngine.speakBangla(
            CallScreeningCoordinator.INITIAL_CALLER_GREETING,
            settings.value.speechSpeed
        ) {
            CallScreeningCoordinator.updatePhase(CallScreenPhase.LISTENING_CALLER)
            if (hasMicPermission()) {
                listenToCallerVoiceForScreening()
            }
        }
    }

    fun listenToCallerVoiceForScreening() {
        CallScreeningCoordinator.updatePhase(CallScreenPhase.LISTENING_CALLER)
        speechEngine.startSingleCommandListening(
            onResult = { callerSpoken -> submitCallerResponse(callerSpoken) },
            onError = { err -> _errorBanner.value = err }
        )
    }

    fun submitCallerResponse(callerSpokenText: String) {
        val current = callSession.value
        val currentSettings = settings.value
        val cleaned = callerSpokenText.trim()
        if (cleaned.isBlank()) return

        CallScreeningCoordinator.updatePhase(CallScreenPhase.ANALYZING_CALLER)

        viewModelScope.launch {
            try {
                val analysis = aiRepo.analyzeIncomingCaller(
                    callerNumber = current.callerNumber,
                    knownContactName = current.knownContactName,
                    channel = current.channel,
                    isVideoCall = current.isVideoCall,
                    bossSchedule = currentSettings.bossSchedule,
                    dialectMode = currentSettings.dialectMode,
                    callerSpokenText = cleaned
                )

                CallScreeningCoordinator.updateCallerTranscript(cleaned, analysis.isEmergency)

                // Schedule Check: If Boss is sleeping/working and NOT emergency, Chhotu tells caller automatically!
                if (currentSettings.bossSchedule != BossScheduleStatus.FREE && !analysis.isEmergency) {
                    val busyReply = analysis.callerAutoReplyIfBusyBangla
                    CallScreeningCoordinator.rejectCallPolitely(appContext, busyReply)
                    speechEngine.speakBangla(busyReply, currentSettings.speechSpeed) {
                        resumeWakeStandbyIfActive()
                    }
                    return@launch
                }

                // Private Boss briefing (caller cannot hear): "বস, শামীম ভাই ফোন করছে, দিমু?"
                CallScreeningCoordinator.setBossBriefing(
                    briefing = analysis.bossBriefingBangla,
                    isEmergency = analysis.isEmergency
                )
                speechEngine.speakBangla(
                    analysis.bossBriefingBangla,
                    currentSettings.speechSpeed
                ) {
                    if (hasMicPermission()) {
                        listenBossCallDecision()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Call screening analysis error", e)
                val callerLabel = current.knownContactName.ifBlank { current.callerNumber }
                val fallback = "বস, $callerLabel ফোন করছে, দিমু?"
                CallScreeningCoordinator.setBossBriefing(fallback, isEmergency = false)
                speechEngine.speakBangla(fallback, currentSettings.speechSpeed) {
                    if (hasMicPermission()) {
                        listenBossCallDecision()
                    }
                }
            }
        }
    }

    fun listenBossCallDecision() {
        speechEngine.startSingleCommandListening(
            onResult = { bossVoice -> submitBossCallDecision(bossVoice) },
            onError = { err -> _errorBanner.value = err }
        )
    }

    fun submitBossCallDecision(bossReplyText: String) {
        val cleaned = bossReplyText.trim()
        if (cleaned.isBlank()) return

        viewModelScope.launch {
            val decision = aiRepo.evaluateBossCallDecision(cleaned)
            if (decision.shouldConnect) {
                connectAndMaybeRecordCall()
            } else {
                CallScreeningCoordinator.rejectCallPolitely(
                    appContext,
                    decision.callerClosingBangla
                )
                speechEngine.speakBangla(
                    "${decision.bossConfirmationBangla} কলাররে কইছি: ${decision.callerClosingBangla}",
                    settings.value.speechSpeed
                ) {
                    resumeWakeStandbyIfActive()
                }
            }
        }
    }

    fun acceptScreenedCallDirectly() {
        connectAndMaybeRecordCall()
    }

    private fun connectAndMaybeRecordCall() {
        val current = callSession.value
        val shouldRecord = settings.value.autoRecordCalls
        if (shouldRecord) {
            val callerDisplay = current.knownContactName.ifBlank { current.callerNumber }
            val channelLabel = current.channel.labelBangla
            // Announce to caller BEFORE starting recording: "কলটি রেকর্ড হচ্ছে"
            speechEngine.speakBangla(
                CallScreeningCoordinator.RECORDING_NOTICE_CALLER,
                settings.value.speechSpeed
            ) {
                val started = callRecorder.startRecording(callerDisplay, channelLabel)
                CallScreeningCoordinator.connectCallToBoss(appContext, isRecording = started)
            }
        } else {
            CallScreeningCoordinator.connectCallToBoss(appContext, isRecording = false)
            speechEngine.speakBangla("জি বস, কল কানেক্ট কইরা দিছি!", settings.value.speechSpeed)
        }
    }

    fun finishActiveCallAndMaybeKeep(keepRecording: Boolean) {
        viewModelScope.launch {
            val saved = callRecorder.finishRecording(keepByDefault = keepRecording)
            CallScreeningCoordinator.markRecordingStopped()
            if (!keepRecording) {
                // Rule #5: Auto-delete when call finishes unless Boss asks to keep it
                callRecorder.autoDeleteTransientRecordings()
                if (saved != null) {
                    speechEngine.speakBangla(
                        "বস, কল শেষ — অস্থায়ী রেকর্ড অটো ডিলিট কইরা দিছি।",
                        settings.value.speechSpeed
                    )
                }
            } else {
                speechEngine.speakBangla(
                    "জি বস, আপনার কথা মতো কল রেকর্ড স্টোরেজে রাইখা দিছি!",
                    settings.value.speechSpeed
                )
            }
            CallScreeningCoordinator.resetSession()
        }
    }

    fun keepRecordingForever(id: Long) {
        viewModelScope.launch {
            callRecorder.keepRecording(id)
        }
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        viewModelScope.launch {
            if (_playingRecordingPath.value == recording.filePath) {
                callRecorder.stopPlayback()
                _playingRecordingPath.value = null
            }
            callRecorder.deleteRecording(recording)
        }
    }

    fun togglePlayRecording(recording: CallRecordingEntity) {
        if (_playingRecordingPath.value == recording.filePath) {
            callRecorder.stopPlayback()
            _playingRecordingPath.value = null
            return
        }
        _playingRecordingPath.value = recording.filePath
        callRecorder.playRecording(
            filePath = recording.filePath,
            onFinished = { _playingRecordingPath.value = null },
            onError = { err ->
                _playingRecordingPath.value = null
                _errorBanner.value = err
            }
        )
    }

    fun rejectScreenedCallDirectly() {
        val closing = "দুঃখিত, সুমন বস অহন কথা কইতে পারতেছেন না। পরে কল দিয়েন।"
        CallScreeningCoordinator.rejectCallPolitely(appContext, closing)
        speechEngine.speakBangla("জি বস, কল কাইটা দিছি!", settings.value.speechSpeed)
    }

    fun resetCallScreeningSession() {
        callRecorder.stopRecordingWithoutSaving()
        CallScreeningCoordinator.resetSession()
    }

    private fun isAffirmativeYes(text: String): Boolean {
        val t = text.trim().lowercase()
        return t == "হ" || t == "হ্যাঁ" || t.startsWith("হ ") || t.contains("দাও") ||
            t.contains("দে") || t.contains("কর") || t.contains("yes") || t.contains("ok")
    }

    private fun isNegativeNo(text: String): Boolean {
        val t = text.trim().lowercase()
        return t == "না" || t.startsWith("না ") || t.contains("থাক") ||
            t.contains("দরকার নাই") || t.contains("কাট") || t.contains("no")
    }

    private fun appendBossMessage(text: String) {
        _conversations.value = (_conversations.value + ConversationEntry(
            isBoss = true,
            text = text
        )).takeLast(40)
    }

    private fun appendServantMessage(
        text: String,
        badge: String? = null,
        isBlocked: Boolean = false
    ) {
        _conversations.value = (_conversations.value + ConversationEntry(
            isBoss = false,
            text = text,
            actionBadge = badge,
            isBlocked = isBlocked
        )).takeLast(40)
    }

    override fun onCleared() {
        super.onCleared()
        callRecorder.stopPlayback()
        callRecorder.stopRecordingWithoutSaving()
        speechEngine.release()
    }
}
