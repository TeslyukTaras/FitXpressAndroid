package com.hexis.bi.ui.dark

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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.theme.GradientDividerCenter
import com.hexis.bi.ui.theme.GradientDividerEdge
import com.hexis.bi.utils.constants.GradientDividerConstants

private fun horizontalFadeBrush(): Brush = Brush.horizontalGradient(
    GradientDividerConstants.EDGE_STOP to GradientDividerEdge,
    GradientDividerConstants.CENTER_STOP to GradientDividerCenter,
    GradientDividerConstants.END_STOP to GradientDividerEdge,
)

private fun verticalFadeBrush(): Brush = Brush.verticalGradient(
    GradientDividerConstants.EDGE_STOP to GradientDividerEdge,
    GradientDividerConstants.CENTER_STOP to GradientDividerCenter,
    GradientDividerConstants.END_STOP to GradientDividerEdge,
)

/**
 * Horizontal rule: strong in the middle, fading to transparent at the left and right
 * (CSS `linear-gradient(90deg, rgba(189,190,192,0) 0%, #EFF0F3 50%, rgba(189,190,192,0) 100%)`).
 */
@Composable
fun AppHorizontalGradientDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = dimensionResource(R.dimen.border_line),
) {
    val brush = remember { horizontalFadeBrush() }
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
    val brush = remember { verticalFadeBrush() }
    Box(
        modifier
            .width(thickness)
            .fillMaxHeight()
            .background(brush),
    )
}
