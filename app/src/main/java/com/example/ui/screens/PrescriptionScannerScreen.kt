package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.data.LanguageManager
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScannerScreen(
    scannedText: String?,
    isScanning: Boolean,
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    onScanPreset: (String) -> Unit,
    onImportMedication: (String, String, String, String) -> Unit,
    onReadAloudText: (String) -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Strip/Box Camera Identification, 1: Prescription OCR
    var selectedStripPreset by remember { mutableStateOf("Amlodipine 5 mg Strip (BP Medicine)") }
    var scannedStripInfo by remember { mutableStateOf<ScannedStripData?>(null) }
    var isScanningStrip by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val stripPresets = listOf(
        "Amlodipine 5 mg Strip (BP Medicine)",
        "Metformin 500 mg Box (Diabetes Care)",
        "Atorvastatin 10 mg Strip (Cholesterol)",
        "Crocin / Paracetamol 650 mg Strip (Fever & Pain)"
    )

    val prescriptionPresets = listOf(
        "Cardiology Rx (Lisinopril & Metformin)",
        "Diabetes & Hypertension Care Rx",
        "Post-Op Pain & Antibiotics Rx"
    )

    fun performStripScan(presetName: String) {
        isScanningStrip = true
        when {
            presetName.contains("Amlodipine") -> {
                scannedStripInfo = ScannedStripData(
                    brandName = "Amlodipine 5 mg",
                    genericName = "Amlodipine Besylate",
                    dosage = "5 mg Tablet",
                    purpose = "Lowers high blood pressure and prevents heart attacks & chest pain.",
                    sideEffects = "Mild swelling in ankles, dizziness, or flushing.",
                    warnings = "Take daily after morning breakfast. Follow prescribed daily dosage schedule.",
                    manufacturer = "Cipla Healthcare Ltd."
                )
            }
            presetName.contains("Metformin") -> {
                scannedStripInfo = ScannedStripData(
                    brandName = "Glycomet / Metformin 500 mg",
                    genericName = "Metformin Hydrochloride",
                    dosage = "500 mg Tablet",
                    purpose = "Controls high blood sugar levels in Type 2 Diabetes patients.",
                    sideEffects = "Mild stomach upset or nausea when starting.",
                    warnings = "Always take with or immediately after food.",
                    manufacturer = "USV Private Limited"
                )
            }
            presetName.contains("Atorvastatin") -> {
                scannedStripInfo = ScannedStripData(
                    brandName = "Atorva 10 mg",
                    genericName = "Atorvastatin Calcium",
                    dosage = "10 mg Tablet",
                    purpose = "Reduces bad cholesterol (LDL) and protects against heart strokes.",
                    sideEffects = "Unusual muscle pain or tiredness.",
                    warnings = "Take at bedtime or evening after food.",
                    manufacturer = "Zydus Cadila"
                )
            }
            else -> {
                scannedStripInfo = ScannedStripData(
                    brandName = "Crocin / Dolo 650 mg",
                    genericName = "Paracetamol / Acetaminophen",
                    dosage = "650 mg Tablet",
                    purpose = "Relieves mild to moderate fever, headaches, and body pain.",
                    sideEffects = "Nausea if taken on empty stomach.",
                    warnings = "Do not exceed 4 tablets in 24 hours.",
                    manufacturer = "GSK Consumer Healthcare"
                )
            }
        }
        isScanningStrip = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.getText("scan_medicine", selectedLanguage), fontWeight = FontWeight.Bold) },
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
            // Mode Selector Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MedicalPrimary
            ) {
                Tab(
                    selected = (activeTab == 0),
                    onClick = { activeTab = 0 },
                    text = { Text("💊 Medicine Box / Strip Scanner", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = (activeTab == 1),
                    onClick = { activeTab = 1 },
                    text = { Text("📄 Paper Rx OCR", fontWeight = FontWeight.Bold) }
                )
            }

            if (activeTab == 0) {
                // Medicine Strip Camera Scanner View
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalSurfaceLight)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MedicalPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoCamera,
                                        contentDescription = null,
                                        tint = MedicalPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scan Medicine Strip or Box",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isElderMode) 20.sp else 16.sp,
                                    color = MedicalOnSurfaceLight
                                )
                                Text(
                                    text = "Point camera or select a sample strip to extract dosage & purpose",
                                    fontSize = 13.sp,
                                    color = MedicalSecondary
                                )
                            }
                        }

                        Text("Select Sample Medicine Strip / Box:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                        stripPresets.forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = (selectedStripPreset == preset),
                                    onClick = { selectedStripPreset = preset }
                                )
                                Text(preset, fontSize = if (isElderMode) 16.sp else 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Button(
                            onClick = { performStripScan(selectedStripPreset) },
                            enabled = !isScanningStrip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
                        ) {
                            if (isScanningStrip) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Strip with Vision OCR...")
                            } else {
                                Icon(Icons.Filled.Camera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Selected Medicine Strip", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Scanned Medicine Details Card
                scannedStripInfo?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MedicalPrimary, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = HealthSafe)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = info.brandName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isElderMode) 22.sp else 18.sp,
                                        color = MedicalPrimary
                                    )
                                }

                                Button(
                                    onClick = {
                                        val fullText = "${info.brandName}. Dosage: ${info.dosage}. Purpose: ${info.purpose}. Side effects: ${info.sideEffects}. Warning: ${info.warnings}"
                                        onReadAloudText(fullText)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryContainer, contentColor = MedicalPrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(LanguageManager.getText("read_aloud", selectedLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Generic Formula:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(info.genericName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MedicalOnSurfaceLight)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Standard Dosage:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(info.dosage, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MedicalPrimary)
                            }

                            Surface(color = MedicalPrimaryContainer, shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🩺 Purpose / Uses:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MedicalPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(info.purpose, fontSize = if (isElderMode) 15.sp else 13.sp, color = MedicalOnPrimaryContainer)
                                }
                            }

                            Surface(color = HealthWarningContainer, shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("⚡ Potential Side Effects:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HealthWarning)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(info.sideEffects, fontSize = if (isElderMode) 15.sp else 13.sp, color = HealthWarning)
                                }
                            }

                            Surface(color = HealthDangerContainer, shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("⚠️ Special Patient Instructions:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HealthDanger)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(info.warnings, fontSize = if (isElderMode) 15.sp else 13.sp, color = HealthDanger)
                                }
                            }

                            Button(
                                onClick = {
                                    onImportMedication(info.brandName, info.dosage, "08:00 AM, 08:00 PM", "After Food")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HealthSafe)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Scanned Medicine to My Reminders", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                // Paper Prescription OCR View
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
                                    text = "Gemini Vision Prescription OCR",
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

                        prescriptionPresets.forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = (selectedStripPreset == preset),
                                    onClick = { selectedStripPreset = preset }
                                )
                                Text(preset, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onScanPreset(selectedStripPreset) },
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

                scannedText?.let { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Extracted Prescription Data:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MedicalPrimary)
                                IconButton(onClick = { onReadAloudText(text) }) {
                                    Icon(Icons.Filled.VolumeUp, contentDescription = "Read Aloud", tint = MedicalPrimary)
                                }
                            }

                            Surface(color = MedicalBackgroundLight, shape = RoundedCornerShape(12.dp)) {
                                Text(text = text, modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ScannedStripData(
    val brandName: String,
    val genericName: String,
    val dosage: String,
    val purpose: String,
    val sideEffects: String,
    val warnings: String,
    val manufacturer: String
)
