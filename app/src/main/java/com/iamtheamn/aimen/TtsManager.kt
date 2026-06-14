package com.iamtheamn.aimen

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this, "com.google.android.tts")
    private var isInitialized = false
    private var currentLocale: Locale = Locale.FRENCH
    var isMaleVoice: Boolean = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyVoiceAndLanguage(currentLocale)
        }
    }

    private fun detectLanguage(text: String): Locale {
        val engWords = listOf("the", "is", "you", "and", "to", "it", "that", "of", "in", "what", "how", "hello", "hi", "for", "on", "with")
        val frWords = listOf("le", "la", "les", "est", "vous", "et", "à", "de", "il", "que", "qui", "quoi", "je", "tu", "un", "une", "bonjour", "salut", "oui", "non")

        val words = text.lowercase().split(Regex("\\W+"))
        var engCount = 0
        var frCount = 0

        for (word in words) {
            if (engWords.contains(word)) engCount++
            if (frWords.contains(word)) frCount++
        }

        return if (engCount > frCount) Locale.US else Locale.FRENCH
    }

    private fun applyVoiceAndLanguage(locale: Locale) {
        currentLocale = locale
        tts?.language = locale
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(1.0f)

        try {
            val voices = tts?.voices
            var foundRealMaleVoice = false

            if (voices != null) {
                val localeVoices = voices.filter { it.locale.language == locale.language }

                var targetVoice = localeVoices.find { voice ->
                    val name = voice.name.lowercase()
                    val features = voice.features.map { it.lowercase() }

                    val hasMaleTag = features.contains("male")
                    val hasFemaleTag = features.contains("female")

                    val isMaleName = (name.contains("male") && !name.contains("female")) ||
                            name.contains("-m-") || name.contains("-b-") ||
                            name.contains("-d-") || hasMaleTag

                    val isFemaleName = name.contains("female") || name.contains("-f-") ||
                            name.contains("-a-") || name.contains("-c-") || hasFemaleTag

                    if (isMaleVoice) isMaleName else isFemaleName
                }

                if (targetVoice == null && isMaleVoice && localeVoices.size > 1) {
                    targetVoice = localeVoices.last()
                }

                if (targetVoice != null) {
                    tts?.voice = targetVoice
                    if (isMaleVoice) foundRealMaleVoice = true
                } else {
                    tts?.voice = localeVoices.firstOrNull()
                }
            }

            if (isMaleVoice && !foundRealMaleVoice) {
                tts?.setPitch(0.75f)
                tts?.setSpeechRate(0.9f)
            }
        } catch (e: Exception) {
            if (isMaleVoice) {
                tts?.setPitch(0.75f)
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    fun setVoiceGender(isMale: Boolean) {
        isMaleVoice = isMale
        if (isInitialized) {
            applyVoiceAndLanguage(currentLocale)
        }
    }

    fun speakChunk(chunk: String, fullTextContext: String) {
        if (isInitialized && chunk.isNotBlank()) {
            val cleanChunk = chunk.replace(Regex("[#*`_]"), "").trim()
            if (cleanChunk.isNotBlank()) {
                val detectedLocale = detectLanguage(fullTextContext)
                if (currentLocale != detectedLocale) {
                    applyVoiceAndLanguage(detectedLocale)
                }
                tts?.speak(cleanChunk, TextToSpeech.QUEUE_ADD, null, null)
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    fun isSpeaking(): Boolean {
        return try {
            tts?.isSpeaking ?: false
        } catch (e: Exception) {
            false
        }
    }
}