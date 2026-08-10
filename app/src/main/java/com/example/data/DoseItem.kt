package com.example.data

data class DoseItem(
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val timeSlot: String,            // e.g., "08:00 AM"
    val beforeOrAfterFood: String,  // e.g., "After Food"
    val instructions: String,
    val dateScheduled: String,       // e.g., "2026-08-10"
    val status: String,              // "TAKEN", "MISSED", "UPCOMING"
    val isDue: Boolean,              // True if status is TAKEN or time slot has passed today
    val timeBucket: String           // "Morning", "Afternoon", "Evening", "Night"
) {
    val isTaken: Boolean get() = status == "TAKEN"
    val isMissed: Boolean get() = status == "MISSED"
    val isUpcoming: Boolean get() = status == "UPCOMING"
}
