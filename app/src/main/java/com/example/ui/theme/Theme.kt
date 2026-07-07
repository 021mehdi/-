package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrightBlue,
    secondary = SlateGray,
    background = DarkNavy,
    surface = Color(0xFF1E293B),
    onPrimary = CardWhite,
    onSecondary = CardWhite,
    onBackground = CardWhite,
    onSurface = CardWhite
)

private val LightColorScheme = lightColorScheme(
    primary = BrightBlue,
    secondary = SlateGray,
    background = LightBg,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun MyApplicationTheme(
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
