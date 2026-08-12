package com.example.data

import java.util.UUID

data class ExtractedMedicineCandidate(
    val id: String = UUID.randomUUID().toString(),
    var name: String,                  // Pure medicine name (e.g. "Amlodipine")
    var strength: String = "",          // Dosage strength (e.g. "5 mg")
    var dosageForm: String = "1 tablet", // Unit (e.g. "1 tablet")
    var frequency: String = "Once daily", // Intake frequency (e.g. "Twice daily")
    var timings: List<String> = listOf("Morning"), // Schedule slots
    var timeOfConsumption: String = "08:00 AM", // Formatted times (e.g. "08:00 AM, 08:00 PM")
    var beforeOrAfterFood: String = "After Food", // Food relation
    var durationDays: Int = 30,
    var instructions: String = "",
    var confidence: String = "HIGH",     // "HIGH", "MEDIUM", "LOW", "UNCERTAIN"
    var isUncertain: Boolean = false,
    var uncertaintyReason: String? = null,
    var isPossibleDuplicate: Boolean = false,
    var duplicateMessage: String? = null,
    var matchedDatabaseItem: MedicineInfo? = null,
    var isConfirmed: Boolean = true
)

data class MedicalReportAnalysisResult(
    val reportId: String = UUID.randomUUID().toString(),
    val rawExtractedText: String,
    val reportType: String = "Doctor Prescription",
    val doctorName: String? = null,
    val patientName: String? = null,
    val medicines: List<ExtractedMedicineCandidate>,
    val doctorNotes: String = "",
    val confidenceNote: String = "",
    val requiresManualVerification: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class MedicalReportRecord(
    val reportId: String = UUID.randomUUID().toString(),
    val userId: String,
    val reportType: String,
    val extractedText: String,
    val medicineCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
