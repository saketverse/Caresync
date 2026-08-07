package com.example.data

object LanguageManager {

    const val LANG_ENGLISH = "EN"
    const val LANG_HINDI = "HI"

    data class LanguageItem(val code: String, val displayName: String, val nativeName: String, val localeCode: String)

    val supportedLanguages = listOf(
        LanguageItem(LANG_ENGLISH, "English", "English", "en_IN"),
        LanguageItem(LANG_HINDI, "Hindi", "हिन्दी", "hi_IN")
    )

    fun getLanguageNativeName(code: String): String {
        return supportedLanguages.find { it.code == code }?.nativeName ?: "English"
    }

    // Key-based translation dictionary
    private val translations = mapOf(
        LANG_ENGLISH to mapOf(
            "app_title" to "CareSync Health",
            "take_medicine_title" to "Time for your medicine",
            "medicine_reminder_prompt" to "Time to take your medicine.",
            "take_now" to "Take Now",
            "taken" to "Taken",
            "missed" to "Missed",
            "pill_organizer" to "Pill Organizer",
            "morning" to "Morning 🌅",
            "afternoon" to "Afternoon ☀️",
            "evening" to "Evening 🌆",
            "night" to "Night 🌙",
            "scan_medicine" to "Scan Medicine",
            "read_aloud" to "Read Aloud 🔊",
            "warning_safe" to "No serious interaction detected.",
            "warning_caution" to "Consult your doctor before taking these medicines together.",
            "warning_danger" to "These medicines should not be taken together.",
            "elder_mode" to "Elder Mode",
            "voice_reminders" to "Voice Reminders",
            "listen_warning" to "Listen to Warning 🔊",
            "missed_alert_caregiver" to "missed their medicine reminder."
        ),
        LANG_HINDI to mapOf(
            "app_title" to "केयरसिंक स्वास्थ्य",
            "take_medicine_title" to "दवा लेने का समय हो गया है",
            "medicine_reminder_prompt" to "दवा लेने का समय हो गया है।",
            "take_now" to "अभी लें",
            "taken" to "ले ली",
            "missed" to "छूट गई",
            "pill_organizer" to "दवा आयोजक (पिल ऑर्गनाइज़र)",
            "morning" to "सुबह 🌅",
            "afternoon" to "दोपहर ☀️",
            "evening" to "शाम 🌆",
            "night" to "रात 🌙",
            "scan_medicine" to "दवा स्कैन करें",
            "read_aloud" to "बोलकर सुनें 🔊",
            "warning_safe" to "कोई गंभीर दुष्परिणाम नहीं मिला।",
            "warning_caution" to "इन दवाओं को साथ लेने से पहले डॉक्टर से सलाह लें।",
            "warning_danger" to "ये दवाएं एक साथ नहीं ली जानी चाहिए।",
            "elder_mode" to "वरिष्ठ नागरिक मोड (एल्डर मोड)",
            "voice_reminders" to "आवाज में याद दिलाएं (वॉइस रिमाइंडर)",
            "listen_warning" to "चेतावनी सुनें 🔊",
            "missed_alert_caregiver" to "ने अपनी दवा का रिमाइंडर मिस कर दिया है।"
        )
    )

    fun getText(key: String, langCode: String = LANG_ENGLISH): String {
        val langMap = translations[langCode] ?: translations[LANG_ENGLISH]!!
        return langMap[key] ?: translations[LANG_ENGLISH]!![key] ?: key
    }

    fun buildLocalizedVoiceReminder(
        userName: String,
        medName: String,
        dosage: String,
        langCode: String
    ): String {
        val name = userName.ifBlank { "Saket" }
        return when (langCode) {
            LANG_HINDI -> "$name, $medName $dosage लेने का समय हो गया है।"
            else -> "$name, it's time to take $medName $dosage."
        }
    }

    fun buildLocalizedCaregiverEscalationMsg(
        patientName: String,
        medName: String,
        time: String,
        langCode: String
    ): String {
        val name = patientName.ifBlank { "Saket" }
        return when (langCode) {
            LANG_HINDI -> "$name ने $time की दवा ($medName) नहीं ली।"
            else -> "Caregiver Alert: $name missed their $time medicine ($medName)."
        }
    }
}
