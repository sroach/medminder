package gy.roach.json.medminder.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * iOS-inspired color palette for Material 3
 */
object IOSColors {
    // Light Theme Colors
    val LightPrimary = Color(0xFF007AFF) // iOS Blue
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFD1E3FF)
    val LightOnPrimaryContainer = Color(0xFF003060)

    val LightSecondary = Color(0xFF5AC8FA) // iOS Light Blue
    val LightOnSecondary = Color(0xFF003547)
    val LightSecondaryContainer = Color(0xFFCFE5FF)
    val LightOnSecondaryContainer = Color(0xFF001E30)

    val LightTertiary = Color(0xFF34C759) // iOS Green
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFDCF8E7)
    val LightOnTertiaryContainer = Color(0xFF002116)

    val LightError = Color(0xFFFF3B30) // iOS Red
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnError = Color(0xFFFFFFFF)
    val LightOnErrorContainer = Color(0xFF410002)

    val LightBackground = Color(0xFFF2F2F7) // iOS Light Background
    val LightOnBackground = Color(0xFF1C1C1E)
    val LightSurface = Color(0xFFFFFFFF)
    val LightOnSurface = Color(0xFF1C1C1E)
    val LightSurfaceVariant = Color(0xFFE5E5EA) // iOS Light Gray
    val LightOnSurfaceVariant = Color(0xFF44444A)

    val LightOutline = Color(0xFF8E8E93) // iOS Gray
    val LightOutlineVariant = Color(0xFFC5C5D0)
    val LightInverseSurface = Color(0xFF303030)
    val LightInverseOnSurface = Color(0xFFEFEFEF)
    val LightInversePrimary = Color(0xFF9FCAFF)
    val LightSurfaceTint = LightPrimary
    val LightScrim = Color(0xFF000000)

    // Dark Theme Colors
    val DarkPrimary = Color(0xFF0A84FF) // iOS Blue (Dark Mode)
    val DarkOnPrimary = Color(0xFFFFFFFF)
    val DarkPrimaryContainer = Color(0xFF004881)
    val DarkOnPrimaryContainer = Color(0xFFD1E3FF)

    val DarkSecondary = Color(0xFF64D2FF) // iOS Light Blue (Dark Mode)
    val DarkOnSecondary = Color(0xFF003547)
    val DarkSecondaryContainer = Color(0xFF004D66)
    val DarkOnSecondaryContainer = Color(0xFFCFE5FF)

    val DarkTertiary = Color(0xFF30D158) // iOS Green (Dark Mode)
    val DarkOnTertiary = Color(0xFFFFFFFF)
    val DarkTertiaryContainer = Color(0xFF003823)
    val DarkOnTertiaryContainer = Color(0xFFDCF8E7)

    val DarkError = Color(0xFFFF453A) // iOS Red (Dark Mode)
    val DarkErrorContainer = Color(0xFF8C1D18)
    val DarkOnError = Color(0xFFFFFFFF)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)

    val DarkBackground = Color(0xFF1C1C1E) // iOS Dark Background
    val DarkOnBackground = Color(0xFFEFEFEF)
    val DarkSurface = Color(0xFF2C2C2E) // iOS Dark Gray
    val DarkOnSurface = Color(0xFFEFEFEF)
    val DarkSurfaceVariant = Color(0xFF3A3A3C) // iOS Dark Gray 2
    val DarkOnSurfaceVariant = Color(0xFFCACAD0)

    val DarkOutline = Color(0xFF8E8E93) // iOS Gray
    val DarkOutlineVariant = Color(0xFF48484A) // iOS Dark Gray 3
    val DarkInverseSurface = Color(0xFFEFEFEF)
    val DarkInverseOnSurface = Color(0xFF1C1C1E)
    val DarkInversePrimary = Color(0xFF0A84FF)
    val DarkSurfaceTint = DarkPrimary
    val DarkScrim = Color(0xFF000000)

    // cards

    val CardBackground = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFE5E5EA)
    val iosShadowColor = Color.Black.copy(alpha = 0.1f)


    val LightCardBackground = Color(0xFFFFFFFF)
    val LightCardBorder = Color(0xFFE5E5EA)
    val LightCardShadow = Color(0x33000000) // Black with 20% opacity

    val DarkCardBackground = Color(0xFF1C1C1E)
    val DarkCardBorder = Color(0xFF2C2C2E)
    val DarkCardShadow = Color(0x33000000) // Black with 20% opacity
}