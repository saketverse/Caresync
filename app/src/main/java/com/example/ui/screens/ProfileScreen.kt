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
import com.example.data.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    isDarkMode: Boolean,
    isVoiceEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    onToggleVoice: () -> Unit,
    onNavigateToConnectPatient: () -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Profile & Settings", fontWeight = FontWeight.Bold) },
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
                        text = userProfile.name.ifBlank { "CareSync User" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = userProfile.email,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role Badge
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
                        ProfileMetricChip("Age", "${if (userProfile.age > 0) userProfile.age else 28} yrs")
                        ProfileMetricChip("Blood Type", "O+")
                        ProfileMetricChip("Primary MD", "Dr. Vance")
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
                                    text = userProfile.connectionCode.ifBlank { "123456" },
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Connection Code", userProfile.connectionCode)
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
            } else {
                // Parent View
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Parent / Caregiver Monitoring Link",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        if (!userProfile.connectedPatientName.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = HealthSafe)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Connected Patient: ${userProfile.connectedPatientName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Connection Code: ${userProfile.connectedPatientCode ?: "Linked"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No patient linked yet. Connect using the patient's 6-digit code.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToConnectPatient,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (userProfile.connectedPatientName.isNullOrBlank()) "Connect to Patient" else "Switch Linked Patient")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Parent Permission Restricted: View-only access to medication schedules and logs. Cannot modify sensitive patient details or passwords.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Application Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("App Settings & Preferences", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MedicalPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Theme")
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = MedicalPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Voice Assistant Speech Alerts")
                        }
                        Switch(checked = isVoiceEnabled, onCheckedChange = { onToggleVoice() })
                    }
                }
            }

            // Emergency Contacts
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
                            Text("Dr. Robert Vance (Primary Cardiologist)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("+1 (555) 019-2831", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = HealthDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Eleanor Mitchell (Primary Caregiver)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("+1 (555) 234-5678", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Sign Out Button
            Button(
                onClick = onLogout,
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
fun ProfileMetricChip(label: String, value: String) {
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
