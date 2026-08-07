package com.example.data

import com.example.api.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MedicationRepository(private val dao: MedicationDao) {

    val activeMedications: Flow<List<Medication>> = dao.getAllActiveMedications()
    val medicationLogs: Flow<List<MedicationLog>> = dao.getAllLogs()
    val familyMembers: Flow<List<FamilyMember>> = dao.getAllFamilyMembers()
    val drugInteractions: Flow<List<DrugInteraction>> = dao.getAllInteractions()

    suspend fun addMedication(medication: Medication): Long {
        return dao.insertMedication(medication)
    }

    suspend fun updateMedication(medication: Medication) {
        dao.updateMedication(medication)
    }

    suspend fun deleteMedication(medication: Medication) {
        dao.deleteMedication(medication)
    }

    suspend fun markAsTaken(id: Long) {
        val med = dao.getMedicationById(id) ?: return
        dao.markMedicationTaken(id, true, System.currentTimeMillis())

        val log = MedicationLog(
            medicationId = id,
            medicationName = med.name,
            dosage = med.dosage,
            dateScheduled = "2026-08-06",
            timeScheduled = med.timeOfConsumption,
            status = "TAKEN",
            timestampTaken = System.currentTimeMillis()
        )
        dao.insertLog(log)
    }

    suspend fun refillStock(id: Long, addedCount: Int) {
        dao.refillStock(id, addedCount)
    }

    suspend fun addFamilyMember(member: FamilyMember) {
        dao.insertFamilyMember(member)
    }

    suspend fun deleteFamilyMember(member: FamilyMember) {
        dao.deleteFamilyMember(member)
    }

    // --- Gemini AI Features ---

    suspend fun checkDrugInteractions(medications: List<Medication>): String {
        if (medications.size < 2) {
            return "At least 2 active medications are required to analyze interactions."
        }

        val medNames = medications.joinToString(", ") { "${it.name} (${it.dosage})" }
        val prompt = """
            Analyze potential drug-drug interactions for the following medications: $medNames.
            Format your response clearly using sections:
            1. Risk Level: Choose one of [SAFE 🟢], [MODERATE RISK 🟡], [HIGH RISK 🔴].
            2. Summary of Interaction: A 2-sentence simple human explanation.
            3. Mechanism & Symptoms: What happens when taken together.
            4. Recommended Action: What the patient should do or ask their doctor.
        """.trimIndent()

        val systemInstruction = "You are CareSync AI, an expert clinical pharmacology assistant providing clear, patient-friendly medication safety guidance."
        val response = GeminiClient.generateText(prompt, systemInstruction)

        // Save to DB
        val risk = when {
            response.contains("HIGH RISK", ignoreCase = true) -> "HIGH"
            response.contains("MODERATE RISK", ignoreCase = true) -> "MODERATE"
            else -> "SAFE"
        }

        val interaction = DrugInteraction(
            drugA = medications.getOrNull(0)?.name ?: "",
            drugB = medications.getOrNull(1)?.name ?: "",
            riskLevel = risk,
            summary = response.take(150) + "...",
            mechanism = response,
            recommendation = "Consult your prescribing doctor before altering schedule."
        )
        dao.insertInteraction(interaction)

        return response
    }

    suspend fun askMedicalChatbot(query: String, currentMeds: List<Medication>): String {
        val medContext = if (currentMeds.isNotEmpty()) {
            "Patient's Active Medications: " + currentMeds.joinToString(", ") { "${it.name} ${it.dosage} (${it.beforeOrAfterFood})" }
        } else {
            "No active medications currently listed."
        }

        val prompt = """
            $medContext
            User Query: "$query"

            Please answer concisely and accurately in patient-friendly terms with medical guidelines and safety warnings where appropriate.
        """.trimIndent()

        val system = "You are CareSync AI Chatbot, a friendly, accurate healthcare medication safety guide. Always include a brief reminder that advice is for informational purposes and to consult a doctor for clinical decisions."
        return GeminiClient.generateText(prompt, system)
    }

    suspend fun scanPrescriptionImage(imageBase64: String): String {
        val prompt = """
            Examine this prescription image. Extract all prescribed medications and return a structured list with:
            - Medication Name
            - Dosage (e.g., 500 mg)
            - Tablet Count / Quantity
            - Frequency & Timing (e.g. Morning, Evening)
            - Food instructions (Before or After Food)
            - Special Doctor Notes
        """.trimIndent()

        val system = "You are CareSync Vision AI. Extract accurate prescription details for automated medication intake setup."
        return GeminiClient.analyzeImageWithText(prompt, imageBase64, systemInstruction = system)
    }

    // --- Seed Demo Data if Database Empty ---
    suspend fun seedInitialDataIfEmpty() {
        val currentMeds = dao.getAllActiveMedications().first()
        if (currentMeds.isEmpty()) {
            val defaultMeds = listOf(
                Medication(
                    name = "Metformin",
                    dosage = "500 mg",
                    totalTablets = 60,
                    remainingTablets = 14,
                    startDate = "2026-08-01",
                    endDate = "2026-08-30",
                    timeOfConsumption = "08:00 AM, 08:00 PM",
                    beforeOrAfterFood = "After Food",
                    instructions = "Take with morning & evening meals for type-2 diabetes control",
                    category = "Prescription",
                    prescribedBy = "Dr. Robert Vance"
                ),
                Medication(
                    name = "Lisinopril",
                    dosage = "10 mg",
                    totalTablets = 30,
                    remainingTablets = 5,
                    startDate = "2026-08-01",
                    endDate = "2026-08-30",
                    timeOfConsumption = "08:00 AM",
                    beforeOrAfterFood = "Before Food",
                    instructions = "Take once daily for blood pressure control",
                    category = "Prescription",
                    prescribedBy = "Dr. Sarah Chen"
                ),
                Medication(
                    name = "Atorvastatin",
                    dosage = "20 mg",
                    totalTablets = 30,
                    remainingTablets = 22,
                    startDate = "2026-08-01",
                    endDate = "2026-08-30",
                    timeOfConsumption = "09:00 PM",
                    beforeOrAfterFood = "After Food",
                    instructions = "Take before bedtime for cholesterol balance",
                    category = "Prescription",
                    prescribedBy = "Dr. Sarah Chen"
                ),
                Medication(
                    name = "Multivitamin Complex",
                    dosage = "1 Tablet",
                    totalTablets = 90,
                    remainingTablets = 75,
                    startDate = "2026-08-01",
                    endDate = "2026-11-01",
                    timeOfConsumption = "01:00 PM",
                    beforeOrAfterFood = "After Food",
                    instructions = "Daily health supplement",
                    category = "Supplement",
                    prescribedBy = "Self"
                )
            )

            for (m in defaultMeds) {
                dao.insertMedication(m)
            }

            // Seed Family Members
            val family = listOf(
                FamilyMember(
                    name = "Eleanor Mitchell",
                    relation = "Grandmother",
                    phone = "+1 (555) 234-5678",
                    email = "eleanor.m@example.com",
                    activeMedicationsCount = 4,
                    adherenceRate = 95,
                    missedDosesCount = 0,
                    lastDoseStatus = "Taken Metformin (8:00 AM)",
                    isEmergencyContact = true
                ),
                FamilyMember(
                    name = "Arthur Mitchell",
                    relation = "Father",
                    phone = "+1 (555) 876-5432",
                    email = "arthur.m@example.com",
                    activeMedicationsCount = 2,
                    adherenceRate = 88,
                    missedDosesCount = 1,
                    lastDoseStatus = "Missed Lisinopril yesterday",
                    isEmergencyContact = true
                )
            )

            for (f in family) {
                dao.insertFamilyMember(f)
            }

            // Seed sample logs
            val sampleLogs = listOf(
                MedicationLog(
                    medicationId = 1,
                    medicationName = "Metformin",
                    dosage = "500 mg",
                    dateScheduled = "2026-08-06",
                    timeScheduled = "08:00 AM",
                    status = "TAKEN",
                    timestampTaken = System.currentTimeMillis() - 3600000
                ),
                MedicationLog(
                    medicationId = 2,
                    medicationName = "Lisinopril",
                    dosage = "10 mg",
                    dateScheduled = "2026-08-06",
                    timeScheduled = "08:00 AM",
                    status = "TAKEN",
                    timestampTaken = System.currentTimeMillis() - 3600000
                )
            )

            for (l in sampleLogs) {
                dao.insertLog(l)
            }

            // Seed sample interaction check
            dao.insertInteraction(
                DrugInteraction(
                    drugA = "Lisinopril",
                    drugB = "Spironolactone",
                    riskLevel = "MODERATE",
                    summary = "Combining ACE inhibitors with potassium-sparing diuretics may increase serum potassium levels.",
                    mechanism = "Both agents diminish renal potassium excretion, raising risk of hyperkalemia.",
                    recommendation = "Monitor electrolytes periodically with blood test."
                )
            )
        }
    }
}
