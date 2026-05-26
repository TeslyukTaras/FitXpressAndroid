package com.hexis.bi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.hexis.bi.ui.theme.dark.Black
import com.hexis.bi.ui.theme.dark.DarkOverlayBorder
import com.hexis.bi.ui.theme.dark.White

/** Surfaces at or below this luminance are treated as a dark theme. */
private const val DARK_SURFACE_LUMINANCE_MAX = 0.5f

/**
 * Subtle edge for overlay surfaces (dialogs, bottom sheets): contrasting `#FFFFFF66` on dark
 * themes, the surface colour on light themes (so it blends with the background and reads as no
 * border). Derived from the active scheme — no separate dark-theme flag needed.
 */
fun ColorScheme.overlayBorder(): Color =
    if (surface.luminance() < DARK_SURFACE_LUMINANCE_MAX) DarkOverlayBorder else surfaceContainerHighest

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
    surfaceContainerHighest = White,

    outline = Gray500,
    outlineVariant = Gray600,

    error = Red100,
    onError = White,

    inverseSurface = Black,
    inverseOnSurface = White,
    inversePrimary = White,

    scrim = DialogBackdrop
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
    surfaceContainerHighest = White,

    outline = Gray500,
    outlineVariant = Gray600,

    error = Red100,
    onError = White,

    inverseSurface = Black,
    inverseOnSurface = White,
    inversePrimary = White,

    scrim = DialogBackdrop
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
