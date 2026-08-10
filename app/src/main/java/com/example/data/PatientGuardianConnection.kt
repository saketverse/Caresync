package com.example.data

data class PatientGuardianConnection(
    val connectionId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientEmail: String = "",
    val guardianId: String = "",
    val guardianName: String = "",
    val guardianEmail: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }

    val isAccepted: Boolean get() = status == STATUS_ACCEPTED
    val isPending: Boolean get() = status == STATUS_PENDING
}
