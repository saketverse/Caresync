package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.Medication
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    medications: List<Medication>,
    interactionResult: String?,
    isCheckingInteractions: Boolean,
    onMarkTaken: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToInteractions: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToFamily: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onTriggerEmergency: () -> Unit
) {
    var showEmergencyDialog by remember { mutableStateOf(false) }

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
                            text = "CareSync",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalPrimary
                            )
                        )
                        Text(
                            text = "Welcome, $userName",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MedicalPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = MedicalPrimary
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
                    colors = CardDefaults.cardColors(containerColor = MedicalPrimary),
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
                                    text = "Daily Adherence Rate",
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

            // 2. Urgent Safety & Drug Interaction Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToInteractions() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (interactionResult?.contains("HIGH") == true) HealthDangerContainer else HealthSafeContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = if (interactionResult?.contains("HIGH") == true) HealthDanger else HealthSafe,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Drug Interaction Shield",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCheckingInteractions) "Analyzing interactions with Gemini AI..."
                                else interactionResult?.take(75) ?: "Active medicines verified. Tap to run live AI check.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Quick Action Buttons Grid
            item {
                Column {
                    Text(
                        text = "Quick Assistant Tools",
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
                            containerColor = MedicalPrimaryContainer,
                            iconColor = MedicalPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdd
                        )
                        QuickActionButton(
                            title = "Scan Prescription",
                            icon = Icons.Filled.DocumentScanner,
                            containerColor = MedicalSecondaryContainer,
                            iconColor = MedicalSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToScanner
                        )
                        QuickActionButton(
                            title = "AI Chatbot",
                            icon = Icons.Filled.SmartToy,
                            containerColor = MedicalTertiaryContainer,
                            iconColor = MedicalTertiary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToChatbot
                        )
                        QuickActionButton(
                            title = "Family Care",
                            icon = Icons.Filled.People,
                            containerColor = HealthWarningContainer,
                            iconColor = HealthWarning,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFamily
                        )
                    }
                }
            }

            // 4. Low Stock Inventory Alert Banner
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
                                    text = "Low Medicine Inventory Alert",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${lowStockMeds.first().name} has only ${lowStockMeds.first().remainingTablets} tablets left.",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Today's Medication Schedule
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onNavigateToAdd) {
                        Text("+ New Medicine")
                    }
                }
            }

            if (medications.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No medications scheduled yet. Tap + New Medicine to begin.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(medications, key = { it.id }) { med ->
                    MedicationScheduleCard(
                        medication = med,
                        onMarkTaken = { onMarkTaken(med.id) }
                    )
                }
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
                        text = "Emergency SOS Alert Dispatch",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("Send Emergency SOS?") },
            text = { Text("This will notify your registered emergency contacts (Dr. Vance & Eleanor Mitchell) with your current status and medication list.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEmergencyDialog = false
                        onTriggerEmergency()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HealthDanger)
                ) {
                    Text("Send SOS Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SummaryChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MedicationScheduleCard(
    medication: Medication,
    onMarkTaken: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.isTakenToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (medication.isTakenToday) HealthSafeContainer else MedicalPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (medication.isTakenToday) Icons.Filled.CheckCircle else Icons.Filled.Medication,
                        contentDescription = null,
                        tint = if (medication.isTakenToday) HealthSafe else MedicalPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${medication.dosage} • ${medication.beforeOrAfterFood}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MedicalPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = medication.timeOfConsumption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MedicalPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${medication.remainingTablets} pills left",
                        fontSize = 12.sp,
                        color = if (medication.remainingTablets <= 7) HealthWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!medication.isTakenToday) {
                Button(
                    onClick = onMarkTaken,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Take", fontSize = 12.sp)
                }
            } else {
                Surface(
                    color = HealthSafeContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Taken Today",
                        color = HealthSafe,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
