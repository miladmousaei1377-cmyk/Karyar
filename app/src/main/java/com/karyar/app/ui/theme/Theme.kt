package com.karyar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Green800,
    secondary = Green500,
    onSecondary = Color.White,
    secondaryContainer = Green200,
    onSecondaryContainer = Green800,
    background = Color.White,
    onBackground = Color(0xFF1C1C1C),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Green50,
    onSurfaceVariant = Color(0xFF424242)
)

private val DarkColorScheme = darkColorScheme(
    primary = Green400,
    onPrimary = Color(0xFF003300),
    primaryContainer = Green700,
    onPrimaryContainer = Green100,
    secondary = Green500,
    onSecondary = Color(0xFF003300),
    secondaryContainer = Green800,
    onSecondaryContainer = Green200,
    background = DarkBackground,
    onBackground = Color(0xFFE0E0E0),
    surface = DarkSurface,
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun KaryarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
