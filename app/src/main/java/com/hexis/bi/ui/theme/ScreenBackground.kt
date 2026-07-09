package com.hexis.bi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.hexis.bi.utils.constants.BackgroundConstants
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
internal fun Modifier.screenBackground(
    scrimReferenceHeight: Dp? = null,
): Modifier {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    val referenceHeight = scrimReferenceHeight ?: screenHeight

    return drawBehind {
        val referenceHeightPx = referenceHeight.toPx()
        drawScreenMesh()
        drawTopScrim(referenceHeightPx)
        drawBottomScrim(referenceHeightPx)
    }
}

private fun DrawScope.drawScreenMesh() {
    val (meshStart, meshEnd) = gradientEndpoints(size, BackgroundConstants.MESH_GRADIENT_ANGLE_DEG)
    drawRect(
        brush = Brush.linearGradient(
            0f to MeshTop,
            BackgroundConstants.MESH_GRADIENT_HOLD_FRACTION to MeshTop,
            1f to MeshBottom,
            start = meshStart,
            end = meshEnd,
        ),
    )
}

private fun DrawScope.drawTopScrim(referenceHeightPx: Float) {
    val height = (referenceHeightPx * BackgroundConstants.TOP_SCRIM_HEIGHT_FRACTION)
        .coerceAtMost(size.height)
    if (height <= 0f) return
    drawRect(
        topLeft = Offset.Zero,
        size = Size(size.width, height),
        brush = Brush.verticalGradient(
            0f to Color.Black,
            BackgroundConstants.TOP_SCRIM_HOLD_FRACTION to Color.Black,
            1f to Color.Transparent,
            startY = 0f,
            endY = height,
        ),
    )
}

private fun DrawScope.drawBottomScrim(referenceHeightPx: Float) {
    val height = (referenceHeightPx * BackgroundConstants.BOTTOM_SCRIM_HEIGHT_FRACTION)
        .coerceAtMost(size.height)
    if (height <= 0f) return
    val top = size.height - height
    drawRect(
        topLeft = Offset(0f, top),
        size = Size(size.width, height),
        brush = Brush.verticalGradient(
            colors = listOf(
                BackgroundConstants.BOTTOM_SCRIM_START,
                BackgroundConstants.BOTTOM_SCRIM_END,
            ),
            startY = top,
            endY = size.height,
        ),
    )
}

internal fun bodyGlassCardFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, BackgroundConstants.CARD_FILL_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        BackgroundConstants.CARD_FILL_STOP_START to BodyGlassGreen,
        BackgroundConstants.CARD_FILL_STOP_END to BodyGlassInk,
        start = start,
        end = end,
    )
}

internal fun promoBannerFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, BackgroundConstants.PROMO_BANNER_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        BackgroundConstants.PROMO_BANNER_STOP_START to PromoBannerStart,
        BackgroundConstants.PROMO_BANNER_STOP_END to PromoBannerEnd,
        start = start,
        end = end,
    )
}

internal fun mainNavBarFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, BackgroundConstants.MAIN_NAV_BAR_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        0f to BackgroundConstants.MAIN_NAV_BAR_START_COLOR,
        BackgroundConstants.MAIN_NAV_BAR_HOLD_FRACTION to BackgroundConstants.MAIN_NAV_BAR_START_COLOR,
        1f to BackgroundConstants.MAIN_NAV_BAR_END_COLOR,
        start = start,
        end = end,
    )
}

internal fun scanFabFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, BackgroundConstants.SCAN_FAB_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        0f to BackgroundConstants.SCAN_FAB_START_COLOR,
        BackgroundConstants.SCAN_FAB_HOLD_FRACTION to BackgroundConstants.SCAN_FAB_START_COLOR,
        1f to BackgroundConstants.SCAN_FAB_END_COLOR,
        start = start,
        end = end,
    )
}

/** Converts a CSS gradient angle (0° = up, clockwise) to a line through the box center. */
internal fun gradientEndpoints(size: Size, cssAngleDeg: Float): Pair<Offset, Offset> {
    val theta = Math.toRadians(cssAngleDeg.toDouble())
    val vx = sin(theta).toFloat()
    val vy = -cos(theta).toFloat()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val extent = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
    return Offset(cx - vx * extent, cy - vy * extent) to Offset(cx + vx * extent, cy + vy * extent)
}
