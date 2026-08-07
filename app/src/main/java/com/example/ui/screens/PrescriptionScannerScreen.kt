package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScannerScreen(
    scannedText: String?,
    isScanning: Boolean,
    onScanPreset: (String) -> Unit,
    onImportMedication: (String, String, String, String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf("Cardiology Rx (Lisinopril & Metformin)") }
    val scrollState = rememberScrollState()

    val presetOptions = listOf(
        "Cardiology Rx (Lisinopril & Metformin)",
        "Diabetes & Hypertension Care Rx",
        "Post-Op Pain & Antibiotics Rx"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Prescription Scanner (OCR)", fontWeight = FontWeight.Bold) },
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
            // Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalSecondaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = MedicalSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini Vision OCR Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MedicalOnSecondaryContainer
                            )
                            Text(
                                text = "Instantly scan paper prescriptions to auto-create medication reminders.",
                                fontSize = 12.sp,
                                color = MedicalOnSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Select Prescription Image Preset:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    presetOptions.forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = (selectedPreset == preset),
                                onClick = { selectedPreset = preset }
                            )
                            Text(preset, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onScanPreset(selectedPreset) },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalSecondary)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning with Gemini Vision AI...")
                        } else {
                            Icon(Icons.Filled.CameraEnhance, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Extract Details", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scanned Output Result
            if (scannedText != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = HealthSafe)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracted Medicine Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = scannedText,
                                modifier = Modifier.padding(14.dp),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Text("Detected Quick Action Import:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        Button(
                            onClick = {
                                onImportMedication("Amoxicillin", "500 mg", "08:00 AM, 08:00 PM", "After Food")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthSafe)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("One-Tap Import Amoxicillin to Reminders")
                        }
                    }
                }
            }
        }
    }
}
