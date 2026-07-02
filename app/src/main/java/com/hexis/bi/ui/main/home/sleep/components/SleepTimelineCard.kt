package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.sleep.TimelineSegment
import com.hexis.bi.ui.main.home.sleep.nameRes
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.constants.TimeConstants
import com.hexis.bi.utils.formatHour

private const val SHADOW_ALPHA = 0.5f
private const val LINE_ALPHA = 0.6f

/** Stage rows top-to-bottom; the grid line sits at the bottom of each row. */
private val STAGE_ORDER =
    listOf(SleepStage.Awake, SleepStage.REM, SleepStage.Light, SleepStage.Deep)

/** Awake/REM plateaus sit above their grid line (mid-row) and cast their shadow downward. */
private fun SleepStage.shadowGoesDown(): Boolean =
    this == SleepStage.Awake || this == SleepStage.REM

@Composable
fun SleepTimelineCard(
    totalSleepMinutes: Int,
    timeStartHour: Int,
    timeEndHour: Int,
    segments: List<TimelineSegment>,
    modifier: Modifier = Modifier,
) {
    val stageHeight = dimensionResource(R.dimen.sleep_timeline_stage_height)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f)
    )
    val measurer = rememberTextMeasurer()
    val labelTexts = STAGE_ORDER.map { stringResource(it.nameRes()) }
    // Plot starts 16dp past the widest stage label; grid lines still span the full width.
    val longestLabel = with(LocalDensity.current) {
        labelTexts.maxOf { measurer.measure(it, labelStyle).size.width }.toDp()
    }
    val plotStartX = longestLabel + dimensionResource(R.dimen.spacer_l)
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_l),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_l)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sleep_timeline_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(text = timelineDurationValue(totalSleepMinutes))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stageHeight * STAGE_ORDER.size),
        ) {
            TimelineChart(
                segments = segments,
                stageHeight = stageHeight,
                plotStartX = plotStartX,
                modifier = Modifier.fillMaxSize(),
            )
            TimelineStageLabels(
                stageHeight = stageHeight,
                labelStyle = labelStyle,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

        TimelineTimeLabels(
            timeStartHour = timeStartHour,
            timeEndHour = timeEndHour,
            modifier = Modifier.padding(start = plotStartX),
        )
    }
}

/** Total sleep header value: white bodyLarge numbers with muted bodyMedium h/m units (matches Sleep Stages). */
@Composable
private fun timelineDurationValue(totalMinutes: Int): AnnotatedString {
    val hours = totalMinutes / SleepConstants.MINUTES_PER_HOUR
    val minutes = totalMinutes % SleepConstants.MINUTES_PER_HOUR
    val numberStyle = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = Color.White)
    val unitStyle =
        MaterialTheme.typography.bodyMedium.toSpanStyle()
            .copy(color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f))
    val hourUnit = stringResource(R.string.unit_hours_short)
    val minuteUnit = stringResource(R.string.unit_minutes_short)
    return buildAnnotatedString {
        withStyle(numberStyle) { append(hours.toString()) }
        withStyle(unitStyle) { append(" $hourUnit") }
        withStyle(numberStyle) { append(" ${minutes.toString().padStart(2, '0')}") }
        withStyle(unitStyle) { append(" $minuteUnit") }
    }
}

@Composable
private fun TimelineStageLabels(
    stageHeight: Dp,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        STAGE_ORDER.forEach { stage ->
            // Grid line sits at the row bottom; label rides 8dp above it, line passing underneath.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stageHeight),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text(
                    text = stringResource(stage.nameRes()),
                    style = labelStyle,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacer_xs)),
                )
            }
        }
    }
}

@Composable
private fun TimelineChart(
    segments: List<TimelineSegment>,
    stageHeight: Dp,
    plotStartX: Dp,
    modifier: Modifier = Modifier,
) {
    val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val lineWidth = dimensionResource(R.dimen.sleep_timeline_line_width)
    val shadowHeight = dimensionResource(R.dimen.sleep_timeline_shadow_height)
    val plateauOffset = dimensionResource(R.dimen.sleep_timeline_plateau_grid_offset)
    val cornerRadius = dimensionResource(R.dimen.sleep_timeline_corner_radius)
    val stageColor = rememberSleepStageColors()

    Canvas(modifier = modifier) {
        val stageHeightPx = stageHeight.toPx()
        val lineWidthPx = lineWidth.toPx()
        val shadowHeightPx = shadowHeight.toPx()
        val plateauOffsetPx = plateauOffset.toPx()
        val cornerRadiusPx = cornerRadius.toPx()
        val plotStartXPx = plotStartX.toPx()
        val plotWidth = size.width - plotStartXPx
        fun plotX(fraction: Float) = plotStartXPx + fraction.coerceIn(0f, 1f) * plotWidth

        // Grid line at the bottom of each stage's row; plateaus sit above it.
        fun gridY(stage: SleepStage) = (STAGE_ORDER.indexOf(stage) + 1) * stageHeightPx
        fun plateauY(stage: SleepStage): Float =
            if (stage.shadowGoesDown()) gridY(stage) - stageHeightPx / 2f
            else gridY(stage) - plateauOffsetPx

        // Four full-width grid lines, one per stage.
        STAGE_ORDER.forEach { stage ->
            val y = gridY(stage)
            drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // Translucent vertical gradient shared by the whole hypnogram line (Figma border image).
        // Anchored to the plateau band so each stage's line shows its own colour (orange included).
        val lineBrush = Brush.verticalGradient(
            0f to stageColor(SleepStage.Awake).copy(alpha = LINE_ALPHA),
            0.33f to stageColor(SleepStage.REM).copy(alpha = LINE_ALPHA),
            0.66f to stageColor(SleepStage.Light).copy(alpha = LINE_ALPHA),
            1f to stageColor(SleepStage.Deep).copy(alpha = LINE_ALPHA),
            startY = plateauY(SleepStage.Awake),
            endY = plateauY(SleepStage.Deep),
        )
        val linePath = Path()

        segments.forEachIndexed { index, segment ->
            val x0 = plotX(segment.startFraction)
            val x1 = plotX(segment.endFraction)
            val py = plateauY(segment.stage)
            val color = stageColor(segment.stage)

            // Colorful shadow: downward for Awake/REM, upward for Light/Deep.
            if (segment.stage.shadowGoesDown()) {
                val end = py + shadowHeightPx
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = SHADOW_ALPHA), Color.Transparent),
                        startY = py,
                        endY = end,
                    ),
                    topLeft = Offset(x0, py),
                    size = Size(x1 - x0, (end.coerceAtMost(size.height)) - py),
                )
            } else {
                val start = (py - shadowHeightPx).coerceAtLeast(0f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, color.copy(alpha = SHADOW_ALPHA)),
                        startY = py - shadowHeightPx,
                        endY = py,
                    ),
                    topLeft = Offset(x0, start),
                    size = Size(x1 - x0, py - start),
                )
            }

            // Build the continuous step line: vertical into this plateau (x0 == previous x1), then across.
            if (index == 0) linePath.moveTo(x0, py) else linePath.lineTo(x0, py)
            linePath.lineTo(x1, py)
        }

        // One translucent, color-shifting line with 2dp rounded corners.
        drawPath(
            path = linePath,
            brush = lineBrush,
            style = Stroke(
                width = lineWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.cornerPathEffect(cornerRadiusPx),
            ),
        )
    }
}

@Composable
private fun TimelineTimeLabels(
    timeStartHour: Int,
    timeEndHour: Int,
    modifier: Modifier = Modifier,
) {
    val labels = timelineLabelHours(timeStartHour, timeEndHour)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { hour ->
            Text(
                text = formatHour(hour),
                style = MaterialTheme.typography.labelSmall,
                color = NocturnePulseTheme.extendedColors.gray200,
            )
        }
    }
}

private fun timelineLabelHours(startHour: Int, endHour: Int): List<Int> {
    val normalizedEnd =
        if (endHour <= startHour) endHour + TimeConstants.HOURS_IN_DAY else endHour
    val span = (normalizedEnd - startHour).coerceAtLeast(1)
    val intervals = SleepConstants.TIMELINE_LABEL_COUNT - 1
    return List(SleepConstants.TIMELINE_LABEL_COUNT) { index ->
        (startHour + span * index / intervals) % TimeConstants.HOURS_IN_DAY
    }
}
