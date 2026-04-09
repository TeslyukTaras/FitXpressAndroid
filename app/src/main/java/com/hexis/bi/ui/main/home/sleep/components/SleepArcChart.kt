package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.sleep.SleepStageData
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.formatSleepDuration
import kotlin.math.asin

@Composable
fun SleepArcChart(
    progress: Float,
    totalSleepMinutes: Int,
    sleepGoalHours: Int,
    stages: List<SleepStageData>,
    modifier: Modifier = Modifier,
) {
    val arcWidth = dimensionResource(R.dimen.sleep_arc_width)
    val arcHeight = dimensionResource(R.dimen.sleep_arc_height)
    val strokeWidth = dimensionResource(R.dimen.sleep_arc_stroke_width)
    val filledSweep = SleepConstants.ARC_TOTAL_SWEEP * progress.coerceIn(0f, 1f)
    val totalMinutes = stages.sumOf { it.durationMinutes }.toFloat()

    Box(
        modifier = modifier.size(width = arcWidth, height = arcHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = arcWidth, height = arcHeight)) {
            val sw = strokeWidth.toPx()
            val padding = sw / 2f
            // Arc rect must be square — use width to keep the arc circular
            val arcDiameter = size.width - sw
            val arcRect = Size(arcDiameter, arcDiameter)
            val topLeft = Offset(padding, padding)
            val arcRadius = arcDiameter / 2f
            val capRadius = sw / 2f

            /* TODO: check if we need this track
            // Gray background track
            drawArc(
                color = Gray600,
                startAngle = SleepConstants.ARC_START_ANGLE,
                sweepAngle = SleepConstants.ARC_TOTAL_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )*/

            if (totalMinutes > 0f && filledSweep > 0f) {
                // How many degrees a circle cap of radius sw/2 spans on the arc
                val capAngleDeg = Math.toDegrees(asin((capRadius / arcRadius).toDouble())).toFloat()
                val numGaps = (stages.size - 1).coerceAtLeast(0)
                // Gap between consecutive arc bodies = 2 × capAngle so their end caps just touch
                val gapBetweenArcs = SleepConstants.ARC_CAP_ENDS_PER_GAP * capAngleDeg
                val availableSweep = (filledSweep - numGaps * gapBetweenArcs).coerceAtLeast(0f)

                var currentAngle = SleepConstants.ARC_START_ANGLE

                stages.forEach { stage ->
                    val segSweep = (stage.durationMinutes / totalMinutes) * availableSweep
                    if (segSweep > 0f) {
                        val arcStart = currentAngle
                        val arcEnd = arcStart + segSweep

                        drawArc(
                            color = stage.color,
                            startAngle = arcStart,
                            sweepAngle = segSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcRect,
                            style = Stroke(width = sw, cap = StrokeCap.Round),
                        )

                        currentAngle = arcEnd + gapBetweenArcs
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = dimensionResource(R.dimen.sleep_arc_text_offset)),
        ) {
            Text(
                text = formatSleepDuration(totalSleepMinutes),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.sleep_goal_hours, sleepGoalHours),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
