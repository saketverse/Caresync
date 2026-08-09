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

private val DarkColorScheme = darkColorScheme(
    primary = MedicalPrimaryDark,
    onPrimary = MedicalOnPrimaryDark,
    primaryContainer = MedicalPrimaryContainerDark,
    onPrimaryContainer = MedicalOnPrimaryContainerDark,
    secondary = MedicalSecondaryDark,
    onSecondary = MedicalOnSurfaceDark,
    secondaryContainer = MedicalSecondaryContainerDark,
    onSecondaryContainer = MedicalOnSecondaryContainerDark,
    tertiary = MedicalTertiary,
    tertiaryContainer = MedicalSecondaryContainerDark,
    background = MedicalBackgroundDark,
    surface = MedicalSurfaceDark,
    onBackground = MedicalOnSurfaceDark,
    onSurface = MedicalOnSurfaceDark,
    onSurfaceVariant = MedicalSecondaryDark,
    surfaceVariant = MedicalSurfaceVariantDark,
    outline = MedicalOutlineDark,
    outlineVariant = MedicalSurfaceVariantDark
)

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep medical brand blue identity active
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

