package com.herrderb.launcherli.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.White,
    surface = Color.Transparent,
    background = Color.Transparent,
    onSurface = Color.White,
    onBackground = Color.White,
    error = Color(0xFFEF5350),
    surfaceContainerLow = Color(0xFF1E1E1E).copy(alpha = 0.85f),
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    secondary = Color.Black,
    surface = Color.Transparent,
    background = Color.Transparent,
    onSurface = Color.Black,
    onBackground = Color.Black,
    error = Color(0xFFC62828),
    surfaceContainerLow = Color(0xFFFAFAFA).copy(alpha = 0.85f),
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun LauncherliTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
