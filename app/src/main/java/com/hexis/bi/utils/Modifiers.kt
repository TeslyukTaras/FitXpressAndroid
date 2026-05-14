package com.hexis.bi.utils

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import com.hexis.bi.ui.theme.GlassRimHighlight
import com.hexis.bi.utils.constants.GlassConstants
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

private fun glassDeadZoneCenters(size: Size, isCircle: Boolean): Pair<Offset, Offset> {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val rIn = min(size.width, size.height) * 0.5f
    val aspect = min(size.width, size.height) / max(size.width, size.height).coerceAtLeast(1e-6f)
    val circularish = isCircle || aspect >= GlassConstants.DEAD_ZONE_POLAR_ASPECT_THRESHOLD

    val dist = rIn * when {
        isCircle -> 0.25f
        circularish -> GlassConstants.DEAD_ZONE_CENTER_INSET_POLAR_FRAC * 0.6f
        else -> GlassConstants.DEAD_ZONE_CENTER_INSET_FRAC
    }

    return if (circularish) {
        val tr = -PI / 4f
        val bl = 3f * PI / 4f
        Offset(cx + dist * cos(tr).toFloat(), cy + dist * sin(tr).toFloat()) to
                Offset(cx + dist * cos(bl).toFloat(), cy + dist * sin(bl).toFloat())
    } else {
        val rdx = size.width * 0.5f
        val rdy = size.height * 0.5f
        fun unit(dx: Float, dy: Float): Offset {
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1e-4f)
            return Offset(dx / len, dy / len)
        }

        val uTr = unit(rdx, -rdy)
        val uBl = unit(-rdx, rdy)
        Offset(cx + uTr.x * dist, cy + uTr.y * dist) to Offset(cx + uBl.x * dist, cy + uBl.y * dist)
    }
}

private fun glassDeadZoneBrush(
    center: Offset,
    radius: Float,
    peakAlpha: Float,
    softPolar: Boolean
): Brush {
    val r = radius.coerceAtLeast(1f)
    return Brush.radialGradient(
        0f to Color.Black.copy(alpha = if (softPolar) (peakAlpha * 1.2f).coerceAtMost(1f) else peakAlpha),
        0.42f to Color.Black.copy(alpha = peakAlpha * 0.5f),
        1f to Color.Transparent,
        center = center,
        radius = r,
    )
}

fun Modifier.gradientBackground(brush: Brush, shape: Shape): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline = outline, brush = brush)
}

fun Modifier.glass(
    shape: Shape,
    level: Int = GlassConstants.LEVEL_DEFAULT,
    fill: Color = Color.Transparent,
    fillBrush: ((Size) -> Brush)? = null,
    tint: Color = GlassRimHighlight,
    backgroundAlpha: Float? = null,
    backgroundBlur: Dp = Dp.Unspecified,
    rimWidth: Dp = Dp.Unspecified,
    lightingStrength: Float = 1f,
    hazeAlpha: Float? = null,
): Modifier {
    val intensity = (level / 100f).coerceIn(0f, 1f)
    val isCircleShape = shape == CircleShape
    val glassLayerAlpha = (backgroundAlpha ?: intensity).coerceIn(0f, 1f)
    val lightingAlpha = lightingStrength.coerceIn(0f, 1f)
    val hazeLayerAlpha = (hazeAlpha ?: intensity).coerceIn(0f, 1f)

    return this
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val strokeWidthPx = if (rimWidth.value.isFinite()) rimWidth.toPx() * 2f else 0f
            val rimStroke = Stroke(width = strokeWidthPx)
            val backgroundBlurPx = if (backgroundBlur.value.isFinite()) backgroundBlur.toPx() else 0f
            val shouldUseGlassLayer = glassLayerAlpha < 1f || backgroundBlurPx > 0f
            val glassLayerPaint = Paint().apply {
                alpha = (glassLayerAlpha * 255).roundToInt()
                if (backgroundBlurPx > 0f) {
                    maskFilter = BlurMaskFilter(
                        backgroundBlurPx,
                        BlurMaskFilter.Blur.NORMAL,
                    )
                }
            }

            val minSide = min(size.width, size.height).coerceAtLeast(1f)
            val aspect = minSide / max(size.width, size.height).coerceAtLeast(1e-6f)
            val trueCircle =
                isCircleShape && aspect >= GlassConstants.DEAD_ZONE_POLAR_ASPECT_THRESHOLD
            val pillShape = isCircleShape && !trueCircle
            val polarGlass = trueCircle || aspect >= GlassConstants.DEAD_ZONE_POLAR_ASPECT_THRESHOLD

            val rimAlpha =
                (GlassConstants.RIM_BASE_ALPHA + GlassConstants.RIM_RANGE_ALPHA * intensity) * lightingAlpha * if (trueCircle) 0.75f else 1f
            val glowAlpha =
                (GlassConstants.GLOW_BASE_ALPHA + GlassConstants.GLOW_RANGE_ALPHA * intensity) * lightingAlpha * if (trueCircle) 0.4f else 1f
            val blInnerAlpha =
                (GlassConstants.INNER_BL_SHADOW_BASE_ALPHA + GlassConstants.INNER_BL_SHADOW_RANGE_ALPHA * intensity) * lightingAlpha
            val trHighlightAlpha =
                (GlassConstants.INNER_TR_HIGHLIGHT_BASE_ALPHA + GlassConstants.INNER_TR_HIGHLIGHT_RANGE_ALPHA * intensity) * lightingAlpha * if (trueCircle) 0.6f else 1f
            val deadAlpha =
                (GlassConstants.DEAD_ZONE_BASE_ALPHA + GlassConstants.DEAD_ZONE_RANGE_ALPHA * intensity) * lightingAlpha * if (trueCircle) 1.5f else 1f
            val blVignetteAlpha =
                (GlassConstants.BL_VIGNETTE_BASE_ALPHA + GlassConstants.BL_VIGNETTE_RANGE_ALPHA * intensity) * lightingAlpha * if (trueCircle) 1.5f else 1f

            val glowBrush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(size.width, 0f), radius = size.maxDimension.coerceAtLeast(1f),
            )
            val blInnerShadowBrush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = blInnerAlpha), Color.Transparent),
                center = Offset(0f, size.height),
                radius = minSide * GlassConstants.INNER_BL_SHADOW_RADIUS_FRAC,
            )
            val trInnerHighlightBrush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = trHighlightAlpha), Color.Transparent),
                center = Offset(size.width, 0f),
                radius = minSide * GlassConstants.INNER_TR_HIGHLIGHT_RADIUS_FRAC,
            )
            val blVignetteBrush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = blVignetteAlpha)),
                center = Offset(0f, size.height), radius = minSide * 0.95f,
            )
            val hazeBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF173735).copy(alpha = hazeLayerAlpha * 0.88f),
                    Color(0xFF020808).copy(alpha = hazeLayerAlpha * 0.96f),
                ),
            )

            val (deadTrCenter, deadBlCenter) = glassDeadZoneCenters(size, trueCircle)

            // logic: rotate by 45 degrees by creating a square bounding box for the gradient
            // even if the pill is wide, this anchors the light strictly to the corner.
            val roundRimBrush = Brush.linearGradient(
                0.0f to tint.copy(alpha = rimAlpha),
                0.4f to tint.copy(alpha = rimAlpha * 0.05f),
                0.6f to tint.copy(alpha = rimAlpha * 0.05f),
                1.0f to tint.copy(alpha = rimAlpha * 0.4f),
                start = Offset.Zero,
                end = Offset(minSide, minSide) // forces a 45-degree angle
            )

            val cx = size.width / 2f
            val cy = size.height / 2f
            val shift = minSide * GlassConstants.RIM_SWEEP_NW_SHIFT
            val sx = cx - shift
            val sy = cy - shift

            fun sweepFrac(dx: Float, dy: Float): Float =
                ((atan2(dy, dx).let { if (it < 0f) it + TWO_PI else it }) / TWO_PI)

            val fNW = sweepFrac(-sx, -sy)
            val fNE = sweepFrac(size.width - sx, -sy)
            val fSE = sweepFrac(size.width - sx, size.height - sy)
            val fSW = sweepFrac(-sx, size.height - sy)
            val g = if (pillShape) {
                GlassConstants.RIM_TRANSITION_FRACTION * 3.25f
            } else {
                GlassConstants.RIM_TRANSITION_FRACTION
            }
            val minT = if (pillShape) {
                GlassConstants.RIM_MIN_TRANSITION * 3.5f
            } else {
                GlassConstants.RIM_MIN_TRANSITION
            }

            fun trans(edgeSpan: Float) =
                (g * edgeSpan).coerceAtLeast(minT).coerceAtMost(edgeSpan * 0.49f)

            val tBottom = trans(fSW - fSE)
            val tLeft = trans(fNW - fSW)
            val tTop = trans(fNE - fNW)
            val tRightLo = trans(fSE)
            val tRightHi = trans(1f - fNE)
            fun rim(f: Float) = tint.copy(alpha = (rimAlpha * f).coerceIn(0f, 1f))
            fun quietRim(f: Float): Color = rim(if (pillShape) f * 0.45f else f)

            val rectRimBrush = Brush.sweepGradient(
                0f to rim(GlassConstants.RIM_RIGHT),
                fSE - tRightLo to rim(GlassConstants.RIM_RIGHT),
                fSE to rim(GlassConstants.RIM_SE),
                fSE + tBottom to rim(GlassConstants.RIM_BOTTOM),
                fSW - tBottom to rim(GlassConstants.RIM_BOTTOM),
                fSW to quietRim(GlassConstants.RIM_SW),
                fSW + tLeft to rim(GlassConstants.RIM_LEFT),
                fNW - tLeft to rim(GlassConstants.RIM_LEFT),
                fNW to rim(GlassConstants.RIM_NW),
                fNW + tTop to rim(GlassConstants.RIM_TOP),
                fNE - tTop to rim(GlassConstants.RIM_TOP),
                fNE to quietRim(GlassConstants.RIM_NE),
                fNE + tRightHi to rim(GlassConstants.RIM_RIGHT),
                1f to rim(GlassConstants.RIM_RIGHT),
                center = Offset(sx, sy)
            )
            val circleRimBrush = Brush.sweepGradient(
                0.000f to rim(GlassConstants.RIM_RIGHT * 0.42f),
                0.090f to rim(GlassConstants.RIM_SE * 0.28f),
                0.250f to rim(GlassConstants.RIM_BOTTOM * 0.24f),
                0.375f to rim(GlassConstants.RIM_SW),
                0.500f to rim(GlassConstants.RIM_LEFT * 0.72f),
                0.625f to rim(GlassConstants.RIM_NW),
                0.760f to rim(GlassConstants.RIM_TOP),
                0.875f to rim(GlassConstants.RIM_NE),
                1.000f to rim(GlassConstants.RIM_RIGHT * 0.42f),
                center = Offset(cx, cy),
            )

            onDrawWithContent {
                val glassLayerCheckpoint = if (shouldUseGlassLayer) {
                    drawContext.canvas.nativeCanvas.saveLayer(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        glassLayerPaint,
                    )
                } else {
                    null
                }

                if (hazeLayerAlpha > 0f) {
                    drawOutline(outline = outline, brush = hazeBrush)
                }
                when {
                    fillBrush != null -> drawOutline(outline = outline, brush = fillBrush(size))
                    fill.alpha > 0f -> drawRect(color = fill)
                }
                drawRect(brush = blInnerShadowBrush)

                if (deadAlpha > 0f) {
                    drawRect(
                        brush = glassDeadZoneBrush(
                            deadTrCenter,
                            minSide * 1.5f,
                            deadAlpha * 0.35f,
                            polarGlass
                        ), blendMode = if (polarGlass) BlendMode.Multiply else BlendMode.SrcOver
                    )
                    drawRect(
                        brush = glassDeadZoneBrush(
                            deadBlCenter,
                            minSide * 1.5f,
                            deadAlpha * 1.15f,
                            polarGlass
                        ), blendMode = if (polarGlass) BlendMode.Multiply else BlendMode.SrcOver
                    )
                }
                if (glowAlpha > 0f) drawRect(brush = glowBrush)
                if (blVignetteAlpha > 0f) drawRect(brush = blVignetteBrush)
                drawRect(brush = trInnerHighlightBrush)

                val sX = (size.width - strokeWidthPx) / size.width
                val sY = (size.height - strokeWidthPx) / size.height

                if (trueCircle) {
                    drawCircle(
                        brush = circleRimBrush,
                        radius = minSide * 0.5f - strokeWidthPx * 0.5f,
                        center = Offset(cx, cy),
                        style = rimStroke,
                    )
                } else {
                    scale(sX, sY, pivot = Offset(cx, cy)) {
                        drawOutline(
                            outline = outline,
                            brush = if (polarGlass) roundRimBrush else rectRimBrush,
                            style = rimStroke
                        )
                    }
                }

                if (trueCircle) {
                    drawCircle(
                        color = tint.copy(alpha = rimAlpha * 0.16f),
                        radius = minSide * 0.5f - strokeWidthPx * 1.6f,
                        center = Offset(cx, cy),
                        style = Stroke(width = strokeWidthPx * 0.45f),
                    )
                }

                if (glassLayerCheckpoint != null) {
                    drawContext.canvas.nativeCanvas.restoreToCount(glassLayerCheckpoint)
                }

                drawContent()
            }
        }
}
