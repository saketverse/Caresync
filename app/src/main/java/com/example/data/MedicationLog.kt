package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val dateScheduled: String,   // e.g. "2026-08-06"
    val timeScheduled: String,   // e.g. "08:00 AM"
    val status: String,          // "TAKEN", "MISSED", "SNOOZED", "PENDING"
    val timestampTaken: Long = 0L
)
