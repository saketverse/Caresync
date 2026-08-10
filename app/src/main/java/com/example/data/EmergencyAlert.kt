package com.example.data

data class EmergencyAlert(
    val alertId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val guardianId: String = "",
    val type: String = "SOS",
    val status: String = STATUS_ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val message: String = ""
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_RESOLVED = "resolved"
    }
}
