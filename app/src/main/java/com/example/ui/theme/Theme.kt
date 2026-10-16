package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Explicit Smallpdf Light Color Scheme - Dynamic colors and default purple are strictly disabled
private val SmallpdfLightColorScheme = lightColorScheme(
    primary = SmallpdfPrimary,
    onPrimary = Color.White,
    primaryContainer = SmallpdfInfoBg,
    onPrimaryContainer = SmallpdfNavy,
    secondary = SmallpdfNavy,
    onSecondary = Color.White,
    secondaryContainer = SmallpdfBorder,
    onSecondaryContainer = SmallpdfNavy,
    tertiary = ToolMerge,
    onTertiary = Color.White,
    background = SmallpdfBackground,
    onBackground = SmallpdfTextPrimary,
    surface = SmallpdfSurface,
    onSurface = SmallpdfTextPrimary,
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = SmallpdfTextSecondary,
    outline = SmallpdfBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = SmallpdfError,
    onError = Color.White
)

// Explicit Smallpdf Dark Color Scheme
private val SmallpdfDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF60A5FA),
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = ToolMerge,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF334155),
    error = SmallpdfError,
    onError = Color.White
)

@Composable
fun SmallpdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We intentionally enforce Smallpdf custom palette and never let Android dynamic colors override
    val colorScheme = if (darkTheme) SmallpdfDarkColorScheme else SmallpdfLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
