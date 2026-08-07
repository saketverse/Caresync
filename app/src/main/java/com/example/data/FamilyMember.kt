package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relation: String,        // "Father", "Mother", "Grandmother", etc.
    val phone: String,
    val email: String,
    val activeMedicationsCount: Int = 3,
    val adherenceRate: Int = 92, // e.g. 92%
    val missedDosesCount: Int = 0,
    val lastDoseStatus: String = "Taken 10 mins ago",
    val isEmergencyContact: Boolean = true
)
