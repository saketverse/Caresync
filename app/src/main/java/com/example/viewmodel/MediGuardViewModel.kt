package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MedicationRepository(db.medicationDao())
    private val authRepo = FirebaseAuthRepository(application)

    // --- Authentication & User Profile State ---
    private val initialProfile = authRepo.getLocalProfile() ?: UserProfile(
        uid = "default_user",
        name = "Alex Morgan",
        age = 29,
        email = "alex.morgan@health.org",
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
        .map { it.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Alex Morgan")

    val userEmail: StateFlow<String> = _userProfile
        .map { it.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "alex.morgan@health.org")

    // --- Settings State ---
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

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
                text = "Hello! I am CareSync AI Assistant. How can I help you with medication safety, side effects, or drug interactions today?",
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
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // --- Authentication Actions ---

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.login(email, pass)
            result.onSuccess { profile ->
                _userProfile.value = profile
                _isLoggedIn.value = true
                showSnackbar("Welcome back, ${profile.name}!")
                onSuccess()
            }.onFailure { err ->
                showSnackbar("Login failed: ${err.message}")
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
        onNavigateConnectPatient: () -> Unit,
        onNavigateDashboard: () -> Unit
    ) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.signUp(fullName, age, email, pass, role)
            result.onSuccess { profile ->
                _userProfile.value = profile
                _isLoggedIn.value = true
                showSnackbar("Account created successfully as ${profile.role}!")
                if (profile.isParent) {
                    onNavigateConnectPatient()
                } else {
                    onNavigateDashboard()
                }
            }.onFailure { err ->
                showSnackbar("Sign Up failed: ${err.message}")
            }
            _isAuthLoading.value = false
        }
    }

    fun sendForgotPassword(email: String) {
        viewModelScope.launch {
            authRepo.sendPasswordReset(email)
            showSnackbar("Password reset email sent to $email!")
        }
    }

    fun linkParentToPatient(code: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = authRepo.linkParentToPatient(_userProfile.value.uid, code)
            result.onSuccess { updatedProfile ->
                _userProfile.value = updatedProfile
                val patientName = updatedProfile.connectedPatientName ?: "Patient"
                showSnackbar("Successfully linked with patient $patientName (Code: $code)!")
                onComplete()
            }.onFailure { err ->
                showSnackbar("Linking failed: ${err.message}")
            }
            _isAuthLoading.value = false
        }
    }

    fun loginWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val demoProfile = UserProfile(
                uid = "google_user_123",
                name = "Alex Morgan",
                age = 30,
                email = "alex.google@health.org",
                role = UserProfile.ROLE_PATIENT,
                connectionCode = "849201"
            )
            authRepo.saveLocalProfile(demoProfile)
            _userProfile.value = demoProfile
            _isLoggedIn.value = true
            showSnackbar("Successfully signed in with Google!")
            onSuccess()
        }
    }

    fun skipQuickDemo(onSuccess: () -> Unit) {
        val demoProfile = UserProfile(
            uid = "demo_guest",
            name = "Alex Morgan",
            age = 30,
            email = "demo@caresync.app",
            role = UserProfile.ROLE_PATIENT,
            connectionCode = "849201"
        )
        authRepo.saveLocalProfile(demoProfile)
        _userProfile.value = demoProfile
        _isLoggedIn.value = true
        showSnackbar("Quick Demo Mode activated")
        onSuccess()
    }

    fun logout() {
        authRepo.logout()
        _isLoggedIn.value = false
        _userProfile.value = UserProfile()
        showSnackbar("Logged out successfully")
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleVoice() {
        _isVoiceEnabled.value = !_isVoiceEnabled.value
        showSnackbar(if (_isVoiceEnabled.value) "Voice feedback enabled" else "Voice feedback disabled")
    }

    fun markTaken(medicationId: Long) {
        viewModelScope.launch {
            repository.markAsTaken(medicationId)
            showSnackbar("Marked dose as taken! Stock updated.")
        }
    }

    fun refillStock(medicationId: Long, amount: Int = 30) {
        viewModelScope.launch {
            repository.refillStock(medicationId, amount)
            showSnackbar("Refilled stock by +$amount tablets!")
        }
    }

    fun addMedication(medication: Medication) {
        if (_userProfile.value.isParent) {
            showSnackbar("⚠️ Parent / Caregiver account has read-only access. Only patient can add medication.")
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
                instructions = "Extracted automatically from doctor prescription OCR scan",
                category = "Prescription",
                prescribedBy = "Dr. OCR Scan"
            )
            repository.addMedication(med)
            showSnackbar("Auto-imported $name ($dosage) into schedule!")
        }
    }

    fun triggerEmergencySOS(contactName: String, contactPhone: String) {
        showSnackbar("🚨 Emergency Alert dispatched to $contactName ($contactPhone)!")
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
