package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun AddMedicationScreen(
    onSaveMedication: (Medication) -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("500 mg") }
    var totalTablets by remember { mutableStateOf("30") }
    var timeOfConsumption by remember { mutableStateOf("08:00 AM, 08:00 PM") }
    var beforeOrAfterFood by remember { mutableStateOf("After Food") }
    var startDate by remember { mutableStateOf("2026-08-06") }
    var endDate by remember { mutableStateOf("2026-09-06") }
    var instructions by remember { mutableStateOf("Take with water") }
    var category by remember { mutableStateOf("Prescription") }
    var prescribedBy by remember { mutableStateOf("Dr. Smith") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Medication", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Medicine Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedicalPrimary
                        )
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medicine Name *") },
                        placeholder = { Text("e.g. Paracetamol, Metformin") },
                        leadingIcon = { Icon(Icons.Filled.Medication, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = dosage,
                            onValueChange = { dosage = it },
                            label = { Text("Dosage") },
                            placeholder = { Text("250 mg, 10 ml") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = totalTablets,
                            onValueChange = { totalTablets = it },
                            label = { Text("Total Count") },
                            placeholder = { Text("30") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Food Instruction Selector
                    Text(
                        text = "Food Timing Instruction",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Before Food", "After Food", "With Food").forEach { option ->
                            FilterChip(
                                selected = (beforeOrAfterFood == option),
                                onClick = { beforeOrAfterFood = option },
                                label = { Text(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = timeOfConsumption,
                        onValueChange = { timeOfConsumption = it },
                        label = { Text("Schedule Time(s)") },
                        placeholder = { Text("e.g. 08:00 AM, 08:00 PM") },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Date") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("End Date") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Special Doctor Notes / Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = prescribedBy,
                        onValueChange = { prescribedBy = it },
                        label = { Text("Prescribed By Doctor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val count = totalTablets.toIntOrNull() ?: 30
                        val newMed = Medication(
                            name = name,
                            dosage = if (dosage.isBlank()) "500 mg" else dosage,
                            totalTablets = count,
                            remainingTablets = count,
                            startDate = startDate,
                            endDate = endDate,
                            timeOfConsumption = if (timeOfConsumption.isBlank()) "08:00 AM" else timeOfConsumption,
                            beforeOrAfterFood = beforeOrAfterFood,
                            instructions = instructions,
                            category = category,
                            prescribedBy = prescribedBy
                        )
                        onSaveMedication(newMed)
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Medication & Enable Reminders", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
