package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.GradientDividerConstants

/**
 * Which edge(s) a gradient divider fades toward.
 * - [BOTH]: solid in the middle, fading to transparent at both ends (default).
 * - [START]: solid at the start edge, fading to transparent at the end.
 * - [END]: transparent at the start edge, strengthening to solid at the end.
 */
enum class GradientDividerDirection { BOTH, START, END }

private fun horizontalFadeBrush(
    direction: GradientDividerDirection,
    edge: Color,
    center: Color
): Brush =
    when (direction) {
        GradientDividerDirection.BOTH -> Brush.horizontalGradient(
            GradientDividerConstants.EDGE_STOP to edge,
            GradientDividerConstants.CENTER_STOP to center,
            GradientDividerConstants.END_STOP to edge,
        )

        GradientDividerDirection.START -> Brush.horizontalGradient(
            GradientDividerConstants.EDGE_STOP to center,
            GradientDividerConstants.END_STOP to edge,
        )

        GradientDividerDirection.END -> Brush.horizontalGradient(
            GradientDividerConstants.EDGE_STOP to edge,
            GradientDividerConstants.END_STOP to center,
        )
    }

private fun verticalFadeBrush(edge: Color, center: Color): Brush = Brush.verticalGradient(
    GradientDividerConstants.EDGE_STOP to edge,
    GradientDividerConstants.CENTER_STOP to center,
    GradientDividerConstants.END_STOP to edge,
)

/**
 * Horizontal rule: strong in the middle, fading to transparent at the left and right
 * (CSS `linear-gradient(90deg, rgba(189,190,192,0) 0%, #EFF0F3 50%, rgba(189,190,192,0) 100%)`).
 */
@Composable
fun AppHorizontalGradientDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = dimensionResource(R.dimen.border_line),
    direction: GradientDividerDirection = GradientDividerDirection.BOTH,
) {
    val edge = NocturnePulseTheme.extendedColors.gradientDividerEdge
    val center = NocturnePulseTheme.extendedColors.gradientDividerCenter
    val brush = remember(direction, edge, center) { horizontalFadeBrush(direction, edge, center) }
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(brush),
    )
}

/**
 * Vertical rule: strong in the middle, fading to transparent at the top and bottom
 * (same center color as the horizontal variant; gradient runs along height).
 */
@Composable
fun AppVerticalGradientDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = dimensionResource(R.dimen.border_line),
) {
    val edge = NocturnePulseTheme.extendedColors.gradientDividerEdge
    val center = NocturnePulseTheme.extendedColors.gradientDividerCenter
    val brush = remember(edge, center) { verticalFadeBrush(edge, center) }
    Box(
        modifier
            .width(thickness)
            .fillMaxHeight()
            .background(brush),
    )
}
