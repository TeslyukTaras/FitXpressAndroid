package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.IntelligenceConstants

/**
 * Downward-opening semicircle gauge: a grey track with a red → yellow → green fill proportional to
 * [fraction], and [value] centred at the flat edge. When [comingSoon] is true, the arc stays empty
 * and a muted "Coming" label is shown instead of a value. Used by the Body Intelligence cards.
 */
@Composable
internal fun IntelligenceGauge(
    fraction: Float,
    value: String,
    modifier: Modifier = Modifier,
    comingSoon: Boolean = false,
) {
    val ext = NocturnePulseTheme.extendedColors
    val trackColor = ext.gaugeTrack
    val sw = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_intelligence_gauge_stroke).toPx()
    }
    val stroke = remember(sw) { Stroke(width = sw, cap = StrokeCap.Round) }
    val gradientStops = remember(ext.gaugeLow, ext.gaugeMid, ext.gaugeHigh) {
        arrayOf(
            0f to ext.gaugeLow,
            IntelligenceConstants.GAUGE_GRADIENT_LEFT_STOP to ext.gaugeLow,
            IntelligenceConstants.GAUGE_GRADIENT_TOP_STOP to ext.gaugeMid,
            IntelligenceConstants.GAUGE_GRADIENT_RIGHT_STOP to ext.gaugeHigh,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth(IntelligenceConstants.GAUGE_WIDTH_FRACTION)
            .aspectRatio(2f),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.width - sw
            val arcRect = Size(diameter, diameter)
            // Anchor the semicircle to the bottom edge so its flat side sits at the value.
            val center = Offset(size.width / 2f, size.height - sw / 2f)
            val topLeft = Offset(sw / 2f, center.y - diameter / 2f)

            drawArc(
                color = trackColor,
                startAngle = IntelligenceConstants.ARC_START_ANGLE,
                sweepAngle = IntelligenceConstants.ARC_TOTAL_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = stroke,
            )

            val filledSweep = IntelligenceConstants.ARC_TOTAL_SWEEP * fraction.coerceIn(0f, 1f)
            if (filledSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(*gradientStops, center = center),
                    startAngle = IntelligenceConstants.ARC_START_ANGLE,
                    sweepAngle = filledSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = stroke,
                )
            }
        }

        Text(
            text = if (comingSoon) stringResource(R.string.intelligence_coming) else value,
            style = if (comingSoon) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (comingSoon) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        )
    }
}
