package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.LanguageManager
import com.example.data.Medication
import com.example.ui.theme.*

@Composable
fun PillOrganizerSection(
    medications: List<Medication>,
    selectedLanguage: String,
    isElderMode: Boolean,
    onMarkTaken: (Long) -> Unit,
    onSpeakReminder: (Medication) -> Unit
) {
    // Categorize medications into 4 time buckets
    val morningMeds = remember(medications) {
        medications.filter { med ->
            val time = med.timeOfConsumption.lowercase()
            time.contains("06:") || time.contains("07:") || time.contains("08:") || time.contains("09:") || time.contains("10:") || time.contains("11:") || time.contains("am") || time.contains("morning") || time.contains("breakfast")
        }
    }

    val afternoonMeds = remember(medications) {
        medications.filter { med ->
            val time = med.timeOfConsumption.lowercase()
            time.contains("12:") || time.contains("01:") || time.contains("02:") || time.contains("03:") || time.contains("04:") || time.contains("afternoon") || time.contains("lunch") || (time.contains("pm") && !time.contains("07:") && !time.contains("08:") && !time.contains("09:") && !time.contains("10:"))
        }
    }

    val eveningMeds = remember(medications) {
        medications.filter { med ->
            val time = med.timeOfConsumption.lowercase()
            time.contains("05:") || time.contains("06:") || time.contains("07:") || time.contains("08:") || time.contains("evening") || time.contains("dinner") || time.contains("snacks")
        }
    }

    val nightMeds = remember(medications) {
        medications.filter { med ->
            val time = med.timeOfConsumption.lowercase()
            time.contains("09:") || time.contains("10:") || time.contains("11:") || time.contains("night") || time.contains("bedtime") || time.contains("sleep")
        }
    }

    // Fallback if none categorized specifically
    val displayMeds = if (morningMeds.isEmpty() && afternoonMeds.isEmpty() && eveningMeds.isEmpty() && nightMeds.isEmpty()) {
        listOf("Today's Medicines" to medications)
    } else {
        listOf(
            LanguageManager.getText("morning", selectedLanguage) to morningMeds,
            LanguageManager.getText("afternoon", selectedLanguage) to afternoonMeds,
            LanguageManager.getText("evening", selectedLanguage) to eveningMeds,
            LanguageManager.getText("night", selectedLanguage) to nightMeds
        ).filter { it.second.isNotEmpty() }
    }

    val fontSizeHeading = if (isElderMode) 22.sp else 18.sp
    val fontSizeTitle = if (isElderMode) 20.sp else 16.sp
    val fontSizeBody = if (isElderMode) 16.sp else 14.sp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = null,
                        tint = MedicalPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = LanguageManager.getText("pill_organizer", selectedLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeHeading,
                            color = MedicalOnSurfaceLight
                        )
                        Text(
                            text = if (isElderMode) "Large-print easy daily schedule" else "Grouped daily dosage manager",
                            fontSize = fontSizeBody,
                            color = MedicalSecondary
                        )
                    }
                }

                Surface(
                    color = MedicalPrimaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${medications.count { it.isTakenToday }}/${medications.size} Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MedicalPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (displayMeds.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MedicalSurfaceLight
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No medications scheduled for today.",
                        fontSize = fontSizeTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MedicalSecondary
                    )
                }
            }
        } else {
            displayMeds.forEach { (timeBucketTitle, medsInBucket) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timeBucketTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeTitle,
                            color = MedicalOnSurfaceLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MedicalSecondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${medsInBucket.size} Item(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedicalSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    medsInBucket.forEach { med ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (med.isTakenToday) 1.dp else 2.dp,
                                    color = if (med.isTakenToday) HealthSafeContainer else MedicalPrimary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (med.isTakenToday) HealthSafeContainer.copy(alpha = 0.4f) else MedicalSurfaceLight
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (med.isTakenToday) HealthSafeContainer else MedicalPrimaryContainer,
                                            modifier = Modifier.size(if (isElderMode) 52.dp else 44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (med.isTakenToday) Icons.Filled.CheckCircle else Icons.Filled.Medication,
                                                    contentDescription = null,
                                                    tint = if (med.isTakenToday) HealthSafe else MedicalPrimary,
                                                    modifier = Modifier.size(if (isElderMode) 32.dp else 24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = med.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = fontSizeTitle,
                                                color = MedicalOnSurfaceLight
                                            )
                                            Text(
                                                text = "Dosage: ${med.dosage} • ${med.beforeOrAfterFood}",
                                                fontSize = fontSizeBody,
                                                fontWeight = FontWeight.Medium,
                                                color = MedicalSecondary
                                            )
                                            Text(
                                                text = "Time: ${med.timeOfConsumption}",
                                                fontSize = fontSizeBody,
                                                color = MedicalPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onSpeakReminder(med) },
                                        modifier = Modifier
                                            .background(MedicalPrimaryContainer, RoundedCornerShape(10.dp))
                                            .size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = MedicalPrimary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (med.instructions.isNotBlank()) {
                                        Text(
                                            text = "Note: ${med.instructions}",
                                            fontSize = 12.sp,
                                            color = MedicalSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    Button(
                                        onClick = { onMarkTaken(med.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (med.isTakenToday) HealthSafe else MedicalPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(if (isElderMode) 52.dp else 44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (med.isTakenToday) Icons.Filled.DoneAll else Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (med.isTakenToday) LanguageManager.getText("taken", selectedLanguage) else LanguageManager.getText("take_now", selectedLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = fontSizeBody
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
