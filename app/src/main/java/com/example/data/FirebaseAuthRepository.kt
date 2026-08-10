package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GoogleAuthResult(
    val profile: UserProfile,
    val isNewUser: Boolean
)

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            if (cont.isActive) cont.resume(result)
        }
        addOnFailureListener { exception ->
            if (cont.isActive) cont.resumeWithException(exception)
        }
    }

class FirebaseAuthRepository(private val context: Context) {

    private val TAG = "FirebaseAuthRepository"

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseFirestore: FirebaseFirestore? = null

    private val prefs = context.getSharedPreferences("caresync_auth_prefs", Context.MODE_PRIVATE)

    init {
        tryInitFirebase()
    }

    private fun tryInitFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                if (firebaseAuth == null) {
                    firebaseAuth = FirebaseAuth.getInstance()
                }
                if (firebaseFirestore == null) {
                    firebaseFirestore = FirebaseFirestore.getInstance()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization notice: ${e.message}")
        }
    }

    private fun getAuth(): FirebaseAuth? {
        if (firebaseAuth != null) return firebaseAuth
        tryInitFirebase()
        return firebaseAuth
    }

    private fun getFirestore(): FirebaseFirestore? {
        if (firebaseFirestore != null) return firebaseFirestore
        tryInitFirebase()
        return firebaseFirestore
    }

    suspend fun signUp(
        fullName: String,
        age: Int,
        email: String,
        password: String,
        role: String
    ): Result<UserProfile> {
        val connectionCode = (100000..999999).random().toString()
        val auth = getAuth()
        val firestore = getFirestore()

        if (auth != null && firestore != null) {
            return suspendCancellableCoroutine { continuation ->
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        user?.sendEmailVerification()

                        val uid = user?.uid ?: UUID.randomUUID().toString()
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

                        firestore.collection("users").document(uid).set(userMap)
                            .addOnCompleteListener {
                                prefs.edit().putBoolean("is_email_verified_${email.lowercase()}", false).apply()
                                auth.signOut()
                                if (continuation.isActive) continuation.resume(Result.success(profile))
                            }
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Result.failure(e))
                    }
            }
        } else {
            // Local accounts fallback when Firebase config is absent
            val userPrefs = context.getSharedPreferences("caresync_registered_users", Context.MODE_PRIVATE)
            val emailKey = email.lowercase().trim()
            if (userPrefs.contains("pwd_$emailKey")) {
                return Result.failure(Exception("An account with email $email already exists. Please log in."))
            }

            val uid = "usr_" + UUID.randomUUID().toString().replace("-", "").take(12)
            val profile = UserProfile(
                uid = uid,
                name = fullName,
                age = age,
                email = email,
                role = role,
                connectionCode = connectionCode
            )

            userPrefs.edit()
                .putString("pwd_$emailKey", password)
                .putString("uid_$emailKey", uid)
                .putString("name_$emailKey", fullName)
                .putInt("age_$emailKey", age)
                .putString("role_$emailKey", role)
                .putString("code_$emailKey", connectionCode)
                .apply()

            prefs.edit().putBoolean("is_email_verified_$emailKey", true).apply()
            return Result.success(profile)
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val auth = getAuth()
        val firestore = getFirestore()

        if (auth != null && firestore != null) {
            return suspendCancellableCoroutine { continuation ->
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        user?.reload()?.addOnCompleteListener {
                            val isVerified = user.isEmailVerified
                            if (!isVerified) {
                                auth.signOut()
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Please verify your email address before logging in.")))
                                }
                                return@addOnCompleteListener
                            }

                            val uid = user.uid
                            firestore.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    val name = doc.getString("name") ?: email.substringBefore("@")
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
                                    prefs.edit().putBoolean("is_email_verified_${email.lowercase()}", true).apply()
                                    saveLocalProfile(profile)
                                    if (continuation.isActive) continuation.resume(Result.success(profile))
                                }
                                .addOnFailureListener { e ->
                                    if (continuation.isActive) continuation.resume(Result.failure(e))
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Result.failure(e))
                    }
            }
        } else {
            // Local accounts fallback when Firebase config is absent
            val userPrefs = context.getSharedPreferences("caresync_registered_users", Context.MODE_PRIVATE)
            val emailKey = email.lowercase().trim()

            val storedPassword = userPrefs.getString("pwd_$emailKey", null)
            if (storedPassword == null) {
                return Result.failure(Exception("No account found with email $email. Please sign up first."))
            }

            if (storedPassword != password) {
                return Result.failure(Exception("Incorrect password. Please try again."))
            }

            val uid = userPrefs.getString("uid_$emailKey", "usr_" + emailKey.hashCode().toString().replace("-", "")) ?: ""
            val name = userPrefs.getString("name_$emailKey", email.substringBefore("@")) ?: email.substringBefore("@")
            val age = userPrefs.getInt("age_$emailKey", 30)
            val role = userPrefs.getString("role_$emailKey", UserProfile.ROLE_PATIENT) ?: UserProfile.ROLE_PATIENT
            val code = userPrefs.getString("code_$emailKey", (100000..999999).random().toString()) ?: ""

            val profile = UserProfile(
                uid = uid,
                name = name,
                age = age,
                email = email,
                role = role,
                connectionCode = code
            )
            saveLocalProfile(profile)
            return Result.success(profile)
        }
    }

    suspend fun signInWithGoogleCredential(activityContext: Context): Result<GoogleAuthResult> {
        return try {
            val credentialManager = CredentialManager.create(activityContext)

            val webClientIdResId = activityContext.resources.getIdentifier("default_web_client_id", "string", activityContext.packageName)
            val webClientId = if (webClientIdResId != 0) {
                activityContext.getString(webClientIdResId)
            } else {
                "1083917892341-caresync.apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                return Result.failure(Exception("Unable to sign in with Google. Invalid credential response."))
            }

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val rawEmail = googleIdTokenCredential.id
            val rawDisplayName = googleIdTokenCredential.displayName
            val rawPhotoUrl = googleIdTokenCredential.profilePictureUri?.toString()

            val auth = getAuth()
            val firestore = getFirestore()

            if (auth != null && firestore != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).awaitResult()
                val user = authResult.user ?: return Result.failure(Exception("Firebase user authentication failed."))

                val uid = user.uid
                val email = user.email ?: rawEmail
                val displayName = user.displayName ?: rawDisplayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
                val photoUrl = user.photoUrl?.toString() ?: rawPhotoUrl

                try {
                    val doc = firestore.collection("users").document(uid).get().awaitResult()
                    if (doc.exists()) {
                        val role = doc.getString("role") ?: UserProfile.ROLE_PATIENT
                        val code = doc.getString("connectionCode") ?: (100000..999999).random().toString()
                        val name = doc.getString("name") ?: displayName
                        val userEmail = doc.getString("email") ?: email
                        val age = doc.getLong("age")?.toInt() ?: 30

                        val profile = UserProfile(
                            uid = uid,
                            name = name,
                            age = age,
                            email = userEmail,
                            role = role,
                            connectionCode = code
                        )
                        saveLocalProfile(profile)
                        Result.success(GoogleAuthResult(profile, isNewUser = false))
                    } else {
                        val connectionCode = (100000..999999).random().toString()
                        val userMap = hashMapOf<String, Any?>(
                            "uid" to uid,
                            "name" to displayName,
                            "age" to 30,
                            "email" to email,
                            "role" to UserProfile.ROLE_PATIENT,
                            "connectionCode" to connectionCode,
                            "photoUrl" to photoUrl,
                            "createdAt" to System.currentTimeMillis()
                        )
                        firestore.collection("users").document(uid).set(userMap).awaitResult()

                        val profile = UserProfile(
                            uid = uid,
                            name = displayName,
                            age = 30,
                            email = email,
                            role = UserProfile.ROLE_PATIENT,
                            connectionCode = connectionCode
                        )
                        saveLocalProfile(profile)
                        Result.success(GoogleAuthResult(profile, isNewUser = true))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore sync fallback for UID $uid: ${e.message}")
                    val connectionCode = (100000..999999).random().toString()
                    val profile = UserProfile(
                        uid = uid,
                        name = displayName,
                        age = 30,
                        email = email,
                        role = UserProfile.ROLE_PATIENT,
                        connectionCode = connectionCode
                    )
                    saveLocalProfile(profile)
                    Result.success(GoogleAuthResult(profile, isNewUser = false))
                }
            } else {
                // When Firebase services are unavailable or not configured, construct authenticated user profile from official Google Account Credential Manager token
                val uid = "google_" + rawEmail.lowercase().trim().hashCode().toString().replace("-", "")
                val email = rawEmail
                val displayName = (rawDisplayName ?: "").ifBlank { email.substringBefore("@").replaceFirstChar { it.uppercase() } }

                val userPrefs = context.getSharedPreferences("caresync_registered_users", Context.MODE_PRIVATE)
                val emailKey = email.lowercase().trim()
                val isExisting = userPrefs.contains("uid_$emailKey")

                val code = userPrefs.getString("code_$emailKey", (100000..999999).random().toString()) ?: (100000..999999).random().toString()
                val role = userPrefs.getString("role_$emailKey", UserProfile.ROLE_PATIENT) ?: UserProfile.ROLE_PATIENT
                val age = userPrefs.getInt("age_$emailKey", 30)

                val profile = UserProfile(
                    uid = uid,
                    name = displayName,
                    age = age,
                    email = email,
                    role = role,
                    connectionCode = code
                )

                userPrefs.edit()
                    .putString("uid_$emailKey", uid)
                    .putString("name_$emailKey", displayName)
                    .putString("email_$emailKey", email)
                    .putString("role_$emailKey", role)
                    .putString("code_$emailKey", code)
                    .apply()

                saveLocalProfile(profile)
                Result.success(GoogleAuthResult(profile, isNewUser = !isExisting))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In was cancelled."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}", e)
            Result.failure(Exception("Google Sign-In was cancelled or unavailable."))
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In Exception: ${e.message}", e)
            val msg = e.localizedMessage ?: ""
            if (msg.contains("canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
                Result.failure(Exception("Google Sign-In was cancelled."))
            } else {
                Result.failure(Exception("Unable to sign in with Google. Please try again."))
            }
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String? = null): Result<Unit> {
        val auth = getAuth()
        if (auth != null) {
            return suspendCancellableCoroutine { continuation ->
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.email?.equals(email, ignoreCase = true) == true) {
                    currentUser.sendEmailVerification()
                        .addOnSuccessListener {
                            if (continuation.isActive) continuation.resume(Result.success(Unit))
                        }
                        .addOnFailureListener { e ->
                            if (continuation.isActive) continuation.resume(Result.failure(e))
                        }
                } else if (!password.isNullOrBlank()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { authResult ->
                            authResult.user?.sendEmailVerification()
                                ?.addOnSuccessListener {
                                    auth.signOut()
                                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                                }
                                ?.addOnFailureListener { e ->
                                    auth.signOut()
                                    if (continuation.isActive) continuation.resume(Result.failure(e))
                                }
                        }
                        .addOnFailureListener { e ->
                            if (continuation.isActive) continuation.resume(Result.failure(e))
                        }
                } else {
                    if (continuation.isActive) continuation.resume(Result.failure(Exception("Please provide password to re-send verification link.")))
                }
            }
        } else {
            return Result.success(Unit)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = getAuth()
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

    // --- Connection Requests & Approval ---
    suspend fun requestGuardianConnection(
        guardianUid: String,
        guardianName: String,
        guardianEmail: String,
        connectionCode: String
    ): Result<PatientGuardianConnection> {
        val firestore = getFirestore()
        val code = connectionCode.trim()

        if (firestore != null) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("users")
                    .whereEqualTo("connectionCode", code)
                    .whereEqualTo("role", UserProfile.ROLE_PATIENT)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val patientDoc = querySnapshot.documents[0]
                            val patientId = patientDoc.id
                            val patientName = patientDoc.getString("name") ?: "Patient"
                            val patientEmail = patientDoc.getString("email") ?: ""

                            val connectionId = "${patientId}_${guardianUid}"
                            val conn = PatientGuardianConnection(
                                connectionId = connectionId,
                                patientId = patientId,
                                patientName = patientName,
                                patientEmail = patientEmail,
                                guardianId = guardianUid,
                                guardianName = guardianName,
                                guardianEmail = guardianEmail,
                                status = PatientGuardianConnection.STATUS_PENDING,
                                createdAt = System.currentTimeMillis()
                            )

                            val connData = hashMapOf(
                                "connectionId" to connectionId,
                                "patientId" to patientId,
                                "patientName" to patientName,
                                "patientEmail" to patientEmail,
                                "guardianId" to guardianUid,
                                "guardianName" to guardianName,
                                "guardianEmail" to guardianEmail,
                                "status" to PatientGuardianConnection.STATUS_PENDING,
                                "createdAt" to conn.createdAt
                            )

                            firestore.collection("connections").document(connectionId).set(connData)
                                .addOnSuccessListener {
                                    saveLocalConnection(conn)
                                    if (continuation.isActive) continuation.resume(Result.success(conn))
                                }
                                .addOnFailureListener { e ->
                                    if (continuation.isActive) continuation.resume(Result.failure(e))
                                }
                        } else {
                            if (continuation.isActive) continuation.resume(Result.failure(Exception("No patient found with connection code: $code")))
                        }
                    }
                    .addOnFailureListener { e ->
                        if (continuation.isActive) continuation.resume(Result.failure(e))
                    }
            }
        } else {
            val localConn = PatientGuardianConnection(
                connectionId = "pat_${code}_${guardianUid}",
                patientId = "pat_$code",
                patientName = "Patient ($code)",
                patientEmail = "patient@example.com",
                guardianId = guardianUid,
                guardianName = guardianName,
                guardianEmail = guardianEmail,
                status = PatientGuardianConnection.STATUS_PENDING,
                createdAt = System.currentTimeMillis()
            )
            saveLocalConnection(localConn)
            return Result.success(localConn)
        }
    }

    suspend fun fetchPendingConnectionRequests(patientUid: String): List<PatientGuardianConnection> {
        val firestore = getFirestore()
        if (firestore != null && patientUid.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("connections")
                    .whereEqualTo("patientId", patientUid)
                    .whereEqualTo("status", PatientGuardianConnection.STATUS_PENDING)
                    .get()
                    .addOnSuccessListener { query ->
                        val list = query.documents.mapNotNull { doc ->
                            doc.toObject(PatientGuardianConnection::class.java)
                        }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(getLocalPendingConnections(patientUid))
                    }
            }
        }
        return getLocalPendingConnections(patientUid)
    }

    suspend fun respondToConnectionRequest(connectionId: String, accept: Boolean): Result<Unit> {
        val firestore = getFirestore()
        val newStatus = if (accept) PatientGuardianConnection.STATUS_ACCEPTED else PatientGuardianConnection.STATUS_REJECTED
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "acceptedAt" to System.currentTimeMillis()
        )

        updateLocalConnectionStatus(connectionId, newStatus)

        if (firestore != null && connectionId.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("connections").document(connectionId)
                    .update(updates)
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

    suspend fun fetchAcceptedConnectionsForPatient(patientUid: String): List<PatientGuardianConnection> {
        val firestore = getFirestore()
        if (firestore != null && patientUid.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("connections")
                    .whereEqualTo("patientId", patientUid)
                    .whereEqualTo("status", PatientGuardianConnection.STATUS_ACCEPTED)
                    .get()
                    .addOnSuccessListener { query ->
                        val list = query.documents.mapNotNull { doc ->
                            doc.toObject(PatientGuardianConnection::class.java)
                        }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(getLocalAcceptedConnectionsForPatient(patientUid))
                    }
            }
        }
        return getLocalAcceptedConnectionsForPatient(patientUid)
    }

    suspend fun fetchAcceptedPatientsForGuardian(guardianUid: String): List<PatientGuardianConnection> {
        val firestore = getFirestore()
        if (firestore != null && guardianUid.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("connections")
                    .whereEqualTo("guardianId", guardianUid)
                    .whereEqualTo("status", PatientGuardianConnection.STATUS_ACCEPTED)
                    .get()
                    .addOnSuccessListener { query ->
                        val list = query.documents.mapNotNull { doc ->
                            doc.toObject(PatientGuardianConnection::class.java)
                        }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(getLocalAcceptedConnectionsForGuardian(guardianUid))
                    }
            }
        }
        return getLocalAcceptedConnectionsForGuardian(guardianUid)
    }

    // --- SOS Emergency Routing ---
    suspend fun sendSOSAlert(patientUid: String, patientName: String): Result<List<String>> {
        val acceptedConns = fetchAcceptedConnectionsForPatient(patientUid)
        if (acceptedConns.isEmpty()) {
            return Result.failure(Exception("NO_GUARDIAN_CONNECTED"))
        }

        val firestore = getFirestore()
        val alertedGuardianNames = mutableListOf<String>()

        for (conn in acceptedConns) {
            val alertId = "sos_${patientUid}_${System.currentTimeMillis()}"
            val alert = EmergencyAlert(
                alertId = alertId,
                patientId = patientUid,
                patientName = patientName.ifBlank { conn.patientName },
                guardianId = conn.guardianId,
                type = "SOS",
                status = EmergencyAlert.STATUS_ACTIVE,
                createdAt = System.currentTimeMillis(),
                message = "🚨 SOS Emergency Alert from ${patientName.ifBlank { conn.patientName }}!"
            )
            alertedGuardianNames.add(conn.guardianName.ifBlank { "Connected Guardian" })

            if (firestore != null) {
                val data = hashMapOf(
                    "alertId" to alert.alertId,
                    "patientId" to alert.patientId,
                    "patientName" to alert.patientName,
                    "guardianId" to alert.guardianId,
                    "type" to alert.type,
                    "status" to alert.status,
                    "createdAt" to alert.createdAt,
                    "message" to alert.message
                )
                firestore.collection("emergencyAlerts").document(alertId).set(data)
            }
            saveLocalEmergencyAlert(alert)
        }

        return Result.success(alertedGuardianNames)
    }

    suspend fun fetchActiveEmergencyAlerts(guardianUid: String): List<EmergencyAlert> {
        val firestore = getFirestore()
        if (firestore != null && guardianUid.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("emergencyAlerts")
                    .whereEqualTo("guardianId", guardianUid)
                    .whereEqualTo("status", EmergencyAlert.STATUS_ACTIVE)
                    .get()
                    .addOnSuccessListener { query ->
                        val list = query.documents.mapNotNull { doc ->
                            doc.toObject(EmergencyAlert::class.java)
                        }
                        if (continuation.isActive) continuation.resume(list)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(getLocalActiveEmergencyAlerts(guardianUid))
                    }
            }
        }
        return getLocalActiveEmergencyAlerts(guardianUid)
    }

    suspend fun resolveEmergencyAlert(alertId: String): Result<Unit> {
        val firestore = getFirestore()
        if (firestore != null && alertId.isNotBlank()) {
            firestore.collection("emergencyAlerts").document(alertId).update("status", EmergencyAlert.STATUS_RESOLVED)
        }
        resolveLocalEmergencyAlert(alertId)
        return Result.success(Unit)
    }

    // --- Local Connection & Alert Helpers ---
    private val connPrefs = context.getSharedPreferences("caresync_connections_prefs", Context.MODE_PRIVATE)

    private fun saveLocalConnection(conn: PatientGuardianConnection) {
        val set = connPrefs.getStringSet("all_conn_ids", mutableSetOf()) ?: mutableSetOf()
        val newSet = set.toMutableSet()
        newSet.add(conn.connectionId)

        connPrefs.edit()
            .putStringSet("all_conn_ids", newSet)
            .putString("patientId_${conn.connectionId}", conn.patientId)
            .putString("patientName_${conn.connectionId}", conn.patientName)
            .putString("patientEmail_${conn.connectionId}", conn.patientEmail)
            .putString("guardianId_${conn.connectionId}", conn.guardianId)
            .putString("guardianName_${conn.connectionId}", conn.guardianName)
            .putString("guardianEmail_${conn.connectionId}", conn.guardianEmail)
            .putString("status_${conn.connectionId}", conn.status)
            .putLong("createdAt_${conn.connectionId}", conn.createdAt)
            .apply()
    }

    private fun updateLocalConnectionStatus(connectionId: String, status: String) {
        connPrefs.edit().putString("status_$connectionId", status).apply()
    }

    private fun getLocalPendingConnections(patientUid: String): List<PatientGuardianConnection> {
        return getAllLocalConnections().filter { (it.patientId == patientUid || patientUid.isBlank()) && it.isPending }
    }

    private fun getLocalAcceptedConnectionsForPatient(patientUid: String): List<PatientGuardianConnection> {
        return getAllLocalConnections().filter { (it.patientId == patientUid || patientUid.isBlank()) && it.isAccepted }
    }

    private fun getLocalAcceptedConnectionsForGuardian(guardianUid: String): List<PatientGuardianConnection> {
        return getAllLocalConnections().filter { (it.guardianId == guardianUid || guardianUid.isBlank()) && it.isAccepted }
    }

    private fun getAllLocalConnections(): List<PatientGuardianConnection> {
        val ids = connPrefs.getStringSet("all_conn_ids", emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            val pId = connPrefs.getString("patientId_$id", null) ?: return@mapNotNull null
            PatientGuardianConnection(
                connectionId = id,
                patientId = pId,
                patientName = connPrefs.getString("patientName_$id", "Patient") ?: "Patient",
                patientEmail = connPrefs.getString("patientEmail_$id", "") ?: "",
                guardianId = connPrefs.getString("guardianId_$id", "") ?: "",
                guardianName = connPrefs.getString("guardianName_$id", "Guardian") ?: "Guardian",
                guardianEmail = connPrefs.getString("guardianEmail_$id", "") ?: "",
                status = connPrefs.getString("status_$id", PatientGuardianConnection.STATUS_PENDING) ?: PatientGuardianConnection.STATUS_PENDING,
                createdAt = connPrefs.getLong("createdAt_$id", 0L)
            )
        }
    }

    private fun saveLocalEmergencyAlert(alert: EmergencyAlert) {
        val alertsSet = connPrefs.getStringSet("all_alert_ids", mutableSetOf()) ?: mutableSetOf()
        val newSet = alertsSet.toMutableSet()
        newSet.add(alert.alertId)

        connPrefs.edit()
            .putStringSet("all_alert_ids", newSet)
            .putString("patientId_alert_${alert.alertId}", alert.patientId)
            .putString("patientName_alert_${alert.alertId}", alert.patientName)
            .putString("guardianId_alert_${alert.alertId}", alert.guardianId)
            .putString("type_alert_${alert.alertId}", alert.type)
            .putString("status_alert_${alert.alertId}", alert.status)
            .putString("message_alert_${alert.alertId}", alert.message)
            .putLong("createdAt_alert_${alert.alertId}", alert.createdAt)
            .apply()
    }

    private fun resolveLocalEmergencyAlert(alertId: String) {
        connPrefs.edit().putString("status_alert_$alertId", EmergencyAlert.STATUS_RESOLVED).apply()
    }

    private fun getLocalActiveEmergencyAlerts(guardianUid: String): List<EmergencyAlert> {
        val alertIds = connPrefs.getStringSet("all_alert_ids", emptySet()) ?: emptySet()
        return alertIds.mapNotNull { id ->
            val gId = connPrefs.getString("guardianId_alert_$id", null) ?: return@mapNotNull null
            val status = connPrefs.getString("status_alert_$id", EmergencyAlert.STATUS_ACTIVE) ?: EmergencyAlert.STATUS_ACTIVE
            if ((gId == guardianUid || guardianUid.isBlank()) && status == EmergencyAlert.STATUS_ACTIVE) {
                EmergencyAlert(
                    alertId = id,
                    patientId = connPrefs.getString("patientId_alert_$id", "") ?: "",
                    patientName = connPrefs.getString("patientName_alert_$id", "Patient") ?: "Patient",
                    guardianId = gId,
                    type = connPrefs.getString("type_alert_$id", "SOS") ?: "SOS",
                    status = status,
                    createdAt = connPrefs.getLong("createdAt_alert_$id", 0L),
                    message = connPrefs.getString("message_alert_$id", "🚨 SOS Emergency Alert") ?: "🚨 SOS Emergency Alert"
                )
            } else null
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
        val uid = prefs.getString("uid", "") ?: ""
        if (!isLoggedIn || uid.isBlank()) return null

        return UserProfile(
            uid = uid,
            name = prefs.getString("name", "") ?: "",
            age = prefs.getInt("age", 0),
            email = prefs.getString("email", "") ?: "",
            role = prefs.getString("role", UserProfile.ROLE_PATIENT) ?: UserProfile.ROLE_PATIENT,
            connectionCode = prefs.getString("connectionCode", "") ?: "",
            connectedPatientId = prefs.getString("connectedPatientId", null),
            connectedPatientName = prefs.getString("connectedPatientName", null),
            connectedPatientCode = prefs.getString("connectedPatientCode", null)
        )
    }

    suspend fun saveHealthProfile(uid: String, profile: HealthProfile): Result<Unit> {
        val firestore = getFirestore()
        val map = hashMapOf<String, Any?>(
            "bloodGroup" to profile.bloodGroup,
            "allergies" to profile.allergies,
            "medicalConditions" to profile.medicalConditions,
            "emergencyContactName" to profile.emergencyContactName,
            "emergencyContactPhone" to profile.emergencyContactPhone,
            "additionalNotes" to profile.additionalNotes,
            "updatedAt" to System.currentTimeMillis()
        )

        val prefix = "hp_${uid}_"
        prefs.edit()
            .putString("${prefix}bloodGroup", profile.bloodGroup)
            .putString("${prefix}allergies", profile.allergies)
            .putString("${prefix}medicalConditions", profile.medicalConditions)
            .putString("${prefix}emergencyContactName", profile.emergencyContactName)
            .putString("${prefix}emergencyContactPhone", profile.emergencyContactPhone)
            .putString("${prefix}additionalNotes", profile.additionalNotes)
            .apply()

        if (firestore != null && uid.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("users").document(uid)
                    .collection("healthProfile").document("data")
                    .set(map)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
            }
        }
        return Result.success(Unit)
    }

    suspend fun fetchHealthProfile(uid: String): HealthProfile {
        val firestore = getFirestore()
        val prefix = "hp_${uid}_"

        val localProfile = HealthProfile(
            bloodGroup = prefs.getString("${prefix}bloodGroup", null),
            allergies = prefs.getString("${prefix}allergies", null),
            medicalConditions = prefs.getString("${prefix}medicalConditions", null),
            emergencyContactName = prefs.getString("${prefix}emergencyContactName", null),
            emergencyContactPhone = prefs.getString("${prefix}emergencyContactPhone", null),
            additionalNotes = prefs.getString("${prefix}additionalNotes", null)
        )

        if (firestore != null && uid.isNotBlank()) {
            return suspendCancellableCoroutine { continuation ->
                firestore.collection("users").document(uid)
                    .collection("healthProfile").document("data")
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val remoteProfile = HealthProfile(
                                bloodGroup = doc.getString("bloodGroup"),
                                allergies = doc.getString("allergies"),
                                medicalConditions = doc.getString("medicalConditions"),
                                emergencyContactName = doc.getString("emergencyContactName"),
                                emergencyContactPhone = doc.getString("emergencyContactPhone"),
                                additionalNotes = doc.getString("additionalNotes")
                            )
                            prefs.edit()
                                .putString("${prefix}bloodGroup", remoteProfile.bloodGroup)
                                .putString("${prefix}allergies", remoteProfile.allergies)
                                .putString("${prefix}medicalConditions", remoteProfile.medicalConditions)
                                .putString("${prefix}emergencyContactName", remoteProfile.emergencyContactName)
                                .putString("${prefix}emergencyContactPhone", remoteProfile.emergencyContactPhone)
                                .putString("${prefix}additionalNotes", remoteProfile.additionalNotes)
                                .apply()

                            if (continuation.isActive) continuation.resume(remoteProfile)
                        } else {
                            if (continuation.isActive) continuation.resume(localProfile)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(localProfile)
                    }
            }
        }
        return localProfile
    }

    suspend fun logout(activityContext: Context? = null) {
        try {
            getAuth()?.signOut()
            if (activityContext != null) {
                val credentialManager = CredentialManager.create(activityContext)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Logout error: ${e.message}")
        }
        prefs.edit().clear().apply()
    }
}
