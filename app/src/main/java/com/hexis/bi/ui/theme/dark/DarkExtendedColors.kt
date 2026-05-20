package com.hexis.bi.ui.theme.dark

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hexis.bi.ui.theme.AccentBlue

/** Tokens not on [androidx.compose.material3.ColorScheme]; read via [DarkTheme.extendedColors]. */
@Immutable
data class DarkExtendedColors(
    val positive: Color,
    val negative: Color,
    val chartArea: Color,
    val chartLine: Color,
    val bodyMesh: Color,
    val accentBright: Color,
    val accentDeep: Color,
    val accentContainer: Color,
    val cardBorder: Color,
    val tileSurface: Color,
    val textTertiary: Color,
    val screenMeshTop: Color,
    val screenMeshBottom: Color,
    val glassCardFillTop: Color,
    val glassCardFillBottom: Color,
    val surfaceTranslucent: Color,
    val destructive: Color,
    val accentBlue: Color,
)

internal val DefaultDarkExtendedColors = DarkExtendedColors(
    positive = Positive,
    negative = Negative,
    chartArea = ChartBlue,
    chartLine = ChartTeal,
    bodyMesh = BodyCyan,
    accentBright = Green400,
    accentDeep = GreenDeep,
    accentContainer = GreenSoft,
    cardBorder = Hairline,
    tileSurface = SurfaceMuted,
    textTertiary = TextTertiary,
    screenMeshTop = DarkMeshTop,
    screenMeshBottom = DarkMeshBottom,
    glassCardFillTop = BodyGlassGreen,
    glassCardFillBottom = BodyGlassInk,
    surfaceTranslucent = SurfaceTranslucent,
    destructive = ActionRed,
    accentBlue = AccentBlue,
)

internal val LocalDarkExtendedColors = staticCompositionLocalOf { DefaultDarkExtendedColors }
