package com.hexis.bi.ui.theme.dark

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Green500,
    onPrimary = Black,
    primaryContainer = GreenSoft,
    onPrimaryContainer = Green400,

    secondary = TextSecondary,
    onSecondary = Ink,
    secondaryContainer = SurfaceElevated,
    onSecondaryContainer = TextPrimary,

    tertiary = BodyCyan,
    onTertiary = Black,
    tertiaryContainer = SurfaceMuted,
    onTertiaryContainer = BodyCyan,

    background = Ink,
    onBackground = TextPrimary,

    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = SurfaceMuted,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceElevated,

    outline = Hairline,
    outlineVariant = HairlineStrong,

    error = Negative,
    onError = Black,

    inverseSurface = TextPrimary,
    inverseOnSurface = Ink,
    inversePrimary = GreenDeep,

    scrim = Black,
)
