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
import com.example.data.DoseItem
import com.example.data.LanguageManager
import com.example.data.Medication
import com.example.ui.theme.*

@Composable
fun PillOrganizerSection(
    doseItems: List<DoseItem>,
    selectedLanguage: String,
    isElderMode: Boolean,
    onMarkDoseTaken: (Long, String) -> Unit,
    onSpeakReminder: (String) -> Unit
) {
    // Categorize dose items by time bucket
    val morningMeds = remember(doseItems) { doseItems.filter { it.timeBucket == "Morning" } }
    val afternoonMeds = remember(doseItems) { doseItems.filter { it.timeBucket == "Afternoon" } }
    val eveningMeds = remember(doseItems) { doseItems.filter { it.timeBucket == "Evening" } }
    val nightMeds = remember(doseItems) { doseItems.filter { it.timeBucket == "Night" } }

    val displayBuckets = if (morningMeds.isEmpty() && afternoonMeds.isEmpty() && eveningMeds.isEmpty() && nightMeds.isEmpty()) {
        if (doseItems.isNotEmpty()) listOf("Today's Medicines" to doseItems) else emptyList()
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

    val totalDoses = doseItems.size
    val takenDoses = doseItems.count { it.isTaken }

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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = LanguageManager.getText("pill_organizer", selectedLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeHeading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isElderMode) "Large-print easy daily schedule" else "Grouped daily dosage manager",
                            fontSize = fontSizeBody,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$takenDoses/$totalDoses Done",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (displayBuckets.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No medications scheduled for today.",
                        fontSize = fontSizeTitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            displayBuckets.forEach { (timeBucketTitle, itemsInBucket) ->
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${itemsInBucket.size} Dose(s)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    itemsInBucket.forEach { item ->
                        val isTaken = item.isTaken
                        val isMissed = item.isMissed

                        val containerBg = when {
                            isTaken -> HealthSafeContainer.copy(alpha = 0.25f)
                            isMissed -> HealthDangerContainer.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val borderColor = when {
                            isTaken -> HealthSafeContainer
                            isMissed -> HealthDanger
                            else -> MaterialTheme.colorScheme.outline
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isTaken) 1.dp else 1.5.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = containerBg)
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
                                            color = when {
                                                isTaken -> HealthSafeContainer
                                                isMissed -> HealthDangerContainer
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            },
                                            modifier = Modifier.size(if (isElderMode) 52.dp else 44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when {
                                                        isTaken -> Icons.Filled.CheckCircle
                                                        isMissed -> Icons.Filled.Warning
                                                        else -> Icons.Filled.Medication
                                                    },
                                                    contentDescription = null,
                                                    tint = when {
                                                        isTaken -> HealthSafe
                                                        isMissed -> HealthDanger
                                                        else -> MaterialTheme.colorScheme.primary
                                                    },
                                                    modifier = Modifier.size(if (isElderMode) 32.dp else 24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = item.medicationName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = fontSizeTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Dosage: ${item.dosage} • ${item.beforeOrAfterFood}",
                                                fontSize = fontSizeBody,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Scheduled Time: ${item.timeSlot}",
                                                fontSize = fontSizeBody,
                                                color = if (isMissed) HealthDanger else MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onSpeakReminder("${item.medicationName} ${item.dosage} at ${item.timeSlot}") },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                                            .size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.instructions.isNotBlank()) {
                                        Text(
                                            text = "Note: ${item.instructions}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    Button(
                                        onClick = { onMarkDoseTaken(item.medicationId, item.timeSlot) },
                                        enabled = !isTaken,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTaken) HealthSafe else MaterialTheme.colorScheme.primary,
                                            disabledContainerColor = HealthSafeContainer,
                                            contentColor = Color.White,
                                            disabledContentColor = HealthSafe
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(if (isElderMode) 52.dp else 44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTaken) Icons.Filled.DoneAll else Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isTaken) LanguageManager.getText("taken", selectedLanguage) else LanguageManager.getText("take_now", selectedLanguage),
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
