package com.hexis.bi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.bodyGlassCardFillBrush
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass

@Composable
fun BodyGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(dimensionResource(R.dimen.spacer_m)),
    shape: Shape = MaterialTheme.shapes.medium,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    loading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NocturnePulseTheme.extendedColors
    val loadingIntensity = animateFloatAsState(
        targetValue = if (loading) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (loading) {
                AnimationConstants.GLASS_LOADING_FADE_IN_MS
            } else {
                AnimationConstants.GLASS_LOADING_FADE_OUT_MS
            },
            easing = FastOutSlowInEasing,
        ),
        label = "glassLoadingIntensity",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glass(
                tint = NocturnePulseTheme.extendedColors.glassRimHighlight,
                shape = shape,
                level = GlassConstants.LEVEL_DEFAULT,
                fillBrush = { bodyGlassCardFillBrush(it) },
                backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                rimWidth = dimensionResource(R.dimen.glass_rim_width),
            )
            .then(
                if (loading || loadingIntensity.value > 0f) {
                    Modifier.bodyGlassLoadingSheen(colors.glassRimHighlight, loadingIntensity)
                } else {
                    Modifier
                }
            )
            .then(
                if (highlighted) Modifier.bodyGlassHighlight(
                    topStart = colors.glassCardHighlightTopStart,
                    bottomEnd = colors.glassCardHighlightBottomEnd,
                ) else Modifier
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
private fun Modifier.bodyGlassLoadingSheen(tint: Color, intensity: State<Float>): Modifier {
    val travel = rememberInfiniteTransition(label = "glassLoading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(AnimationConstants.GLASS_LOADING_SWEEP_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glassLoadingSweep",
    )
    return drawWithCache {
        val band = size.width * AnimationConstants.GLASS_LOADING_BAND_FRACTION
        onDrawBehind {
            val sheen = tint.copy(
                alpha = AnimationConstants.GLASS_LOADING_SHEEN_ALPHA * intensity.value,
            )
            val start = -band + (size.width + band * 2) * travel.value
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, sheen, Color.Transparent),
                    start = Offset(start, 0f),
                    end = Offset(start + band, size.height),
                ),
            )
        }
    }
}

private fun Modifier.bodyGlassHighlight(
    topStart: Color,
    bottomEnd: Color,
): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(topStart, Color.Transparent),
            center = Offset.Zero,
            radius = BodyGlassHighlightTopStartRadius.toPx(),
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(bottomEnd, Color.Transparent),
            center = Offset(
                x = size.width - BodyGlassHighlightBottomEndOffset.toPx(),
                y = size.height - BodyGlassHighlightBottomEndOffset.toPx(),
            ),
            radius = BodyGlassHighlightBottomEndRadius.toPx(),
        ),
    )
}

private val BodyGlassHighlightTopStartRadius = 80.dp
private val BodyGlassHighlightBottomEndRadius = 45.dp
private val BodyGlassHighlightBottomEndOffset = 10.dp
