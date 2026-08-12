package com.example

import com.example.data.ExtractedMedicineCandidate
import com.example.data.Medication
import com.example.util.MedicalReportExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationDuplicationLogicTest {

    private fun createMedication(
        id: Long = 1,
        name: String,
        dosage: String,
        timeOfConsumption: String = "08:00 AM",
        beforeOrAfterFood: String = "After Food"
    ) = Medication(
        id = id,
        name = name,
        dosage = dosage,
        totalTablets = 30,
        remainingTablets = 30,
        startDate = "2026-08-01",
        endDate = "2026-08-30",
        timeOfConsumption = timeOfConsumption,
        beforeOrAfterFood = beforeOrAfterFood
    )

    @Test
    fun exact_name_and_strength_match_is_flagged_as_duplicate() {
        val existingMedications = listOf(
            createMedication(
                id = 1,
                name = "Amlodipine",
                dosage = "5 mg"
            )
        )

        val newCandidates = listOf(
            ExtractedMedicineCandidate(
                name = "Amlodipine",
                strength = "5 mg",
                dosageForm = "1 tablet",
                frequency = "Once daily"
            )
        )

        val result = MedicalReportExtractor.compareCandidatesWithExistingRecords(
            candidates = newCandidates,
            existingRecords = existingMedications
        )

        assertEquals(1, result.size)
        assertTrue("Exact name and strength match should be flagged as possible duplicate", result[0].isPossibleDuplicate)
        assertNotNull(result[0].duplicateMessage)
        assertTrue(result[0].duplicateMessage!!.contains("Matches existing record"))
    }

    @Test
    fun different_strength_same_name_passes_as_unique_record() {
        val existingMedications = listOf(
            createMedication(
                id = 1,
                name = "Amlodipine",
                dosage = "5 mg"
            )
        )

        val newCandidates = listOf(
            ExtractedMedicineCandidate(
                name = "Amlodipine",
                strength = "10 mg",
                dosageForm = "1 tablet",
                frequency = "Once daily"
            )
        )

        val result = MedicalReportExtractor.compareCandidatesWithExistingRecords(
            candidates = newCandidates,
            existingRecords = existingMedications
        )

        assertEquals(1, result.size)
        assertFalse("Different strength (10 mg vs 5 mg) should pass as a unique record", result[0].isPossibleDuplicate)
        assertNull(result[0].duplicateMessage)
    }

    @Test
    fun variation_in_frequency_or_timing_does_not_prevent_duplicate_flag_if_strength_matches() {
        val existingMedications = listOf(
            createMedication(
                id = 2,
                name = "Metformin",
                dosage = "500 mg",
                timeOfConsumption = "08:00 AM",
                beforeOrAfterFood = "After Food"
            )
        )

        val newCandidates = listOf(
            ExtractedMedicineCandidate(
                name = "Metformin",
                strength = "500 mg",
                dosageForm = "1 tablet",
                frequency = "Twice daily", // Different frequency
                timings = listOf("Morning", "Night"), // Different timings
                beforeOrAfterFood = "Before Food" // Different food relation
            )
        )

        val result = MedicalReportExtractor.compareCandidatesWithExistingRecords(
            candidates = newCandidates,
            existingRecords = existingMedications
        )

        assertEquals(1, result.size)
        assertTrue("Matches name and strength despite frequency/timing variations", result[0].isPossibleDuplicate)
    }

    @Test
    fun completely_different_medication_passes_as_unique_record() {
        val existingMedications = listOf(
            createMedication(
                id = 3,
                name = "Paracetamol",
                dosage = "650 mg"
            )
        )

        val newCandidates = listOf(
            ExtractedMedicineCandidate(
                name = "Aspirin",
                strength = "81 mg"
            )
        )

        val result = MedicalReportExtractor.compareCandidatesWithExistingRecords(
            candidates = newCandidates,
            existingRecords = existingMedications
        )

        assertEquals(1, result.size)
        assertFalse("Completely different medication should not be marked as duplicate", result[0].isPossibleDuplicate)
        assertNull(result[0].duplicateMessage)
    }

    @Test
    fun duplicate_within_same_scanned_report_is_flagged() {
        val existingMedications = emptyList<Medication>()

        val newCandidates = listOf(
            ExtractedMedicineCandidate(
                name = "Atorvastatin",
                strength = "20 mg"
            ),
            ExtractedMedicineCandidate(
                name = "Atorvastatin",
                strength = "20 mg"
            )
        )

        val result = MedicalReportExtractor.compareCandidatesWithExistingRecords(
            candidates = newCandidates,
            existingRecords = existingMedications
        )

        assertEquals(2, result.size)
        assertFalse("First occurrence in report should not be duplicate", result[0].isPossibleDuplicate)
        assertTrue("Second occurrence of same medicine in same report should be flagged as duplicate", result[1].isPossibleDuplicate)
        assertTrue(result[1].duplicateMessage!!.contains("multiple times in this report"))
    }

    @Test
    fun case_and_whitespace_insensitive_matching_for_name_and_strength() {
        val existingMedications = listOf(
            createMedication(
                id = 4,
                name = "  Lisinopril  ",
                dosage = "10 mg"
            )
        )

        val newCandidates = listOf(
            ExtractedMedicineCandidate(
                name = "LISINOPRIL",
                strength = "  10 MG  "
            )
        )

        val result = MedicalReportExtractor.compareCandidatesWithExistingRecords(
            candidates = newCandidates,
            existingRecords = existingMedications
        )

        assertEquals(1, result.size)
        assertTrue("Case and whitespace variations should still trigger duplicate warning", result[0].isPossibleDuplicate)
    }
}
