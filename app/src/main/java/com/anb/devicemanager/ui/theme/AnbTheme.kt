package com.anb.devicemanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

enum class AnbThemeMode {
    LIGHT,
    DARK,
    ADAPTIVE
}

private val PremiumLightColors = lightColorScheme(
    primary = Color(0xFF2459C5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF102F70),
    secondary = Color(0xFFB68A32),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9B5),
    onSecondaryContainer = Color(0xFF3C2B00),
    tertiary = Color(0xFF92701F),
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEAF0FA),
    onSurface = Color(0xFF172033),
    onSurfaceVariant = Color(0xFF53627A),
    outline = Color(0xFF8998B0)
)

private val PremiumDarkColors = darkColorScheme(
    primary = Color(0xFF78A7FF),
    onPrimary = Color(0xFF092B68),
    primaryContainer = Color(0xFF19458F),
    onPrimaryContainer = Color(0xFFDCE7FF),
    secondary = Color(0xFFE7BD65),
    onSecondary = Color(0xFF3C2B00),
    secondaryContainer = Color(0xFF5B4511),
    onSecondaryContainer = Color(0xFFFFE9B5),
    tertiary = Color(0xFFFFD66E),
    background = Color(0xFF0B1020),
    surface = Color(0xFF121A2B),
    surfaceVariant = Color(0xFF202B40),
    onSurface = Color(0xFFE8EEF9),
    onSurfaceVariant = Color(0xFFB3C0D5),
    outline = Color(0xFF62718A)
)

@Composable
fun AnbDeviceManagerTheme(
    mode: AnbThemeMode = AnbThemeMode.ADAPTIVE,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        AnbThemeMode.LIGHT -> false
        AnbThemeMode.DARK -> true
        AnbThemeMode.ADAPTIVE -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (dark) PremiumDarkColors else PremiumLightColors,
        content = content
    )
}
