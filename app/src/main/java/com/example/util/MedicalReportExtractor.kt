package com.example.util

import android.util.Log
import com.example.data.ExtractedMedicineCandidate
import com.example.data.MedicalReportAnalysisResult
import com.example.data.Medication
import com.example.data.MedicineDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object MedicalReportExtractor {

    private const val TAG = "MedicalReportExtractor"

    /**
     * Parses raw response from Gemini Vision or fallback OCR text into a structured MedicalReportAnalysisResult
     */
    fun parseGeminiReportResponse(
        rawResponse: String,
        activeMedications: List<Medication> = emptyList()
    ): MedicalReportAnalysisResult {
        Log.d(TAG, "Parsing raw Gemini OCR response length: ${rawResponse.length}")

        // Attempt JSON extraction first
        val jsonStart = rawResponse.indexOf("{")
        val jsonEnd = rawResponse.lastIndexOf("}")

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            val jsonStr = rawResponse.substring(jsonStart, jsonEnd + 1)
            try {
                val jsonObject = JSONObject(jsonStr)
                val rawExtractedText = jsonObject.optString("rawExtractedText", rawResponse)
                val reportType = jsonObject.optString("reportType", "Doctor Prescription")
                val doctorName = jsonObject.optString("doctorName", "").ifBlank { null }
                val patientName = jsonObject.optString("patientName", "").ifBlank { null }
                val doctorNotes = jsonObject.optString("doctorNotes", "")
                val confidenceNote = jsonObject.optString("confidenceNote", "Processed via Vision AI")
                val requiresManualVerification = jsonObject.optBoolean("requiresManualVerification", false)

                val medicinesArray = jsonObject.optJSONArray("medicines") ?: JSONArray()
                val candidates = mutableListOf<ExtractedMedicineCandidate>()

                for (i in 0 until medicinesArray.length()) {
                    val medObj = medicinesArray.optJSONObject(i) ?: continue
                    val rawName = medObj.optString("name", "Unknown Medicine")
                    val rawStrength = medObj.optString("strength", "")
                    val dosageForm = medObj.optString("dosageForm", "1 tablet")
                    val frequency = medObj.optString("frequency", "Once daily")
                    
                    val timingsList = mutableListOf<String>()
                    val timingsJsonArray = medObj.optJSONArray("timings")
                    if (timingsJsonArray != null) {
                        for (j in 0 until timingsJsonArray.length()) {
                            timingsList.add(timingsJsonArray.optString(j))
                        }
                    }
                    if (timingsList.isEmpty()) {
                        timingsList.add("Morning")
                    }

                    val foodInstruction = medObj.optString("beforeOrAfterFood", "After Food")
                    val durationDays = medObj.optInt("durationDays", 30)
                    val instructions = medObj.optString("instructions", "")
                    val confidence = medObj.optString("confidence", "HIGH")
                    val isUncertain = medObj.optBoolean("isUncertain", confidence == "LOW" || confidence == "UNCERTAIN")

                    val candidate = buildCleanCandidate(
                        rawName = rawName,
                        rawStrength = rawStrength,
                        dosageForm = dosageForm,
                        frequency = frequency,
                        timings = timingsList,
                        foodInstruction = foodInstruction,
                        durationDays = durationDays,
                        instructions = instructions,
                        confidence = confidence,
                        isUncertain = isUncertain,
                        activeMedications = activeMedications
                    )
                    candidates.add(candidate)
                }

                if (candidates.isNotEmpty()) {
                    return MedicalReportAnalysisResult(
                        rawExtractedText = rawExtractedText,
                        reportType = reportType,
                        doctorName = doctorName,
                        patientName = patientName,
                        medicines = checkForDuplicates(candidates, activeMedications),
                        doctorNotes = doctorNotes,
                        confidenceNote = confidenceNote,
                        requiresManualVerification = requiresManualVerification
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON response from Gemini, falling back to heuristic regex text parser: ${e.message}")
            }
        }

        // Fallback Heuristic Text Parser for unstructured text
        return parseHeuristicTextReport(rawResponse, activeMedications)
    }

    /**
     * Cleans up and separates raw medicine name from dosage/strength if mixed together
     */
    fun buildCleanCandidate(
        rawName: String,
        rawStrength: String,
        dosageForm: String,
        frequency: String,
        timings: List<String>,
        foodInstruction: String,
        durationDays: Int,
        instructions: String,
        confidence: String,
        isUncertain: Boolean,
        activeMedications: List<Medication> = emptyList()
    ): ExtractedMedicineCandidate {
        var cleanName = rawName.trim()
        var cleanStrength = rawStrength.trim()

        // Regex to separate strength if stuck inside the name (e.g., "Metformin 500mg" -> "Metformin" & "500 mg")
        val dosageRegex = Regex("(?i)\\b(\\d+\\.?\\d*\\s*(?:mg|g|mcg|ml|iu|tablet|capsule|mg/ml))\\b")
        val match = dosageRegex.find(cleanName)
        if (match != null) {
            if (cleanStrength.isBlank()) {
                cleanStrength = match.value
            }
            cleanName = cleanName.replace(match.value, "").trim()
        }

        // Clean trailing symbols or common instructions from name
        cleanName = cleanName.replace(Regex("(?i)\\b(tab|cap|tablet|capsule|syrup|injection|once|twice|daily|tds|bd|od)\\b"), "")
            .replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            .trim()

        if (cleanName.isBlank()) cleanName = "Prescription Medicine"

        // Map timing string to formatted standard reminder times
        val formattedTimes = deriveTimeOfConsumption(frequency, timings)

        // Lookup matching item in trusted database
        val matchedDb = MedicineDatabase.commonMedicines.find {
            it.name.trim().equals(cleanName, ignoreCase = true) ||
            it.brandName.contains(cleanName, ignoreCase = true) ||
            it.genericName.contains(cleanName, ignoreCase = true)
        }

        val finalConfidence = if (matchedDb != null && !isUncertain) "HIGH" else confidence

        return ExtractedMedicineCandidate(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            strength = cleanStrength.ifBlank { matchedDb?.dosage?.substringBefore(",") ?: "As prescribed" },
            dosageForm = dosageForm,
            frequency = frequency,
            timings = timings,
            timeOfConsumption = formattedTimes,
            beforeOrAfterFood = foodInstruction,
            durationDays = durationDays,
            instructions = instructions.ifBlank { matchedDb?.uses ?: "Take as prescribed by doctor." },
            confidence = finalConfidence,
            isUncertain = isUncertain || finalConfidence == "LOW" || finalConfidence == "UNCERTAIN",
            uncertaintyReason = if (isUncertain) "Handwriting or low text clarity detected" else null,
            matchedDatabaseItem = matchedDb,
            isConfirmed = true
        )
    }

    /**
     * Map dosage frequency and timings to standard clock time strings
     */
    fun deriveTimeOfConsumption(frequency: String, timings: List<String>): String {
        val timingSet = mutableSetOf<String>()

        val lowerFreq = frequency.lowercase()
        val lowerTimings = timings.map { it.lowercase() }

        if (lowerTimings.any { it.contains("morning") || it.contains("breakfast") } || lowerFreq.contains("morning")) {
            timingSet.add("08:00 AM")
        }
        if (lowerTimings.any { it.contains("afternoon") || it.contains("lunch") } || lowerFreq.contains("afternoon")) {
            timingSet.add("02:00 PM")
        }
        if (lowerTimings.any { it.contains("evening") || it.contains("dinner") } || lowerFreq.contains("evening")) {
            timingSet.add("08:00 PM")
        }
        if (lowerTimings.any { it.contains("night") || it.contains("bed") } || lowerFreq.contains("night") || lowerFreq.contains("bedtime")) {
            timingSet.add("10:00 PM")
        }

        if (timingSet.isEmpty()) {
            if (lowerFreq.contains("twice") || lowerFreq.contains("2 times") || lowerFreq.contains("bd") || lowerFreq.contains("bid")) {
                timingSet.add("08:00 AM")
                timingSet.add("08:00 PM")
            } else if (lowerFreq.contains("thrice") || lowerFreq.contains("3 times") || lowerFreq.contains("tds") || lowerFreq.contains("tid")) {
                timingSet.add("08:00 AM")
                timingSet.add("02:00 PM")
                timingSet.add("08:00 PM")
            } else if (lowerFreq.contains("four") || lowerFreq.contains("qid")) {
                timingSet.add("08:00 AM")
                timingSet.add("01:00 PM")
                timingSet.add("06:00 PM")
                timingSet.add("10:00 PM")
            } else {
                timingSet.add("08:00 AM")
            }
        }

        return timingSet.sorted().joinToString(", ")
    }

    /**
     * Compares newly extracted medication candidates with existing medication records from Firestore/Room,
     * specifically checking name and strength, and triggering a 'Possible duplicate' warning UI if matches are found.
     */
    fun compareCandidatesWithExistingRecords(
        candidates: List<ExtractedMedicineCandidate>,
        existingRecords: List<Medication>
    ): List<ExtractedMedicineCandidate> {
        val seenInReport = mutableSetOf<String>()

        return candidates.map { candidate ->
            val normName = candidate.name.trim().lowercase()
            val normStrength = candidate.strength.trim().lowercase()

            var isDup = false
            var dupMsg: String? = null

            // Compare with existing records in database / Firestore checking specifically name AND strength
            val existingMatch = existingRecords.find { existing ->
                val existingName = existing.name.trim().lowercase()
                val existingDosage = existing.dosage.trim().lowercase()

                val nameMatches = existingName == normName ||
                    (existingName.contains(normName) && normName.length >= 3) ||
                    (normName.contains(existingName) && existingName.length >= 3)

                val strengthMatches = normStrength.isBlank() ||
                    existingDosage.isBlank() ||
                    existingDosage.contains(normStrength) ||
                    normStrength.contains(existingDosage)

                nameMatches && strengthMatches
            }

            if (existingMatch != null) {
                isDup = true
                dupMsg = "Possible duplicate: Matches existing record (${existingMatch.name} - ${existingMatch.dosage})"
            } else if (normName in seenInReport) {
                isDup = true
                dupMsg = "Possible duplicate: Listed multiple times in this report"
            } else {
                seenInReport.add(normName)
            }

            candidate.copy(
                isPossibleDuplicate = isDup,
                duplicateMessage = dupMsg
            )
        }
    }

    /**
     * Duplicate detection against both internal list and active medications
     */
    fun checkForDuplicates(
        candidates: List<ExtractedMedicineCandidate>,
        activeMedications: List<Medication>
    ): List<ExtractedMedicineCandidate> {
        return compareCandidatesWithExistingRecords(candidates, activeMedications)
    }

    /**
     * Heuristic parser for raw unstructured text
     */
    private fun parseHeuristicTextReport(
        text: String,
        activeMedications: List<Medication>
    ): MedicalReportAnalysisResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidates = mutableListOf<ExtractedMedicineCandidate>()

        val knownMeds = MedicineDatabase.commonMedicines

        for (med in knownMeds) {
            if (text.contains(med.name, ignoreCase = true)) {
                val candidate = buildCleanCandidate(
                    rawName = med.name,
                    rawStrength = med.dosage.substringBefore(","),
                    dosageForm = "1 tablet",
                    frequency = "Once daily",
                    timings = listOf("Morning"),
                    foodInstruction = "After Food",
                    durationDays = 30,
                    instructions = med.uses,
                    confidence = "HIGH",
                    isUncertain = false,
                    activeMedications = activeMedications
                )
                candidates.add(candidate)
            }
        }

        val finalCandidates = if (candidates.isNotEmpty()) {
            checkForDuplicates(candidates, activeMedications)
        } else {
            // Unclear or unknown prescription text
            listOf(
                ExtractedMedicineCandidate(
                    name = "Unclear Medication Entry",
                    strength = "Requires verification",
                    confidence = "UNCERTAIN",
                    isUncertain = true,
                    uncertaintyReason = "We couldn't confidently read part of this prescription text.",
                    isConfirmed = false
                )
            )
        }

        return MedicalReportAnalysisResult(
            rawExtractedText = text,
            reportType = "Prescription Report",
            medicines = finalCandidates,
            requiresManualVerification = finalCandidates.any { it.isUncertain },
            confidenceNote = if (candidates.isNotEmpty()) "Extracted via smart keyword analysis" else "Uncertain text extraction"
        )
    }

    /**
     * Returns built-in sample reports for testing and instant preview
     */
    fun getSampleReportPresets(activeMedications: List<Medication> = emptyList()): List<Pair<String, MedicalReportAnalysisResult>> {
        val preset1 = MedicalReportAnalysisResult(
            rawExtractedText = """
                CARE HOSPITAL & HEART CENTER
                Doctor: Dr. A. Sharma (MD, Cardiology)
                Patient: Ramesh Kumar (Age: 62)
                Date: 10-Aug-2026
                
                Rx (Prescription):
                1. Tab. Amlodipine 5 mg - 1 tablet Morning after breakfast (30 days)
                2. Tab. Metformin 500 mg - 1 tablet Morning & Evening after meals (30 days)
                3. Tab. Atorvastatin 20 mg - 1 tablet at Night before bedtime (30 days)
                
                Note: Check BP and blood sugar after 2 weeks.
            """.trimIndent(),
            reportType = "Doctor Prescription (Cardiology)",
            doctorName = "Dr. A. Sharma (MD, Cardiology)",
            patientName = "Ramesh Kumar",
            doctorNotes = "Check BP and blood sugar after 2 weeks.",
            confidenceNote = "High confidence printed prescription",
            requiresManualVerification = false,
            medicines = checkForDuplicates(
                listOf(
                    buildCleanCandidate("Amlodipine", "5 mg", "1 tablet", "Once daily", listOf("Morning"), "After Food", 30, "Take for hypertension control", "HIGH", false, activeMedications),
                    buildCleanCandidate("Metformin", "500 mg", "1 tablet", "Twice daily", listOf("Morning", "Evening"), "After Food", 30, "Take for blood glucose balance", "HIGH", false, activeMedications),
                    buildCleanCandidate("Atorvastatin", "20 mg", "1 tablet", "Once daily", listOf("Night"), "After Food", 30, "Take at bedtime for cholesterol", "HIGH", false, activeMedications)
                ),
                activeMedications
            )
        )

        val preset2 = MedicalReportAnalysisResult(
            rawExtractedText = """
                MAX HEALTHCARE - DISCHARGE SUMMARY
                Department: Internal Medicine
                Patient: Sunita Devi (Age: 55)
                
                Discharge Medications:
                1. Tab Pantoprazole 40 mg - 1 tab Morning BEFORE FOOD (14 days)
                2. Tab Paracetamol 650 mg - 1 tab Morning, Afternoon & Evening AFTER FOOD (5 days)
                3. Cap Amoxicillin 500 mg - 1 cap Twice daily AFTER FOOD (7 days)
                4. Tab Multivitamin - 1 tab Once daily AFTER FOOD (30 days)
            """.trimIndent(),
            reportType = "Hospital Discharge Prescription",
            doctorName = "Dr. S. Mukherjee",
            patientName = "Sunita Devi",
            doctorNotes = "Drink plenty of water and rest.",
            confidenceNote = "Printed Hospital Discharge Summary",
            requiresManualVerification = false,
            medicines = checkForDuplicates(
                listOf(
                    buildCleanCandidate("Pantoprazole", "40 mg", "1 tablet", "Once daily", listOf("Morning"), "Before Food", 14, "Take on empty stomach", "HIGH", false, activeMedications),
                    buildCleanCandidate("Paracetamol", "650 mg", "1 tablet", "Thrice daily", listOf("Morning", "Afternoon", "Evening"), "After Food", 5, "Take for fever and discomfort", "HIGH", false, activeMedications),
                    buildCleanCandidate("Amoxicillin", "500 mg", "1 capsule", "Twice daily", listOf("Morning", "Evening"), "After Food", 7, "Complete 7-day antibiotic course", "HIGH", false, activeMedications),
                    buildCleanCandidate("Multivitamin Complex", "1 tablet", "1 tablet", "Once daily", listOf("Morning"), "After Food", 30, "Daily health supplement", "HIGH", false, activeMedications)
                ),
                activeMedications
            )
        )

        val preset3 = MedicalReportAnalysisResult(
            rawExtractedText = """
                CITY CLINIC
                Rx:
                1. Lisinopril 10mg - 1 tab OD Morning
                2. [Unclear Handwriting] - 1 tab Afternoon (UNCERTAIN)
                3. Aspirin 81mg - 1 tab Night
            """.trimIndent(),
            reportType = "Handwritten Clinic Slip",
            doctorName = "Dr. R. Gupta",
            patientName = "Patient",
            doctorNotes = "Review in 10 days.",
            confidenceNote = "Handwritten prescription with 1 uncertain entry",
            requiresManualVerification = true,
            medicines = checkForDuplicates(
                listOf(
                    buildCleanCandidate("Lisinopril", "10 mg", "1 tablet", "Once daily", listOf("Morning"), "Before Food", 30, "For blood pressure control", "HIGH", false, activeMedications),
                    ExtractedMedicineCandidate(
                        id = UUID.randomUUID().toString(),
                        name = "Unclear Prescription Entry",
                        strength = "Check doctor slip",
                        dosageForm = "1 tablet",
                        frequency = "Once daily",
                        timings = listOf("Afternoon"),
                        timeOfConsumption = "02:00 PM",
                        beforeOrAfterFood = "After Food",
                        durationDays = 10,
                        instructions = "Handwriting is unclear. Please edit or re-verify with your doctor.",
                        confidence = "UNCERTAIN",
                        isUncertain = true,
                        uncertaintyReason = "Handwriting in middle line could not be read confidently.",
                        isConfirmed = false
                    ),
                    buildCleanCandidate("Aspirin", "81 mg", "1 tablet", "Once daily", listOf("Night"), "After Food", 30, "Cardioprotection baby aspirin", "HIGH", false, activeMedications)
                ),
                activeMedications
            )
        )

        return listOf(
            "Printed Cardiology Prescription (3 Meds)" to preset1,
            "Hospital Discharge Summary (4 Meds)" to preset2,
            "Handwritten Prescription (Uncertain Entry)" to preset3
        )
    }
}
