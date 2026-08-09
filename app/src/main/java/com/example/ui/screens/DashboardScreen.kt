package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LanguageManager
import com.example.data.Medication
import com.example.ui.components.ColorWarningView
import com.example.ui.components.PillOrganizerSection
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    medications: List<Medication>,
    interactionResult: String?,
    isCheckingInteractions: Boolean,
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    onMarkTaken: (Long) -> Unit,
    onSpeakReminder: (Medication) -> Unit = {},
    onListenWarning: (String) -> Unit = {},
    onNavigateToAdd: () -> Unit,
    onNavigateToInteractions: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTriggerEmergency: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val takenCount = medications.count { it.isTakenToday }
    val totalMeds = medications.size
    val adherencePercent = if (totalMeds > 0) ((takenCount.toFloat() / totalMeds) * 100).toInt() else 100
    val lowStockMeds = medications.filter { it.remainingTablets <= 7 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LanguageManager.getText("app_title", selectedLanguage),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Namaste, $userName",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Health Summary Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Adherence Score",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "$adherencePercent%",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = HealthSafe,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryChip(
                                label = "Active Meds",
                                value = "$totalMeds",
                                icon = Icons.Filled.Medication
                            )
                            SummaryChip(
                                label = "Doses Taken",
                                value = "$takenCount / $totalMeds",
                                icon = Icons.Filled.Done
                            )
                            SummaryChip(
                                label = "Low Stock",
                                value = "${lowStockMeds.size}",
                                icon = Icons.Filled.Warning
                            )
                        }
                    }
                }
            }

            // 2. Color-based Safety Indicator Card
            item {
                ColorWarningView(
                    riskLevel = if (interactionResult?.contains("HIGH") == true) "HIGH" else "SAFE",
                    rawSummary = interactionResult ?: "Active medications in schedule: ${medications.joinToString { it.name }}. All combinations safe.",
                    selectedLanguage = selectedLanguage,
                    isElderMode = isElderMode,
                    onListenWarning = onListenWarning
                )
            }

            // 3. Quick Assistant Tools
            item {
                Column {
                    Text(
                        text = "Quick Care Tools",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            title = "Add Med",
                            icon = Icons.Filled.AddCircle,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdd
                        )
                        QuickActionButton(
                            title = "Scan Strip",
                            icon = Icons.Filled.Camera,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconColor = MaterialTheme.colorScheme.secondary,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToScanner
                        )
                        QuickActionButton(
                            title = "AI Chatbot",
                            icon = Icons.Filled.SmartToy,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToChatbot
                        )
                        QuickActionButton(
                            title = "Family Care",
                            icon = Icons.Filled.People,
                            containerColor = HealthWarningContainer,
                            iconColor = HealthWarning,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFamily
                        )
                    }
                }
            }

            // 4. Low Stock Inventory Banner
            if (lowStockMeds.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = HealthWarningContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddShoppingCart,
                                contentDescription = null,
                                tint = HealthWarning,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Low Medicine Refill Warning",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${lowStockMeds.first().name} has only ${lowStockMeds.first().remainingTablets} tablets remaining.",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Elder Pill Organizer Section (Morning, Afternoon, Evening, Night)
            item {
                PillOrganizerSection(
                    medications = medications,
                    selectedLanguage = selectedLanguage,
                    isElderMode = isElderMode,
                    onMarkTaken = onMarkTaken,
                    onSpeakReminder = onSpeakReminder
                )
            }

            // Emergency SOS Dispatch Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showEmergencyDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthDanger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Emergency,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🚨 Emergency SOS Alert Dispatch",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("Send Emergency SOS Alert?") },
            text = { Text("This will send an emergency notification to your registered emergency contact with your location and medication status.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onTriggerEmergency()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthDanger)
                ) {
                    Text("Send Emergency SOS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of CareSync?") },
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
}

@Composable
private fun SummaryChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconColor: Color,
    textColor: Color = iconColor,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
