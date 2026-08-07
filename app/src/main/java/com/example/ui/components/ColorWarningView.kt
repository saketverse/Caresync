package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LanguageManager
import com.example.ui.theme.*

@Composable
fun ColorWarningView(
    riskLevel: String, // "SAFE", "MODERATE" / "CAUTION", "HIGH" / "DANGER"
    rawSummary: String = "",
    selectedLanguage: String = LanguageManager.LANG_ENGLISH,
    isElderMode: Boolean = true,
    onListenWarning: (String) -> Unit = {}
) {
    val (badgeColor, containerColor, iconVector, titleText, descKey, emojiPrefix) = when (riskLevel.uppercase()) {
        "HIGH", "DANGER", "RED" -> Tuple6(
            HealthDanger,
            HealthDangerContainer,
            Icons.Filled.Cancel,
            "🔴 Red: Dangerous Combination",
            "warning_danger",
            "🔴"
        )
        "MODERATE", "CAUTION", "YELLOW" -> Tuple6(
            HealthWarning,
            HealthWarningContainer,
            Icons.Filled.Warning,
            "🟡 Yellow: Use Carefully",
            "warning_caution",
            "🟡"
        )
        else -> Tuple6(
            HealthSafe,
            HealthSafeContainer,
            Icons.Filled.CheckCircle,
            "🟢 Green: Safe Combination",
            "warning_safe",
            "🟢"
        )
    }

    val localizedDesc = LanguageManager.getText(descKey, selectedLanguage)
    val textToSpeak = "$titleText. $localizedDesc. ${rawSummary.take(120)}"

    val fontSizeTitle = if (isElderMode) 20.sp else 16.sp
    val fontSizeBody = if (isElderMode) 16.sp else 14.sp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Surface(
                        shape = CircleShape,
                        color = badgeColor,
                        modifier = Modifier.size(if (isElderMode) 48.dp else 40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isElderMode) 28.dp else 24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeTitle,
                            color = badgeColor
                        )
                        Text(
                            text = "Safety Status Indicator",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { onListenWarning(textToSpeak) },
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LanguageManager.getText("listen_warning", selectedLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "$emojiPrefix $localizedDesc",
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSizeTitle,
                        color = MedicalOnSurfaceLight
                    )

                    if (rawSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rawSummary,
                            fontSize = fontSizeBody,
                            color = MedicalSecondary
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)
