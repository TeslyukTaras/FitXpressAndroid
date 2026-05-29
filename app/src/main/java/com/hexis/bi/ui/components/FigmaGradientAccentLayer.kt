package com.hexis.bi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.hexis.bi.ui.theme.AlphaMaskOpaque
import com.hexis.bi.ui.theme.AlphaMaskTransparent
import com.hexis.bi.utils.constants.FigmaGradientAccentConstants
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BoxScope.FigmaGradientAccentLayer(
    figmaLeftDp: Float,
    figmaTopDp: Float,
    figmaWidthDp: Float,
    figmaHeightDp: Float,
    figmaAngleDeg: Float,
    opacity: Float,
    gradientStartColor: Color,
    gradientEndColor: Color,
    modifier: Modifier = Modifier,
    cornerFadeStart: Float = FigmaGradientAccentConstants.DEFAULT_CORNER_FADE_START,
    cornerFadeRadiusFactor: Float = FigmaGradientAccentConstants.DEFAULT_CORNER_FADE_RADIUS_FACTOR,
    sideFadeEnd: Float = FigmaGradientAccentConstants.DEFAULT_SIDE_FADE_END,
) {
    val composeRotationDeg = figmaAngleDeg.toComposeMirroredRotation()
    val rotatedInset = rotatedBoundsInset(
        width = figmaWidthDp,
        height = figmaHeightDp,
        rotationDeg = composeRotationDeg,
    )

    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .offset(
                x = (figmaLeftDp + rotatedInset.x).dp,
                y = (figmaTopDp + rotatedInset.y).dp,
            )
            .size(
                width = figmaWidthDp.dp,
                height = figmaHeightDp.dp,
            )
            .alpha(opacity)
            .rotate(composeRotationDeg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            gradientStartColor,
                            gradientEndColor,
                        ),
                    ),
                )
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                FigmaGradientAccentConstants.MASK_START_FRACTION to AlphaMaskOpaque,
                                cornerFadeStart to AlphaMaskOpaque,
                                FigmaGradientAccentConstants.MASK_END_FRACTION to AlphaMaskTransparent,
                            ),
                            center = center,
                            radius = size.minDimension * cornerFadeRadiusFactor,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                FigmaGradientAccentConstants.MASK_START_FRACTION to AlphaMaskTransparent,
                                sideFadeEnd to AlphaMaskOpaque,
                                (FigmaGradientAccentConstants.MASK_END_FRACTION - sideFadeEnd) to AlphaMaskOpaque,
                                FigmaGradientAccentConstants.MASK_END_FRACTION to AlphaMaskTransparent,
                            ),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
    }
}

private data class BoundsInset(
    val x: Float,
    val y: Float,
)

private fun Float.toComposeMirroredRotation(): Float =
    FigmaGradientAccentConstants.FULL_ROTATION_DEG - this

private fun rotatedBoundsInset(
    width: Float,
    height: Float,
    rotationDeg: Float,
): BoundsInset {
    val radians = rotationDeg * FigmaGradientAccentConstants.RADIANS_PER_DEGREE
    val rotatedWidth = (width * abs(cos(radians))) + (height * abs(sin(radians)))
    val rotatedHeight = (width * abs(sin(radians))) + (height * abs(cos(radians)))

    return BoundsInset(
        x = (rotatedWidth - width) / FigmaGradientAccentConstants.BOUNDS_CENTER_DIVISOR,
        y = (rotatedHeight - height) / FigmaGradientAccentConstants.BOUNDS_CENTER_DIVISOR,
    )
}
