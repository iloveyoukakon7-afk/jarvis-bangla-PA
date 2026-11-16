package com.example.device

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.DeviceContact
import com.example.data.JarvisIntentType
import com.example.data.ParsedJarvisCommand
import com.example.data.PermissionCategory
import com.example.security.BankGuardrail
import java.net.URLEncoder
import java.util.Locale

private const val TAG = "DeviceActionExecutor"

data class ActionExecutionResult(
    val executed: Boolean,
    val statusBadgeBangla: String,
    val followUpReplyBangla: String? = null,
    val requestPhotoPicker: Boolean = false,
    val requestFilePicker: Boolean = false,
    val requestVoiceUnlock: Boolean = false,
    val openedAiStudioInBrowser: Boolean = false
)

class DeviceActionExecutor(private val context: Context) {

    fun requiredPermissionCategory(intentType: JarvisIntentType): PermissionCategory? {
        return when (intentType) {
            JarvisIntentType.CALL_CONTACT -> PermissionCategory.CALL
            JarvisIntentType.SEND_SMS -> PermissionCategory.CONTACTS
            JarvisIntentType.WHATSAPP_MESSAGE -> PermissionCategory.WHATSAPP
            JarvisIntentType.IMO_CALL -> PermissionCategory.IMO
            JarvisIntentType.MESSENGER_MESSAGE -> PermissionCategory.MESSENGER
            JarvisIntentType.OPEN_BROWSER_AISTUDIO -> PermissionCategory.BROWSER
            JarvisIntentType.OPEN_FILES,
            JarvisIntentType.OPEN_GALLERY,
            JarvisIntentType.ENABLE_CALL_RECORDING -> PermissionCategory.STORAGE
            else -> null
        }
    }

    fun execute(
        command: ParsedJarvisCommand,
        contacts: List<DeviceContact>
    ): ActionExecutionResult {
        if (command.intentType == JarvisIntentType.BLOCKED_BANKING ||
            BankGuardrail.isBlockedRequest(command.targetContactOrApp) ||
            BankGuardrail.isBlockedRequest(command.messageOrTopic)
        ) {
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ব্যাংক সুরক্ষা",
                followUpReplyBangla = BankGuardrail.REFUSAL_MESSAGE_BANGLA
            )
        }

        return when (command.intentType) {
            JarvisIntentType.CALL_CONTACT -> handleCallContact(command.targetContactOrApp, contacts)
            JarvisIntentType.SEND_SMS -> handleSendSms(
                command.targetContactOrApp,
                command.messageOrTopic,
                contacts
            )
            JarvisIntentType.WHATSAPP_MESSAGE -> handleWhatsApp(
                command.targetContactOrApp,
                command.messageOrTopic,
                contacts
            )
            JarvisIntentType.IMO_CALL -> launchPackageOrFallback(
                packageName = "com.imo.android.imoim",
                banglaAppName = "ইমো",
                webFallbackUrl = "https://imo.im"
            )
            JarvisIntentType.MESSENGER_MESSAGE -> launchPackageOrFallback(
                packageName = "com.facebook.orca",
                banglaAppName = "মেসেঞ্জার",
                webFallbackUrl = "https://www.messenger.com"
            )
            JarvisIntentType.OPEN_BROWSER_AISTUDIO -> handleOpenChromeAiStudio()
            JarvisIntentType.SET_ALARM -> handleSetAlarm(
                command.alarmHour,
                command.alarmMinute,
                command.messageOrTopic
            )
            JarvisIntentType.OPEN_CAMERA -> handleOpenCamera()
            JarvisIntentType.OPEN_GALLERY -> ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "গ্যালারি",
                requestPhotoPicker = true
            )
            JarvisIntentType.OPEN_FILES -> ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "ফাইল স্টোরেজ",
                requestFilePicker = true
            )
            JarvisIntentType.TOGGLE_WIFI -> handleWifi()
            JarvisIntentType.OPEN_BLUETOOTH -> handleBluetooth()
            JarvisIntentType.PLAY_MUSIC -> handlePlayMusic()
            JarvisIntentType.OPEN_APP -> handleOpenApp(command.targetContactOrApp)
            JarvisIntentType.CHECK_LOCATION -> handleCheckLocation()
            JarvisIntentType.VOICE_UNLOCK -> ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "ভয়েস আনলক",
                requestVoiceUnlock = true
            )
            JarvisIntentType.ENABLE_CALL_RECORDING,
            JarvisIntentType.KEEP_LAST_RECORDING,
            JarvisIntentType.PIN_MEMORY,
            JarvisIntentType.DELETE_MEMORY,
            JarvisIntentType.SET_SCHEDULE_SLEEPING,
            JarvisIntentType.SET_SCHEDULE_WORKING,
            JarvisIntentType.SET_SCHEDULE_FREE,
            JarvisIntentType.FORBID_ACTION,
            JarvisIntentType.OPEN_SETTINGS_VOICE_PROMPT,
            JarvisIntentType.CHANGE_VOICE_SPEED_FAST,
            JarvisIntentType.CHANGE_VOICE_SPEED_SLOW,
            JarvisIntentType.CHANGE_VOICE_SPEED_NORMAL,
            JarvisIntentType.CHANGE_DIALECT_DHAKAIYA,
            JarvisIntentType.CHANGE_DIALECT_GRAMYO,
            JarvisIntentType.CHANGE_MEMORY_10_DAYS,
            JarvisIntentType.CHANGE_MEMORY_5_DAYS,
            JarvisIntentType.GENERAL_CHAT -> ActionExecutionResult(
                executed = true,
                statusBadgeBangla = command.confidenceNote.ifBlank { "হুকুম পালন" }
            )
            JarvisIntentType.BLOCKED_BANKING -> ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ব্যাংক সুরক্ষা",
                followUpReplyBangla = BankGuardrail.REFUSAL_MESSAGE_BANGLA
            )
        }
    }

    private fun handleOpenChromeAiStudio(): ActionExecutionResult {
        val aiStudioUrl = "https://aistudio.google.com"
        return try {
            val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(aiStudioUrl)).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (chromeIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chromeIntent)
            } else {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(aiStudioUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "ক্রোম — এআই স্টুডিও",
                followUpReplyBangla = "জি বস, ক্রোম খুইলা এআই স্টুডিও দিয়া দিছি! দেইখা 'হ' কইলে আপডেট করুম, 'না' কইলে করুম না বস।",
                openedAiStudioInBrowser = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Chrome AI Studio", e)
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ব্রাউজার ত্রুটি",
                followUpReplyBangla = "মাফ করবেন বস, ক্রোম ব্রাউজার খুলতে সমস্যা হইছে।"
            )
        }
    }

    private fun handleCallContact(
        targetName: String,
        contacts: List<DeviceContact>
    ): ActionExecutionResult {
        val matched = ContactsHelper.findBestContact(contacts, targetName)
        if (matched == null) {
            val digitsOnly = targetName.filter { it.isDigit() || it == '+' }
            if (digitsOnly.length >= 3) {
                return placeCallOrDial(digitsOnly, targetName)
            }
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "কনট্যাক্ট নাই",
                followUpReplyBangla = "বস, '$targetName' নামে আপনার ফোনে কাউরে খুঁইজা পাইলাম না।"
            )
        }
        return placeCallOrDial(matched.phoneNumber, matched.name)
    }

    private fun placeCallOrDial(phoneNumber: String, displayName: String): ActionExecutionResult {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = Intent(
            if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL,
            Uri.parse("tel:${Uri.encode(phoneNumber)}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "কল: $displayName",
                followUpReplyBangla = "জি বস, $displayName-রে কল লাগায়া দিতাছি!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call", e)
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "কল ব্যর্থ",
                followUpReplyBangla = "মাফ করবেন বস, কল লাগানো যায় নাই।"
            )
        }
    }

    private fun handleSendSms(
        targetName: String,
        message: String,
        contacts: List<DeviceContact>
    ): ActionExecutionResult {
        val matched = ContactsHelper.findBestContact(contacts, targetName)
        val phoneNumber = matched?.phoneNumber ?: targetName.filter { it.isDigit() || it == '+' }
        val displayName = matched?.name ?: targetName

        if (phoneNumber.isBlank()) {
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "নম্বর নাই",
                followUpReplyBangla = "বস, '$targetName'-এর নম্বর কনট্যাক্টে নাই।"
            )
        }

        val smsBody = message.ifBlank { "আসসালামু আলাইকুম।" }
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) {
            return try {
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager?.sendTextMessage(phoneNumber, null, smsBody, null, null)
                ActionExecutionResult(
                    executed = true,
                    statusBadgeBangla = "এসএমএস পাঠাইছি",
                    followUpReplyBangla = "জি বস, হইয়া গেছে! $displayName-রে মেসেজ পাঠায়া দিছি।"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Direct SMS failed", e)
                openSmsComposer(phoneNumber, smsBody, displayName)
            }
        }
        return openSmsComposer(phoneNumber, smsBody, displayName)
    }

    private fun openSmsComposer(
        phoneNumber: String,
        body: String,
        displayName: String
    ): ActionExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phoneNumber)}")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "এসএমএস রেডি",
                followUpReplyBangla = "জি বস, $displayName-এর লাইগা মেসেজ খুইলা দিছি।"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "এসএমএস ব্যর্থ",
                followUpReplyBangla = "মাফ করবেন বস, এসএমএস খোলা যায় নাই।"
            )
        }
    }

    private fun handleWhatsApp(
        targetName: String,
        message: String,
        contacts: List<DeviceContact>
    ): ActionExecutionResult {
        val matched = ContactsHelper.findBestContact(contacts, targetName)
        if (matched == null && targetName.isBlank()) {
            return launchPackageOrFallback(
                "com.whatsapp",
                "হোয়াটসঅ্যাপ",
                "https://web.whatsapp.com"
            )
        }

        val rawDigits = (matched?.phoneNumber ?: targetName).replace(Regex("[^0-9+]"), "")
        val cleanNumber = rawDigits.removePrefix("+")
        return try {
            val encodedText = URLEncoder.encode(message, "UTF-8")
            val uri = if (cleanNumber.length >= 6) {
                Uri.parse("https://wa.me/$cleanNumber?text=$encodedText")
            } else {
                Uri.parse("https://wa.me/?text=$encodedText")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val who = matched?.name ?: targetName.ifBlank { "হোয়াটসঅ্যাপ" }
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "হোয়াটসঅ্যাপ: $who",
                followUpReplyBangla = "জি বস, হইয়া যাইব! হোয়াটসঅ্যাপে $who-এর চ্যাট খুইলা দিছি বস।"
            )
        } catch (e: Exception) {
            launchPackageOrFallback("com.whatsapp", "হোয়াটসঅ্যাপ", null)
        }
    }

    private fun handleSetAlarm(hour: Int, minute: Int, label: String): ActionExecutionResult {
        val resolvedHour = if (hour in 0..23) hour else 7
        val resolvedMinute = if (minute in 0..59) minute else 0
        val alarmLabel = label.ifBlank { "বসের অ্যালার্ম" }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, resolvedHour)
                putExtra(AlarmClock.EXTRA_MINUTES, resolvedMinute)
                putExtra(AlarmClock.EXTRA_MESSAGE, alarmLabel)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedMin = resolvedMinute.toString().padStart(2, '0')
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "অ্যালার্ম $resolvedHour:$formattedMin",
                followUpReplyBangla = "জি বস, $resolvedHour টা $formattedMin মিনিটে অ্যালার্ম লাগায়া দিছি!"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "অ্যালার্ম ব্যর্থ",
                followUpReplyBangla = "মাফ করবেন বস, অ্যালার্ম সেট করা যায় নাই।"
            )
        }
    }

    private fun handleOpenCamera(): ActionExecutionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "ক্যামেরা",
                followUpReplyBangla = "জি বস, ক্যামেরা খুইলা দিছি!"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ক্যামেরা ব্যর্থ",
                followUpReplyBangla = "মাফ করবেন বস, ক্যামেরা চালু হয় নাই।"
            )
        }
    }

    private fun handleWifi(): ActionExecutionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
            } else {
                @Suppress("DEPRECATION")
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiManager?.let { it.isWifiEnabled = !it.isWifiEnabled }
            }
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "ওয়াই-ফাই",
                followUpReplyBangla = "জি বস, ওয়াই-ফাই প্যানেল খুইলা দিছি।"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ওয়াই-ফাই ব্যর্থ",
                followUpReplyBangla = "বস, ওয়াই-ফাই খোলা যায় নাই।"
            )
        }
    }

    private fun handleBluetooth(): ActionExecutionResult {
        val btManager = context.getSystemService(BluetoothManager::class.java)
        val isEnabled = btManager?.adapter?.isEnabled == true
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val stateText = if (isEnabled) "চালু আছে" else "বন্ধ আছে"
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "ব্লুটুথ",
                followUpReplyBangla = "জি বস, ব্লুটুথ অহন $stateText। সেটিংস খুইলা দিছি।"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ব্লুটুথ ব্যর্থ",
                followUpReplyBangla = "মাফ করবেন বস, ব্লুটুথ খোলা যায় নাই।"
            )
        }
    }

    private fun handlePlayMusic(): ActionExecutionResult {
        return try {
            val intent = Intent.makeMainSelectorActivity(
                Intent.ACTION_MAIN,
                Intent.CATEGORY_APP_MUSIC
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "গান চালু",
                followUpReplyBangla = "জি বস, গানের প্লেয়ার চালাইয়া দিছি!"
            )
        } catch (e: Exception) {
            launchPackageOrFallback("com.spotify.music", "গান", "https://open.spotify.com")
        }
    }

    private fun handleOpenApp(appQuery: String): ActionExecutionResult {
        if (BankGuardrail.isBlockedRequest(appQuery)) {
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ব্যাংক সুরক্ষা",
                followUpReplyBangla = BankGuardrail.REFUSAL_MESSAGE_BANGLA
            )
        }
        val lower = appQuery.lowercase()
        return when {
            lower.contains("imo") || lower.contains("ইমো") ->
                launchPackageOrFallback("com.imo.android.imoim", "ইমো", "https://imo.im")
            lower.contains("messenger") || lower.contains("মেসেঞ্জার") ->
                launchPackageOrFallback("com.facebook.orca", "মেসেঞ্জার", "https://www.messenger.com")
            lower.contains("brilliant") || lower.contains("ব্রিলিয়ান্ট") ->
                launchPackageOrFallback("com.brilliant.connect.app", "ব্রিলিয়ান্ট", null)
            lower.contains("facebook") || lower.contains("ফেসবুক") || lower.contains("fb") ->
                launchPackageOrFallback("com.facebook.katana", "ফেসবুক", "https://m.facebook.com")
            lower.contains("gmail") || lower.contains("জিমেইল") || lower.contains("mail") ->
                launchPackageOrFallback("com.google.android.gm", "জিমেইল", "https://mail.google.com")
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") ->
                launchPackageOrFallback("com.whatsapp", "হোয়াটসঅ্যাপ", "https://web.whatsapp.com")
            lower.contains("youtube") || lower.contains("ইউটিউব") ->
                launchPackageOrFallback("com.google.android.youtube", "ইউটিউব", "https://m.youtube.com")
            lower.contains("chrome") || lower.contains("ক্রোম") || lower.contains("browser") ->
                handleOpenChromeAiStudio()
            lower.contains("spotify") ->
                launchPackageOrFallback("com.spotify.music", "মিউজিক", "https://open.spotify.com")
            else -> ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "অ্যাপ নাই",
                followUpReplyBangla = "বস, এই অ্যাপটা ফোনে খুঁইজা পাইলাম না।"
            )
        }
    }

    private fun launchPackageOrFallback(
        packageName: String,
        banglaAppName: String,
        webFallbackUrl: String?
    ): ActionExecutionResult {
        if (BankGuardrail.isBlockedPackage(packageName)) {
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "ব্যাংক সুরক্ষা",
                followUpReplyBangla = BankGuardrail.REFUSAL_MESSAGE_BANGLA
            )
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return ActionExecutionResult(
                executed = true,
                statusBadgeBangla = "$banglaAppName চালু",
                followUpReplyBangla = "জি বস, আপনার কথামতো $banglaAppName খুইলা দিছি!"
            )
        }

        if (!webFallbackUrl.isNullOrBlank()) {
            return try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webFallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionExecutionResult(
                    executed = true,
                    statusBadgeBangla = "$banglaAppName (ওয়েব)",
                    followUpReplyBangla = "জি বস, ফোনে অ্যাপ নাই দেইখা ব্রাউজারে $banglaAppName খুইলা দিছি!"
                )
            } catch (e: Exception) {
                ActionExecutionResult(
                    executed = false,
                    statusBadgeBangla = "$banglaAppName নাই",
                    followUpReplyBangla = "বস, ফোনে $banglaAppName ইনস্টল করা নাই।"
                )
            }
        }

        return ActionExecutionResult(
            executed = false,
            statusBadgeBangla = "$banglaAppName নাই",
            followUpReplyBangla = "বস, ফোনে $banglaAppName ইনস্টল করা নাই।"
        )
    }

    private fun handleCheckLocation(): ActionExecutionResult {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) {
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "অনুমতি নাই",
                followUpReplyBangla = "বস, জায়গা জানার লাইগা লোকেশন অনুমতি লাগবে।"
            )
        }

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        @Suppress("MissingPermission")
        val location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (location == null) {
            return ActionExecutionResult(
                executed = false,
                statusBadgeBangla = "লোকেশন নাই",
                followUpReplyBangla = "বস, অহন লোকেশন পাওয়া যাইতাছে না।"
            )
        }

        val placeLabel = runCatching {
            val geocoder = Geocoder(context, Locale("bn", "BD"))
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val addr = addresses?.firstOrNull()
            listOfNotNull(addr?.subLocality, addr?.locality, addr?.adminArea)
                .distinct()
                .joinToString(", ")
        }.getOrDefault("")

        val coordSummary = if (placeLabel.isNotBlank()) {
            placeLabel
        } else {
            String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude)
        }

        return ActionExecutionResult(
            executed = true,
            statusBadgeBangla = "অবস্থান",
            followUpReplyBangla = "জি বস, আপনি অহন আছেন: $coordSummary।"
        )
    }
}
