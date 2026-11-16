package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.CallChannel
import com.example.data.CallScreenLogEntry
import com.example.data.CallScreenPhase
import com.example.data.CallScreenSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "CallScreenCoordinator"

object CallScreeningCoordinator {

    const val INITIAL_CALLER_GREETING =
        "আমি ছোটু, সুমন বসের অ্যাসিস্ট্যান্ট। আপনি কে বলতেছেন? বসকে কি দরকার?"

    const val RECORDING_NOTICE_CALLER = "কলটি রেকর্ড হচ্ছে।"

    private val _session = MutableStateFlow(CallScreenSession())
    val session: StateFlow<CallScreenSession> = _session.asStateFlow()

    private val _logs = MutableStateFlow<List<CallScreenLogEntry>>(emptyList())
    val logs: StateFlow<List<CallScreenLogEntry>> = _logs.asStateFlow()

    fun beginIncomingScreening(
        callerNumber: String,
        knownContactName: String,
        channel: CallChannel = CallChannel.PHONE,
        isVideoCall: Boolean = false,
        isLiveSystemCall: Boolean
    ) {
        _session.value = CallScreenSession(
            phase = CallScreenPhase.GREETING_CALLER,
            channel = channel,
            isVideoCall = isVideoCall,
            callerNumber = callerNumber,
            knownContactName = knownContactName,
            isLiveSystemCall = isLiveSystemCall
        )
    }

    fun updatePhase(phase: CallScreenPhase) {
        _session.value = _session.value.copy(phase = phase)
    }

    fun updateCallerTranscript(transcript: String, isEmergency: Boolean = false) {
        _session.value = _session.value.copy(
            callerTranscript = transcript,
            isEmergency = isEmergency
        )
    }

    fun setBossBriefing(briefing: String, isEmergency: Boolean = false) {
        _session.value = _session.value.copy(
            phase = CallScreenPhase.ASKING_BOSS,
            bossBriefing = briefing,
            isEmergency = isEmergency
        )
    }

    @SuppressLint("MissingPermission")
    fun connectCallToBoss(context: Context, isRecording: Boolean) {
        val current = _session.value
        if (current.isLiveSystemCall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasAnswerPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasAnswerPerm) {
                runCatching {
                    val telecom =
                        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    @Suppress("DEPRECATION")
                    telecom?.acceptRingingCall()
                }.onFailure {
                    Log.e(TAG, "acceptRingingCall failed", it)
                }
            }
        }
        _session.value = current.copy(
            phase = CallScreenPhase.CONNECTED_TO_BOSS,
            isRecordingActive = isRecording
        )
        appendLog(
            callerDisplay = current.knownContactName.ifBlank {
                current.callerNumber.ifBlank { "অজানা কলার" }
            },
            channelLabel = if (current.isVideoCall) {
                "${current.channel.labelBangla} ভিডিও"
            } else {
                current.channel.labelBangla
            },
            reason = current.callerTranscript.ifBlank { "বস কল ধরছেন" },
            decision = if (isRecording) "সংযুক্ত + রেকর্ড" else "সংযুক্ত"
        )
    }

    @SuppressLint("MissingPermission")
    fun rejectCallPolitely(context: Context, closingMessage: String) {
        val current = _session.value
        if (current.isLiveSystemCall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val hasAnswerPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasAnswerPerm) {
                runCatching {
                    val telecom =
                        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    @Suppress("DEPRECATION")
                    telecom?.endCall()
                }.onFailure {
                    Log.e(TAG, "endCall failed", it)
                }
            }
        }
        _session.value = current.copy(
            phase = CallScreenPhase.REJECTED_POLITELY,
            callerClosingLine = closingMessage,
            isRecordingActive = false
        )
        appendLog(
            callerDisplay = current.knownContactName.ifBlank {
                current.callerNumber.ifBlank { "অজানা কলার" }
            },
            channelLabel = if (current.isVideoCall) {
                "${current.channel.labelBangla} ভিডিও"
            } else {
                current.channel.labelBangla
            },
            reason = current.callerTranscript.ifBlank { "বস ব্যস্ত আছেন" },
            decision = "কেটে দেওয়া হইছে"
        )
    }

    fun markRecordingStopped() {
        _session.value = _session.value.copy(isRecordingActive = false)
    }

    fun resetSession() {
        _session.value = CallScreenSession()
    }

    private fun appendLog(
        callerDisplay: String,
        channelLabel: String,
        reason: String,
        decision: String
    ) {
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val entry = CallScreenLogEntry(
            callerDisplay = callerDisplay,
            channelLabel = channelLabel,
            callerReason = reason,
            decisionLabel = decision,
            timestampLabel = time
        )
        _logs.value = (listOf(entry) + _logs.value).take(30)
    }
}
