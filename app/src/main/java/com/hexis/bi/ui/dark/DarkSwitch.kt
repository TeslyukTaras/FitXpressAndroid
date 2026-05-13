package com.hexis.bi.ui.dark

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import com.hexis.bi.R
import com.hexis.bi.ui.theme.dark.DarkBorderMuted
import com.hexis.bi.ui.theme.dark.DarkSwitchActiveTrackBottom
import com.hexis.bi.ui.theme.dark.DarkSwitchActiveTrackTop
import com.hexis.bi.ui.theme.dark.DarkSwitchThumbShadow
import com.hexis.bi.ui.theme.dark.SurfaceTranslucent
import com.hexis.bi.ui.theme.dark.TextPrimary

@Composable
fun DarkSwitch(
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
        targetValue = if (checked) TextPrimary else DarkBorderMuted,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "dark_switch_thumb_color",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .drawBehind { drawTrack(progress, borderWidthPx) }
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
                    ambientColor = DarkSwitchThumbShadow,
                    spotColor = DarkSwitchThumbShadow,
                )
                .drawBehind { drawCircle(thumbColor) },
        )
    }
}

private fun DrawScope.drawTrack(progress: Float, borderWidthPx: Float) {
    val cornerRadius = CornerRadius(size.height / 2)
    val activeBrush = Brush.verticalGradient(
        colors = listOf(DarkSwitchActiveTrackTop, DarkSwitchActiveTrackBottom),
        startY = -size.height * 0.5312f,
        endY = size.height * 1.3021f,
    )

    drawRoundRect(
        color = SurfaceTranslucent,
        alpha = 1f - progress,
        cornerRadius = cornerRadius,
    )
    if (progress < 1f) {
        val inset = borderWidthPx / 2f
        drawRoundRect(
            color = DarkBorderMuted,
            alpha = 1f - progress,
            topLeft = Offset(inset, inset),
            size = Size(size.width - borderWidthPx, size.height - borderWidthPx),
            cornerRadius = CornerRadius(cornerRadius.x - inset),
            style = Stroke(width = borderWidthPx),
        )
    }
    drawRoundRect(brush = activeBrush, alpha = progress, cornerRadius = cornerRadius)
}
