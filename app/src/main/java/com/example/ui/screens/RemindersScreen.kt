package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Medication
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    medications: List<Medication>,
    onMarkTaken: (Long) -> Unit,
    onRefillStock: (Long) -> Unit,
    onShowTestNotification: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Reminders, 1: Low Inventory

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Reminders & Inventory", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Daily Schedule") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Stock Inventory") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Test Sound & Notification Trigger Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalPrimaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = MedicalPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Alarm Sound Alerts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Simulate alarm alert and push notification chime", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onShowTestNotification("⏰ REMINDER: Time for Metformin 500mg (After Food)") },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Test Sound")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(medications, key = { it.id }) { med ->
                        ReminderCard(
                            medication = med,
                            onMarkTaken = { onMarkTaken(med.id) },
                            onSnooze = { onShowTestNotification("⏰ Snoozed ${med.name} for 15 minutes.") }
                        )
                    }
                }
            } else {
                // Stock Inventory View
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(medications, key = { it.id }) { med ->
                        InventoryCard(
                            medication = med,
                            onRefill = { onRefillStock(med.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(
    medication: Medication,
    onMarkTaken: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = MedicalPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = medication.timeOfConsumption,
                        fontWeight = FontWeight.Bold,
                        color = MedicalPrimary,
                        fontSize = 15.sp
                    )
                }

                Surface(
                    color = if (medication.isTakenToday) HealthSafeContainer else HealthWarningContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (medication.isTakenToday) "COMPLETED" else "PENDING DOSE",
                        color = if (medication.isTakenToday) HealthSafe else HealthWarning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = medication.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Dosage: ${medication.dosage} (${medication.beforeOrAfterFood})",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (medication.instructions.isNotBlank()) {
                Text(
                    text = "Instruction: ${medication.instructions}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!medication.isTakenToday) {
                    Button(
                        onClick = onMarkTaken,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Taken")
                    }

                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze 15m")
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Taken Today 👍")
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryCard(
    medication: Medication,
    onRefill: () -> Unit
) {
    val progress = (medication.remainingTablets.toFloat() / medication.totalTablets.coerceAtLeast(1)).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = medication.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${medication.dosage} • ${medication.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onRefill,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalSecondary)
                ) {
                    Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refill +30")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining Pill Stock:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${medication.remainingTablets} / ${medication.totalTablets} tablets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (medication.remainingTablets <= 7) HealthWarning else MedicalPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (medication.remainingTablets <= 7) HealthWarning else MedicalPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
