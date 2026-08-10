package com.example.util

import com.example.data.DoseItem
import com.example.data.Medication
import com.example.data.MedicationLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AdherenceData(
    val percentage: Int?,          // null if totalDue == 0 (no history or no due doses yet)
    val totalDue: Int,             // number of doses that were due (taken + missed)
    val takenCount: Int,           // number of doses marked as taken
    val missedCount: Int,          // number of doses missed
    val upcomingCount: Int         // number of future scheduled doses not due yet
)

object AdherenceCalculator {

    fun calculateTodayDoseItems(
        medications: List<Medication>,
        logs: List<MedicationLog>,
        dateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ): List<DoseItem> {
        val items = mutableListOf<DoseItem>()
        val calendar = Calendar.getInstance()
        val currentMinutesFromMidnight = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        for (med in medications) {
            if (!med.isActive) continue

            val rawTimeTokens = med.timeOfConsumption.split(",", ";", "&", "/").map { it.trim() }.filter { it.isNotBlank() }
            val timeTokens = if (rawTimeTokens.isNotEmpty()) rawTimeTokens else listOf(med.timeOfConsumption.ifBlank { "08:00 AM" })

            for (slotToken in timeTokens) {
                val slotMinutes = parseSingleTimeSlotMinutes(slotToken)

                val matchingLog = logs.find { log ->
                    log.medicationId == med.id &&
                    log.dateScheduled == dateStr &&
                    (log.timeScheduled.trim().equals(slotToken, ignoreCase = true) || parseSingleTimeSlotMinutes(log.timeScheduled) == slotMinutes)
                }

                val status = when {
                    matchingLog?.status?.uppercase(Locale.ROOT) == "TAKEN" -> "TAKEN"
                    matchingLog?.status?.uppercase(Locale.ROOT) == "MISSED" -> "MISSED"
                    slotMinutes <= currentMinutesFromMidnight -> "MISSED"
                    else -> "UPCOMING"
                }

                val isDue = status == "TAKEN" || status == "MISSED"

                val timeBucket = when {
                    slotMinutes in 0 until 12 * 60 -> "Morning"
                    slotMinutes in 12 * 60 until 17 * 60 -> "Afternoon"
                    slotMinutes in 17 * 60 until 21 * 60 -> "Evening"
                    else -> "Night"
                }

                items.add(
                    DoseItem(
                        medicationId = med.id,
                        medicationName = med.name,
                        dosage = med.dosage,
                        timeSlot = slotToken,
                        beforeOrAfterFood = med.beforeOrAfterFood,
                        instructions = med.instructions,
                        dateScheduled = dateStr,
                        status = status,
                        isDue = isDue,
                        timeBucket = timeBucket
                    )
                )
            }
        }
        return items
    }

    fun calculateAdherence(
        medications: List<Medication>,
        logs: List<MedicationLog>
    ): AdherenceData {
        val todayItems = calculateTodayDoseItems(medications, logs)

        var takenCount = todayItems.count { it.status == "TAKEN" }
        var missedCount = todayItems.count { it.status == "MISSED" }
        val upcomingCount = todayItems.count { it.status == "UPCOMING" }

        // Also add historical logs from previous dates
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        for (log in logs) {
            if (log.dateScheduled != todayStr) {
                when (log.status.uppercase(Locale.ROOT)) {
                    "TAKEN" -> takenCount++
                    "MISSED" -> missedCount++
                }
            }
        }

        val totalDue = takenCount + missedCount
        val percentage = if (totalDue > 0) {
            ((takenCount.toFloat() / totalDue) * 100).toInt()
        } else {
            null
        }

        return AdherenceData(
            percentage = percentage,
            totalDue = totalDue,
            takenCount = takenCount,
            missedCount = missedCount,
            upcomingCount = upcomingCount
        )
    }

    fun parseSingleTimeSlotMinutes(token: String): Int {
        if (token.isBlank()) return 8 * 60
        val cleanToken = token.trim()

        val regex12 = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM|am|pm)""", RegexOption.IGNORE_CASE)
        val match12 = regex12.find(cleanToken)
        if (match12 != null) {
            var h = match12.groupValues[1].toIntOrNull() ?: 8
            val m = match12.groupValues[2].toIntOrNull() ?: 0
            val ampm = match12.groupValues[3].uppercase(Locale.ROOT)
            if (ampm == "PM" && h < 12) h += 12
            if (ampm == "AM" && h == 12) h = 0
            return h * 60 + m
        }

        val regex24 = Regex("""(\d{1,2}):(\d{2})""")
        val match24 = regex24.find(cleanToken)
        if (match24 != null) {
            val h = match24.groupValues[1].toIntOrNull() ?: 8
            val m = match24.groupValues[2].toIntOrNull() ?: 0
            return h * 60 + m
        }

        val lower = cleanToken.lowercase(Locale.ROOT)
        return when {
            lower.contains("morning") || lower.contains("breakfast") -> 8 * 60
            lower.contains("afternoon") || lower.contains("lunch") -> 13 * 60
            lower.contains("evening") || lower.contains("dinner") -> 18 * 60
            lower.contains("night") || lower.contains("bedtime") || lower.contains("sleep") -> 21 * 60
            else -> 8 * 60
        }
    }

    fun parseTimeSlots(timeStr: String): List<Int> {
        if (timeStr.isBlank()) return listOf(8 * 60)
        val tokens = timeStr.split(",", ";", "&", "/")
        val slots = tokens.map { parseSingleTimeSlotMinutes(it) }
        return if (slots.isNotEmpty()) slots else listOf(8 * 60)
    }
}

