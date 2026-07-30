package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.AnimationConstants

@Composable
fun ActivityCircularProgress(
    stepsProgress: Float,
    distanceProgress: Float,
    caloriesProgress: Float,
    showCalories: Boolean,
    progressPercent: Int,
    modifier: Modifier = Modifier,
) {
    val arcSize = dimensionResource(R.dimen.activity_arc_size)
    val strokeWidth = dimensionResource(R.dimen.activity_arc_stroke_width)
    val ringGap = dimensionResource(R.dimen.activity_arc_ring_gap)

    val steps = rememberRingReveal(stepsProgress)
    val distance = rememberRingReveal(distanceProgress)
    val calories = rememberRingReveal(caloriesProgress)

    val ext = NocturnePulseTheme.extendedColors
    val ringColors = listOf(ext.activityStepsProgress, ext.activityDistanceProgress, ext.accentBlue)
    val ringTrackColor = ext.activityProgressTrack
    val rings = if (showCalories) {
        listOf(steps, distance, calories).zip(ringColors)
    } else {
        listOf(steps, distance).zip(ringColors.dropLast(1))
    }

    Box(
        modifier = modifier.size(arcSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(arcSize)) {
            val sw = strokeWidth.toPx()
            val gap = ringGap.toPx()

            rings.forEachIndexed { index, (progress, color) ->
                val inset = (rings.size - 1 - index) * (sw + gap)
                val padding = sw / 2f + inset
                val arcDiameter = size.width - sw - inset * 2
                if (arcDiameter <= 0f) return@forEachIndexed

                val arcRect = Size(arcDiameter, arcDiameter)
                val topLeft = Offset(padding, padding)
                val filledSweep = ActivityConstants.CIRCLE_FULL_SWEEP * progress

                // Gray track
                drawArc(
                    color = ringTrackColor,
                    startAngle = ActivityConstants.CIRCLE_START_ANGLE,
                    sweepAngle = ActivityConstants.CIRCLE_FULL_SWEEP,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )

                // Colored progress
                if (filledSweep > 0f) drawArc(
                    color = color,
                    startAngle = ActivityConstants.CIRCLE_START_ANGLE,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }

        Text(
            text = stringResource(R.string.format_percentage, progressPercent),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun rememberRingReveal(target: Float): Float {
    val clamped = target.coerceIn(0f, 1f)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(clamped) {
        reveal.animateTo(clamped, tween(AnimationConstants.ARC_FILL_MS, easing = FastOutSlowInEasing))
    }
    return reveal.value
}
