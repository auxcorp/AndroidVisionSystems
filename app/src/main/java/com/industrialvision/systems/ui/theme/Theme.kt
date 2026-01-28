package com.industrialvision.systems.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Industrial Vision Color Palette

// Primary - Industrial Blue
private val Blue80 = Color(0xFFB0C6FF)
private val Blue40 = Color(0xFF0055D4)
private val Blue20 = Color(0xFF003087)

// Secondary - Quality Green
private val Green80 = Color(0xFFA8F0C4)
private val Green40 = Color(0xFF00875A)
private val Green20 = Color(0xFF00522E)

// Tertiary - Alert Orange
private val Orange80 = Color(0xFFFFB86B)
private val Orange40 = Color(0xFFE65100)
private val Orange20 = Color(0xFF8B3000)

// Error - Critical Red
private val Red80 = Color(0xFFFFB4AB)
private val Red40 = Color(0xFFDE3730)
private val Red20 = Color(0xFF8C1D18)

// Neutral colors
private val NeutralDark = Color(0xFF1A1C1E)
private val NeutralLight = Color(0xFFFBFCFE)
private val NeutralVariant40 = Color(0xFF44474F)
private val NeutralVariant90 = Color(0xFFE1E2EC)

// Surface colors for depth
private val SurfaceDark = Color(0xFF121316)
private val SurfaceContainerDark = Color(0xFF1E2022)
private val SurfaceContainerHighDark = Color(0xFF282A2D)

private val SurfaceLight = Color(0xFFFBFCFE)
private val SurfaceContainerLight = Color(0xFFF0F1F5)
private val SurfaceContainerHighLight = Color(0xFFE6E8EC)

// Status Colors
val StatusPass = Color(0xFF00C853)
val StatusFail = Color(0xFFFF1744)
val StatusWarning = Color(0xFFFFAB00)
val StatusPending = Color(0xFF448AFF)
val StatusProcessing = Color(0xFF7C4DFF)

// Severity Colors
val SeverityCritical = Color(0xFFFF1744)
val SeverityMajor = Color(0xFFFF6D00)
val SeverityMinor = Color(0xFFFFAB00)
val SeverityInfo = Color(0xFF448AFF)

// Chart Colors
val ChartColors = listOf(
    Color(0xFF0055D4),
    Color(0xFF00875A),
    Color(0xFFE65100),
    Color(0xFF7C4DFF),
    Color(0xFF00BCD4),
    Color(0xFFFF4081)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue80,

    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green40,
    onSecondaryContainer = Green80,

    tertiary = Orange80,
    onTertiary = Orange20,
    tertiaryContainer = Orange40,
    onTertiaryContainer = Orange80,

    error = Red80,
    onError = Red20,
    errorContainer = Red40,
    onErrorContainer = Red80,

    background = SurfaceDark,
    onBackground = NeutralLight,
    surface = SurfaceDark,
    onSurface = NeutralLight,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = NeutralVariant90,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,

    outline = NeutralVariant40,
    outlineVariant = Color(0xFF44474F)
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Blue20,

    secondary = Green40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F4DC),
    onSecondaryContainer = Green20,

    tertiary = Orange40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Orange20,

    error = Red40,
    onError = Color.White,
    errorContainer = Color(0xFFFCD8D5),
    onErrorContainer = Red20,

    background = SurfaceLight,
    onBackground = NeutralDark,
    surface = SurfaceLight,
    onSurface = NeutralDark,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = NeutralVariant40,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,

    outline = Color(0xFF74777F),
    outlineVariant = NeutralVariant90
)

@Composable
fun IndustrialVisionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
