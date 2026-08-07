package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.navigation.Screen
import com.example.ui.navigation.bottomNavItems
import com.example.ui.screens.*
import com.example.ui.theme.MediGuardTheme
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
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()

    val medications by viewModel.activeMedications.collectAsStateWithLifecycle()
    val savedInteractions by viewModel.drugInteractions.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle()

    val interactionResult by viewModel.interactionResult.collectAsStateWithLifecycle()
    val isCheckingInteractions by viewModel.isCheckingInteractions.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    val scannedText by viewModel.scannedText.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    MediGuardTheme(darkTheme = isDarkMode) {
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
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
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
            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Splash.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(androidx.compose.animation.core.tween(250)) },
                exitTransition = { fadeOut(androidx.compose.animation.core.tween(250)) },
                popEnterTransition = { fadeIn(androidx.compose.animation.core.tween(250)) },
                popExitTransition = { fadeOut(androidx.compose.animation.core.tween(250)) }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onNavigateNext = {
                            if (isLoggedIn) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
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
                            viewModel.login(email, pass) {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        },
                        onSignUp = { name, age, email, pass, role ->
                            viewModel.signUp(
                                fullName = name,
                                age = age,
                                email = email,
                                pass = pass,
                                role = role,
                                onNavigateConnectPatient = {
                                    navController.navigate(Screen.ConnectPatient.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                },
                                onNavigateDashboard = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        },
                        onForgotPassword = { email ->
                            viewModel.sendForgotPassword(email)
                        },
                        onGoogleLoginSuccess = {
                            viewModel.loginWithGoogle {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        },
                        onSkipQuickDemo = {
                            viewModel.skipQuickDemo {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
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
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.ConnectPatient.route) { inclusive = true }
                                }
                            }
                        },
                        onSkip = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.ConnectPatient.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        userName = userName,
                        medications = medications,
                        interactionResult = interactionResult,
                        isCheckingInteractions = isCheckingInteractions,
                        onMarkTaken = { id -> viewModel.markTaken(id) },
                        onNavigateToAdd = { navController.navigate(Screen.AddMedication.route) },
                        onNavigateToInteractions = { navController.navigate(Screen.DrugInteractions.route) },
                        onNavigateToChatbot = { navController.navigate(Screen.Chatbot.route) },
                        onNavigateToScanner = { navController.navigate(Screen.PrescriptionScanner.route) },
                        onNavigateToFamily = { navController.navigate(Screen.FamilyMonitoring.route) },
                        onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        onTriggerEmergency = { viewModel.triggerEmergencySOS("Dr. Robert Vance", "+1 (555) 019-2831") }
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
                        onMarkTaken = { id -> viewModel.markTaken(id) },
                        onRefillStock = { id -> viewModel.refillStock(id) },
                        onShowTestNotification = { msg -> viewModel.showSnackbar(msg) }
                    )
                }

                composable(Screen.DrugInteractions.route) {
                    DrugInteractionsScreen(
                        medications = medications,
                        savedInteractions = savedInteractions,
                        interactionResult = interactionResult,
                        isChecking = isCheckingInteractions,
                        onRunAnalysis = { viewModel.runDrugInteractionCheck() }
                    )
                }

                composable(Screen.Chatbot.route) {
                    ChatbotScreen(
                        messages = chatMessages,
                        isLoading = isChatLoading,
                        onSendMessage = { text -> viewModel.sendChatMessage(text) }
                    )
                }

                composable(Screen.PrescriptionScanner.route) {
                    PrescriptionScannerScreen(
                        scannedText = scannedText,
                        isScanning = isScanning,
                        onScanPreset = { preset -> viewModel.scanPrescription(preset) },
                        onImportMedication = { name, dosage, time, food ->
                            viewModel.autoAddScannedMedicationToSchedule(name, dosage, time, food)
                        }
                    )
                }

                composable(Screen.FamilyMonitoring.route) {
                    FamilyMonitoringScreen(
                        familyMembers = familyMembers,
                        onAddFamilyMember = { name, rel, phone, email ->
                            viewModel.addFamilyMember(name, rel, phone, email)
                        },
                        onDeleteFamilyMember = { member -> viewModel.deleteFamilyMember(member) },
                        onTriggerSOS = { name, phone -> viewModel.triggerEmergencySOS(name, phone) }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        userProfile = userProfile,
                        isDarkMode = isDarkMode,
                        isVoiceEnabled = isVoiceEnabled,
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onToggleVoice = { viewModel.toggleVoice() },
                        onNavigateToConnectPatient = { navController.navigate(Screen.ConnectPatient.route) },
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
