package com.budzetdomowy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B5E4B)
private val GreenLight = Color(0xFF4C8C74)
private val Cream = Color(0xFFF4F7F5)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = GreenLight,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Color(0xFF102A22),
    surface = Color.White,
    onSurface = Color(0xFF102A22),
    error = Color(0xFFB3261E),
    tertiary = Color(0xFF2E7D32)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CBC9F),
    onPrimary = Color(0xFF003828),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003828),
    background = Color(0xFF0E1A16),
    onBackground = Color(0xFFE6F0EB),
    surface = Color(0xFF16241F),
    onSurface = Color(0xFFE6F0EB),
    error = Color(0xFFF2B8B5),
    tertiary = Color(0xFF81C784)
)

@Composable
fun BudzetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
