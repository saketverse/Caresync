package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.data.LanguageManager
import com.example.data.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    isDarkMode: Boolean,
    isVoiceEnabled: Boolean,
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    voiceGender: String = "Female",
    voiceVolume: Float = 1.0f,
    escalationMinutes: Int = 30,
    isBatteryOptimizationIgnored: Boolean = true,
    onToggleDarkMode: () -> Unit,
    onToggleVoice: () -> Unit,
    onSelectLanguage: (String) -> Unit = {},
    onToggleElderMode: () -> Unit = {},
    onSetVoiceGender: (String) -> Unit = {},
    onSetVoiceVolume: (Float) -> Unit = {},
    onSetEscalationMinutes: (Int) -> Unit = {},
    onTestVoiceReminder: () -> Unit = {},
    onRequestDisableBatteryOptimization: () -> Unit = {},
    onStartForegroundService: () -> Unit = {},
    onNavigateToConnectPatient: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Profile & Settings", fontWeight = FontWeight.Bold) },
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
                        text = userProfile.name.ifBlank { "Mr. Sharma" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = userProfile.email.ifBlank { "sharma.elder@health.org" },
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
                        ProfileMetricChip("Age", "${if (userProfile.age > 0) userProfile.age else 68} yrs")
                        ProfileMetricChip("Blood Type", "B+")
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

                    // Language Selector (English & हिन्दी)
                    Text("Select App Language / भाषा चुनें:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MedicalPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = (selectedLanguage == LanguageManager.LANG_ENGLISH),
                            onClick = { onSelectLanguage(LanguageManager.LANG_ENGLISH) },
                            label = {
                                Text(
                                    text = "English",
                                    fontWeight = if (selectedLanguage == LanguageManager.LANG_ENGLISH) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = (selectedLanguage == LanguageManager.LANG_HINDI),
                            onClick = { onSelectLanguage(LanguageManager.LANG_HINDI) },
                            label = {
                                Text(
                                    text = "हिन्दी",
                                    fontWeight = if (selectedLanguage == LanguageManager.LANG_HINDI) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
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
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryContainer, contentColor = MedicalPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice Reminder Speech 🔊", fontWeight = FontWeight.Bold)
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
                                containerColor = if (isBatteryOptimizationIgnored) MedicalPrimaryContainer else HealthWarning,
                                contentColor = if (isBatteryOptimizationIgnored) MedicalPrimary else Color.White
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

            // App Dark Theme Preference
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MedicalPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Theme Canvas", fontWeight = FontWeight.SemiBold)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
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
                            Icon(Icons.Filled.Key, contentDescription = null, tint = MedicalPrimary)
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
                                    text = userProfile.connectionCode.ifBlank { "849201" },
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Connection Code", userProfile.connectionCode.ifBlank { "849201" })
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Connection Code Copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Code", tint = MedicalPrimary)
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
                            Text("Primary Emergency Helpline", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("+91 98765 43210", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = HealthDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Aarav Sharma (Son / Primary Caregiver)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("+91 91234 56789", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
