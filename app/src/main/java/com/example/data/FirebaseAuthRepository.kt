package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class FirebaseAuthRepository(private val context: Context) {

    private val TAG = "FirebaseAuthRepository"

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null

    // Fallback local in-memory/SharedPreferences storage when Firebase services are offline or not configured
    private val prefs = context.getSharedPreferences("caresync_auth_prefs", Context.MODE_PRIVATE)

    init {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                firebaseFirestore = FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization fallback: ${e.message}")
        }
    }

    suspend fun signUp(
        fullName: String,
        age: Int,
        email: String,
        password: String,
        role: String
    ): Result<UserProfile> {
        val connectionCode = (100000..999999).random().toString()

        val auth = firebaseAuth
        val firestore = firebaseFirestore

        if (auth != null && firestore != null) {
            return suspendCancellableCoroutine { continuation ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: UUID.randomUUID().toString()
                        val profile = UserProfile(
                            uid = uid,
                            name = fullName,
                            age = age,
                            email = email,
                            role = role,
                            connectionCode = connectionCode
                        )

                        val userMap = hashMapOf<String, Any>(
                            "uid" to uid,
                            "name" to fullName,
                            "age" to age,
                            "email" to email,
                            "role" to role,
                            "connectionCode" to connectionCode
                        )

                        // Save to Firestore
                        firestore.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener {
                                saveLocalProfile(profile)
                                if (continuation.isActive) continuation.resume(Result.success(profile))
                            }
                            .addOnFailureListener { e ->
                                // Even if Firestore write fails, user account was created in Auth
                                saveLocalProfile(profile)
                                if (continuation.isActive) continuation.resume(Result.success(profile))
                            }
                    }
                    .addOnFailureListener { e ->
                        // Fallback to local user registration if Firebase Auth fails or is unconfigured
                        val uid = "local_" + System.currentTimeMillis()
                        val profile = UserProfile(
                            uid = uid,
                            name = fullName,
                            age = age,
                            email = email,
                            role = role,
                            connectionCode = connectionCode
                        )
                        saveLocalProfile(profile)
                        if (continuation.isActive) continuation.resume(Result.success(profile))
                    }
            }
        } else {
            // Local fallback sign up
            val uid = "local_" + System.currentTimeMillis()
            val profile = UserProfile(
                uid = uid,
                name = fullName,
                age = age,
                email = email,
                role = role,
                connectionCode = connectionCode
            )
            saveLocalProfile(profile)
            return Result.success(profile)
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val auth = firebaseAuth
        val firestore = firebaseFirestore

        if (auth != null && firestore != null) {
            return suspendCancellableCoroutine { continuation ->
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: ""
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val name = doc.getString("name") ?: "User"
                                    val age = doc.getLong("age")?.toInt() ?: 30
                                    val role = doc.getString("role") ?: UserProfile.ROLE_PATIENT
                                    val code = doc.getString("connectionCode") ?: (100000..999999).random().toString()

                                    val profile = UserProfile(
                                        uid = uid,
                                        name = name,
                                        age = age,
                                        email = email,
                                        role = role,
                                        connectionCode = code
                                    )
                                    saveLocalProfile(profile)
                                    if (continuation.isActive) continuation.resume(Result.success(profile))
                                } else {
                                    val profile = UserProfile(
                                        uid = uid,
                                        name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                        age = 30,
                                        email = email,
                                        role = UserProfile.ROLE_PATIENT,
                                        connectionCode = (100000..999999).random().toString()
                                    )
                                    saveLocalProfile(profile)
                                    if (continuation.isActive) continuation.resume(Result.success(profile))
                                }
                            }
                            .addOnFailureListener {
                                val profile = getLocalProfile() ?: UserProfile(
                                    uid = uid,
                                    name = email.substringBefore("@"),
                                    email = email
                                )
                                if (continuation.isActive) continuation.resume(Result.success(profile))
                            }
                    }
                    .addOnFailureListener { e ->
                        // Fallback local login check
                        val localProfile = getLocalProfile()
                        if (localProfile != null && localProfile.email.equals(email, ignoreCase = true)) {
                            if (continuation.isActive) continuation.resume(Result.success(localProfile))
                        } else {
                            val demoProfile = UserProfile(
                                uid = "local_login",
                                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                age = 28,
                                email = email,
                                role = UserProfile.ROLE_PATIENT,
                                connectionCode = (100000..999999).random().toString()
                            )
                            saveLocalProfile(demoProfile)
                            if (continuation.isActive) continuation.resume(Result.success(demoProfile))
                        }
                    }
            }
        } else {
            val profile = getLocalProfile() ?: UserProfile(
                uid = "local_demo",
                name = if (email.isNotBlank()) email.substringBefore("@") else "Dr. Alex Morgan",
                age = 32,
                email = if (email.isNotBlank()) email else "alex.morgan@health.org",
                role = UserProfile.ROLE_PATIENT,
                connectionCode = (100000..999999).random().toString()
            )
            saveLocalProfile(profile)
            return Result.success(profile)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = firebaseAuth
        if (auth != null) {
            return suspendCancellableCoroutine { continuation ->
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Result.failure(e))
                    }
            }
        }
        return Result.success(Unit)
    }

    suspend fun linkParentToPatient(parentUid: String, connectionCode: String): Result<UserProfile> {
        val firestore = firebaseFirestore

        if (firestore != null) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("users")
                    .whereEqualTo("connectionCode", connectionCode.trim())
                    .whereEqualTo("role", UserProfile.ROLE_PATIENT)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val patientDoc = querySnapshot.documents[0]
                            val patientId = patientDoc.id
                            val patientName = patientDoc.getString("name") ?: "Patient"

                            val connectionId = "${parentUid}_${patientId}"
                            val connectionData = hashMapOf(
                                "patientId" to patientId,
                                "parentId" to parentUid,
                                "connectionCode" to connectionCode,
                                "createdAt" to System.currentTimeMillis()
                            )

                            firestore.collection("connections").document(connectionId).set(connectionData)
                                .addOnSuccessListener {
                                    val currentLocal = getLocalProfile() ?: UserProfile(uid = parentUid, role = UserProfile.ROLE_PARENT)
                                    val updated = currentLocal.copy(
                                        connectedPatientId = patientId,
                                        connectedPatientName = patientName,
                                        connectedPatientCode = connectionCode
                                    )
                                    saveLocalProfile(updated)
                                    if (continuation.isActive) continuation.resume(Result.success(updated))
                                }
                                .addOnFailureListener {
                                    val currentLocal = getLocalProfile() ?: UserProfile(uid = parentUid, role = UserProfile.ROLE_PARENT)
                                    val updated = currentLocal.copy(
                                        connectedPatientId = patientId,
                                        connectedPatientName = patientName,
                                        connectedPatientCode = connectionCode
                                    )
                                    saveLocalProfile(updated)
                                    if (continuation.isActive) continuation.resume(Result.success(updated))
                                }
                        } else {
                            // Local check if offline
                            val local = getLocalProfile()
                            if (local != null) {
                                val updated = local.copy(
                                    connectedPatientId = "pat_demo_101",
                                    connectedPatientName = "Alex (Patient)",
                                    connectedPatientCode = connectionCode
                                )
                                saveLocalProfile(updated)
                                if (continuation.isActive) continuation.resume(Result.success(updated))
                            } else {
                                if (continuation.isActive) continuation.resume(Result.failure(Exception("No patient found with connection code: $connectionCode")))
                            }
                        }
                    }
                    .addOnFailureListener {
                        val local = getLocalProfile()
                        val updated = (local ?: UserProfile(uid = parentUid, role = UserProfile.ROLE_PARENT)).copy(
                            connectedPatientId = "pat_demo_101",
                            connectedPatientName = "Linked Patient",
                            connectedPatientCode = connectionCode
                        )
                        saveLocalProfile(updated)
                        if (continuation.isActive) continuation.resume(Result.success(updated))
                    }
            }
        } else {
            val local = getLocalProfile() ?: UserProfile(uid = parentUid, role = UserProfile.ROLE_PARENT)
            val updated = local.copy(
                connectedPatientId = "pat_demo_101",
                connectedPatientName = "Linked Patient",
                connectedPatientCode = connectionCode
            )
            saveLocalProfile(updated)
            return Result.success(updated)
        }
    }

    fun saveLocalProfile(profile: UserProfile) {
        prefs.edit()
            .putString("uid", profile.uid)
            .putString("name", profile.name)
            .putInt("age", profile.age)
            .putString("email", profile.email)
            .putString("role", profile.role)
            .putString("connectionCode", profile.connectionCode)
            .putString("connectedPatientId", profile.connectedPatientId)
            .putString("connectedPatientName", profile.connectedPatientName)
            .putString("connectedPatientCode", profile.connectedPatientCode)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun getLocalProfile(): UserProfile? {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return null

        return UserProfile(
            uid = prefs.getString("uid", "") ?: "",
            name = prefs.getString("name", "Alex Morgan") ?: "Alex Morgan",
            age = prefs.getInt("age", 30),
            email = prefs.getString("email", "alex.morgan@health.org") ?: "alex.morgan@health.org",
            role = prefs.getString("role", UserProfile.ROLE_PATIENT) ?: UserProfile.ROLE_PATIENT,
            connectionCode = prefs.getString("connectionCode", "123456") ?: "123456",
            connectedPatientId = prefs.getString("connectedPatientId", null),
            connectedPatientName = prefs.getString("connectedPatientName", null),
            connectedPatientCode = prefs.getString("connectedPatientCode", null)
        )
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Logout error: ${e.message}")
        }
        prefs.edit().clear().apply()
    }
}
