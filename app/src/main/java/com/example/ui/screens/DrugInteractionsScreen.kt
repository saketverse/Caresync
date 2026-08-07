package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrugInteraction
import com.example.data.Medication
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrugInteractionsScreen(
    medications: List<Medication>,
    savedInteractions: List<DrugInteraction>,
    interactionResult: String?,
    isChecking: Boolean,
    onRunAnalysis: () -> Unit
) {
    var customDrugA by remember { mutableStateOf("") }
    var customDrugB by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Drug Interaction Detector", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MedicalPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = HealthSafe,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini Clinical Safety Engine",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Real-time AI analysis of potential multi-drug contraindications & risks.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onRunAnalysis,
                        enabled = !isChecking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MedicalPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing Schedule with Gemini AI...")
                        } else {
                            Icon(Icons.Filled.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze All Active Medications (${medications.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Risk Scale Guide Legend
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Risk Level Classification Legend:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RiskBadge("🟢 SAFE", HealthSafeContainer, HealthSafe)
                        RiskBadge("🟡 MODERATE RISK", HealthWarningContainer, HealthWarning)
                        RiskBadge("🔴 HIGH RISK", HealthDangerContainer, HealthDanger)
                    }
                }
            }

            // Live Analysis Result Box
            if (interactionResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (interactionResult.contains("HIGH RISK")) HealthDangerContainer
                        else if (interactionResult.contains("MODERATE RISK")) HealthWarningContainer
                        else HealthSafeContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MedicalPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gemini AI Safety Evaluation Output",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = interactionResult,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // History / Verified Interaction Cards
            Text(
                text = "Verified Drug Interactions Database",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            savedInteractions.forEach { item ->
                InteractionCard(item = item)
            }
        }
    }
}

@Composable
fun RiskBadge(text: String, containerColor: Color, textColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun InteractionCard(item: DrugInteraction) {
    val (bgColor, textColor) = when (item.riskLevel) {
        "HIGH" -> HealthDangerContainer to HealthDanger
        "MODERATE" -> HealthWarningContainer to HealthWarning
        else -> HealthSafeContainer to HealthSafe
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.drugA} + ${item.drugB}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                RiskBadge(text = "${item.riskLevel} RISK", containerColor = bgColor, textColor = textColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.summary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.recommendation.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = MedicalPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guidance: ${item.recommendation}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MedicalPrimary
                    )
                }
            }
        }
    }
}
