package com.kuote.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KuoteCyan,
    onPrimary = Color(0xFF00363A),
    primaryContainer = KuoteCyanDark,
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = KuoteGreen,
    onSecondary = Color(0xFF003822),
    secondaryContainer = KuoteGreenBg,
    onSecondaryContainer = Color(0xFFA7F3D0),
    background = KuoteBackground,
    onBackground = KuoteTextPrimary,
    surface = KuoteSurface,
    onSurface = KuoteTextPrimary,
    surfaceVariant = KuoteSurfaceVariant,
    onSurfaceVariant = KuoteTextSecondary,
    outline = KuoteBorder,
    outlineVariant = KuoteBorderActive
)

private val LightColorScheme = lightColorScheme(
    primary = KuoteCyanVariant,
    onPrimary = Color.White,
    primaryContainer = KuoteCyan,
    onPrimaryContainer = Color.Black,
    secondary = KuoteGreen,
    onSecondary = Color.White,
    secondaryContainer = KuoteGreenBg,
    onSecondaryContainer = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = KuoteCyan
)

@Composable
fun KuoteTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun CallCatchTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    KuoteTheme(darkTheme = darkTheme, content = content)
}
