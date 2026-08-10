package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HealthProfile
import com.example.data.LanguageManager
import com.example.data.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    healthProfile: HealthProfile = HealthProfile(),
    isVoiceEnabled: Boolean,
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    voiceGender: String = "Female",
    voiceVolume: Float = 1.0f,
    escalationMinutes: Int = 30,
    isBatteryOptimizationIgnored: Boolean = true,
    onToggleVoice: () -> Unit,
    onSelectLanguage: (String) -> Unit = {},
    onToggleElderMode: () -> Unit = {},
    onSetVoiceGender: (String) -> Unit = {},
    onSetVoiceVolume: (Float) -> Unit = {},
    onSetEscalationMinutes: (Int) -> Unit = {},
    onTestVoiceReminder: () -> Unit = {},
    onRequestDisableBatteryOptimization: () -> Unit = {},
    onStartForegroundService: () -> Unit = {},
    onUpdateHealthProfile: (HealthProfile) -> Unit = {},
    onNavigateToConnectPatient: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditHealthProfileDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditHealthProfileDialog) {
        var bloodGroupInput by remember { mutableStateOf(healthProfile.bloodGroup ?: "") }
        var allergiesInput by remember { mutableStateOf(healthProfile.allergies ?: "") }
        var conditionsInput by remember { mutableStateOf(healthProfile.medicalConditions ?: "") }
        var emergencyNameInput by remember { mutableStateOf(healthProfile.emergencyContactName ?: "") }
        var emergencyPhoneInput by remember { mutableStateOf(healthProfile.emergencyContactPhone ?: "") }
        var notesInput by remember { mutableStateOf(healthProfile.additionalNotes ?: "") }

        AlertDialog(
            onDismissRequest = { showEditHealthProfileDialog = false },
            title = { Text("Edit Health Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = bloodGroupInput,
                        onValueChange = { bloodGroupInput = it },
                        label = { Text("Blood Group") },
                        placeholder = { Text("e.g. A+, B+, O-, etc.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = allergiesInput,
                        onValueChange = { allergiesInput = it },
                        label = { Text("Allergies") },
                        placeholder = { Text("e.g., Penicillin, Latex") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = conditionsInput,
                        onValueChange = { conditionsInput = it },
                        label = { Text("Medical Conditions") },
                        placeholder = { Text("e.g., Asthma, Diabetes") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = emergencyNameInput,
                        onValueChange = { emergencyNameInput = it },
                        label = { Text("Emergency Contact Name") },
                        placeholder = { Text("e.g., Caregiver Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = emergencyPhoneInput,
                        onValueChange = { emergencyPhoneInput = it },
                        label = { Text("Emergency Contact Phone") },
                        placeholder = { Text("e.g., +1 555-0199") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Additional Health Notes") },
                        placeholder = { Text("e.g., Special instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditHealthProfileDialog = false
                        onUpdateHealthProfile(
                            HealthProfile(
                                bloodGroup = bloodGroupInput.ifBlank { null },
                                allergies = allergiesInput.ifBlank { null },
                                medicalConditions = conditionsInput.ifBlank { null },
                                emergencyContactName = emergencyNameInput.ifBlank { null },
                                emergencyContactPhone = emergencyPhoneInput.ifBlank { null },
                                additionalNotes = notesInput.ifBlank { null }
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Profile", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditHealthProfileDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Profile & Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Log Out",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape),
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (userProfile.isParent) Icons.Filled.FamilyRestroom else Icons.Filled.Person,
                                contentDescription = null,
                                tint = MedicalPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userProfile.name.ifBlank { "User Profile" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = userProfile.email.ifBlank { "No email registered" },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Role: ${userProfile.role}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileMetricChip("Age", if (userProfile.age > 0) "${userProfile.age} yrs" else "Not set")
                        ProfileMetricChip("Blood Type", healthProfile.bloodGroup?.takeIf { it.isNotBlank() } ?: "Not set")
                        ProfileMetricChip("Status", "Active")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out / Sign Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Personal Health Profile Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Personal Health Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        TextButton(onClick = { showEditHealthProfileDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider()

                    HealthDetailRow(
                        label = "Blood Group",
                        value = healthProfile.bloodGroup?.takeIf { it.isNotBlank() } ?: "Not set (Tap Edit to enter)"
                    )
                    HealthDetailRow(
                        label = "Allergies",
                        value = healthProfile.allergies?.takeIf { it.isNotBlank() } ?: "None reported"
                    )
                    HealthDetailRow(
                        label = "Medical Conditions",
                        value = healthProfile.medicalConditions?.takeIf { it.isNotBlank() } ?: "None reported"
                    )
                    HealthDetailRow(
                        label = "Emergency Contact",
                        value = if (!healthProfile.emergencyContactName.isNullOrBlank()) {
                            "${healthProfile.emergencyContactName} ${healthProfile.emergencyContactPhone?.let { "($it)" } ?: ""}"
                        } else "Not set"
                    )
                    if (!healthProfile.additionalNotes.isNullOrBlank()) {
                        HealthDetailRow(label = "Notes", value = healthProfile.additionalNotes)
                    }
                }
            }

            // Elder Mode & Multi-Language Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Elderly, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(LanguageManager.getText("elder_mode", selectedLanguage), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Large text, high-contrast, & easy spacing", fontSize = 12.sp, color = MedicalSecondary)
                            }
                        }
                        Switch(checked = isElderMode, onCheckedChange = { onToggleElderMode() })
                    }

                    HorizontalDivider()

                    // Language Selector (All 7 Supported Indian Languages)
                    Text("Select App & Voice Language / भाषा चुनें:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MedicalPrimary)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(LanguageManager.supportedLanguages) { lang ->
                            FilterChip(
                                selected = (selectedLanguage == lang.code),
                                onClick = { onSelectLanguage(lang.code) },
                                label = {
                                    Text(
                                        text = "${lang.nativeName} (${lang.displayName})",
                                        fontWeight = if (selectedLanguage == lang.code) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Voice Reminders Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = MedicalPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(LanguageManager.getText("voice_reminders", selectedLanguage), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Switch(checked = isVoiceEnabled, onCheckedChange = { onToggleVoice() })
                    }

                    if (isVoiceEnabled) {
                        HorizontalDivider()

                        // Voice Locale Status Indicator
                        Surface(
                            color = MedicalPrimaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Active Voice Engine",
                                        fontSize = 12.sp,
                                        color = MedicalSecondary
                                    )
                                    Text(
                                        text = "${LanguageManager.getLanguageNativeName(selectedLanguage)} (${LanguageManager.getLanguageLocaleCode(selectedLanguage)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MedicalPrimary
                                    )
                                }
                                Surface(
                                    color = Color(0xFF34C759).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "TTS Ready",
                                        color = Color(0xFF34C759),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text("Voice Assistant Settings:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        // Voice Gender Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = (voiceGender == "Female"),
                                onClick = { onSetVoiceGender("Female") },
                                label = { Text("Female Voice 👩") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = (voiceGender == "Male"),
                                onClick = { onSetVoiceGender("Male") },
                                label = { Text("Male Voice 👨") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Volume Control Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Voice Volume Level:", fontSize = 13.sp)
                                Text("${(voiceVolume * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MedicalPrimary)
                            }
                            Slider(
                                value = voiceVolume,
                                onValueChange = { onSetVoiceVolume(it) },
                                valueRange = 0.2f..1.0f
                            )
                        }

                        Button(
                            onClick = onTestVoiceReminder,
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice (${LanguageManager.getLanguageNativeName(selectedLanguage)}) 🔊", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Smart Reminder Repetition & Caregiver Escalation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Smart Reminder Repetition & Caregiver Escalation", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MedicalPrimary)

                    Surface(color = HealthWarningContainer, shape = RoundedCornerShape(10.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⏱️ Missed Reminder Protocol:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HealthWarning)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• 5 mins: Repeat gentle voice reminder", fontSize = 12.sp)
                            Text("• 15 mins: Repeat with urgent louder notification", fontSize = 12.sp)
                            Text("• 30 mins: Send missed dose alert to parent / caregiver", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Caregiver Escalation Timeout:", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 30, 45).forEach { mins ->
                                FilterChip(
                                    selected = (escalationMinutes == mins),
                                    onClick = { onSetEscalationMinutes(mins) },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }
                    }
                }
            }

            // Background Service & Battery Optimization Safeguard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.surface else HealthWarningContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBatteryOptimizationIgnored) Icons.Filled.VerifiedUser else Icons.Filled.BatteryAlert,
                            contentDescription = null,
                            tint = if (isBatteryOptimizationIgnored) MedicalPrimary else HealthWarning,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Battery Optimization & Background Safeguard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isBatteryOptimizationIgnored) MedicalPrimary else HealthWarning
                            )
                            Text(
                                text = if (isBatteryOptimizationIgnored)
                                    "Status: Exempted (Background reminders & exact alarms will NOT be killed by Android battery optimization)"
                                else
                                    "Status: Optimization Active (May delay or kill background medication alerts)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRequestDisableBatteryOptimization,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.primaryContainer else HealthWarning,
                                contentColor = if (isBatteryOptimizationIgnored) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBatteryOptimizationIgnored) "Verify Status ⚡" else "Grant Whitelist ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onStartForegroundService,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart Safeguard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connection Code Card (For Patients) or Linked Account Card (For Parents)
            if (userProfile.isPatient) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your Patient Connection Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Share this 6-digit code with your Parent or Caregiver so they can connect and monitor your medication schedule.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = userProfile.connectionCode.ifBlank { "N/A" },
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Connection Code", userProfile.connectionCode.ifBlank { "N/A" })
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Connection Code Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Code", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Emergency Contacts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Registered Emergency Contacts", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = HealthDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Primary Emergency Services", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("112 / 102 (National Emergency Helpline)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = HealthDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = healthProfile.emergencyContactName?.takeIf { it.isNotBlank() } ?: "Personal Caregiver / Emergency Contact (Not Set)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = healthProfile.emergencyContactPhone?.takeIf { it.isNotBlank() } ?: "Tap 'Edit Profile' above to configure contact phone",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Sign Out Button
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HealthDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ProfileMetricChip(label: String, value: String) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}
