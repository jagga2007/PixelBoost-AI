package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF041E28),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = VioletAccent,
    onSecondary = Color(0xFF1E0060),
    secondaryContainer = Color(0xFF3B1F8E),
    onSecondaryContainer = Color(0xFFEADBFF),
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF003919),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007A99),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAEDFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF6200EE),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DDFF),
    onSecondaryContainer = Color(0xFF22005D),
    tertiary = Color(0xFF00875A),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1),
)

@Composable
fun PixelBoostTheme(
    darkTheme: Boolean = true, // Default to sleek studio dark theme for photo editing
    dynamicColor: Boolean = false, // Keep intentional luxury palette
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
