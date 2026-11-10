package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PharaohGold,
    onPrimary = PharaohNavyDark,
    primaryContainer = PharaohNavyLight,
    onPrimaryContainer = PharaohGoldLight,
    secondary = PharaohGoldLight,
    onSecondary = PharaohNavyDark,
    secondaryContainer = PharaohNavy,
    onSecondaryContainer = Color.White,
    tertiary = PharaohTerracotta,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFD1D5DB)
)

private val LightColorScheme = lightColorScheme(
    primary = PharaohNavy,
    onPrimary = Color.White,
    primaryContainer = PharaohNavyDark,
    onPrimaryContainer = PharaohGoldContainer,
    secondary = PharaohGoldDark,
    onSecondary = Color.White,
    secondaryContainer = PharaohGoldContainer,
    onSecondaryContainer = PharaohNavyDark,
    tertiary = PharaohTerracotta,
    background = LightBackground,
    surface = LightSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = PharaohPapyrus,
    onSurfaceVariant = TextSecondary
)

@Composable
fun PharaohsTheme(
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
