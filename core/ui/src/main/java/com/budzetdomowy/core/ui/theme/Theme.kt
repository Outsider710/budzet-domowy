package com.budzetdomowy.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.budzetdomowy.core.data.ThemeMode

private val Green = Color(0xFF1B5E4B)
private val GreenLight = Color(0xFF4C8C74)
private val Cream = Color(0xFFF4F7F5)
private val Ink = Color(0xFF102A22)
private val SoftGreen = Color(0xFFD7EBE2)
private val SoftMint = Color(0xFFE8F5EE)
private val CardWhite = Color(0xFFFFFFFF)
private val BorderSoft = Color(0xFFC5D9CF)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = SoftGreen,
    onPrimaryContainer = Ink,
    secondary = GreenLight,
    onSecondary = Color.White,
    secondaryContainer = SoftMint,
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF0D3B12),
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE3EFE9),
    onSurfaceVariant = Color(0xFF3A5348),
    outline = BorderSoft,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CBC9F),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF1F3F34),
    onPrimaryContainer = Color(0xFFD7EBE2),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF003828),
    secondaryContainer = Color(0xFF244037),
    onSecondaryContainer = Color(0xFFE6F0EB),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF003910),
    tertiaryContainer = Color(0xFF1E3A28),
    onTertiaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFF0E1A16),
    onBackground = Color(0xFFE6F0EB),
    surface = Color(0xFF16241F),
    onSurface = Color(0xFFE6F0EB),
    surfaceVariant = Color(0xFF243530),
    onSurfaceVariant = Color(0xFFC5D5CC),
    outline = Color(0xFF4A6358),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun BudzetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val bg = colorScheme.background.toArgb()
            window.decorView.setBackgroundColor(bg)
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.statusBarColor = colorScheme.primary.toArgb()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
