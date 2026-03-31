package com.hexis.bi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Blue100,
    onPrimary = White,
    primaryFixed = Gray200,
    primaryContainer = Gray600,
    onPrimaryContainer = Black,

    secondary = Gray100,
    onSecondary = White,
    secondaryFixed = Gray400,
    secondaryContainer = Blue300,
    onSecondaryContainer = White,

    tertiary = Gray300,
    onTertiary = White,
    tertiaryContainer = Gray600,
    onTertiaryContainer = Blue300,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Bg,
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
    primaryFixed = Gray200,
    primaryContainer = Gray600,
    onPrimaryContainer = Black,

    secondary = Gray100,
    onSecondary = White,
    secondaryFixed = Gray400,
    secondaryContainer = Blue300,
    onSecondaryContainer = White,

    tertiary = Gray300,
    onTertiary = White,
    tertiaryContainer = Gray600,
    onTertiaryContainer = Blue300,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,
    surfaceVariant = Bg,
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
        shapes = AppShapes,
        content = content,
    )
}
