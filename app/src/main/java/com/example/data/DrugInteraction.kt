package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drug_interactions")
data class DrugInteraction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drugA: String,
    val drugB: String,
    val riskLevel: String, // "SAFE", "MODERATE", "HIGH"
    val summary: String,
    val mechanism: String,
    val recommendation: String,
    val checkedTimestamp: Long = System.currentTimeMillis()
)
