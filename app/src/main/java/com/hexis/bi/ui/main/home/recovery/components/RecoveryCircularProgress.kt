package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.recovery.RecoveryStatus
import com.hexis.bi.ui.theme.Bg
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
    val statusColor = status.color

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

            // Gray track
            drawArc(
                color = Bg,
                startAngle = RecoveryConstants.ARC_START_ANGLE,
                sweepAngle = RecoveryConstants.ARC_TOTAL_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )

            // Colored progress
            if (filledSweep > 0f) drawArc(
                color = statusColor,
                startAngle = RecoveryConstants.ARC_START_ANGLE,
                sweepAngle = filledSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = dimensionResource(R.dimen.recovery_arc_text_offset)),
        ) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.headlineLarge,
                color = statusColor,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))

            Text(
                text = stringResource(status.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
