package com.hexis.bi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue100,
    onPrimary = White,
    primaryContainer = Gray600,
    onPrimaryContainer = Black,

    secondary = Gray200,
    onSecondary = White,
    secondaryContainer = Gray600,
    onSecondaryContainer = Black,

    tertiary = Gray300,
    onTertiary = White,
    tertiaryContainer = Gray600,
    onTertiaryContainer = Black,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Gray600,
    onSurfaceVariant = SubtitleBlue,

    outline = Gray500,
    outlineVariant = Gray600,

    error = Red100,
    onError = White,

    inverseSurface = Black,
    inverseOnSurface = White,
    inversePrimary = White,
)

// Dark theme mirrors light theme — reserved for future use
private val DarkColorScheme = darkColorScheme(
    primary = Blue100,
    onPrimary = White,
    primaryContainer = Gray600,
    onPrimaryContainer = Black,

    secondary = Gray200,
    onSecondary = White,
    secondaryContainer = Gray600,
    onSecondaryContainer = Black,

    tertiary = Gray300,
    onTertiary = White,
    tertiaryContainer = Gray600,
    onTertiaryContainer = Black,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Gray600,
    onSurfaceVariant = SubtitleBlue,

    outline = Gray500,
    outlineVariant = Gray600,

    error = Red100,
    onError = White,

    inverseSurface = Black,
    inverseOnSurface = White,
    inversePrimary = White,
)

@Composable
fun FitXpressTheme(
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
