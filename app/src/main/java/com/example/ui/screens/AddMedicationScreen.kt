package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Medication
import com.example.data.MedicineDatabase
import com.example.data.MedicineInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch

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

    // Search & Autocomplete State
    var searchedInfo by remember { mutableStateOf<MedicineInfo?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchAttempted by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val autocompleteSuggestions = remember(name) {
        if (name.length >= 2 && !showDropdown) {
            MedicineDatabase.getAutocompleteSuggestions(name)
        } else {
            emptyList()
        }
    }

    fun performDatabaseSearch(queryName: String) {
        if (queryName.isBlank()) return
        isSearching = true
        searchAttempted = true
        showDropdown = false
        coroutineScope.launch {
            val result = MedicineDatabase.searchMedicine(queryName, context)
            searchedInfo = result
            isSearching = false
            if (result != null) {
                if (dosage.isBlank() || dosage == "500 mg") {
                    dosage = result.dosage.substringBefore(",").ifBlank { "500 mg" }
                }
                if (instructions.isBlank() || instructions == "Take with water") {
                    instructions = "${result.uses.take(80)}. ${result.warnings.take(80)}"
                }
            }
        }
    }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Medicine Details",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalPrimary
                            )
                        )

                        TextButton(
                            onClick = { performDatabaseSearch(name) },
                            enabled = name.isNotBlank() && !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search Database", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Medicine Name input with Live Autocomplete
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newText ->
                                name = newText
                                searchAttempted = false
                                searchedInfo = null
                            },
                            label = { Text("Medicine Name *") },
                            placeholder = { Text("e.g. Paracetamol, Metformin, Lisinopril") },
                            leadingIcon = { Icon(Icons.Filled.Medication, contentDescription = null) },
                            trailingIcon = {
                                if (name.isNotBlank()) {
                                    IconButton(onClick = { performDatabaseSearch(name) }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = MedicalPrimary)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Autocomplete suggestions dropdown chips
                        if (autocompleteSuggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "Suggestions (Click to load details):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        autocompleteSuggestions.take(3).forEach { suggestion ->
                                            SuggestionChip(
                                                onClick = {
                                                    name = suggestion
                                                    performDatabaseSearch(suggestion)
                                                },
                                                label = { Text(suggestion, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Display Fallback Message if not found
                    if (searchAttempted && searchedInfo == null && !isSearching) {
                        Surface(
                            color = HealthWarningContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = HealthWarning)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Medicine not found. Please verify the name.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HealthWarning
                                )
                            }
                        }
                    }

                    // Display Detailed Searched Medicine Card
                    searchedInfo?.let { info ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = info.brandName.ifBlank { info.name },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MedicalPrimary
                                    )
                                    Surface(
                                        color = if (info.isFromOpenFDA) HealthSafeContainer else MedicalPrimaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (info.isFromOpenFDA) "OpenFDA Verified" else "CareSync DB",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (info.isFromOpenFDA) HealthSafe else MedicalPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text("Generic Name: ${info.genericName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Manufacturer: ${info.manufacturer}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Standard Dosage: ${info.dosage}", fontSize = 12.sp)

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                Text("🩺 Uses: ${info.uses}", fontSize = 12.sp)
                                Text("⚡ Side Effects: ${info.sideEffects}", fontSize = 12.sp, color = HealthWarning)
                                Text("🔄 Interactions: ${info.drugInteractions}", fontSize = 12.sp)
                                Text("⚠️ Warnings: ${info.warnings}", fontSize = 12.sp, color = HealthDanger)
                            }
                        }
                    }

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
                        label = { Text("Special Instructions / Usage Notes") },
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
                            prescribedBy = if (prescribedBy.isBlank() && searchedInfo != null) searchedInfo!!.manufacturer else prescribedBy
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
