package com.hexis.bi.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import com.hexis.bi.R
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300

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

    val offColor = MaterialTheme.colorScheme.outlineVariant
    val checkedBrush = Brush.verticalGradient(
        listOf(Blue300, Blue200)
    )
    val thumbShadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "switch_progress",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .drawBehind { drawTrack(offColor, checkedBrush, progress) }
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
                    elevation = dimensionResource(R.dimen.elevation_card),
                    shape = CircleShape,
                    ambientColor = thumbShadowColor,
                    spotColor = thumbShadowColor,
                )
                .drawBehind { drawCircle(Color.White) },
        )
    }
}

private fun DrawScope.drawTrack(
    offColor: Color,
    onBrush: Brush,
    progress: Float,
) {
    val cornerRadius = size.height / 2
    drawRoundRect(
        color = offColor,
        alpha = 1f - progress,
        cornerRadius = CornerRadius(cornerRadius)
    )
    drawRoundRect(brush = onBrush, alpha = progress, cornerRadius = CornerRadius(cornerRadius))
}
