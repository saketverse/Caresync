package com.example.data

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceReminderManager private constructor(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private var currentLangCode: String = LanguageManager.LANG_ENGLISH
    private var isFemaleVoice: Boolean = true
    private var volumeLevel: Float = 1.0f
    private var speechRate: Float = 0.9f

    init {
        tts = TextToSpeech(appContext, this)
    }

    companion object {
        @Volatile
        private var instance: VoiceReminderManager? = null

        fun getInstance(context: Context): VoiceReminderManager {
            return instance ?: synchronized(this) {
                instance ?: VoiceReminderManager(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguageLocale(currentLangCode)
            applyVoiceSettings()
            Log.d("VoiceReminderManager", "TTS Initialized successfully.")
        } else {
            Log.e("VoiceReminderManager", "TTS Initialization failed with status $status")
        }
    }

    fun configureSettings(langCode: String, isFemale: Boolean, volume: Float, rate: Float) {
        currentLangCode = langCode
        isFemaleVoice = isFemale
        volumeLevel = volume.coerceIn(0.2f, 1.0f)
        speechRate = rate.coerceIn(0.5f, 1.5f)

        if (isInitialized) {
            setLanguageLocale(langCode)
            applyVoiceSettings()
        }
    }

    private fun setLanguageLocale(langCode: String) {
        val locale = when (langCode) {
            LanguageManager.LANG_HINDI -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if exact Indian regional voice data is not present on device
            tts?.setLanguage(Locale.ENGLISH)
        }
    }

    private fun applyVoiceSettings() {
        tts?.let { engine ->
            // Pitch adjustment: 1.2f for Female, 0.8f for Male
            val pitch = if (isFemaleVoice) 1.2f else 0.82f
            engine.setPitch(pitch)
            engine.setSpeechRate(speechRate)
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) return

        if (!isInitialized) {
            // Re-init if needed
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    setLanguageLocale(currentLangCode)
                    applyVoiceSettings()
                    executeSpeak(text, onDone)
                }
            }
        } else {
            setLanguageLocale(currentLangCode)
            applyVoiceSettings()
            executeSpeak(text, onDone)
        }
    }

    private fun executeSpeak(text: String, onDone: (() -> Unit)?) {
        val utteranceId = "CareSyncVoice_${System.currentTimeMillis()}"

        onDone?.let { callback ->
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    callback()
                }
                override fun onError(utteranceId: String?) {
                    callback()
                }
            })
        }

        // Adjust device alarm/media volume if needed
        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 10
            val targetVol = (maxVol * volumeLevel).toInt()
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
        } catch (e: Exception) {
            Log.w("VoiceReminderManager", "Could not adjust audio stream volume", e)
        }

        val params = android.os.Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun speakMedicineReminder(
        userName: String,
        medName: String,
        dosage: String,
        langCode: String = currentLangCode
    ) {
        val text = LanguageManager.buildLocalizedVoiceReminder(userName, medName, dosage, langCode)
        speak(text)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("VoiceReminderManager", "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            isInitialized = false
        } catch (e: Exception) {
            Log.e("VoiceReminderManager", "Error shutting down TTS", e)
        }
    }
}
