package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.CallRecordingEntity
import com.example.data.ChhotuDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "CallRecorderManager"

class CallRecorderManager(private val context: Context) {

    private val dao = ChhotuDatabase.getInstance(context).memoryDao()
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var activeFile: File? = null
    private var activeCaller: String = ""
    private var activeChannel: String = ""
    private var startTimeMillis: Long = 0L

    fun startRecording(callerDisplay: String, channelLabel: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "RECORD_AUDIO permission missing for call recording")
            return false
        }

        stopRecordingWithoutSaving()

        val dir = File(context.filesDir, "chhotu_recordings").apply { mkdirs() }
        val file = File(dir, "chhotu_call_${System.currentTimeMillis()}.m4a")

        return try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            activeFile = file
            activeCaller = callerDisplay
            activeChannel = channelLabel
            startTimeMillis = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaRecorder", e)
            runCatching { file.delete() }
            recorder = null
            activeFile = null
            false
        }
    }

    suspend fun finishRecording(keepByDefault: Boolean = false): CallRecordingEntity? =
        withContext(Dispatchers.IO) {
            val currentFile = activeFile
            val durationSec = if (startTimeMillis > 0L) {
                ((System.currentTimeMillis() - startTimeMillis) / 1000L).toInt().coerceAtLeast(1)
            } else {
                1
            }

            runCatching {
                recorder?.stop()
                recorder?.release()
            }
            recorder = null
            activeFile = null
            startTimeMillis = 0L

            if (currentFile != null && currentFile.exists() && currentFile.length() > 0) {
                val entity = CallRecordingEntity(
                    callerDisplay = activeCaller.ifBlank { "অজানা কলার" },
                    channelLabel = activeChannel.ifBlank { "ফোন কল" },
                    filePath = currentFile.absolutePath,
                    durationSec = durationSec,
                    keepForever = keepByDefault
                )
                dao.insertRecording(entity)
                entity
            } else {
                runCatching { currentFile?.delete() }
                null
            }
        }

    fun stopRecordingWithoutSaving() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        runCatching { activeFile?.delete() }
        activeFile = null
        startTimeMillis = 0L
    }

    suspend fun autoDeleteTransientRecordings() = withContext(Dispatchers.IO) {
        val transients = dao.getTransientRecordings()
        transients.forEach { rec ->
            runCatching { File(rec.filePath).delete() }
            dao.deleteRecordingById(rec.id)
        }
    }

    suspend fun keepRecording(id: Long) = withContext(Dispatchers.IO) {
        dao.keepRecording(id)
    }

    suspend fun keepLatestRecording() = withContext(Dispatchers.IO) {
        dao.keepLatestRecording()
    }

    suspend fun deleteRecording(recording: CallRecordingEntity) = withContext(Dispatchers.IO) {
        runCatching { File(recording.filePath).delete() }
        dao.deleteRecordingById(recording.id)
    }

    fun playRecording(filePath: String, onFinished: () -> Unit, onError: (String) -> Unit) {
        stopPlayback()
        val file = File(filePath)
        if (!file.exists()) {
            onError("বস, এই রেকর্ডিং ফাইলটি আর স্টোরেজে নাই।")
            return
        }
        try {
            player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    onFinished()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Playback error", e)
            onError("বস, রেকর্ডিং চালাইতে সমস্যা হইছে।")
        }
    }

    fun stopPlayback() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }
}
