package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,             // e.g. "500 mg", "10 ml"
    val totalTablets: Int,          // Total prescribed/bought count
    val remainingTablets: Int,      // Current stock
    val startDate: String,          // e.g. "2026-08-01"
    val endDate: String,            // e.g. "2026-08-30"
    val timeOfConsumption: String,  // e.g. "08:00 AM, 08:00 PM"
    val beforeOrAfterFood: String,  // "Before Food", "After Food", "With Food"
    val instructions: String = "",  // e.g. "Take with a full glass of water"
    val category: String = "Prescription", // "Prescription", "OTC", "Supplement"
    val prescribedBy: String = "",
    val isActive: Boolean = true,
    val isTakenToday: Boolean = false,
    val lastTakenTimestamp: Long = 0L
)
