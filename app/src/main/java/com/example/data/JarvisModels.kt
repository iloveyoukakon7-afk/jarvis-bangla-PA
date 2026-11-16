package com.example.data

enum class OrbStatus {
    IDLE,
    WAKE_STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    CALL_SCREENING,
    RECORDING_CALL,
    BLOCKED_SHIELD
}

enum class DialectMode(val labelBangla: String) {
    DHAKAIYA("ঢাকাইয়া"),
    GRAMYO("গ্রাম্য")
}

enum class BossScheduleStatus(val labelBangla: String, val callerReplyBangla: String) {
    FREE("ফ্রি আছি", ""),
    SLEEPING("ঘুমাচ্ছি", "সুমন বস এখন ঘুমাইতাছেন। ইমারজেন্সি না হইলে পরে কল দিয়েন।"),
    WORKING("কাজ করছি", "সুমন বস এখন জরুরি কাজে ব্যস্ত আছেন। ইমারজেন্সি না হইলে পরে কল দিয়েন।")
}

enum class BossMood {
    NORMAL,
    HAPPY,
    SAD,
    STRESSED
}

enum class PermissionCategory(val labelBangla: String) {
    CONTACTS("কনট্যাক্টস"),
    CALL("কল"),
    WHATSAPP("হোয়াটসঅ্যাপ"),
    IMO("ইমো"),
    MESSENGER("মেসেঞ্জার"),
    BROWSER("ব্রাউজার"),
    STORAGE("স্টোরেজ ও রেকর্ড"),
    NOTIFICATION("নোটিফিকেশন")
}

enum class CallChannel(val labelBangla: String) {
    PHONE("ফোন কল"),
    WHATSAPP("হোয়াটসঅ্যাপ"),
    IMO("ইমো"),
    MESSENGER("মেসেঞ্জার"),
    BRILLIANT("ব্রিলিয়ান্ট")
}

enum class JarvisIntentType {
    CALL_CONTACT,
    SEND_SMS,
    WHATSAPP_MESSAGE,
    IMO_CALL,
    MESSENGER_MESSAGE,
    OPEN_BROWSER_AISTUDIO,
    ENABLE_CALL_RECORDING,
    KEEP_LAST_RECORDING,
    PIN_MEMORY,
    DELETE_MEMORY,
    SET_SCHEDULE_SLEEPING,
    SET_SCHEDULE_WORKING,
    SET_SCHEDULE_FREE,
    FORBID_ACTION,
    OPEN_SETTINGS_VOICE_PROMPT,
    CHANGE_VOICE_SPEED_FAST,
    CHANGE_VOICE_SPEED_SLOW,
    CHANGE_VOICE_SPEED_NORMAL,
    CHANGE_DIALECT_DHAKAIYA,
    CHANGE_DIALECT_GRAMYO,
    CHANGE_MEMORY_10_DAYS,
    CHANGE_MEMORY_5_DAYS,
    SET_ALARM,
    OPEN_CAMERA,
    OPEN_GALLERY,
    TOGGLE_WIFI,
    OPEN_BLUETOOTH,
    PLAY_MUSIC,
    OPEN_APP,
    CHECK_LOCATION,
    OPEN_FILES,
    VOICE_UNLOCK,
    GENERAL_CHAT,
    BLOCKED_BANKING
}

data class ParsedJarvisCommand(
    val intentType: JarvisIntentType,
    val targetContactOrApp: String = "",
    val messageOrTopic: String = "",
    val alarmHour: Int = -1,
    val alarmMinute: Int = -1,
    val spokenReply: String,
    val confidenceNote: String = "",
    val detectedMood: BossMood = BossMood.NORMAL,
    val learnedHabitNote: String = ""
)

data class PendingPermissionPrompt(
    val category: PermissionCategory,
    val questionBangla: String,
    val command: ParsedJarvisCommand,
    val originalUtterance: String
)

data class PendingBrowserUpdatePrompt(
    val url: String,
    val questionBangla: String
)

data class ConversationEntry(
    val id: Long = System.currentTimeMillis(),
    val isBoss: Boolean,
    val text: String,
    val actionBadge: String? = null,
    val isBlocked: Boolean = false
)

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

data class AssistantSettings(
    val wakeName: String = "ওই ছোটু",
    val unlockPhrase: String = "ওই ছোটু, স্ক্রিন খোলো",
    val dialectMode: DialectMode = DialectMode.DHAKAIYA,
    val memoryRetentionDays: Int = 10,
    val speechSpeed: Float = 1.0f,
    val onboardingCompleted: Boolean = false,
    val permissionsGrantedOnce: Boolean = false,
    val callScreeningEnabled: Boolean = true,
    val wakeWordActive: Boolean = true,
    val autoRecordCalls: Boolean = false,
    val bossSchedule: BossScheduleStatus = BossScheduleStatus.FREE,
    val firstLaunchMillis: Long = System.currentTimeMillis(),
    val grantedCategories: Set<String> = emptySet()
)

enum class CallScreenPhase {
    IDLE,
    GREETING_CALLER,
    LISTENING_CALLER,
    ANALYZING_CALLER,
    ASKING_BOSS,
    CONNECTED_TO_BOSS,
    REJECTED_POLITELY
}

data class CallScreenSession(
    val phase: CallScreenPhase = CallScreenPhase.IDLE,
    val channel: CallChannel = CallChannel.PHONE,
    val isVideoCall: Boolean = false,
    val callerNumber: String = "",
    val knownContactName: String = "",
    val callerTranscript: String = "",
    val bossBriefing: String = "",
    val callerClosingLine: String = "",
    val isEmergency: Boolean = false,
    val isRecordingActive: Boolean = false,
    val isLiveSystemCall: Boolean = false
)

data class CallScreenLogEntry(
    val id: Long = System.currentTimeMillis(),
    val callerDisplay: String,
    val channelLabel: String,
    val callerReason: String,
    val decisionLabel: String,
    val timestampLabel: String
)
