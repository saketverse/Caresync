package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    isLoading: Boolean,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, Int, String, String, String) -> Unit, // fullName, age, email, password, role
    onForgotPassword: (String) -> Unit,
    onGoogleLoginSuccess: () -> Unit,
    onSkipQuickDemo: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }

    // Form fields
    var fullName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserProfile.ROLE_PATIENT) }

    // Dialog state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        color = MedicalPrimary,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "CareSync",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Firebase Auth & Medication Safety",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Selector
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = !isSignUp,
                            onClick = {
                                isSignUp = false
                                validationError = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Sign In", fontWeight = FontWeight.SemiBold)
                        }
                        SegmentedButton(
                            selected = isSignUp,
                            onClick = {
                                isSignUp = true
                                validationError = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Create Account", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Signup only fields
                    AnimatedVisibility(visible = isSignUp) {
                        Column {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = MedicalPrimary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = ageText,
                                onValueChange = {
                                    if (it.length <= 3 && it.all { c -> c.isDigit() }) ageText = it
                                },
                                label = { Text("Age") },
                                leadingIcon = { Icon(Icons.Filled.Numbers, contentDescription = null, tint = MedicalPrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Shared Fields (Email & Password)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = MedicalPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MedicalPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Signup only confirm password & role selection
                    AnimatedVisibility(visible = isSignUp) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null, tint = MedicalPrimary) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Select Account Role:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = selectedRole == UserProfile.ROLE_PATIENT,
                                    onClick = { selectedRole = UserProfile.ROLE_PATIENT },
                                    label = { Text("Patient") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedRole == UserProfile.ROLE_PARENT,
                                    onClick = { selectedRole = UserProfile.ROLE_PARENT },
                                    label = { Text("Parent / Caregiver") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.FamilyRestroom, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Forgot password option for Sign In mode
                    if (!isSignUp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmailInput = email
                                    showForgotPasswordDialog = true
                                }
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = MedicalPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Submit Button (Login or Signup)
                    Button(
                        onClick = {
                            if (!isSignUp) {
                                if (email.isBlank() || password.isBlank()) {
                                    validationError = "Please fill in email and password"
                                } else {
                                    validationError = null
                                    onLogin(email, password)
                                }
                            } else {
                                if (fullName.isBlank() || email.isBlank() || password.isBlank() || ageText.isBlank()) {
                                    validationError = "Please fill in all fields"
                                } else if (password != confirmPassword) {
                                    validationError = "Passwords do not match"
                                } else if (password.length < 6) {
                                    validationError = "Password must be at least 6 characters"
                                } else {
                                    validationError = null
                                    val ageVal = ageText.toIntOrNull() ?: 25
                                    onSignUp(fullName, ageVal, email, password, selectedRole)
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUp) "Create Account" else "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Sign-In button
                    OutlinedButton(
                        onClick = onGoogleLoginSuccess,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Google Sign In",
                            tint = MedicalPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch between Login / Create Account
                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            validationError = null
                        }
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Create Account",
                            color = MedicalPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(onClick = onSkipQuickDemo) {
                        Text(
                            text = "Skip for Now (Quick Demo Mode)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            icon = { Icon(Icons.Filled.LockReset, contentDescription = null, tint = MedicalPrimary) },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your account email address. We'll send you a password reset link via Firebase Auth.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmailInput.isNotBlank()) {
                            onForgotPassword(resetEmailInput)
                            showForgotPasswordDialog = false
                        }
                    }
                ) {
                    Text("Send Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
