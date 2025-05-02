package gy.roach.json.medminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = IOSColors.LightPrimary,
    onPrimary = IOSColors.LightOnPrimary,
    primaryContainer = IOSColors.LightPrimaryContainer,
    onPrimaryContainer = IOSColors.LightOnPrimaryContainer,
    secondary = IOSColors.LightSecondary,
    onSecondary = IOSColors.LightOnSecondary,
    secondaryContainer = IOSColors.LightSecondaryContainer,
    onSecondaryContainer = IOSColors.LightOnSecondaryContainer,
    tertiary = IOSColors.LightTertiary,
    onTertiary = IOSColors.LightOnTertiary,
    tertiaryContainer = IOSColors.LightTertiaryContainer,
    onTertiaryContainer = IOSColors.LightOnTertiaryContainer,
    error = IOSColors.LightError,
    errorContainer = IOSColors.LightErrorContainer,
    onError = IOSColors.LightOnError,
    onErrorContainer = IOSColors.LightOnErrorContainer,
    background = IOSColors.LightBackground,
    onBackground = IOSColors.LightOnBackground,
    surface = IOSColors.LightSurface,
    onSurface = IOSColors.LightOnSurface,
    surfaceVariant = IOSColors.LightSurfaceVariant,
    onSurfaceVariant = IOSColors.LightOnSurfaceVariant,
    outline = IOSColors.LightOutline,
    outlineVariant = IOSColors.LightOutlineVariant,
    inverseSurface = IOSColors.LightInverseSurface,
    inverseOnSurface = IOSColors.LightInverseOnSurface,
    inversePrimary = IOSColors.LightInversePrimary,
    surfaceTint = IOSColors.LightSurfaceTint,
    scrim = IOSColors.LightScrim
)


// Dark color scheme
private val DarkColorScheme = darkColorScheme(
        primary = IOSColors.DarkPrimary,
        onPrimary = IOSColors.DarkOnPrimary,
        primaryContainer = IOSColors.DarkPrimaryContainer,
        onPrimaryContainer = IOSColors.DarkOnPrimaryContainer,
        secondary = IOSColors.DarkSecondary,
        onSecondary = IOSColors.DarkOnSecondary,
        secondaryContainer = IOSColors.DarkSecondaryContainer,
        onSecondaryContainer = IOSColors.DarkOnSecondaryContainer,
        tertiary = IOSColors.DarkTertiary,
        onTertiary = IOSColors.DarkOnTertiary,
        tertiaryContainer = IOSColors.DarkTertiaryContainer,
        onTertiaryContainer = IOSColors.DarkOnTertiaryContainer,
        error = IOSColors.DarkError,
        errorContainer = IOSColors.DarkErrorContainer,
        onError = IOSColors.DarkOnError,
        onErrorContainer = IOSColors.DarkOnErrorContainer,
        background = IOSColors.DarkBackground,
        onBackground = IOSColors.DarkOnBackground,
        surface = IOSColors.DarkSurface,
        onSurface = IOSColors.DarkOnSurface,
        surfaceVariant = IOSColors.DarkSurfaceVariant,
        onSurfaceVariant = IOSColors.DarkOnSurfaceVariant,
        outline = IOSColors.DarkOutline,
        outlineVariant = IOSColors.DarkOutlineVariant,
        inverseSurface = IOSColors.DarkInverseSurface,
        inverseOnSurface = IOSColors.DarkInverseOnSurface,
        inversePrimary = IOSColors.DarkInversePrimary,
        surfaceTint = IOSColors.DarkSurfaceTint,
        scrim = IOSColors.DarkScrim
    )

/**
 * Determines if dark theme should be used based on the provided theme mode
 */
@Composable
fun shouldUseDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

/**
 * MedMinder app theme that supports light/dark mode based on user preferences or system settings.
 * Uses Material 3 for styling.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = shouldUseDarkTheme(themeMode)

    val colorScheme = if (!useDarkTheme) {
        LightColorScheme
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
