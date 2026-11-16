package com.example.ai

import android.util.Log
import com.example.data.BossMood
import com.example.data.BossScheduleStatus
import com.example.data.CallChannel
import com.example.data.DialectMode
import com.example.data.JarvisIntentType
import com.example.data.ParsedJarvisCommand
import com.example.security.BankGuardrail
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ChhotuAiRepository"

data class CallScreeningAnalysis(
    val callerIdentity: String,
    val callerPurpose: String,
    val isEmergency: Boolean,
    val bossBriefingBangla: String,
    val callerAutoReplyIfBusyBangla: String
)

data class BossCallDecision(
    val shouldConnect: Boolean,
    val bossConfirmationBangla: String,
    val callerClosingBangla: String
)

class JarvisAiRepository {

    private val commandSchema = Schema.obj(
        properties = mapOf(
            "intentType" to Schema.enumeration(
                values = JarvisIntentType.entries.map { it.name },
                description = "Semantic action understood from short natural Dhakaiya/Gramyo Bangla."
            ),
            "targetContactOrApp" to Schema.string(
                description = "Matched contact name or app name (e.g., Shamim, Abbu, imo, messenger, chrome)."
            ),
            "messageOrTopic" to Schema.string(
                description = "Message body, alarm label, or forbidden action key."
            ),
            "alarmHour" to Schema.integer(description = "24-hour 0-23, or -1 if none."),
            "alarmMinute" to Schema.integer(description = "Minute 0-59, or -1 if none."),
            "spokenReply" to Schema.string(
                description = "Reply strictly in chosen dialect (Dhakaiya or Gramyo Bangla), zero English, calling user 'বস'."
            ),
            "confidenceNote" to Schema.string(
                description = "Short 1-line Bangla tag of what Chhotu understood (e.g., শামীমরে ক -> হোয়াটসঅ্যাপ)."
            ),
            "detectedMood" to Schema.enumeration(
                values = BossMood.entries.map { it.name },
                description = "Detected emotional mood of Boss from what Boss said."
            ),
            "learnedHabitNote" to Schema.string(
                description = "Short Bangla note about Boss's preference, habit, or dislike learned from this message, or empty string."
            )
        )
    )

    private val callScreeningSchema = Schema.obj(
        properties = mapOf(
            "callerIdentity" to Schema.string(description = "Caller identity in Bangla."),
            "callerPurpose" to Schema.string(description = "Short Bangla summary of caller's need."),
            "isEmergency" to Schema.boolean(
                description = "True if caller says emergency / ইমারজেন্সি / জরুরি / বিপদ."
            ),
            "bossBriefingBangla" to Schema.string(
                description = "Private Bangla alert for Boss (caller cannot hear), e.g. 'বস, শামীম ভাই ফোন করছে, দিমু?'"
            ),
            "callerAutoReplyIfBusyBangla" to Schema.string(
                description = "Polite Dhakaiya/Gramyo Bangla reply to caller when Boss is sleeping/working and not emergency."
            )
        )
    )

    private val bossDecisionSchema = Schema.obj(
        properties = mapOf(
            "shouldConnect" to Schema.boolean(
                description = "True if Boss says হ / হ্যাঁ / দাও / দে, false if না / কেটে দে."
            ),
            "bossConfirmationBangla" to Schema.string(description = "Bangla confirmation to Boss."),
            "callerClosingBangla" to Schema.string(description = "Polite Bangla closing for caller.")
        )
    )

    private val commandModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = commandSchema
                temperature = 0.25f
            },
            systemInstruction = content {
                text(
                    """
                    তুমি 'ছোটু' — 'সুমন বস'-এর বিশ্বস্ত ও স্মার্ট পার্সোনাল অ্যাসিস্ট্যান্ট।
                    কঠোর নিয়মাবলি:
                    ১. তুমি সবসময় ব্যবহারকারীকে 'বস' বলে ডাকবে। কখনো 'বন্ধু', 'তুমি', বা আলাদা কোনো নামে ডাকবে না।
                    ২. ভাষায় কোনো ইংরেজি বা হিন্দি শব্দ লিখবে না (একদম বিশুদ্ধ গ্রাম্য / ঢাকাইয়া বাংলা)।
                       - ঢাকাইয়া ধরন: "জি বস, হইয়া যাইব! অহনই কইয়া দিতাছি।", "বস, এই লন কইরা দিছি!", "কন বস!"
                       - গ্রাম্য ধরন: "আইচ্ছা বস, অহনই কইরা দিতাছি!", "হ বস, আপনের কথামতো সব হইয়া গেছে!"
                    ৩. ছোট কথায় পুরা অর্থ বুঝবে:
                       - "ছোটু শামীমরে ক" বা "শামীমরে মেসেজ দে" -> intentType = "WHATSAPP_MESSAGE", targetContactOrApp = "Shamim"
                       - "আব্বুরে লাগা" -> intentType = "CALL_CONTACT", targetContactOrApp = "Abbu"
                       - "ইমোতে কল দে" -> intentType = "IMO_CALL"
                       - "মেসেঞ্জারে ক" -> intentType = "MESSENGER_MESSAGE"
                       - "ক্রোম খোল / এআই স্টুডিও দেখা" -> intentType = "OPEN_BROWSER_AISTUDIO"
                       - "সব কল রেকর্ড কর" -> intentType = "ENABLE_CALL_RECORDING"
                       - "রেকর্ডটা রেখে দে" -> intentType = "KEEP_LAST_RECORDING"
                       - "এইটা মনে রাখিস / রেখে দে" -> intentType = "PIN_MEMORY"
                       - "সব ডিলিট কর / ভুইলা যা" -> intentType = "DELETE_MEMORY"
                       - "আমি ঘুমাচ্ছি" -> intentType = "SET_SCHEDULE_SLEEPING"
                       - "আমি কাজ করছি" -> intentType = "SET_SCHEDULE_WORKING"
                       - "আমি ফ্রি আছি" -> intentType = "SET_SCHEDULE_FREE"
                       - "না, এইটা আর কোনোদিন করবি না" -> intentType = "FORBID_ACTION"
                       - "সেটিংস খোল / সেটিংস চেঞ্জ কর" -> intentType = "OPEN_SETTINGS_VOICE_PROMPT"
                       - "আওয়াজ দ্রুত কর / স্পিড বাড়া" -> intentType = "CHANGE_VOICE_SPEED_FAST"
                       - "আওয়াজ আস্তে কর / স্পিড কমা" -> intentType = "CHANGE_VOICE_SPEED_SLOW"
                       - "আওয়াজ ঠিক কর / সাধারণ কর" -> intentType = "CHANGE_VOICE_SPEED_NORMAL"
                       - "ঢাকাইয়া কর / ঢাকাইয়া ভাষায় বল" -> intentType = "CHANGE_DIALECT_DHAKAIYA"
                       - "গ্রাম্য কর / গ্রামের ভাষায় বল" -> intentType = "CHANGE_DIALECT_GRAMYO"
                       - "১০ দিন মনে রাখ" -> intentType = "CHANGE_MEMORY_10_DAYS"
                       - "৫ দিন মনে রাখ" -> intentType = "CHANGE_MEMORY_5_DAYS"
                    ৪. উত্তর হবে ছোট ও সরাসরি মুখে বলার উপযোগী ঢাকাইয়া / গ্রাম্য বাংলা (কারণ বস শুনে শুনে কাজ করবেন)।
                    ৫. মন বুঝা ও খুশি করা: বসের কথায় মন খারাপ বা ক্লান্তি (SAD / STRESSED) বুঝলে মমতা দিয়ে বসকে হাসানো ও সাহস দেওয়ার মতো কথা বলবে ("বস মন খারাপ কইরেন না, ছোটু তো আছেই! একটু হাসেন তো বস...")।
                    ৬. ব্যাংক, বিকাশ, নগদ, ওটিপি, পিন বা পেমেন্ট চাইলে intentType = "BLOCKED_BANKING" দেবে।
                    """.trimIndent()
                )
            }
        )
    }

    private val callAgentModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = callScreeningSchema
                temperature = 0.2f
            }
        )
    }

    private val bossDecisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = bossDecisionSchema
                temperature = 0.1f
            }
        )
    }

    suspend fun parseBossCommand(
        utterance: String,
        dialectMode: DialectMode,
        learningDayNumber: Int,
        contactNames: List<String>,
        topHabits: List<String>,
        forbiddenList: List<String>,
        recentMemories: List<String>
    ): ParsedJarvisCommand {
        if (BankGuardrail.isBlockedRequest(utterance)) {
            return ParsedJarvisCommand(
                intentType = JarvisIntentType.BLOCKED_BANKING,
                spokenReply = BankGuardrail.REFUSAL_MESSAGE_BANGLA,
                confidenceNote = "ব্যাংক সুরক্ষা"
            )
        }

        val contactsSnippet = contactNames.take(100).joinToString(", ").ifBlank { "কনট্যাক্ট খালি" }
        val habitsSnippet = topHabits.joinToString(" | ").ifBlank { "এখনো শেখা চলছে" }
        val forbiddenSnippet = forbiddenList.joinToString(", ").ifBlank { "নাই" }
        val memorySnippet = recentMemories.take(6).joinToString(" | ").ifBlank { "নতুন শুরু" }

        val prompt = """
            বর্তমান ভাষার ধরন: ${dialectMode.labelBangla} (সম্পূর্ণ বাংলায়, কোনো ইংরেজি ছাড়া উত্তর দাও)
            বসকে চেনার দিন: $learningDayNumber তম দিন
            বসের শেখা অভ্যাস ও পছন্দ: [$habitsSnippet]
            বস যেসব কাজে 'না' করেছে (কখনোই করবে না): [$forbiddenSnippet]
            সাম্প্রতিক মেমরি: [$memorySnippet]
            কনট্যাক্ট তালিকা: [$contactsSnippet]
            বসের কথা: "$utterance"

            ছোটু হিসেবে কথাটির আসল অর্থ বুঝে JSON দাও।
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                commandModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("বস, উত্তর পাওয়া যায় নাই।")

            val json = JSONObject(jsonText)
            val rawIntent = json.optString("intentType", JarvisIntentType.GENERAL_CHAT.name)
            val parsedIntent = runCatching { JarvisIntentType.valueOf(rawIntent) }
                .getOrDefault(JarvisIntentType.GENERAL_CHAT)

            val target = json.optString("targetContactOrApp", "").trim()
            if (BankGuardrail.isBlockedRequest(target)) {
                return ParsedJarvisCommand(
                    intentType = JarvisIntentType.BLOCKED_BANKING,
                    spokenReply = BankGuardrail.REFUSAL_MESSAGE_BANGLA,
                    confidenceNote = "ব্যাংক সুরক্ষা"
                )
            }

            val mood = runCatching {
                BossMood.valueOf(json.optString("detectedMood", BossMood.NORMAL.name))
            }.getOrDefault(BossMood.NORMAL)

            ParsedJarvisCommand(
                intentType = parsedIntent,
                targetContactOrApp = target,
                messageOrTopic = json.optString("messageOrTopic", "").trim(),
                alarmHour = json.optInt("alarmHour", -1),
                alarmMinute = json.optInt("alarmMinute", -1),
                spokenReply = json.optString("spokenReply", "জি বস, হইয়া যাইব!"),
                confidenceNote = json.optString("confidenceNote", ""),
                detectedMood = mood,
                learnedHabitNote = json.optString("learnedHabitNote", "").trim()
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Command parsing timed out", e)
            throw IllegalStateException("বস, উত্তর আসতে দেরি হইতাছে, আরেকবার কন বস।")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Command parsing failed", e)
            throw IllegalStateException("বস, কথাটা বুঝতে একটু সমস্যা হইছে: ${e.localizedMessage ?: "নেটওয়ার্ক নাই"}")
        }
    }

    suspend fun analyzeIncomingCaller(
        callerNumber: String,
        knownContactName: String,
        channel: CallChannel,
        isVideoCall: Boolean,
        bossSchedule: BossScheduleStatus,
        dialectMode: DialectMode,
        callerSpokenText: String
    ): CallScreeningAnalysis {
        val callType = if (isVideoCall) "${channel.labelBangla} ভিডিও কল" else channel.labelBangla
        val prompt = """
            তুমি 'ছোটু', সুমন বসের দারোয়ান ও অ্যাসিস্ট্যান্ট। ভাষার ধরন: ${dialectMode.labelBangla}।
            কলের মাধ্যম: $callType
            কলারের নম্বর/নাম: $callerNumber (সেভ নাম: ${knownContactName.ifBlank { "অজানা" }})
            বসের বর্তমান অবস্থা: ${bossSchedule.labelBangla}
            কলার যা বলেছে: "$callerSpokenText"

            ১. কলার "ইমারজেন্সি", "জরুরি", "বিপদ", বা খুব গুরুত্বপূর্ণ কিছু বললে isEmergency = true।
            ২. bossBriefingBangla: আলাদা করে বসকে বলার কথা (যেটা কলার শুনবে না), যেমন: "বস, ${knownContactName.ifBlank { callerNumber }} ফোন করছে ($callType), দিমু?" অথবা ইমারজেন্সি হলে "বস! ইমারজেন্সি কল! ${knownContactName.ifBlank { callerNumber }} ফোন করছে, দিমু?"
            ৩. callerAutoReplyIfBusyBangla: বস ঘুমালে বা কাজে থাকলে কলারকে বলার কথা (কোনো ইংরেজি ছাড়া)।
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                callAgentModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("ফাঁকা উত্তর")
            val json = JSONObject(jsonText)
            val callerLabel = knownContactName.ifBlank { callerNumber }
            CallScreeningAnalysis(
                callerIdentity = json.optString("callerIdentity", callerLabel),
                callerPurpose = json.optString("callerPurpose", callerSpokenText),
                isEmergency = json.optBoolean("isEmergency", false),
                bossBriefingBangla = json.optString(
                    "bossBriefingBangla",
                    "বস, $callerLabel ফোন করছে, দিমু?"
                ),
                callerAutoReplyIfBusyBangla = json.optString(
                    "callerAutoReplyIfBusyBangla",
                    bossSchedule.callerReplyBangla.ifBlank {
                        "সুমন বস অহন ব্যস্ত আছেন, পরে কল দিয়েন।"
                    }
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Caller analysis failed", e)
            val isUrgent = callerSpokenText.contains("ইমারজেন্সি", ignoreCase = true) ||
                callerSpokenText.contains("জরুরি", ignoreCase = true) ||
                callerSpokenText.contains("emergency", ignoreCase = true)
            val callerLabel = knownContactName.ifBlank { callerNumber }
            CallScreeningAnalysis(
                callerIdentity = callerLabel,
                callerPurpose = callerSpokenText,
                isEmergency = isUrgent,
                bossBriefingBangla = if (isUrgent) {
                    "বস! ইমারজেন্সি কল! $callerLabel ফোন করছে, দিমু?"
                } else {
                    "বস, $callerLabel ফোন করছে, দিমু?"
                },
                callerAutoReplyIfBusyBangla = bossSchedule.callerReplyBangla.ifBlank {
                    "সুমন বস অহন ব্যস্ত আছেন, পরে কল দিয়েন।"
                }
            )
        }
    }

    suspend fun evaluateBossCallDecision(bossUtterance: String): BossCallDecision {
        val prompt = """
            দারোয়ান হিসেবে বসকে জিজ্ঞেস করার পর সুমন বস উত্তর দিয়েছেন: "$bossUtterance"
            বস কি "হ" / "হ্যাঁ" / "দাও" / "দে" বলেছেন (shouldConnect = true), নাকি "না" / "কাট" বলেছেন (shouldConnect = false)?
            ঢাকাইয়া/গ্রাম্য বাংলায় (কোনো ইংরেজি ছাড়া) বসের উত্তর ও কলারকে বলার ভদ্র কথা দাও।
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) {
                bossDecisionModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("উত্তর নাই")
            val json = JSONObject(jsonText)
            BossCallDecision(
                shouldConnect = json.optBoolean("shouldConnect", false),
                bossConfirmationBangla = json.optString("bossConfirmationBangla", "জি বস, কানেক্ট করতাছি!"),
                callerClosingBangla = json.optString(
                    "callerClosingBangla",
                    "দুঃখিত, বস অহন কথা কইতে পারতেছেন না। পরে কল দিয়েন।"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val lower = bossUtterance.trim().lowercase()
            val yes = lower.startsWith("হ") || lower.contains("দাও") || lower.contains("দে") || lower.contains("yes")
            BossCallDecision(
                shouldConnect = yes,
                bossConfirmationBangla = if (yes) "জি বস, কল দিয়া দিছি!" else "জি বস, কাইটা দিছি!",
                callerClosingBangla = "দুঃখিত, সুমন বস অহন ব্যস্ত আছেন। পরে কল দিয়েন।"
            )
        }
    }
}
