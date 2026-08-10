package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = MedicalPrimary,
    onPrimary = MedicalOnPrimary,
    primaryContainer = MedicalPrimaryContainer,
    onPrimaryContainer = MedicalOnPrimaryContainer,
    secondary = MedicalSecondary,
    onSecondary = MedicalOnSurfaceLight,
    secondaryContainer = MedicalSecondaryContainer,
    onSecondaryContainer = MedicalOnSecondaryContainer,
    tertiary = MedicalTertiary,
    tertiaryContainer = MedicalTertiaryContainer,
    background = MedicalBackgroundLight,
    surface = MedicalSurfaceLight,
    onBackground = MedicalOnSurfaceLight,
    onSurface = MedicalOnSurfaceLight,
    onSurfaceVariant = MedicalSecondary,
    surfaceVariant = MedicalSurfaceVariantLight,
    outline = MedicalOutlineLight,
    outlineVariant = MedicalOutlineLight
)

@Composable
fun MediGuardTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

