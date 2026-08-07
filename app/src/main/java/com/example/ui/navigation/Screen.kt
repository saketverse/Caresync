package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Splash : Screen("splash", "Splash", Icons.Filled.MedicalServices, Icons.Outlined.MedicalServices)
    object Auth : Screen("auth", "Auth", Icons.Filled.Lock, Icons.Outlined.Lock)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object AddMedication : Screen("add_medication", "Add Medicine", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline)
    object Reminders : Screen("reminders", "Reminders", Icons.Filled.Alarm, Icons.Outlined.Alarm)
    object DrugInteractions : Screen("interactions", "AI Safety", Icons.Filled.Shield, Icons.Outlined.Shield)
    object Chatbot : Screen("chatbot", "AI Assistant", Icons.Filled.SmartToy, Icons.Outlined.SmartToy)
    object PrescriptionScanner : Screen("prescription_scanner", "OCR Scanner", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner)
    object FamilyMonitoring : Screen("family", "Family Care", Icons.Filled.People, Icons.Outlined.PeopleOutline)
    object ConnectPatient : Screen("connect_patient", "Connect Patient", Icons.Filled.Link, Icons.Outlined.Link)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Reminders,
    Screen.PrescriptionScanner,
    Screen.Chatbot,
    Screen.FamilyMonitoring
)
