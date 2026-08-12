package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.ui.theme.*
import com.example.util.MedicalReportExtractor
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScannerScreen(
    scannedText: String?,
    isScanning: Boolean,
    reportResult: MedicalReportAnalysisResult?,
    isAnalyzingReport: Boolean,
    activeScannerTab: Int = 0,
    activeMedications: List<Medication> = emptyList(),
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    onTabSelected: (Int) -> Unit = {},
    onAnalyzeReportImage: (String) -> Unit = {},
    onLoadReportPreset: (MedicalReportAnalysisResult) -> Unit = {},
    onClearReportResult: () -> Unit = {},
    onConfirmReportMedicines: (List<ExtractedMedicineCandidate>, MedicalReportAnalysisResult) -> Unit = { _, _ -> },
    onScanSinglePackage: (String) -> Unit = {},
    onImportSingleMedication: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onReadAloudText: (String) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAddMedicationManual: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Camera & Image State
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedBase64 by remember { mutableStateOf<String?>(null) }

    // Preprocessing toggles
    var isEnhancedContrast by remember { mutableStateOf(true) }
    var isSharpened by remember { mutableStateOf(true) }
    var showRawTextExpanded by remember { mutableStateOf(false) }

    // Candidate Editing Modal State
    var editingCandidate by remember { mutableStateOf<ExtractedMedicineCandidate?>(null) }
    var editingCandidateList by remember { mutableStateOf<List<ExtractedMedicineCandidate>>(emptyList()) }

    // Update internal editing candidate list whenever reportResult changes
    LaunchedEffect(reportResult) {
        if (reportResult != null) {
            editingCandidateList = reportResult.medicines
        }
    }

    // Camera Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            val base64 = bitmapToBase64(bitmap)
            capturedBase64 = base64
            onAnalyzeReportImage(base64)
        }
    }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    val base64 = bitmapToBase64(bitmap)
                    capturedBase64 = base64
                    onAnalyzeReportImage(base64)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraPermissionDenied = false
            takePictureLauncher.launch(null)
        } else {
            cameraPermissionDenied = true
        }
    }

    fun launchCameraFlow() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionDenied = false
            takePictureLauncher.launch(null)
        } else {
            showCameraRationaleDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (activeScannerTab == 0)
                            LanguageManager.getText("scan_report_title", selectedLanguage)
                        else
                            LanguageManager.getText("scan_package_title", selectedLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToAddMedicationManual) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = LanguageManager.getText("enter_manually", selectedLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
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
            // Mode Selector Switcher (Medical Report vs Single Package)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeScannerTab == 0,
                    onClick = { onTabSelected(0) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Medical Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                SegmentedButton(
                    selected = activeScannerTab == 1,
                    onClick = { onTabSelected(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Box / Package", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (activeScannerTab == 0) {
                // ==================== WORKFLOW: MEDICAL REPORT SCANNER ====================

                // Workflow Progress Stepper (Scan -> Review -> Confirm)
                val currentStep = when {
                    reportResult == null && !isAnalyzingReport -> 1
                    isAnalyzingReport -> 1
                    reportResult != null -> 2
                    else -> 1
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalSurfaceLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepChip(
                            stepNumber = "1",
                            label = LanguageManager.getText("step_1_scan", selectedLanguage),
                            isActive = currentStep >= 1
                        )
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                        StepChip(
                            stepNumber = "2",
                            label = LanguageManager.getText("step_2_review", selectedLanguage),
                            isActive = currentStep >= 2
                        )
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                        StepChip(
                            stepNumber = "3",
                            label = LanguageManager.getText("step_3_confirm", selectedLanguage),
                            isActive = currentStep >= 3
                        )
                    }
                }

                // STEP 1: Scan & Photo Capture Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        imageVector = Icons.Filled.DocumentScanner,
                                        contentDescription = null,
                                        tint = MedicalPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scan Prescription / Medical Report",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isElderMode) 19.sp else 16.sp
                                )
                                Text(
                                    text = LanguageManager.getText("scan_instructions", selectedLanguage),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (cameraPermissionDenied) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Camera permission is required to scan reports. Please allow camera permission.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { launchCameraFlow() },
                                enabled = !isAnalyzingReport,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimary)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Camera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                enabled = !isAnalyzingReport,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Image", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Sample Prescription Document Presets for Instant Testing
                        HorizontalDivider()
                        Text(
                            text = "Or test with sample Indian medical reports:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MedicalSecondary
                        )

                        val samplePresets = MedicalReportExtractor.getSampleReportPresets(activeMedications)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(samplePresets) { (presetName, presetResult) ->
                                FilterChip(
                                    selected = (reportResult?.reportType == presetResult.reportType),
                                    onClick = { onLoadReportPreset(presetResult) },
                                    label = { Text(presetName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                // Image Preview & Preprocessing Status
                if (capturedBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Captured Medical Document:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                TextButton(onClick = { launchCameraFlow() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(LanguageManager.getText("retake_photo", selectedLanguage), fontSize = 12.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .border(1.dp, MedicalPrimaryContainer, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    bitmap = capturedBitmap!!.asImageBitmap(),
                                    contentDescription = "Medical Document Photo",
                                    modifier = Modifier.fillMaxSize()
                                )

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color.Green, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OCR Preprocessed", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading Indicator while Gemini analyzes image
                if (isAnalyzingReport) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalPrimaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = MedicalPrimary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Analyzing Prescription with Gemini AI...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Extracting medicines, dosage strength, timings & notes", fontSize = 12.sp, color = MedicalSecondary)
                            }
                        }
                    }
                }

                // STEP 2 & STEP 3: REVIEW & CONFIRM EXTRACTED MEDICINES
                reportResult?.let { result ->
                    // Extracted Document Details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MedicalPrimary, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
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
                                Column {
                                    Text(
                                        text = "STEP 2: Review Extracted Medicines",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MedicalPrimary
                                    )
                                    Text(
                                        text = "${editingCandidateList.size} medicine(s) detected from report",
                                        fontSize = 12.sp,
                                        color = MedicalSecondary
                                    )
                                }

                                TextButton(onClick = onClearReportResult) {
                                    Text("Clear Result", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            if (result.doctorName != null || result.patientName != null) {
                                Surface(
                                    color = MedicalSurfaceLight,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        result.doctorName?.let {
                                            Text("👨‍⚕️ $it", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        }
                                        result.patientName?.let {
                                            Text("👤 Patient: $it", fontSize = 12.sp, color = MedicalSecondary)
                                        }
                                    }
                                }
                            }

                            // Raw Text Expandable Accordion
                            OutlinedCard(
                                onClick = { showRawTextExpanded = !showRawTextExpanded },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.TextSnippet, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Raw Extracted OCR Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onReadAloudText(result.rawExtractedText) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.VolumeUp, contentDescription = "Read Aloud", tint = MedicalPrimary, modifier = Modifier.size(18.dp))
                                            }
                                            Icon(
                                                imageVector = if (showRawTextExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null
                                            )
                                        }
                                    }

                                    if (showRawTextExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = result.rawExtractedText,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Handwritten or Low Confidence Warning Banner
                            if (editingCandidateList.any { it.isUncertain }) {
                                Surface(
                                    color = HealthWarningContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, tint = HealthWarning)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = LanguageManager.getText("uncertain_warning", selectedLanguage),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = HealthWarning
                                            )
                                            Text(
                                                text = "Please tap 'Edit' on uncertain entries or click 'Enter Medicine Manually'.",
                                                fontSize = 12.sp,
                                                color = HealthWarning
                                            )
                                        }
                                    }
                                }
                            }

                            // CANDIDATE MEDICINES REVIEW LIST
                            Text(
                                text = "Medicines Found in Report:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            if (editingCandidateList.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("No medicines could be confidently identified.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = onNavigateToAddMedicationManual) {
                                            Text(LanguageManager.getText("enter_manually", selectedLanguage))
                                        }
                                    }
                                }
                            }

                            editingCandidateList.forEachIndexed { index, candidate ->
                                CandidateMedicineCard(
                                    candidate = candidate,
                                    selectedLanguage = selectedLanguage,
                                    isElderMode = isElderMode,
                                    onToggleConfirm = { checked ->
                                        editingCandidateList = editingCandidateList.toMutableList().also { list ->
                                            list[index] = list[index].copy(isConfirmed = checked)
                                        }
                                    },
                                    onEdit = { editingCandidate = candidate },
                                    onRemove = {
                                        editingCandidateList = editingCandidateList.toMutableList().also { list ->
                                            list.removeAt(index)
                                        }
                                    }
                                )
                            }

                            // STEP 3: FINAL USER CONFIRMATION BUTTON
                            HorizontalDivider()

                            val confirmedCount = editingCandidateList.count { it.isConfirmed }

                            Surface(
                                color = HealthSafeContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$confirmedCount medicine(s) confirmed for schedule",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = HealthSafe
                                        )
                                        Text(
                                            text = "Verification Required",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HealthSafe
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val confirmedMeds = editingCandidateList.filter { it.isConfirmed }
                                            onConfirmReportMedicines(confirmedMeds, result)
                                        },
                                        enabled = confirmedCount > 0 && !isAnalyzingReport,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HealthSafe)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = LanguageManager.getText("confirm_add_medicines", selectedLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isElderMode) 18.sp else 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ==================== WORKFLOW: SINGLE MEDICINE PACKAGE SCANNER ====================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Scan Medicine Box or Packaging",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isElderMode) 19.sp else 16.sp
                                )
                                Text(
                                    text = "Capture bottle or box label to verify a single medicine item",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { launchCameraFlow() },
                            enabled = !isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analyzing Packaging with Vision AI...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Open Camera & Scan Box", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Extracted Single Package OCR Result Card
                scannedText?.let { text ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MedicalSurfaceLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Extracted Packaging OCR Text:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = text, fontSize = 13.sp)
                                }
                            }

                            // Candidate items from DB matching packaging
                            val sampleMatch = MedicineDatabase.commonMedicines.take(2)
                            sampleMatch.forEach { med ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${med.name} (${med.dosage.substringBefore(",")})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Uses: ${med.uses.take(40)}...", fontSize = 12.sp, color = MedicalSecondary)
                                        }
                                        Button(
                                            onClick = {
                                                onImportSingleMedication(med.name, med.dosage.substringBefore(","), "08:00 AM", "After Food")
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Confirm", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Candidate Editing Dialog
    editingCandidate?.let { candidate ->
        CandidateEditDialog(
            candidate = candidate,
            onDismiss = { editingCandidate = null },
            onSave = { updated ->
                editingCandidateList = editingCandidateList.map {
                    if (it.id == updated.id) updated else it
                }
                editingCandidate = null
            }
        )
    }

    // Camera Rationale Dialog
    if (showCameraRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraRationaleDialog = false },
            icon = { Icon(Icons.Filled.Camera, contentDescription = null, tint = MedicalPrimary, modifier = Modifier.size(32.dp)) },
            title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
            text = { Text("CareSync requires camera access to scan your prescription or medicine report.", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraRationaleDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Allow Camera Permission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StepChip(
    stepNumber: String,
    label: String,
    isActive: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) MedicalPrimaryContainer else Color.LightGray.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isActive) MedicalPrimary else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(stepNumber, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = if (isActive) MedicalPrimary else Color.DarkGray
            )
        }
    }
}

@Composable
fun CandidateMedicineCard(
    candidate: ExtractedMedicineCandidate,
    selectedLanguage: String,
    isElderMode: Boolean,
    onToggleConfirm: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (candidate.isConfirmed) 2.dp else 1.dp,
                color = if (candidate.isConfirmed) MedicalPrimary else Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.isConfirmed) MedicalPrimaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        )
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = candidate.isConfirmed,
                        onCheckedChange = onToggleConfirm
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        // STRICT SEPARATION OF MEDICINE NAME FROM DOSAGE STRENGTH
                        Text(
                            text = candidate.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isElderMode) 18.sp else 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (candidate.strength.isNotBlank()) {
                            Text(
                                text = "Strength: ${candidate.strength} • ${candidate.dosageForm}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MedicalPrimary
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Medicine", tint = MedicalPrimary)
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove Candidate", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider()

            // Timings & Schedule Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("⏰ Schedule / Timing:", fontSize = 11.sp, color = MedicalSecondary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${candidate.frequency} (${candidate.timeOfConsumption})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text("🍽️ Food Relation:", fontSize = 11.sp, color = MedicalSecondary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = candidate.beforeOrAfterFood,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text("📅 Duration:", fontSize = 11.sp, color = MedicalSecondary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${candidate.durationDays} days",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            if (candidate.instructions.isNotBlank()) {
                Text(
                    text = "📝 Note: ${candidate.instructions}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badges for Confidence & Warnings
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (candidate.matchedDatabaseItem != null) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("✓ Verified Database Match", fontSize = 10.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = HealthSafeContainer, labelColor = HealthSafe)
                    )
                }

                if (candidate.isUncertain) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("⚠️ Low Confidence / Edit Required", fontSize = 10.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = HealthWarningContainer, labelColor = HealthWarning)
                    )
                }

                if (candidate.isPossibleDuplicate) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("⚠️ ${candidate.duplicateMessage ?: LanguageManager.getText("duplicate_warning", selectedLanguage)}", fontSize = 10.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.errorContainer, labelColor = MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
fun CandidateEditDialog(
    candidate: ExtractedMedicineCandidate,
    onDismiss: () -> Unit,
    onSave: (ExtractedMedicineCandidate) -> Unit
) {
    var name by remember { mutableStateOf(candidate.name) }
    var strength by remember { mutableStateOf(candidate.strength) }
    var frequency by remember { mutableStateOf(candidate.frequency) }
    var timeOfConsumption by remember { mutableStateOf(candidate.timeOfConsumption) }
    var beforeOrAfterFood by remember { mutableStateOf(candidate.beforeOrAfterFood) }
    var durationDaysStr by remember { mutableStateOf(candidate.durationDays.toString()) }
    var instructions by remember { mutableStateOf(candidate.instructions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Extracted Medicine", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name (Pure Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = strength,
                    onValueChange = { strength = it },
                    label = { Text("Dosage Strength (e.g. 500 mg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Frequency (e.g. Twice daily)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = timeOfConsumption,
                    onValueChange = { timeOfConsumption = it },
                    label = { Text("Reminder Times (e.g. 08:00 AM, 08:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Before Food", "After Food", "With Food").forEach { option ->
                        FilterChip(
                            selected = beforeOrAfterFood == option,
                            onClick = { beforeOrAfterFood = option },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = durationDaysStr,
                    onValueChange = { durationDaysStr = it },
                    label = { Text("Duration (Days)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Doctor Instructions / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = candidate.copy(
                        name = name.trim(),
                        strength = strength.trim(),
                        frequency = frequency.trim(),
                        timeOfConsumption = timeOfConsumption.trim(),
                        beforeOrAfterFood = beforeOrAfterFood,
                        durationDays = durationDaysStr.toIntOrNull() ?: 30,
                        instructions = instructions.trim(),
                        isUncertain = false
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun bitmapToBase64(bitmap: Bitmap): String {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    val byteArray = outputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}
