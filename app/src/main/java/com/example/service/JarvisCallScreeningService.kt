package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CallChannel
import com.example.data.SettingsRepository
import com.example.device.ContactsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JarvisCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        if (!isIncoming) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        scope.launch {
            val settings = SettingsRepository(applicationContext).settingsFlow.first()
            if (!settings.callScreeningEnabled) {
                respondToCall(callDetails, CallResponse.Builder().build())
                return@launch
            }

            val knownName = ContactsHelper.lookupContactNameByNumber(
                applicationContext,
                incomingNumber
            )

            CallScreeningCoordinator.beginIncomingScreening(
                callerNumber = incomingNumber.ifBlank { "অজানা নম্বর" },
                knownContactName = knownName,
                channel = CallChannel.PHONE,
                isVideoCall = false,
                isLiveSystemCall = true
            )

            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setSilenceCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(callDetails, response)

            postScreeningNotification(
                callerLabel = knownName.ifBlank { incomingNumber.ifBlank { "অজানা কলার" } }
            )
        }
    }

    private fun postScreeningNotification(callerLabel: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chhotu_darowan_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.call_agent_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "দারোয়ান কল স্ক্রিনিং নোটিফিকেশন"
            }
            nm.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_tab", "call_agent")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("বস, ফোন আসছে")
            .setContentText("বস, $callerLabel ফোন করছে, দিমু?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        nm.notify(4001, notification)
    }
}
