package com.example.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "BanglaSpeechEngine"

class BanglaSpeechEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWakeStandby = MutableStateFlow(false)
    val isWakeStandby: StateFlow<Boolean> = _isWakeStandby.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var onSpeechCompleteCallback: (() -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val bdLocale = Locale("bn", "BD")
                val res = tts?.setLanguage(bdLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val genericBn = tts?.setLanguage(Locale("bn"))
                    Log.w(TAG, "bn-BD TTS missing, fallback bn result=$genericBn")
                }
                tts?.setPitch(0.92f)
                tts?.setSpeechRate(1.0f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        mainHandler.post {
                            val cb = onSpeechCompleteCallback
                            onSpeechCompleteCallback = null
                            cb?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        mainHandler.post {
                            val cb = onSpeechCompleteCallback
                            onSpeechCompleteCallback = null
                            cb?.invoke()
                        }
                    }
                })
                ttsReady = true
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    fun speakBangla(
        text: String,
        speed: Float = 1.0f,
        onDone: (() -> Unit)? = null
    ) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }
        onSpeechCompleteCallback = onDone
        if (!ttsReady) {
            _isSpeaking.value = false
            onDone?.invoke()
            return
        }
        tts?.setSpeechRate(speed.coerceIn(0.7f, 1.4f))
        val utteranceId = UUID.randomUUID().toString()
        _isSpeaking.value = true
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeaking() {
        onSpeechCompleteCallback = null
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startSingleCommandListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        mainHandler.post {
            mainHandler.removeCallbacksAndMessages(null)
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError("বস, এই ডিভাইসে ভয়েস রেকগনিশন সার্ভিস পাওয়া যাচ্ছে না।")
                return@post
            }
            stopListeningInternal()
            _isWakeStandby.value = false
            _isListening.value = true
            _partialTranscript.value = ""

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _audioLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "বস, আপনার কথা স্পষ্টভাবে শুনতে পাইনি। আবার বলুন।"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "বস, মাইক্রোফোনের অনুমতি দেওয়া নেই।"
                        else ->
                            "বস, ভয়েস শুনতে সাময়িক সমস্যা হয়েছে (কোড $error)।"
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val matches =
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    if (best.isNotEmpty()) {
                        onResult(best)
                    } else {
                        onError("বস, কোনো কথা শোনা যায়নি।")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(createBanglaRecognizerIntent())
        }
    }

    /**
     * Continuous custom-name wake listening (No "Hey Google").
     * Listens for Boss's chosen wake name (e.g., "ওই ছোটু") and triggers command execution.
     */
    fun startWakeWordStandby(
        wakeNameProvider: () -> String,
        onWakeCommand: (fullCommand: String) -> Unit,
        onWakeOnly: () -> Unit,
        onStatusError: (String) -> Unit
    ) {
        mainHandler.post {
            mainHandler.removeCallbacksAndMessages(null)
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onStatusError("বস, ডিভাইসে স্পিচ রেকগনিশন নেই।")
                return@post
            }
            if (_isListening.value) return@post
            _isWakeStandby.value = true
            listenWakeCycle(wakeNameProvider, onWakeCommand, onWakeOnly)
        }
    }

    private fun listenWakeCycle(
        wakeNameProvider: () -> String,
        onWakeCommand: (String) -> Unit,
        onWakeOnly: () -> Unit
    ) {
        if (!_isWakeStandby.value || _isListening.value) return
        if (_isSpeaking.value) {
            mainHandler.postDelayed({
                listenWakeCycle(wakeNameProvider, onWakeCommand, onWakeOnly)
            }, 600L)
            return
        }

        stopRecognizerOnly()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                _audioLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _audioLevel.value = 0f
            }

            override fun onError(error: Int) {
                _audioLevel.value = 0f
                if (_isWakeStandby.value) {
                    mainHandler.postDelayed({
                        listenWakeCycle(wakeNameProvider, onWakeCommand, onWakeOnly)
                    }, 500L)
                }
            }

            override fun onResults(results: Bundle?) {
                _audioLevel.value = 0f
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val wakeName = wakeNameProvider()
                val matchedTranscript = matches.firstOrNull { containsWakeName(it, wakeName) }

                if (matchedTranscript != null) {
                    val stripped = stripWakeName(matchedTranscript, wakeName)
                    _isWakeStandby.value = false
                    if (stripped.length >= 2) {
                        onWakeCommand(stripped)
                    } else {
                        onWakeOnly()
                    }
                } else if (_isWakeStandby.value) {
                    mainHandler.postDelayed({
                        listenWakeCycle(wakeNameProvider, onWakeCommand, onWakeOnly)
                    }, 250L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    _partialTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(createBanglaRecognizerIntent())
    }

    fun stopAllListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        mainHandler.removeCallbacksAndMessages(null)
        _isWakeStandby.value = false
        _isListening.value = false
        _audioLevel.value = 0f
        stopRecognizerOnly()
    }

    private fun stopRecognizerOnly() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun release() {
        stopAllListening()
        stopSpeaking()
        tts?.shutdown()
        tts = null
    }

    private fun createBanglaRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    companion object {
        fun containsWakeName(transcript: String, customWakeName: String): Boolean {
            val lower = transcript.lowercase()
            val wakeLower = customWakeName.trim().lowercase()
            if (wakeLower.isEmpty()) return false
            if (lower.contains(wakeLower)) return true

            val aliases = listOf(
                "ওই ছোটু", "ঐ ছোটু", "ওই ছোট", "ঐ ছোট",
                "oi chhotu", "oi chotu", "oy chhotu", "oy chotu",
                "oi choto", "ছোটু", "chhotu", "chotu", "choto", "sotu"
            )
            return aliases.any { lower.contains(it) }
        }

        fun stripWakeName(transcript: String, customWakeName: String): String {
            var cleaned = transcript.trim()
            val candidates = listOf(
                customWakeName.trim(),
                "ওই ছোটু", "ঐ ছোটু", "ওই ছোট", "ঐ ছোট",
                "oi chhotu", "oi chotu", "oy chhotu", "oy chotu",
                "ছোটু", "ছোট", "chhotu", "chotu", "choto", "sotu", "ওই", "ঐ"
            ).distinct()
            for (word in candidates) {
                cleaned = cleaned.replace(word, "", ignoreCase = true)
            }
            return cleaned.trim(',', ' ', '।', '.', '!', '?')
        }
    }
}
