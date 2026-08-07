package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_info_cache")
data class MedicineInfo(
    @PrimaryKey val name: String,
    val genericName: String,
    val brandName: String,
    val dosage: String,
    val sideEffects: String,
    val uses: String,
    val drugInteractions: String,
    val warnings: String,
    val manufacturer: String,
    val isFromOpenFDA: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
