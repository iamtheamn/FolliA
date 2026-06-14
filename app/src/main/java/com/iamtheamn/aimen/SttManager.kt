package com.iamtheamn.aimen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SttManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening = false
        private set

    private var isIntentionallyStopped = false
    private var fullTranscript = ""
    private var currentPartial = ""

    private var onResultCallback: ((String) -> Unit)? = null
    private var onSilenceTimeoutCallback: (() -> Unit)? = null
    private var onStateChangedCallback: ((Boolean) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val silenceRunnable = Runnable {
        if (isListening && !isIntentionallyStopped) {
            stopListening()
            onSilenceTimeoutCallback?.invoke()
        }
    }

    private fun startSilenceTimer() {
        handler.removeCallbacks(silenceRunnable)
        handler.postDelayed(silenceRunnable, 5000L)
    }

    private fun cancelSilenceTimer() {
        handler.removeCallbacks(silenceRunnable)
    }

    fun startListening(
        initialText: String,
        onResult: (String) -> Unit,
        onSilence: () -> Unit,
        onError: (String) -> Unit,
        onStateChanged: (Boolean) -> Unit
    ) {
        this.fullTranscript = initialText.trim()
        this.currentPartial = ""
        this.onResultCallback = onResult
        this.onSilenceTimeoutCallback = onSilence
        this.onErrorCallback = onError
        this.onStateChangedCallback = onStateChanged
        this.isIntentionallyStopped = false

        startSilenceTimer()
        startRecognizer()
    }

    private fun restartRecognizerSafely() {
        if (isIntentionallyStopped) return
        handler.postDelayed({
            if (!isIntentionallyStopped) {
                startRecognizer()
            }
        }, 50)
    }

    private fun startRecognizer() {
        if (isIntentionallyStopped) return

        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) { }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isListening) {
                    isListening = true
                    onStateChangedCallback?.invoke(true)
                }
            }

            override fun onBeginningOfSpeech() {
                cancelSilenceTimer()
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (isIntentionallyStopped) return

                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == 11) {
                    restartRecognizerSafely()
                } else {
                    stopListening()
                    onErrorCallback?.invoke(getErrorText(error))
                }
            }

            override fun onResults(results: Bundle?) {
                if (isIntentionallyStopped) return
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    fullTranscript = if (fullTranscript.isEmpty()) text else "$fullTranscript $text"
                    currentPartial = ""
                    onResultCallback?.invoke(fullTranscript)
                }

                startSilenceTimer()

                restartRecognizerSafely()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (isIntentionallyStopped) return

                cancelSilenceTimer()

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentPartial = matches[0]
                    val displayedText = if (fullTranscript.isEmpty()) currentPartial
                    else if (currentPartial.isEmpty()) fullTranscript
                    else "$fullTranscript $currentPartial"
                    onResultCallback?.invoke(displayedText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        isIntentionallyStopped = true
        cancelSilenceTimer()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {}

        if (isListening) {
            isListening = false
            onStateChangedCallback?.invoke(false)
        }
    }

    fun destroy() {
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
            SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission micro refusée"
            SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Délai réseau dépassé"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service occupé"
            SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
            11 -> "Serveur déconnecté"
            12 -> "Langue non supportée"
            else -> "Erreur inconnue ($errorCode)"
        }
    }
}