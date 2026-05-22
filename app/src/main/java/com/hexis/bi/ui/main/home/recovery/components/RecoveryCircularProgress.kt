package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.recovery.RecoveryStatus
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.RecoveryConstants

@Composable
fun RecoveryCircularProgress(
    score: Int,
    status: RecoveryStatus,
    modifier: Modifier = Modifier,
) {
    val arcWidth = dimensionResource(R.dimen.recovery_arc_width)
    val arcHeight = dimensionResource(R.dimen.recovery_arc_height)
    val strokeWidth = dimensionResource(R.dimen.recovery_arc_stroke_width)
    val fraction = (score / RecoveryConstants.MAX_SCORE).coerceIn(0f, 1f)
    val filledSweep = RecoveryConstants.ARC_TOTAL_SWEEP * fraction

    val ext = DarkTheme.extendedColors
    val gaugeLow = ext.gaugeLow
    val gaugeMid = ext.gaugeMid
    val gaugeHigh = ext.gaugeHigh
    val trackColor = ext.gaugeTrack

    Box(
        modifier = modifier.size(width = arcWidth, height = arcHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(width = arcWidth, height = arcHeight)) {
            val sw = strokeWidth.toPx()
            val padding = sw / 2f
            val arcDiameter = size.width - sw
            val arcRect = Size(arcDiameter, arcDiameter)
            val topLeft = Offset(padding, padding)
            val center = Offset(padding + arcDiameter / 2f, padding + arcDiameter / 2f)

            // Gray track spanning the full sweep.
            drawArc(
                color = trackColor,
                startAngle = RecoveryConstants.ARC_START_ANGLE,
                sweepAngle = RecoveryConstants.ARC_TOTAL_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )

            // Red → yellow → green gradient mapped to the arc's angular positions, so the leading
            // color reflects the score band. Stops are keyed to absolute angles (fraction = angle/360):
            // 150° start = red, 270° = yellow, 30° end = green; the unfilled top gap holds the wrap.
            if (filledSweep > 0f) {
                val brush = Brush.sweepGradient(
                    GAUGE_END_STOP to gaugeHigh,
                    GAUGE_START_STOP to gaugeLow,
                    GAUGE_MID_STOP to gaugeMid,
                    1f to gaugeHigh,
                    center = center,
                )
                drawArc(
                    brush = brush,
                    startAngle = RecoveryConstants.ARC_START_ANGLE,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_2xs)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -dimensionResource(R.dimen.spacer_l)),
        ) {
            Text(
                text = stringResource(R.string.format_percentage, score),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(status.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Arc spans 150° → 390°. As sweep-gradient fractions (angle / 360): start 150° = 0.4167,
// mid 270° = 0.75, end 30° (= 390°) = 0.0833.
private const val GAUGE_START_STOP = 150f / 360f
private const val GAUGE_MID_STOP = 270f / 360f
private const val GAUGE_END_STOP = 30f / 360f
