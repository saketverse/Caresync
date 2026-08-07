package com.example.data

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val age: Int = 0,
    val email: String = "",
    val role: String = ROLE_PATIENT,
    val connectionCode: String = "",
    val connectedPatientId: String? = null,
    val connectedPatientName: String? = null,
    val connectedPatientCode: String? = null
) {
    companion object {
        const val ROLE_PATIENT = "Patient"
        const val ROLE_PARENT = "Parent / Caregiver"
    }

    val isPatient: Boolean get() = role == ROLE_PATIENT
    val isParent: Boolean get() = role == ROLE_PARENT
}
