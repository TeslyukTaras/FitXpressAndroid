package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.sleep.SleepStage
import com.hexis.bi.ui.main.home.sleep.TimelineSegment
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.BlueFadedIndicator100
import com.hexis.bi.ui.theme.BlueFadedIndicator200
import com.hexis.bi.ui.theme.BlueFadedIndicator300
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.formatHour
import com.hexis.bi.utils.formatSleepDuration

private val stageYIndex = mapOf(
    SleepStage.Deep to 3,
    SleepStage.REM to 2,
    SleepStage.Light to 1,
    SleepStage.Awake to 0,
)

private fun stageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.Awake -> BlueFadedIndicator100
    SleepStage.Light -> BlueFadedIndicator200
    SleepStage.REM -> BlueFadedIndicator300
    SleepStage.Deep -> Blue300
}

@Composable
fun SleepTimelineCard(
    totalSleepMinutes: Int,
    timeStartHour: Int,
    timeEndHour: Int,
    segments: List<TimelineSegment>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.padding_medium)),
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
            Text(
                text = formatSleepDuration(totalSleepMinutes),
                style = MaterialTheme.typography.headlineSmall,
                color = Blue300,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatHour(timeStartHour),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = formatHour(timeEndHour),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        TimelineChart(
            segments = segments,
            cornerRadius = dimensionResource(R.dimen.sleep_timeline_corner_radius),
            connectorWidth = dimensionResource(R.dimen.sleep_timeline_connector_width),
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.sleep_timeline_chart_height)),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        TimelineLegend()
    }
}

@Composable
private fun TimelineChart(
    segments: List<TimelineSegment>,
    cornerRadius: Dp,
    connectorWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val gridLineColor = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        val trackHeight = size.height / SleepConstants.SLEEP_STAGE_COUNT
        val cornerRadiusPx = cornerRadius.toPx()
        val connectorWidthPx = connectorWidth.toPx()
        val cornerRadiusObj = CornerRadius(cornerRadiusPx)

        // Subtle horizontal grid lines between tracks
        for (i in 0 until SleepConstants.SLEEP_STAGE_COUNT + 1) {
            drawLine(
                color = gridLineColor,
                start = Offset(0f, i * trackHeight),
                end = Offset(size.width, i * trackHeight),
                strokeWidth = connectorWidthPx,
            )
        }

        // Segments — each drawn in its stage's horizontal track
        segments.forEachIndexed { index, segment ->
            val yIndex = stageYIndex[segment.stage] ?: 0
            val left = segment.startFraction * size.width
            val right = segment.endFraction * size.width
            val top = yIndex * trackHeight
            val bottom = (yIndex + 1) * trackHeight

            drawRoundRect(
                color = stageColor(segment.stage),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = cornerRadiusObj,
            )

            // Gradient connector when transitioning to a different sleep stage
            val next = segments.getOrNull(index + 1) ?: return@forEachIndexed
            val nextYIndex = stageYIndex[next.stage] ?: 0
            if (nextYIndex != yIndex) {
                val goingDown = nextYIndex > yIndex
                val fromY = if (goingDown) bottom - cornerRadiusPx else top + cornerRadiusPx
                val toY = if (goingDown) nextYIndex * trackHeight + cornerRadiusPx
                else (nextYIndex + 1) * trackHeight - cornerRadiusPx

                val brush = Brush.verticalGradient(
                    colors = if (goingDown)
                        listOf(stageColor(segment.stage), stageColor(next.stage))
                    else
                        listOf(stageColor(next.stage), stageColor(segment.stage)),
                    startY = minOf(fromY, toY),
                    endY = maxOf(fromY, toY),
                )
                drawLine(
                    brush = brush,
                    start = Offset(right - (connectorWidthPx / 2), fromY),
                    end = Offset(right + (connectorWidthPx / 2), toY),
                    strokeWidth = connectorWidthPx,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun TimelineLegend() {
    val stages = listOf(SleepStage.Deep, SleepStage.REM, SleepStage.Light, SleepStage.Awake)
    Row(modifier = Modifier.fillMaxWidth()) {
        stages.forEach { stage ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.size_indicator_bigger))
                        .clip(CircleShape)
                        .background(stageColor(stage))
                )
                Spacer(Modifier.size(dimensionResource(R.dimen.spacer_xxs)))
                Text(
                    text = stringResource(stage.nameRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
