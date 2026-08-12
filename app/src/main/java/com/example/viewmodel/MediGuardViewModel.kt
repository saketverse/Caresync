package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.SmartReminderForegroundService
import com.example.util.AdherenceCalculator
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

    // --- Authentication & User Profile State ---
    private val savedProfile = authRepo.getLocalProfile()

    private val _userProfile = MutableStateFlow<UserProfile>(
        savedProfile ?: UserProfile(uid = "", name = "", email = "", role = UserProfile.ROLE_PATIENT)
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _healthProfile = MutableStateFlow<HealthProfile>(HealthProfile())
    val healthProfile: StateFlow<HealthProfile> = _healthProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(savedProfile != null && savedProfile.uid.isNotBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    init {
        registerNetworkCallback()
        loadHealthProfile()
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

    fun loadHealthProfile() {
        val uid = _userProfile?.value?.uid ?: ""
        if (uid.isNotBlank()) {
            viewModelScope.launch {
                val hp = authRepo.fetchHealthProfile(uid)
                _healthProfile.value = hp
            }
        } else {
            _healthProfile.value = HealthProfile()
        }
    }

    fun updateHealthProfile(updatedProfile: HealthProfile) {
        val uid = _userProfile?.value?.uid ?: ""
        viewModelScope.launch {
            _healthProfile.value = updatedProfile
            authRepo.saveHealthProfile(uid, updatedProfile)
            showSnackbar("Health profile updated successfully.")
        }
    }

    val userName: StateFlow<String> = _userProfile
        .map { if (it.name.isNotBlank()) it.name else if (it.email.isNotBlank()) it.email.substringBefore("@") else "User" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val userEmail: StateFlow<String> = _userProfile
        .map { it.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // --- Settings & Elder Preferences State ---
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

    val todayDoseItems: StateFlow<List<DoseItem>> = combine(activeMedications, medicationLogs) { meds, logs ->
        AdherenceCalculator.calculateTodayDoseItems(meds, logs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val familyMembers: StateFlow<List<FamilyMember>> = repository.familyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drugInteractions: StateFlow<List<DrugInteraction>> = repository.drugInteractions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Connections & SOS Alerts State ---
    private val _pendingConnectionRequests = MutableStateFlow<List<PatientGuardianConnection>>(emptyList())
    val pendingConnectionRequests: StateFlow<List<PatientGuardianConnection>> = _pendingConnectionRequests.asStateFlow()

    private val _acceptedGuardians = MutableStateFlow<List<PatientGuardianConnection>>(emptyList())
    val acceptedGuardians: StateFlow<List<PatientGuardianConnection>> = _acceptedGuardians.asStateFlow()

    private val _acceptedPatients = MutableStateFlow<List<PatientGuardianConnection>>(emptyList())
    val acceptedPatients: StateFlow<List<PatientGuardianConnection>> = _acceptedPatients.asStateFlow()

    private val _activeEmergencyAlerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val activeEmergencyAlerts: StateFlow<List<EmergencyAlert>> = _activeEmergencyAlerts.asStateFlow()

    private val _showNoGuardianDialog = MutableStateFlow(false)
    val showNoGuardianDialog: StateFlow<Boolean> = _showNoGuardianDialog.asStateFlow()

    private val pendingSubmissions = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun dismissNoGuardianDialog() {
        _showNoGuardianDialog.value = false
    }

    fun refreshConnectionsAndAlerts() {
        viewModelScope.launch {
            val uid = _userProfile.value.uid
            val isParent = _userProfile.value.isParent
            if (uid.isNotBlank()) {
                if (!isParent) {
                    _pendingConnectionRequests.value = authRepo.fetchPendingConnectionRequests(uid)
                    _acceptedGuardians.value = authRepo.fetchAcceptedConnectionsForPatient(uid)
                } else {
                    _acceptedPatients.value = authRepo.fetchAcceptedPatientsForGuardian(uid)
                    _activeEmergencyAlerts.value = authRepo.fetchActiveEmergencyAlerts(uid)
                }
            } else {
                _pendingConnectionRequests.value = authRepo.fetchPendingConnectionRequests("")
                _acceptedGuardians.value = authRepo.fetchAcceptedConnectionsForPatient("")
                _acceptedPatients.value = authRepo.fetchAcceptedPatientsForGuardian("")
                _activeEmergencyAlerts.value = authRepo.fetchActiveEmergencyAlerts("")
            }
        }
    }

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

    // --- Medical Report Scanner State ---
    private val _medicalReportResult = MutableStateFlow<com.example.data.MedicalReportAnalysisResult?>(null)
    val medicalReportResult: StateFlow<com.example.data.MedicalReportAnalysisResult?> = _medicalReportResult.asStateFlow()

    private val _isAnalyzingReport = MutableStateFlow(false)
    val isAnalyzingReport: StateFlow<Boolean> = _isAnalyzingReport.asStateFlow()

    private val _activeScannerTab = MutableStateFlow(0) // 0: Medical Report, 1: Medicine Package
    val activeScannerTab: StateFlow<Int> = _activeScannerTab.asStateFlow()

    fun setActiveScannerTab(tabIndex: Int) {
        _activeScannerTab.value = tabIndex
    }

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
    fun speakText(text: String, langCode: String = _selectedLanguage.value) {
        if (!_isVoiceEnabled.value) return
        updateVoiceEngineConfig()
        voiceManager.speak(
            text = text,
            langCode = langCode,
            onError = { warningMsg ->
                showSnackbar(warningMsg)
            }
        )
    }

    fun speakMedicineReminderForMed(med: Medication) {
        if (!_isVoiceEnabled.value) return
        updateVoiceEngineConfig()
        val langCode = _selectedLanguage.value
        voiceManager.speakMedicineReminder(
            userName = userName.value,
            medName = med.name,
            dosage = med.dosage,
            langCode = langCode,
            onError = { warningMsg ->
                showSnackbar(warningMsg)
            }
        )
    }

    fun testVoice() {
        val langCode = _selectedLanguage.value
        val testText = LanguageManager.getTestVoiceText(langCode)
        speakText(testText, langCode)
    }

    fun stopVoice() {
        voiceManager.stop()
    }

    // --- Preference Setters ---
    fun setLanguage(langCode: String) {
        _selectedLanguage.value = langCode
        settingsPrefs.edit().putString("selected_language", langCode).apply()
        updateVoiceEngineConfig()
        val langName = LanguageManager.getLanguageNativeName(langCode)
        showSnackbar("Language set to $langName")

        val testText = LanguageManager.getTestVoiceText(langCode)
        speakText(testText, langCode)
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
                loadHealthProfile()
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
                loadHealthProfile()
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

    fun isNotificationOnboardingCompleted(): Boolean {
        return settingsPrefs.getBoolean("notification_onboarding_completed", false)
    }

    fun setNotificationOnboardingCompleted(completed: Boolean = true) {
        settingsPrefs.edit().putBoolean("notification_onboarding_completed", completed).apply()
    }

    fun continueWithGoogle(
        context: Context,
        onSuccess: (isNewUser: Boolean, isParent: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.signInWithGoogleCredential(context)
            _isAuthLoading.value = false
            result.onSuccess { authResult ->
                _userProfile.value = authResult.profile
                _isLoggedIn.value = true
                loadHealthProfile()
                showSnackbar("Signed in with Google as ${authResult.profile.name}")
                onSuccess(authResult.isNewUser, authResult.profile.isParent)
            }.onFailure { exception ->
                _isLoggedIn.value = false
                val message = exception.message ?: "Unable to sign in with Google. Please try again."
                showSnackbar(message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout(getApplication())
            repository.clearAllUserData()
            _isLoggedIn.value = false
            _userProfile.value = UserProfile()
            _healthProfile.value = HealthProfile()
            showSnackbar("Signed out successfully.")
        }
    }

    fun linkParentToPatient(code: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val profile = _userProfile.value
            val result = authRepo.requestGuardianConnection(
                guardianUid = profile.uid,
                guardianName = profile.name.ifBlank { "Guardian" },
                guardianEmail = profile.email,
                connectionCode = code
            )
            result.fold(
                onSuccess = { conn ->
                    showSnackbar("Connection request sent to ${conn.patientName}. Awaiting patient approval.")
                    refreshConnectionsAndAlerts()
                    onSuccess()
                },
                onFailure = { e ->
                    showSnackbar(e.message ?: "Failed to connect to patient with code $code")
                }
            )
            _isAuthLoading.value = false
        }
    }

    fun sendGuardianConnectionRequest(code: String, onSuccess: () -> Unit = {}) {
        linkParentToPatient(code, onSuccess)
    }

    fun sendConnectionRequest(code: String, onSuccess: () -> Unit = {}) {
        linkParentToPatient(code, onSuccess)
    }

    fun respondToConnectionRequest(connectionId: String, accept: Boolean) {
        viewModelScope.launch {
            authRepo.respondToConnectionRequest(connectionId, accept)
            showSnackbar(if (accept) "Family connection approved!" else "Connection request declined.")
            refreshConnectionsAndAlerts()
        }
    }

    fun resolveEmergencyAlert(alertId: String) {
        viewModelScope.launch {
            authRepo.resolveEmergencyAlert(alertId)
            showSnackbar("Emergency alert marked as resolved.")
            refreshConnectionsAndAlerts()
        }
    }

    // --- Medication Actions ---
    fun markDoseTaken(medicationId: Long, timeSlot: String, dateScheduled: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }
        viewModelScope.launch {
            val med = activeMedications.value.find { it.id == medicationId }
            repository.markDoseAsTaken(medicationId, timeSlot, dateScheduled)
            val msg = "${med?.name ?: "Medicine"} ($timeSlot) marked as taken!"
            showSnackbar(msg)
            if (_isVoiceEnabled.value) {
                speakText("${med?.name ?: "Medicine"} dose taken. Good job!")
            }
        }
    }

    fun markTaken(medicationId: Long) {
        markDoseTaken(medicationId, "08:00 AM")
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

    fun addMedication(medication: Medication, onComplete: () -> Unit = {}) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            onComplete()
            return
        }

        val uid = _userProfile.value.uid
        val submissionKey = "$uid:${medication.name.trim().lowercase()}"

        if (!pendingSubmissions.add(submissionKey)) {
            Log.w("MediGuardViewModel", "Pending submission detected for key: $submissionKey. Ignoring duplicate write request.")
            showSnackbar("A submission for ${medication.name} is already in progress...")
            onComplete()
            return
        }

        Log.d("MediGuardViewModel", "Tracking new pending medication submission for key: $submissionKey")

        viewModelScope.launch {
            try {
                val existing = activeMedications.value.find { 
                    it.name.trim().equals(medication.name.trim(), ignoreCase = true) && 
                    it.timeOfConsumption.trim().equals(medication.timeOfConsumption.trim(), ignoreCase = true) 
                }
                if (existing != null) {
                    showSnackbar("${medication.name} is already in your active schedule.")
                    return@launch
                }
                // 1. Write to Firestore exactly once per submission with unique auto-generated ID
                authRepo.saveMedicationToFirestore(uid, medication)

                // 2. Local Room database insertion
                repository.addMedication(medication)
                showSnackbar("Added ${medication.name} to medication schedule!")
                runDrugInteractionCheck()
            } catch (e: Exception) {
                Log.e("MediGuardViewModel", "Error adding medication", e)
                showSnackbar("Failed to save medication: ${e.localizedMessage}")
            } finally {
                pendingSubmissions.remove(submissionKey)
                Log.d("MediGuardViewModel", "Cleared pending submission tracker for key: $submissionKey")
                onComplete()
            }
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

    fun analyzeMedicalReport(imageBase64: String) {
        viewModelScope.launch {
            _isAnalyzingReport.value = true
            try {
                val result = repository.analyzeMedicalReportImage(imageBase64, activeMedications.value)
                val updatedMedicines = com.example.util.MedicalReportExtractor.compareCandidatesWithExistingRecords(
                    result.medicines,
                    activeMedications.value
                )
                _medicalReportResult.value = result.copy(medicines = updatedMedicines)
            } catch (e: Exception) {
                Log.e("MediGuardViewModel", "Error analyzing report image", e)
                showSnackbar("Report scan error: ${e.localizedMessage}")
            } finally {
                _isAnalyzingReport.value = false
            }
        }
    }

    fun loadSampleReportPreset(presetResult: com.example.data.MedicalReportAnalysisResult) {
        val updatedCandidates = com.example.util.MedicalReportExtractor.compareCandidatesWithExistingRecords(
            presetResult.medicines,
            activeMedications.value
        )
        _medicalReportResult.value = presetResult.copy(medicines = updatedCandidates)
    }

    fun clearMedicalReportResult() {
        _medicalReportResult.value = null
    }

    fun confirmAndImportSelectedReportMedicines(
        confirmedCandidates: List<com.example.data.ExtractedMedicineCandidate>,
        reportResult: com.example.data.MedicalReportAnalysisResult
    ) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access.")
            return
        }

        if (confirmedCandidates.isEmpty()) {
            showSnackbar("No medicines selected for confirmation.")
            return
        }

        viewModelScope.launch {
            _isAnalyzingReport.value = true
            var importedCount = 0

            for (candidate in confirmedCandidates) {
                val dosageStr = if (candidate.strength.isNotBlank()) candidate.strength else candidate.dosageForm
                val medication = Medication(
                    name = candidate.name,
                    dosage = dosageStr,
                    totalTablets = candidate.durationDays * candidate.timings.size,
                    remainingTablets = candidate.durationDays * candidate.timings.size,
                    startDate = "2026-08-10",
                    endDate = "2026-09-10",
                    timeOfConsumption = candidate.timeOfConsumption,
                    beforeOrAfterFood = candidate.beforeOrAfterFood,
                    instructions = candidate.instructions.ifBlank { "Prescribed via Medical Report" },
                    category = "Prescription",
                    prescribedBy = reportResult.doctorName ?: "Medical Report Scan"
                )

                val uid = _userProfile.value.uid
                authRepo.saveMedicationToFirestore(uid, medication)
                repository.addMedication(medication)
                importedCount++
            }

            val uid = _userProfile.value.uid
            if (uid.isNotBlank()) {
                authRepo.saveMedicalReportToFirestore(
                    uid = uid,
                    reportId = reportResult.reportId,
                    reportType = reportResult.reportType,
                    extractedText = reportResult.rawExtractedText,
                    medicineCount = importedCount
                )
            }

            runDrugInteractionCheck()
            showSnackbar("Successfully added $importedCount medicine(s) to CareSync schedule!")
            _isAnalyzingReport.value = false
            _medicalReportResult.value = null
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

    fun triggerEmergencySOS(contactName: String = "", contactPhone: String = "") {
        viewModelScope.launch {
            val patientUid = _userProfile.value.uid
            val patientName = _userProfile.value.name
            val result = authRepo.sendSOSAlert(patientUid, patientName)

            result.fold(
                onSuccess = { guardianNames ->
                    val namesStr = guardianNames.joinToString(", ")
                    showSnackbar("🚨 Emergency SOS Alert dispatched to $namesStr!")
                    if (_isVoiceEnabled.value) {
                        speakText("Emergency SOS alert dispatched to $namesStr.")
                    }
                },
                onFailure = { error ->
                    if (error.message == "NO_GUARDIAN_CONNECTED") {
                        _showNoGuardianDialog.value = true
                        showSnackbar("⚠️ No connected family member/guardian to receive SOS alerts.")
                    } else {
                        showSnackbar("🚨 SOS Alert failed: ${error.localizedMessage}")
                    }
                }
            )
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
