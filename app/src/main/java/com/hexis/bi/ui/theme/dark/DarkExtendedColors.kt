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
    val chartCenterLine: Color,
    val chartGridLine: Color,
    val chartVerticalLine: Color,
    val chartAxisLine: Color,
    val chartBoundaryLine: Color,
    val chartLeanAdvantage: Color,
    val chartFatAdvantage: Color,
    val chartZeroLabel: Color,
    val chartSelectionLine: Color,
    val bodyMesh: Color,
    val accentBright: Color,
    val accentDeep: Color,
    val accentContainer: Color,
    val cardBorder: Color,
    val tileSurface: Color,
    val textTertiary: Color,
    val timestamp: Color,
    val screenMeshTop: Color,
    val screenMeshBottom: Color,
    val glassCardFillTop: Color,
    val glassCardFillBottom: Color,
    val glassCardHighlightTopStart: Color,
    val glassCardHighlightBottomEnd: Color,
    val surfaceTranslucent: Color,
    val destructive: Color,
    val accentBlue: Color,
    val gaugeLow: Color,
    val gaugeMid: Color,
    val gaugeHigh: Color,
    val gaugeTrack: Color,
)

internal val DefaultDarkExtendedColors = DarkExtendedColors(
    positive = Positive,
    negative = Negative,
    chartArea = ChartBlue,
    chartLine = ChartTeal,
    chartCenterLine = ChartCenterLine,
    chartGridLine = ChartGridLine,
    chartVerticalLine = ChartVerticalLine,
    chartAxisLine = ChartAxisLine,
    chartBoundaryLine = ChartBoundaryLine,
    chartLeanAdvantage = ChartLeanAdvantage,
    chartFatAdvantage = ChartFatAdvantage,
    chartZeroLabel = ChartZeroLabel,
    chartSelectionLine = ChartSelectionLine,
    bodyMesh = BodyCyan,
    accentBright = Green400,
    accentDeep = GreenDeep,
    accentContainer = GreenSoft,
    cardBorder = Hairline,
    tileSurface = SurfaceMuted,
    textTertiary = TextTertiary,
    timestamp = DarkTextMuted,
    screenMeshTop = DarkMeshTop,
    screenMeshBottom = DarkMeshBottom,
    glassCardFillTop = BodyGlassGreen,
    glassCardFillBottom = BodyGlassInk,
    glassCardHighlightTopStart = BodyGlassHighlightTopStart,
    glassCardHighlightBottomEnd = BodyGlassHighlightBottomEnd,
    surfaceTranslucent = SurfaceTranslucent,
    destructive = ActionRed,
    accentBlue = AccentBlue,
    gaugeLow = GaugeLow,
    gaugeMid = GaugeMid,
    gaugeHigh = GaugeHigh,
    gaugeTrack = GaugeTrack,
)

internal val LocalDarkExtendedColors = staticCompositionLocalOf { DefaultDarkExtendedColors }
