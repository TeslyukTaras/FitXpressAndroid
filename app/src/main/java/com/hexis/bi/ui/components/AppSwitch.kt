package com.hexis.bi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.BackgroundConstants

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackWidth = dimensionResource(R.dimen.switch_track_width)
    val trackHeight = dimensionResource(R.dimen.switch_track_height)
    val thumbSize = dimensionResource(R.dimen.switch_thumb_size)
    val thumbPadding = (trackHeight - thumbSize) / 2
    val thumbTravel = trackWidth - thumbSize - thumbPadding * 2
    val borderWidthPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.border_line).toPx()
    }

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "dark_switch_progress",
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "dark_switch_thumb_color",
    )

    val activeTrackTop = NocturnePulseTheme.extendedColors.switchActiveTrackTop
    val activeTrackBottom = NocturnePulseTheme.extendedColors.switchActiveTrackBottom
    val trackSurface = NocturnePulseTheme.extendedColors.surfaceTranslucent
    val trackBorder = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .drawBehind {
                drawTrack(
                    progress,
                    borderWidthPx,
                    activeTrackTop,
                    activeTrackBottom,
                    trackSurface,
                    trackBorder
                )
            }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = thumbPadding + thumbTravel * progress,
                    top = thumbPadding,
                )
                .size(thumbSize)
                .shadow(
                    elevation = dimensionResource(R.dimen.dark_switch_thumb_shadow_blur),
                    shape = CircleShape,
                    ambientColor = NocturnePulseTheme.extendedColors.switchThumbShadow,
                    spotColor = NocturnePulseTheme.extendedColors.switchThumbShadow,
                )
                .drawBehind { drawCircle(thumbColor) },
        )
    }
}

private fun DrawScope.drawTrack(
    progress: Float,
    borderWidthPx: Float,
    activeTrackTop: Color,
    activeTrackBottom: Color,
    trackSurface: Color,
    trackBorder: Color,
) {
    val cornerRadius = CornerRadius(size.height / 2)
    val activeBrush = Brush.verticalGradient(
        colors = listOf(activeTrackTop, activeTrackBottom),
        startY = size.height * BackgroundConstants.COMPONENT_VERTICAL_GRADIENT_START_FRACTION,
        endY = size.height * BackgroundConstants.COMPONENT_VERTICAL_GRADIENT_END_FRACTION,
    )

    drawRoundRect(
        color = trackSurface,
        alpha = 1f - progress,
        cornerRadius = cornerRadius,
    )
    if (progress < 1f) {
        val inset = borderWidthPx / 2f
        drawRoundRect(
            color = trackBorder,
            alpha = 1f - progress,
            topLeft = Offset(inset, inset),
            size = Size(size.width - borderWidthPx, size.height - borderWidthPx),
            cornerRadius = CornerRadius(cornerRadius.x - inset),
            style = Stroke(width = borderWidthPx),
        )
    }
    drawRoundRect(brush = activeBrush, alpha = progress, cornerRadius = cornerRadius)
}
