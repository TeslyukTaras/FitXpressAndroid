package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.dark.ActivityIdleBar
import com.hexis.bi.utils.constants.HomeConstants
import kotlin.random.Random

/**
 * Hourly-steps bar chart for the Activity overview card. Bars with steps share the accent colour and
 * are scaled to the busiest hour; hours with no steps render as a faint, low decorative baseline with
 * a small randomized height (purely visual — not data).
 */
@Composable
internal fun ActivityMiniBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val cornerPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_activity_bar_corner).toPx()
    }
    val spacingPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.home_activity_bar_spacing).toPx()
    }

    val barCount = if (values.isEmpty()) HomeConstants.ACTIVITY_BAR_COUNT_DEFAULT else values.size
    // Stable per-bar idle heights so they don't flicker on recomposition.
    val idleFractions = remember(barCount) {
        val min = HomeConstants.ACTIVITY_IDLE_BAR_MIN_FRACTION
        val range = HomeConstants.ACTIVITY_IDLE_BAR_MAX_FRACTION - min
        List(barCount) { min + Random.nextFloat() * range }
    }
    val maxValue = values.maxOrNull() ?: 0f

    Canvas(modifier = modifier) {
        if (barCount <= 0) return@Canvas
        val barWidth = (size.width - spacingPx * (barCount - 1)) / barCount
        if (barWidth <= 0f) return@Canvas

        repeat(barCount) { index ->
            val value = values.getOrElse(index) { 0f }
            val hasSteps = value > 0f && maxValue > 0f
            val fraction = if (hasSteps) value / maxValue else idleFractions[index]
            val color = if (hasSteps) activeColor else ActivityIdleBar

            val barHeight = size.height * fraction.coerceIn(0f, 1f)
            val left = index * (barWidth + spacingPx)
            // Only the top corners are rounded.
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, size.height - barHeight, left + barWidth, size.height),
                        topLeft = CornerRadius(cornerPx, cornerPx),
                        topRight = CornerRadius(cornerPx, cornerPx),
                        bottomRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius.Zero,
                    ),
                )
            }
            drawPath(path, color)
        }
    }
}
