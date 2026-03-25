package com.hexis.bi.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline

/**
 * Draws a [brush] gradient fill clipped to [shape].
 * Used to apply a gradient background to composables that only accept a solid [Color]
 * (e.g. Material3 [Button] with containerColor = Color.Transparent).
 */
fun Modifier.gradientBackground(brush: Brush, shape: Shape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline = outline, brush = brush)
}
