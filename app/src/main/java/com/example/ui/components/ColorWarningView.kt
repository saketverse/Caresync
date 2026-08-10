package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LanguageManager

@Composable
fun ColorWarningView(
    riskLevel: String, // "SAFE", "MODERATE" / "CAUTION", "HIGH" / "DANGER"
    rawSummary: String = "",
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    onListenWarning: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    val safeGreen = Color(0xFF34C759)
    val warningOrange = Color(0xFFFF9500)
    val dangerRed = Color(0xFFFF3B30)

    val normalizedRisk = riskLevel.uppercase()
    val (accentColor, iconVector, statusTitle, statusSubtitle) = when {
        normalizedRisk.contains("HIGH") || normalizedRisk.contains("DANGER") || normalizedRisk.contains("RED") -> {
            Tuple4(
                dangerRed,
                Icons.Filled.Warning,
                LanguageManager.getText("status_title_danger", selectedLanguage),
                LanguageManager.getText("warning_danger", selectedLanguage)
            )
        }
        normalizedRisk.contains("MODERATE") || normalizedRisk.contains("CAUTION") || normalizedRisk.contains("YELLOW") -> {
            Tuple4(
                warningOrange,
                Icons.Filled.PriorityHigh,
                LanguageManager.getText("status_title_caution", selectedLanguage),
                LanguageManager.getText("warning_caution", selectedLanguage)
            )
        }
        else -> {
            Tuple4(
                safeGreen,
                Icons.Filled.Check,
                LanguageManager.getText("status_title_safe", selectedLanguage),
                LanguageManager.getText("warning_safe", selectedLanguage)
            )
        }
    }

    val textToSpeak = "$statusTitle. $statusSubtitle. ${if (rawSummary.isNotBlank()) rawSummary else ""}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Small circular accent badge
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(if (isElderMode) 40.dp else 34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(if (isElderMode) 22.dp else 18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = statusTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isElderMode) 17.sp else 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = statusSubtitle,
                            fontSize = if (isElderMode) 14.sp else 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Audio Read Button
                IconButton(
                    onClick = { onListenWarning(textToSpeak) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Listen status",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Learn More Section if detailed summary exists
            if (rawSummary.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expanded) "Hide Details" else "Learn More",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = rawSummary,
                            fontSize = if (isElderMode) 14.sp else 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val a: A, val b: B, val c: C, val d: D
)
