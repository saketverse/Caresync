package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.data.LanguageManager
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavItems
import com.example.ui.screens.*
import com.example.ui.theme.MediGuardTheme
import com.example.ui.theme.MedicalPrimary
import com.example.viewmodel.MediGuardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediGuardApp(
    viewModel: MediGuardViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val healthProfile by viewModel.healthProfile.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()

    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isElderMode by viewModel.isElderMode.collectAsStateWithLifecycle()
    val voiceGender by viewModel.voiceGender.collectAsStateWithLifecycle()
    val voiceVolume by viewModel.voiceVolume.collectAsStateWithLifecycle()
    val escalationMinutes by viewModel.escalationMinutes.collectAsStateWithLifecycle()
    val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val medications by viewModel.activeMedications.collectAsStateWithLifecycle()
    val medicationLogs by viewModel.medicationLogs.collectAsStateWithLifecycle()
    val todayDoseItems by viewModel.todayDoseItems.collectAsStateWithLifecycle()
    val savedInteractions by viewModel.drugInteractions.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle()

    val pendingConnectionRequests by viewModel.pendingConnectionRequests.collectAsStateWithLifecycle()
    val acceptedGuardians by viewModel.acceptedGuardians.collectAsStateWithLifecycle()
    val acceptedPatients by viewModel.acceptedPatients.collectAsStateWithLifecycle()
    val activeEmergencyAlerts by viewModel.activeEmergencyAlerts.collectAsStateWithLifecycle()
    val showNoGuardianDialog by viewModel.showNoGuardianDialog.collectAsStateWithLifecycle()

    val interactionResult by viewModel.interactionResult.collectAsStateWithLifecycle()
    val isCheckingInteractions by viewModel.isCheckingInteractions.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    val scannedText by viewModel.scannedText.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Screen.FamilyMonitoring.route || currentRoute == Screen.Dashboard.route) {
            viewModel.refreshConnectionsAndAlerts()
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    var showNotificationOnboarding by remember { mutableStateOf(!viewModel.isNotificationOnboardingCompleted()) }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setNotificationOnboardingCompleted(true)
        if (isGranted) {
            viewModel.showSnackbar("Notifications allowed! You will receive medication reminders.")
        } else {
            viewModel.showSnackbar("Notifications not granted. You can enable them in system Settings.")
        }
        showNotificationOnboarding = false
    }

    if (showNotificationOnboarding) {
        AlertDialog(
            onDismissRequest = {
                viewModel.setNotificationOnboardingCompleted(true)
                showNotificationOnboarding = false
            },
            icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Enable Medication Reminders", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "CareSync needs notifications to remind you when it is time to take your medicine.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setNotificationOnboardingCompleted(true)
                            viewModel.showSnackbar("Notifications enabled!")
                            showNotificationOnboarding = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Allow Notifications", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setNotificationOnboardingCompleted(true)
                        showNotificationOnboarding = false
                    }
                ) {
                    Text("Not Now")
                }
            }
        )
    }

    val navigateToDashboard: () -> Unit = {
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        if (route == Screen.Dashboard.route) {
            navigateToDashboard()
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    if (showNoGuardianDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoGuardianDialog() },
            icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("No Family Guardian Connected", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You do not have any connected family guardian to receive SOS alerts. Go to Family & Guardian Connections to share your code with a caregiver.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissNoGuardianDialog()
                        navigateToTab(Screen.FamilyMonitoring.route)
                    }
                ) {
                    Text("Go to Family Care", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissNoGuardianDialog() }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    MediGuardTheme {
        Scaffold(
            bottomBar = {
                if (isLoggedIn && currentRoute != Screen.Splash.route && currentRoute != Screen.Auth.route && currentRoute != Screen.ConnectPatient.route) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(screen.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(visible = !isOnline) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = "Offline Mode",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Mode: Active schedules & offline reminders working locally",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Splash.route,
                    modifier = Modifier.weight(1f),
                    enterTransition = { fadeIn(androidx.compose.animation.core.tween(250)) },
                    exitTransition = { fadeOut(androidx.compose.animation.core.tween(250)) },
                    popEnterTransition = { fadeIn(androidx.compose.animation.core.tween(250)) },
                    popExitTransition = { fadeOut(androidx.compose.animation.core.tween(250)) }
                ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onNavigateNext = {
                            if (isLoggedIn) {
                                navigateToDashboard()
                            } else {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Auth.route) {
                    AuthScreen(
                        isLoading = isAuthLoading,
                        onLogin = { email, pass ->
                            viewModel.login(email, pass, "Patient") {
                                if (viewModel.userProfile.value.isParent) {
                                    navController.navigate(Screen.ConnectPatient.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                } else {
                                    navigateToDashboard()
                                }
                            }
                        },
                        onSignUp = { name, age, email, pass, role, onVerificationSent ->
                            viewModel.signUp(name, age, email, pass, role, onVerificationSent) {
                                if (viewModel.userProfile.value.isParent) {
                                    navController.navigate(Screen.ConnectPatient.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                } else {
                                    navigateToDashboard()
                                }
                            }
                        },
                        onForgotPassword = { email ->
                            viewModel.sendPasswordReset(email)
                        },
                        onResendVerificationEmail = { email, pass ->
                            viewModel.resendVerificationEmail(email, pass)
                        },
                        onContinueWithGoogle = { context ->
                            viewModel.continueWithGoogle(context) { isNewUser, isParent ->
                                if (isParent) {
                                    navController.navigate(Screen.ConnectPatient.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                } else {
                                    navigateToDashboard()
                                }
                            }
                        }
                    )
                }

                composable(Screen.ConnectPatient.route) {
                    ConnectPatientScreen(
                        parentName = userName,
                        isLoading = isAuthLoading,
                        onConnectCode = { code ->
                            viewModel.linkParentToPatient(code) {
                                navigateToDashboard()
                            }
                        },
                        onSkip = {
                            navigateToDashboard()
                        }
                    )
                }

                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        userName = userName,
                        medications = medications,
                        medicationLogs = medicationLogs,
                        todayDoseItems = todayDoseItems,
                        interactionResult = interactionResult,
                        isCheckingInteractions = isCheckingInteractions,
                        selectedLanguage = selectedLanguage,
                        isElderMode = isElderMode,
                        onMarkTaken = { id -> viewModel.markTaken(id) },
                        onMarkDoseTaken = { medId, timeSlot -> viewModel.markDoseTaken(medId, timeSlot) },
                        onSpeakReminder = { med -> viewModel.speakMedicineReminderForMed(med) },
                        onListenWarning = { text -> viewModel.speakText(text) },
                        onNavigateToAdd = { navigateToTab(Screen.AddMedication.route) },
                        onNavigateToInteractions = { navigateToTab(Screen.DrugInteractions.route) },
                        onNavigateToChatbot = { navigateToTab(Screen.Chatbot.route) },
                        onNavigateToScanner = { navigateToTab(Screen.PrescriptionScanner.route) },
                        onNavigateToFamily = { navigateToTab(Screen.FamilyMonitoring.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onTriggerEmergency = { viewModel.triggerEmergencySOS(healthProfile.emergencyContactName ?: "Emergency Contact", healthProfile.emergencyContactPhone ?: "112") },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.AddMedication.route) {
                    AddMedicationScreen(
                        onSaveMedication = { med -> viewModel.addMedication(med) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Reminders.route) {
                    RemindersScreen(
                        medications = medications,
                        selectedLanguage = selectedLanguage,
                        isElderMode = isElderMode,
                        onMarkTaken = { id -> viewModel.markTaken(id) },
                        onRefillStock = { id -> viewModel.refillStock(id) },
                        onSpeakReminder = { med -> viewModel.speakMedicineReminderForMed(med) },
                        onTriggerEscalation = { med, stage -> viewModel.triggerMissedReminderEscalation(med, stage) },
                        onShowTestNotification = { msg -> viewModel.showSnackbar(msg) },
                        onNavigateToDashboard = navigateToDashboard
                    )
                }

                composable(Screen.DrugInteractions.route) {
                    DrugInteractionsScreen(
                        medications = medications,
                        savedInteractions = savedInteractions,
                        interactionResult = interactionResult,
                        isChecking = isCheckingInteractions,
                        selectedLanguage = selectedLanguage,
                        isElderMode = isElderMode,
                        onRunAnalysis = { viewModel.runDrugInteractionCheck() },
                        onListenWarning = { text -> viewModel.speakText(text) },
                        onNavigateToDashboard = navigateToDashboard
                    )
                }

                composable(Screen.Chatbot.route) {
                    ChatbotScreen(
                        messages = chatMessages,
                        isLoading = isChatLoading,
                        onSendMessage = { text -> viewModel.sendChatMessage(text) },
                        onNavigateToDashboard = navigateToDashboard
                    )
                }

                composable(Screen.PrescriptionScanner.route) {
                    PrescriptionScannerScreen(
                        scannedText = scannedText,
                        isScanning = isScanning,
                        selectedLanguage = selectedLanguage,
                        isElderMode = isElderMode,
                        onScanPreset = { preset -> viewModel.scanPrescription(preset) },
                        onImportMedication = { name, dosage, time, food ->
                            viewModel.autoAddScannedMedicationToSchedule(name, dosage, time, food)
                        },
                        onReadAloudText = { text -> viewModel.speakText(text) },
                        onNavigateToDashboard = navigateToDashboard
                    )
                }

                composable(Screen.FamilyMonitoring.route) {
                    FamilyMonitoringScreen(
                        userRole = userProfile.role,
                        patientConnectionCode = userProfile.connectionCode.ifBlank { userProfile.uid.take(6).uppercase() },
                        pendingRequests = pendingConnectionRequests,
                        acceptedGuardians = acceptedGuardians,
                        acceptedPatients = acceptedPatients,
                        activeEmergencyAlerts = activeEmergencyAlerts,
                        familyMembers = familyMembers,
                        onSendConnectionRequest = { code -> viewModel.sendConnectionRequest(code) },
                        onRespondToRequest = { connId, accept -> viewModel.respondToConnectionRequest(connId, accept) },
                        onResolveEmergencyAlert = { alertId -> viewModel.resolveEmergencyAlert(alertId) },
                        onTriggerSOS = { name, phone -> viewModel.triggerEmergencySOS(name, phone) },
                        onNavigateToDashboard = navigateToDashboard
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        userProfile = userProfile,
                        healthProfile = healthProfile,
                        isVoiceEnabled = isVoiceEnabled,
                        selectedLanguage = selectedLanguage,
                        isElderMode = isElderMode,
                        voiceGender = voiceGender,
                        voiceVolume = voiceVolume,
                        escalationMinutes = escalationMinutes,
                        isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                        onToggleVoice = { viewModel.toggleVoice() },
                        onSelectLanguage = { code -> viewModel.setLanguage(code) },
                        onToggleElderMode = { viewModel.toggleElderMode() },
                        onSetVoiceGender = { gender -> viewModel.setVoiceGender(gender) },
                        onSetVoiceVolume = { vol -> viewModel.setVoiceVolume(vol) },
                        onSetEscalationMinutes = { mins -> viewModel.setEscalationMinutes(mins) },
                        onTestVoiceReminder = {
                            viewModel.testVoice()
                        },
                        onRequestDisableBatteryOptimization = { viewModel.requestDisableBatteryOptimization() },
                        onStartForegroundService = { viewModel.startForegroundReminderService() },
                        onUpdateHealthProfile = { updated -> viewModel.updateHealthProfile(updated) },
                        onNavigateToConnectPatient = { navController.navigate(Screen.ConnectPatient.route) },
                        onNavigateToDashboard = navigateToDashboard,
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(Screen.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
}
