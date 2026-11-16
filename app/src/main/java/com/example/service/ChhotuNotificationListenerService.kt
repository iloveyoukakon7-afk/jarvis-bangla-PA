package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.CallChannel
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Intercepts incoming audio/video call notifications from WhatsApp, IMO, Messenger, and Brilliant
 * when Boss grants Notification Access, forwarding them to Chhotu's Darowan Call Screening System.
 */
class ChhotuNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        val channel = when (pkg) {
            "com.whatsapp" -> CallChannel.WHATSAPP
            "com.imo.android.imoim" -> CallChannel.IMO
            "com.facebook.orca" -> CallChannel.MESSENGER
            "com.brilliant.connect.app" -> CallChannel.BRILLIANT
            else -> return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val category = sbn.notification?.category.orEmpty()

        val looksLikeIncomingCall = category == Notification.CATEGORY_CALL ||
            text.contains("calling", ignoreCase = true) ||
            text.contains("incoming", ignoreCase = true) ||
            text.contains("video call", ignoreCase = true) ||
            text.contains("কল", ignoreCase = true)

        if (!looksLikeIncomingCall) return

        val isVideo = text.contains("video", ignoreCase = true) ||
            text.contains("ভিডিও", ignoreCase = true)

        scope.launch {
            val settings = SettingsRepository(applicationContext).settingsFlow.first()
            if (!settings.callScreeningEnabled) return@launch

            CallScreeningCoordinator.beginIncomingScreening(
                callerNumber = title.ifBlank { channel.labelBangla },
                knownContactName = title,
                channel = channel,
                isVideoCall = isVideo,
                isLiveSystemCall = false
            )
        }
    }
}
