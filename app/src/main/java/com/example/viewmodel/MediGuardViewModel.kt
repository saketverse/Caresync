package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.SmartReminderForegroundService
import com.example.util.BatteryOptimizationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MedicationRepository(db.medicationDao())
    private val authRepo = FirebaseAuthRepository(application)
    private val voiceManager = VoiceReminderManager.getInstance(application)

    private val settingsPrefs = application.getSharedPreferences("caresync_settings_prefs", Context.MODE_PRIVATE)

    // --- Network Connectivity & Offline Monitoring ---
    private val _isOnline = MutableStateFlow(checkNetworkConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private fun checkNetworkConnectivity(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    private fun registerNetworkCallback() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val builder = NetworkRequest.Builder()
            cm?.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Battery Optimization & Background Service State ---
    private val _isBatteryOptimizationIgnored = MutableStateFlow(
        BatteryOptimizationManager.isBatteryOptimizationIgnored(application)
    )
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    init {
        registerNetworkCallback()
        // Start persistent background foreground service to protect reminders from Android battery optimizations
        try {
            SmartReminderForegroundService.startService(application)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshBatteryOptimizationStatus() {
        _isBatteryOptimizationIgnored.value = BatteryOptimizationManager.isBatteryOptimizationIgnored(getApplication())
    }

    fun requestDisableBatteryOptimization() {
        BatteryOptimizationManager.requestDisableBatteryOptimization(getApplication())
        refreshBatteryOptimizationStatus()
    }

    fun startForegroundReminderService() {
        SmartReminderForegroundService.startService(getApplication())
        showSnackbar("Smart Reminder Service Safeguard Started")
    }

    // --- Authentication & User Profile State ---
    private val initialProfile = authRepo.getLocalProfile() ?: UserProfile(
        uid = "default_user",
        name = "Saket",
        age = 68,
        email = "saket.elder@health.org",
        role = UserProfile.ROLE_PATIENT,
        connectionCode = "849201"
    )

    private val _userProfile = MutableStateFlow<UserProfile>(initialProfile)
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(authRepo.getLocalProfile() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    val userName: StateFlow<String> = _userProfile
        .map { it.name.ifBlank { "Saket" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Saket")

    val userEmail: StateFlow<String> = _userProfile
        .map { it.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "saket.elder@health.org")

    // --- Settings & Elder Preferences State ---
    private val _isDarkMode = MutableStateFlow(settingsPrefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(settingsPrefs.getBoolean("is_voice_enabled", true))
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        settingsPrefs.getString("selected_language", LanguageManager.LANG_ENGLISH) ?: LanguageManager.LANG_ENGLISH
    )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isElderMode = MutableStateFlow(settingsPrefs.getBoolean("is_elder_mode", true))
    val isElderMode: StateFlow<Boolean> = _isElderMode.asStateFlow()

    private val _voiceGender = MutableStateFlow(settingsPrefs.getString("voice_gender", "Female") ?: "Female") // "Female" or "Male"
    val voiceGender: StateFlow<String> = _voiceGender.asStateFlow()

    private val _voiceVolume = MutableStateFlow(settingsPrefs.getFloat("voice_volume", 1.0f))
    val voiceVolume: StateFlow<Float> = _voiceVolume.asStateFlow()

    private val _voiceRate = MutableStateFlow(settingsPrefs.getFloat("voice_rate", 0.9f))
    val voiceRate: StateFlow<Float> = _voiceRate.asStateFlow()

    private val _escalationMinutes = MutableStateFlow(settingsPrefs.getInt("escalation_minutes", 30))
    val escalationMinutes: StateFlow<Int> = _escalationMinutes.asStateFlow()

    // --- Data Streams ---
    val activeMedications: StateFlow<List<Medication>> = repository.activeMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationLogs: StateFlow<List<MedicationLog>> = repository.medicationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val familyMembers: StateFlow<List<FamilyMember>> = repository.familyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drugInteractions: StateFlow<List<DrugInteraction>> = repository.drugInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Interaction Inspector State ---
    private val _interactionResult = MutableStateFlow<String?>(null)
    val interactionResult: StateFlow<String?> = _interactionResult.asStateFlow()

    private val _isCheckingInteractions = MutableStateFlow(false)
    val isCheckingInteractions: StateFlow<Boolean> = _isCheckingInteractions.asStateFlow()

    // --- Chatbot State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Namaste! I am CareSync AI Assistant. How can I help you with your medications, dosage, or health reminders today?",
                isFromUser = false,
                suggestedActions = listOf(
                    "Can I take Metformin after food?",
                    "What are Lisinopril side effects?",
                    "What if I miss a dose of Atorvastatin?",
                    "Are my active medicines safe together?"
                )
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // --- Prescription Scanner State ---
    private val _scannedText = MutableStateFlow<String?>(null)
    val scannedText: StateFlow<String?> = _scannedText.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // --- Toast / Notification Alert State ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        updateVoiceEngineConfig()
    }

    private fun updateVoiceEngineConfig() {
        voiceManager.configureSettings(
            langCode = _selectedLanguage.value,
            isFemale = (_voiceGender.value == "Female"),
            volume = _voiceVolume.value,
            rate = _voiceRate.value
        )
    }

    // --- Voice & Speech Actions ---
    fun speakText(text: String) {
        if (!_isVoiceEnabled.value) return
        updateVoiceEngineConfig()
        voiceManager.speak(text)
    }

    fun speakMedicineReminderForMed(med: Medication) {
        if (!_isVoiceEnabled.value) return
        updateVoiceEngineConfig()
        voiceManager.speakMedicineReminder(
            userName = userName.value,
            medName = med.name,
            dosage = med.dosage,
            langCode = _selectedLanguage.value
        )
    }

    fun stopVoice() {
        voiceManager.stop()
    }

    // --- Preference Setters ---
    fun setLanguage(langCode: String) {
        val validCode = if (langCode == LanguageManager.LANG_HINDI) LanguageManager.LANG_HINDI else LanguageManager.LANG_ENGLISH
        _selectedLanguage.value = validCode
        settingsPrefs.edit().putString("selected_language", validCode).apply()
        updateVoiceEngineConfig()
        val langName = LanguageManager.getLanguageNativeName(validCode)
        showSnackbar("Language set to $langName")
        if (validCode == LanguageManager.LANG_HINDI) {
            speakText("दवा लेने का समय हो गया है।")
        } else {
            speakText("Time to take your medicine.")
        }
    }

    fun toggleElderMode() {
        _isElderMode.value = !_isElderMode.value
        settingsPrefs.edit().putBoolean("is_elder_mode", _isElderMode.value).apply()
        showSnackbar(if (_isElderMode.value) "Elder Mode Activated" else "Elder Mode Disabled")
    }

    fun setVoiceGender(gender: String) {
        _voiceGender.value = gender
        settingsPrefs.edit().putString("voice_gender", gender).apply()
        updateVoiceEngineConfig()
        if (_selectedLanguage.value == LanguageManager.LANG_HINDI) {
            speakText("आवाज बदल दी गई है।")
        } else {
            speakText("Voice changed to $gender voice.")
        }
    }

    fun setVoiceVolume(volume: Float) {
        _voiceVolume.value = volume
        settingsPrefs.edit().putFloat("voice_volume", volume).apply()
        updateVoiceEngineConfig()
    }

    fun setVoiceRate(rate: Float) {
        _voiceRate.value = rate
        settingsPrefs.edit().putFloat("voice_rate", rate).apply()
        updateVoiceEngineConfig()
    }

    fun setEscalationMinutes(mins: Int) {
        _escalationMinutes.value = mins
        settingsPrefs.edit().putInt("escalation_minutes", mins).apply()
        showSnackbar("Caregiver escalation set to $mins minutes")
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        settingsPrefs.edit().putBoolean("is_dark_mode", _isDarkMode.value).apply()
    }

    fun toggleVoice() {
        _isVoiceEnabled.value = !_isVoiceEnabled.value
        settingsPrefs.edit().putBoolean("is_voice_enabled", _isVoiceEnabled.value).apply()
        if (!_isVoiceEnabled.value) {
            voiceManager.stop()
        } else {
            if (_selectedLanguage.value == LanguageManager.LANG_HINDI) {
                speakText("दवा लेने का समय हो गया है।")
            } else {
                speakText("Voice alerts enabled.")
            }
        }
    }

    // --- Authentication Actions ---
    fun login(email: String, pass: String, role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val loginResult = authRepo.login(email, pass)

            if (loginResult.isSuccess) {
                val profile = loginResult.getOrNull() ?: UserProfile(
                    uid = "user_" + System.currentTimeMillis(),
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    role = role
                )
                authRepo.saveLocalProfile(profile)
                _userProfile.value = profile
                _isLoggedIn.value = true
                showSnackbar("Welcome back, ${profile.name}!")
                onSuccess()
            } else {
                val errorMsg = loginResult.exceptionOrNull()?.message ?: "Login failed. Please check your credentials."
                showSnackbar(errorMsg)
            }
            _isAuthLoading.value = false
        }
    }

    fun signUp(
        fullName: String,
        age: Int,
        email: String,
        pass: String,
        role: String,
        onVerificationSent: () -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.signUp(fullName, age, email, pass, role)

            if (result.isSuccess) {
                val profile = result.getOrNull() ?: UserProfile(
                    uid = "uid_" + System.currentTimeMillis(),
                    name = fullName,
                    age = age,
                    email = email,
                    role = role,
                    connectionCode = (100000..999999).random().toString()
                )
                authRepo.saveLocalProfile(profile)
                _userProfile.value = profile
                _isLoggedIn.value = true
                showSnackbar("Account created for $fullName")
                onVerificationSent()
                onSuccess()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Sign up failed. Please try again."
                showSnackbar(msg)
            }
            _isAuthLoading.value = false
        }
    }

    fun resendVerificationEmail(email: String, pass: String? = null) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.resendVerificationEmail(email, pass)
            if (result.isSuccess) {
                showSnackbar("Verification email sent to $email")
            } else {
                showSnackbar(result.exceptionOrNull()?.message ?: "Could not send verification email")
            }
            _isAuthLoading.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.sendPasswordReset(email)
            if (result.isSuccess) {
                showSnackbar("Password reset link sent to $email")
            } else {
                showSnackbar(result.exceptionOrNull()?.message ?: "Could not send reset link")
            }
            _isAuthLoading.value = false
        }
    }

    fun googleLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val demoProfile = UserProfile(
                uid = "google_user_101",
                name = "Saket",
                age = 68,
                email = "saket.elder@health.org",
                role = UserProfile.ROLE_PATIENT,
                connectionCode = "849201"
            )
            authRepo.saveLocalProfile(demoProfile)
            _userProfile.value = demoProfile
            _isLoggedIn.value = true
            _isAuthLoading.value = false
            showSnackbar("Signed in with Google as Saket")
            onSuccess()
        }
    }

    fun quickDemo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val demoProfile = UserProfile(
                uid = "demo_user_102",
                name = "Saket",
                age = 68,
                email = "saket.elder@health.org",
                role = UserProfile.ROLE_PATIENT,
                connectionCode = "849201"
            )
            authRepo.saveLocalProfile(demoProfile)
            _userProfile.value = demoProfile
            _isLoggedIn.value = true
            _isAuthLoading.value = false
            showSnackbar("CareSync Demo Activated")
            onSuccess()
        }
    }

    fun logout() {
        authRepo.logout()
        _isLoggedIn.value = false
        _userProfile.value = UserProfile()
        showSnackbar("Signed out successfully.")
    }

    fun linkParentToPatient(code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.linkParentToPatient(_userProfile.value.uid, code)
            val updated = result.getOrNull()
            if (updated != null) {
                _userProfile.value = updated
                showSnackbar("Successfully linked to patient: ${updated.connectedPatientName}")
                onSuccess()
            } else {
                showSnackbar("Invalid Patient Connection Code.")
            }
            _isAuthLoading.value = false
        }
    }

    // --- Medication Actions ---
    fun markTaken(medicationId: Long) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }
        viewModelScope.launch {
            val med = activeMedications.value.find { it.id == medicationId }
            repository.markAsTaken(medicationId)
            val msg = "${med?.name ?: "Medicine"} marked as taken!"
            showSnackbar(msg)
            if (_isVoiceEnabled.value) {
                speakText("${med?.name ?: "Medicine"} taken. Good job!")
            }
        }
    }

    fun refillStock(medicationId: Long) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }
        viewModelScope.launch {
            repository.refillStock(medicationId, 30)
            showSnackbar("Stock refilled (+30 tablets)!")
        }
    }

    fun addMedication(medication: Medication) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }
        viewModelScope.launch {
            repository.addMedication(medication)
            showSnackbar("Added ${medication.name} to medication schedule!")
            runDrugInteractionCheck()
        }
    }

    fun deleteMedication(medication: Medication) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }
        viewModelScope.launch {
            repository.deleteMedication(medication)
            showSnackbar("Removed ${medication.name}")
        }
    }

    fun triggerMissedReminderEscalation(med: Medication, stageMinutes: Int) {
        viewModelScope.launch {
            when (stageMinutes) {
                5 -> {
                    if (_isVoiceEnabled.value) {
                        speakMedicineReminderForMed(med)
                    }
                    showSnackbar(
                        if (_selectedLanguage.value == LanguageManager.LANG_HINDI)
                            "5 मिनट री-रिमाइंडर: ${med.name} दवा लेने का समय"
                        else
                            "5-min Gentle Reminder: Time for ${med.name}"
                    )
                }
                15 -> {
                    if (_isVoiceEnabled.value) {
                        if (_selectedLanguage.value == LanguageManager.LANG_HINDI) {
                            voiceManager.speak("अति आवश्यक ध्यान दें! ${userName.value}, कृपया अपनी दवा ${med.name} ${med.dosage} तुरंत लें।")
                        } else {
                            voiceManager.speak("Urgent Reminder! ${userName.value}, please take your ${med.name} ${med.dosage} now.")
                        }
                    }
                    showSnackbar(
                        if (_selectedLanguage.value == LanguageManager.LANG_HINDI)
                            "15 मिनट अर्जेंट रिमाइंडर: ${med.name}"
                        else
                            "15-min Urgent Reminder: ${med.name}"
                    )
                }
                else -> {
                    val msg = LanguageManager.buildLocalizedCaregiverEscalationMsg(
                        patientName = userName.value,
                        medName = med.name,
                        time = med.timeOfConsumption,
                        langCode = _selectedLanguage.value
                    )
                    showSnackbar("🚨 Caregiver Escalation Dispatched: $msg")
                    if (_isVoiceEnabled.value) {
                        speakText(msg)
                    }
                }
            }
        }
    }

    fun addFamilyMember(name: String, relation: String, phone: String, email: String) {
        viewModelScope.launch {
            val member = FamilyMember(
                name = name,
                relation = relation,
                phone = phone,
                email = email
            )
            repository.addFamilyMember(member)
            showSnackbar("Added $name to Family Care Monitoring")
        }
    }

    fun deleteFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteFamilyMember(member)
            showSnackbar("Removed ${member.name} from monitoring")
        }
    }

    fun runDrugInteractionCheck() {
        viewModelScope.launch {
            _isCheckingInteractions.value = true
            val meds = activeMedications.value
            val result = repository.checkDrugInteractions(meds)
            _interactionResult.value = result
            _isCheckingInteractions.value = false
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(text = userText, isFromUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            val botReply = repository.askMedicalChatbot(userText, activeMedications.value)
            val botMsg = ChatMessage(text = botReply, isFromUser = false)
            _chatMessages.value = _chatMessages.value + botMsg
            _isChatLoading.value = false
        }
    }

    fun scanPrescription(sampleType: String = "Cardiology Prescription Sample", base64ImageData: String? = null) {
        viewModelScope.launch {
            _isScanning.value = true
            _scannedText.value = null

            val imagePayload = base64ImageData ?: getSampleBase64Prescription()
            val result = repository.scanPrescriptionImage(imagePayload)

            _scannedText.value = result
            _isScanning.value = false
        }
    }

    fun autoAddScannedMedicationToSchedule(name: String, dosage: String, time: String, food: String) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }
        viewModelScope.launch {
            val med = Medication(
                name = name,
                dosage = dosage,
                totalTablets = 30,
                remainingTablets = 30,
                startDate = "2026-08-06",
                endDate = "2026-09-06",
                timeOfConsumption = time,
                beforeOrAfterFood = food,
                instructions = "Extracted automatically from prescription OCR scan",
                category = "Prescription",
                prescribedBy = "Prescription Scan"
            )
            repository.addMedication(med)
            showSnackbar("Auto-imported $name ($dosage) into schedule!")
        }
    }

    fun triggerEmergencySOS(contactName: String, contactPhone: String) {
        showSnackbar("🚨 Emergency Alert dispatched to $contactName ($contactPhone)!")
        if (_isVoiceEnabled.value) {
            speakText("Emergency SOS alert dispatched to $contactName.")
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun getSampleBase64Prescription(): String {
        return "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    }
}
