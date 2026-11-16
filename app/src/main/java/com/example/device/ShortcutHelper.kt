package com.example.device

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

object ShortcutHelper {

    const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
    const val EXTRA_PRESET_COMMAND = "preset_command"

    const val ACTION_START_LISTENING = "start_listening"
    const val ACTION_OPEN_CALL_AGENT = "open_call_agent"
    const val ACTION_VOICE_UNLOCK = "voice_unlock"
    const val ACTION_RUN_COMMAND = "run_command"

    fun isPinSupported(context: Context): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    fun pinVoiceShortcut(context: Context, wakeName: String): Boolean {
        val cleanName = wakeName.trim().ifBlank { "জার্ভিস" }
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_ACTION, ACTION_START_LISTENING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val info = ShortcutInfoCompat.Builder(context, "jarvis_pin_voice")
            .setShortLabel("$cleanName ভয়েস")
            .setLongLabel("$cleanName — সরাসরি কথা বলুন")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun pinCallAgentShortcut(context: Context): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_ACTION, ACTION_OPEN_CALL_AGENT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val info = ShortcutInfoCompat.Builder(context, "jarvis_pin_call_agent")
            .setShortLabel("কল এজেন্ট")
            .setLongLabel("জার্ভিস কল স্ক্রিনিং এজেন্ট")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun pinVoiceUnlockShortcut(context: Context): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_ACTION, ACTION_VOICE_UNLOCK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val info = ShortcutInfoCompat.Builder(context, "jarvis_pin_unlock")
            .setShortLabel("ভয়েস আনলক")
            .setLongLabel("জার্ভিস অফিশিয়াল ভয়েস আনলক")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun pinCustomCommandShortcut(
        context: Context,
        shortcutTitle: String,
        banglaCommand: String
    ): Boolean {
        val title = shortcutTitle.trim().ifBlank { "বসের হুকুম" }
        val command = banglaCommand.trim()
        if (command.isBlank()) return false

        val id = "jarvis_custom_${command.hashCode()}"
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_ACTION, ACTION_RUN_COMMAND)
            putExtra(EXTRA_PRESET_COMMAND, command)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val info = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(title.take(18))
            .setLongLabel(title)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun refreshDynamicShortcuts(context: Context, wakeName: String) {
        val cleanName = wakeName.trim().ifBlank { "জার্ভিস" }
        val voiceIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_SHORTCUT_ACTION, ACTION_START_LISTENING)
        }
        val dynamicVoice = ShortcutInfoCompat.Builder(context, "jarvis_dyn_voice")
            .setShortLabel("$cleanName ভয়েস")
            .setLongLabel("$cleanName — ভয়েসে হুকুম দিন")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(voiceIntent)
            .build()

        runCatching {
            ShortcutManagerCompat.pushDynamicShortcut(context, dynamicVoice)
        }
    }
}
