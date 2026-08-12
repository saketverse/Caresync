package com.example.data

object LanguageManager {

    const val LANG_ENGLISH = "EN"
    const val LANG_HINDI = "HI"
    const val LANG_BENGALI = "BN"
    const val LANG_TAMIL = "TA"
    const val LANG_TELUGU = "TE"
    const val LANG_MARATHI = "MR"
    const val LANG_PUNJABI = "PA"

    data class LanguageItem(
        val code: String,
        val displayName: String,
        val nativeName: String,
        val localeCode: String
    )

    val supportedLanguages = listOf(
        LanguageItem(LANG_ENGLISH, "English", "English", "en_IN"),
        LanguageItem(LANG_HINDI, "Hindi", "हिन्दी", "hi_IN"),
        LanguageItem(LANG_BENGALI, "Bengali", "বাংলা", "bn_IN"),
        LanguageItem(LANG_TAMIL, "Tamil", "தமிழ்", "ta_IN"),
        LanguageItem(LANG_TELUGU, "Telugu", "తెలుగు", "te_IN"),
        LanguageItem(LANG_MARATHI, "Marathi", "मराठी", "mr_IN"),
        LanguageItem(LANG_PUNJABI, "Punjabi", "ਪੰਜਾਬੀ", "pa_IN")
    )

    fun getLanguageNativeName(code: String): String {
        return supportedLanguages.find { it.code == code }?.nativeName ?: "English"
    }

    fun getLanguageDisplayName(code: String): String {
        return supportedLanguages.find { it.code == code }?.displayName ?: "English"
    }

    fun getLanguageLocaleCode(code: String): String {
        return supportedLanguages.find { it.code == code }?.localeCode ?: "en_IN"
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
            "scan_report_title" to "Scan Medical Report 📄",
            "scan_package_title" to "Scan Medicine Box 📦",
            "step_1_scan" to "STEP 1: Scan",
            "step_2_review" to "STEP 2: Review",
            "step_3_confirm" to "STEP 3: Confirm",
            "scan_instructions" to "Make sure the entire prescription is visible and the text is clear.",
            "confirm_add_medicines" to "Confirm & Add Medicines",
            "retake_photo" to "Retake Photo 📷",
            "enter_manually" to "Enter Medicine Manually ✍️",
            "uncertain_warning" to "We couldn't confidently read part of this prescription.",
            "duplicate_warning" to "Possible duplicate medicine",
            "read_aloud" to "Read Aloud 🔊",
            "status_title_safe" to "No interaction detected",
            "status_title_caution" to "Interaction caution",
            "status_title_danger" to "Interaction warning",
            "warning_safe" to "No known interaction found",
            "warning_caution" to "These medicines may interact. Consult a healthcare professional.",
            "warning_danger" to "These medicines may have a serious interaction. Do not make medication decisions based solely on this app; consult a healthcare professional.",
            "elder_mode" to "Elder Mode",
            "voice_reminders" to "Voice Reminders",
            "listen_warning" to "Listen 🔊",
            "test_voice" to "Test Voice 🔊",
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
            "scan_report_title" to "मेडिकल रिपोर्ट स्कैन करें 📄",
            "scan_package_title" to "दवा का डिब्बा स्कैन करें 📦",
            "step_1_scan" to "चरण 1: स्कैन",
            "step_2_review" to "चरण 2: समीक्षा",
            "step_3_confirm" to "चरण 3: पुष्टि करें",
            "scan_instructions" to "सुनिश्चित करें कि पूरी पर्ची साफ दिखाई दे रही है।",
            "confirm_add_medicines" to "दवाओं की पुष्टि करें और जोड़ें",
            "retake_photo" to "फिर से फोटो लें 📷",
            "enter_manually" to "दवा खुद दर्ज करें ✍️",
            "uncertain_warning" to "हम इस पर्ची के कुछ हिस्से को स्पष्ट रूप से नहीं पढ़ सके।",
            "duplicate_warning" to "संभावित दोहराई गई दवा",
            "read_aloud" to "बोलकर सुनें 🔊",
            "status_title_safe" to "कोई प्रतिकूल प्रभाव नहीं",
            "status_title_caution" to "सावधानी बरतें",
            "status_title_danger" to "गंभीर चेतावनी",
            "warning_safe" to "दवाओं में कोई प्रतिकूल प्रभाव नहीं मिला।",
            "warning_caution" to "ये दवाएं परस्पर क्रिया कर सकती हैं। डॉक्टर से सलाह लें।",
            "warning_danger" to "इन दवाओं का गंभीर दुष्प्रभाव हो सकता है। डॉक्टर से संपर्क करें।",
            "elder_mode" to "वरिष्ठ नागरिक मोड",
            "voice_reminders" to "आवाज में रिमाइंडर (वॉइस रिमाइंडर)",
            "listen_warning" to "सुनें 🔊",
            "test_voice" to "आवाज़ जांचें 🔊",
            "missed_alert_caregiver" to "ने अपनी दवा का रिमाइंडर मिस कर दिया है।"
        ),
        LANG_BENGALI to mapOf(
            "app_title" to "কেয়ারসিঙ্ক হেলথ",
            "take_medicine_title" to "ওষুধ নেওয়ার সময় হয়েছে",
            "medicine_reminder_prompt" to "ওষুধ নেওয়ার সময় হয়েছে।",
            "take_now" to "এখনই নিন",
            "taken" to "নেওয়া হয়েছে",
            "missed" to "বাদ পড়েছে",
            "pill_organizer" to "ওষুধ আয়োজক",
            "morning" to "সকাল 🌅",
            "afternoon" to "দুপুর ☀️",
            "evening" to "সন্ধ্যা 🌆",
            "night" to "রাত 🌙",
            "scan_medicine" to "ওষুধ স্ক্যান করুন",
            "read_aloud" to "শুনে নিন 🔊",
            "status_title_safe" to "কোনো ঝুঁকি পাওয়া যায়নি",
            "status_title_caution" to "সতর্কতা প্রয়োজন",
            "status_title_danger" to "গুরুতর সতর্কতা",
            "warning_safe" to "কোনো জানা ক্ষতিকর প্রতিক্রিয়া পাওয়া যায়নি।",
            "warning_caution" to "এই ওষুধগুলি পরস্পর প্রতিক্রিয়া করতে পারে। ডাক্তারের পরামর্শ নিন।",
            "warning_danger" to "এই ওষুধগুলির মারাত্মক প্রতিক্রিয়া হতে পারে। অবিলম্বে ডাক্তারের সাথে কথা বলুন।",
            "elder_mode" to "বয়স্কদের জন্য মোড",
            "voice_reminders" to "ভয়েস রিমাইন্ডার",
            "listen_warning" to "শুনুন 🔊",
            "test_voice" to "ভয়েস পরীক্ষা 🔊",
            "missed_alert_caregiver" to "ওষুধ খেতে ভুলে গেছেন।"
        )
    )

    fun getText(key: String, langCode: String = LANG_ENGLISH): String {
        val langMap = translations[langCode] ?: translations[LANG_ENGLISH]!!
        return langMap[key] ?: translations[LANG_ENGLISH]!![key] ?: key
    }

    fun getTestVoiceText(langCode: String): String {
        return when (langCode) {
            LANG_HINDI -> "यह CareSync की हिंदी आवाज़ की जाँच है।"
            LANG_BENGALI -> "এটি CareSync-এর বাংলা গলার স্বর পরীক্ষা।"
            LANG_TAMIL -> "இது CareSync தமிழ் குரல் சோதனை."
            LANG_TELUGU -> "ఇది CareSync తెలుగు వాయిస్ పరీక్ష."
            LANG_MARATHI -> "ही CareSync च्या मराठी आवाजाची चाचणी आहे।"
            LANG_PUNJABI -> "ਇਹ CareSync ਦੀ ਪੰਜਾਬੀ ਆਵਾਜ਼ ਦੀ ਜਾਂਚ ਹੈ।"
            else -> "This is a test of CareSync's English voice."
        }
    }

    fun buildLocalizedVoiceReminder(
        userName: String,
        medName: String,
        dosage: String,
        langCode: String
    ): String {
        val namePrefix = if (userName.isNotBlank()) "$userName, " else ""
        return when (langCode) {
            LANG_HINDI -> "${namePrefix}अब आपकी $medName $dosage की दवा लेने का समय हो गया है।"
            LANG_BENGALI -> "${namePrefix}এখন আপনার $medName $dosage ওষুধ নেওয়ার সময় হয়েছে।"
            LANG_TAMIL -> "${namePrefix}உங்கள் $medName $dosage மருந்தை எடுத்துக்கொள்ள வேண்டிய நேரம் இது."
            LANG_TELUGU -> "${namePrefix}మీ $medName $dosage మందులు తీసుకోవడానికి సమయం అయింది."
            LANG_MARATHI -> "${namePrefix}आता तुमची $medName $dosage औषध घेण्याची वेळ झाली आहे।"
            LANG_PUNJABI -> "${namePrefix}ਹੁਣ ਤੁਹਾਡੀ $medName $dosage ਦਵਾਈ ਲੈਣ ਦਾ ਸਮਾਂ ਹੋ ਗਿਆ ਹੈ।"
            else -> "${namePrefix}it's time to take your $medName $dosage."
        }
    }

    fun buildLocalizedCaregiverEscalationMsg(
        patientName: String,
        medName: String,
        time: String,
        langCode: String
    ): String {
        val name = patientName.ifBlank { "Patient" }
        return when (langCode) {
            LANG_HINDI -> "$name ने $time की दवा ($medName) नहीं ली।"
            LANG_BENGALI -> "$name $time-এর ওষুধ ($medName) নেননি।"
            LANG_TAMIL -> "$name $time மருந்தினை ($medName) எடுக்கவில்லை."
            LANG_TELUGU -> "$name $time మందు ($medName) తీసుకోలేదు."
            LANG_MARATHI -> "$name यांनी $time ची औषध ($medName) घेतली नाही."
            LANG_PUNJABI -> "$name ਨੇ $time ਦੀ ਦਵਾਈ ($medName) ਨਹੀਂ ਲਈ।"
            else -> "Caregiver Alert: $name missed their $time medicine ($medName)."
        }
    }
}
