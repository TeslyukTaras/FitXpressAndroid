package com.hexis.bi.ui.dark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hexis.bi.ui.theme.dark.BodyGlassGreen
import com.hexis.bi.ui.theme.dark.BodyGlassInk
import com.hexis.bi.ui.theme.dark.DarkMeshBottom
import com.hexis.bi.ui.theme.dark.DarkMeshTop
import com.hexis.bi.utils.constants.DarkBackgroundConstants
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
internal fun Modifier.darkScreenBackground(
    scrimReferenceHeight: Dp? = null,
): Modifier {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val referenceHeight = scrimReferenceHeight ?: screenHeight

    return drawBehind {
        val referenceHeightPx = referenceHeight.toPx()
        drawScreenMesh()
        drawTopScrim(referenceHeightPx)
        drawBottomScrim(referenceHeightPx)
    }
}

private fun DrawScope.drawScreenMesh() {
    val (meshStart, meshEnd) = gradientEndpoints(size, DarkBackgroundConstants.MESH_GRADIENT_ANGLE_DEG)
    val line = Offset(meshEnd.x - meshStart.x, meshEnd.y - meshStart.y)
    val extra = DarkBackgroundConstants.MESH_GRADIENT_LINE_LENGTH_FACTOR - 1f
    val startExtended = Offset(meshStart.x - line.x * extra, meshStart.y - line.y * extra)
    drawRect(
        brush = Brush.linearGradient(
            listOf(DarkMeshTop, DarkMeshBottom),
            start = startExtended,
            end = meshEnd,
        ),
    )
}

private fun DrawScope.drawTopScrim(referenceHeightPx: Float) {
    val height = (referenceHeightPx * DarkBackgroundConstants.TOP_SCRIM_HEIGHT_FRACTION)
        .coerceAtMost(size.height)
    if (height <= 0f) return
    drawRect(
        topLeft = Offset.Zero,
        size = Size(size.width, height),
        brush = Brush.verticalGradient(
            0f to Color.Black,
            DarkBackgroundConstants.TOP_SCRIM_HOLD_FRACTION to Color.Black,
            1f to Color.Transparent,
            startY = 0f,
            endY = height,
        ),
    )
}

private fun DrawScope.drawBottomScrim(referenceHeightPx: Float) {
    val height = (referenceHeightPx * DarkBackgroundConstants.BOTTOM_SCRIM_HEIGHT_FRACTION)
        .coerceAtMost(size.height)
    if (height <= 0f) return
    val top = size.height - height
    drawRect(
        topLeft = Offset(0f, top),
        size = Size(size.width, height),
        brush = Brush.verticalGradient(
            colors = listOf(
                DarkBackgroundConstants.BOTTOM_SCRIM_START,
                DarkBackgroundConstants.BOTTOM_SCRIM_END,
            ),
            startY = top,
            endY = size.height,
        ),
    )
}

internal fun bodyGlassCardFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, DarkBackgroundConstants.CARD_FILL_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        DarkBackgroundConstants.CARD_FILL_STOP_START to BodyGlassGreen,
        DarkBackgroundConstants.CARD_FILL_STOP_END to BodyGlassInk,
        start = start,
        end = end,
    )
}

internal fun darkMainNavBarFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, DarkBackgroundConstants.MAIN_NAV_BAR_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        0f to DarkBackgroundConstants.MAIN_NAV_BAR_START_COLOR,
        DarkBackgroundConstants.MAIN_NAV_BAR_HOLD_FRACTION to DarkBackgroundConstants.MAIN_NAV_BAR_START_COLOR,
        1f to DarkBackgroundConstants.MAIN_NAV_BAR_END_COLOR,
        start = start,
        end = end,
    )
}

internal fun darkScanFabFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, DarkBackgroundConstants.SCAN_FAB_GRADIENT_ANGLE_DEG)
    return Brush.linearGradient(
        0f to DarkBackgroundConstants.SCAN_FAB_START_COLOR,
        DarkBackgroundConstants.SCAN_FAB_HOLD_FRACTION to DarkBackgroundConstants.SCAN_FAB_START_COLOR,
        1f to DarkBackgroundConstants.SCAN_FAB_END_COLOR,
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
