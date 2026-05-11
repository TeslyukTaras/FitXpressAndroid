package com.hexis.bi.ui.dark

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.hexis.bi.ui.theme.dark.BodyGlassGreen
import com.hexis.bi.ui.theme.dark.BodyGlassInk
import com.hexis.bi.ui.theme.dark.DarkMeshBottom
import com.hexis.bi.ui.theme.dark.DarkMeshTop
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val MeshGradientAngleDeg = 190.26f

private const val MeshGradientLineLengthFactor = 1.2323f
private const val CardFillGradientAngleDeg = 183.87f
private const val CardFillStopStart = 0.0372f
private const val CardFillStopEnd = 0.9901f

private const val MainNavBarGradientAngleDeg = 146.44f
private const val ScanFabGradientAngleDeg = 262.52f


internal fun Modifier.darkScreenBackground(): Modifier = drawBehind {
    val (meshStart, meshEnd) = gradientEndpoints(size, MeshGradientAngleDeg)
    val line = Offset(meshEnd.x - meshStart.x, meshEnd.y - meshStart.y)
    val extra = MeshGradientLineLengthFactor - 1f
    val meshStartExtended = Offset(
        meshStart.x - line.x * extra,
        meshStart.y - line.y * extra,
    )

    drawRect(
        brush = Brush.linearGradient(
            listOf(DarkMeshTop, DarkMeshBottom),
            start = meshStartExtended,
            end = meshEnd,
        ),
    )

    val scrimH = size.height * 0.3f
    if (scrimH > 0f) {
        drawRect(
            topLeft = Offset.Zero,
            size = Size(size.width, scrimH),
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = 0f,
                endY = scrimH,
            ),
        )
    }
}

internal fun bodyGlassCardFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, CardFillGradientAngleDeg)
    return Brush.linearGradient(
        CardFillStopStart to BodyGlassGreen,
        CardFillStopEnd to BodyGlassInk,
        start = start,
        end = end,
    )
}

/** Main bottom nav track: `linear-gradient(146.44deg, …)` + glass 45. */
internal fun darkMainNavBarFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, MainNavBarGradientAngleDeg)
    val c1 = Color(36 / 255f, 74 / 255f, 73 / 255f, 0.32f)
    val c2 = Color(3 / 255f, 9 / 255f, 9 / 255f, 0.32f)
    return Brush.linearGradient(
        0f to c1,
        0.3932f to c1,
        1f to c2,
        start = start,
        end = end,
    )
}

/** Center scan FAB: `linear-gradient(262.52deg, …)` + glass 80. */
internal fun darkScanFabFillBrush(size: Size): Brush {
    val (start, end) = gradientEndpoints(size, ScanFabGradientAngleDeg)
    val c1 = Color(29 / 255f, 196 / 255f, 179 / 255f, 0.3f)
    val c2 = Color(3 / 255f, 9 / 255f, 9 / 255f, 0.3f)
    return Brush.linearGradient(
        0f to c1,
        0.0721f to c1,
        1f to c2,
        start = start,
        end = end,
    )
}

internal fun gradientEndpoints(size: Size, cssAngleDeg: Float): Pair<Offset, Offset> {
    val theta = Math.toRadians(cssAngleDeg.toDouble())
    val vx = sin(theta).toFloat()
    val vy = -cos(theta).toFloat()
    val cx = size.width / 2f
    val cy = size.height / 2f
    val extent = hypot(size.width.toDouble(), size.height.toDouble()).toFloat() / 2f
    return Offset(cx - vx * extent, cy - vy * extent) to Offset(cx + vx * extent, cy + vy * extent)
}
