package com.iamtheamn.aimen

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.util.concurrent.Executors

/**
 * Dictée vocale 100 % hors ligne, basée sur Vosk : l'audio ne quitte jamais l'appareil
 * et aucun service Google n'est nécessaire. Le modèle de la langue de l'appli doit
 * avoir été téléchargé depuis Réglages > Voix.
 */
class SttManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val modelLoader = Executors.newSingleThreadExecutor()

    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null

    var isListening = false
        private set

    private var isIntentionallyStopped = false
    private var fullTranscript = ""
    /** Incrémenté à chaque démarrage/arrêt : invalide un chargement de modèle devenu obsolète. */
    private var session = 0

    private var onResultCallback: ((String) -> Unit)? = null
    private var onSilenceTimeoutCallback: (() -> Unit)? = null
    private var onStateChangedCallback: ((Boolean) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private val silenceRunnable = Runnable {
        if (isListening && !isIntentionallyStopped) {
            stopListening()
            onSilenceTimeoutCallback?.invoke()
        }
    }

    private fun startSilenceTimer() {
        handler.removeCallbacks(silenceRunnable)
        handler.postDelayed(silenceRunnable, SILENCE_TIMEOUT_MS)
    }

    private fun cancelSilenceTimer() {
        handler.removeCallbacks(silenceRunnable)
    }

    /** Langue de dictée correspondant à la langue actuelle de l'appli. */
    fun currentLanguage(): SttLanguage {
        val appLanguage = appContext
            .getSharedPreferences(PreferencesManager.UI_PREFS_FILE, Context.MODE_PRIVATE)
            .getString(PreferencesManager.KEY_LANGUAGE, "system") ?: "system"
        return SttLanguage.forAppLanguage(appLanguage)
    }

    fun isModelReady(): Boolean = VoskModelManager.isInstalled(appContext, currentLanguage())

    fun startListening(
        initialText: String,
        onResult: (String) -> Unit,
        onSilence: () -> Unit,
        onError: (String) -> Unit,
        onStateChanged: (Boolean) -> Unit
    ) {
        releaseService()
        fullTranscript = initialText.trim()
        onResultCallback = onResult
        onSilenceTimeoutCallback = onSilence
        onErrorCallback = onError
        onStateChangedCallback = onStateChanged
        isIntentionallyStopped = false

        val language = currentLanguage()
        if (!VoskModelManager.isInstalled(appContext, language)) {
            onError(context.getString(R.string.stt_model_missing))
            return
        }

        val currentSession = ++session
        modelLoader.execute {
            val result = runCatching { VoskModelManager.loadModel(appContext, language) }
            handler.post {
                if (currentSession != session || isIntentionallyStopped) return@post
                result
                    .onSuccess { startRecognizer(it) }
                    .onFailure { fail(R.string.stt_model_load_error) }
            }
        }
    }

    private fun startRecognizer(model: Model) {
        try {
            val rec = Recognizer(model, SAMPLE_RATE)
            recognizer = rec
            val service = SpeechService(rec, SAMPLE_RATE)
            speechService = service
            if (!service.startListening(listener)) throw IllegalStateException("Micro déjà utilisé")

            isListening = true
            onStateChangedCallback?.invoke(true)
            startSilenceTimer()
        } catch (e: Exception) {
            e.printStackTrace()
            releaseService()
            fail(R.string.stt_error_audio)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            if (isIntentionallyStopped) return
            val partial = parse(hypothesis, "partial")
            if (partial.isEmpty()) return
            startSilenceTimer() // l'utilisateur parle : on repousse l'envoi automatique
            onResultCallback?.invoke(if (fullTranscript.isEmpty()) partial else "$fullTranscript $partial")
        }

        override fun onResult(hypothesis: String?) {
            if (isIntentionallyStopped) return
            val text = parse(hypothesis, "text")
            if (text.isEmpty()) return
            fullTranscript = if (fullTranscript.isEmpty()) text else "$fullTranscript $text"
            onResultCallback?.invoke(fullTranscript)
            startSilenceTimer()
        }

        override fun onFinalResult(hypothesis: String?) {
            // Appelé après un arrêt volontaire : le texte utile a déjà été transmis par onResult
        }

        override fun onError(exception: Exception?) {
            if (isIntentionallyStopped) return
            exception?.printStackTrace()
            fail(R.string.stt_error_audio)
        }

        override fun onTimeout() {}
    }

    private fun fail(messageRes: Int) {
        stopListening()
        onErrorCallback?.invoke(context.getString(messageRes))
    }

    fun stopListening() {
        isIntentionallyStopped = true
        session++
        cancelSilenceTimer()
        releaseService()

        if (isListening) {
            isListening = false
            onStateChangedCallback?.invoke(false)
        }
    }

    private fun releaseService() {
        try {
            speechService?.stop()
            speechService?.shutdown()
        } catch (e: Exception) {
        }
        try {
            recognizer?.close()
        } catch (e: Exception) {
        }
        speechService = null
        recognizer = null
    }

    fun destroy() {
        stopListening()
        modelLoader.shutdown()
    }

    private fun parse(hypothesis: String?, key: String): String =
        try {
            JSONObject(hypothesis ?: "").optString(key).trim()
        } catch (e: Exception) {
            ""
        }

    private companion object {
        const val SAMPLE_RATE = 16000.0f
        const val SILENCE_TIMEOUT_MS = 5000L
    }
}
