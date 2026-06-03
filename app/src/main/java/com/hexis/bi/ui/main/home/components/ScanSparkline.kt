package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.theme.dark.ChartAxisLine
import com.hexis.bi.ui.theme.dark.ScanSparklineMarkerFill
import com.hexis.bi.utils.SmoothLinePath
import com.hexis.bi.utils.constants.HomeConstants

/**
 * Compact smooth trend line for the Scan overview card. Plots [points] (oldest → newest) auto-scaled
 * to their own min/max, with a soft gradient fill and a marker on the previous (last interior) peak.
 * Renders nothing when there are fewer than two points.
 */
@Composable
internal fun ScanSparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val strokePx = with(density) { dimensionResource(R.dimen.home_sparkline_stroke).toPx() }
    val markerRadiusPx = with(density) {
        dimensionResource(R.dimen.home_sparkline_marker_size).toPx() / 2f
    }
    val markerBorderPx = with(density) {
        dimensionResource(R.dimen.home_sparkline_marker_border).toPx()
    }

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val pad = size.height * HomeConstants.SPARKLINE_VERTICAL_PADDING_FRACTION
        val minValue = points.min()
        val range = (points.max() - minValue).takeIf { it > 0f } ?: 1f
        val usableHeight = size.height - 2f * pad
        val coords = points.mapIndexed { index, value ->
            val x = index.toFloat() / (points.size - 1) * size.width
            val y = pad + (1f - (value - minValue) / range) * usableHeight
            Offset(x, y)
        }

        val linePath = SmoothLinePath.build(coords)
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, size.height)
            lineTo(coords.first().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to lineColor.copy(alpha = HomeConstants.SPARKLINE_FILL_TOP_ALPHA),
                    1f to lineColor.copy(alpha = HomeConstants.SPARKLINE_FILL_BOTTOM_ALPHA),
                ),
                startY = coords.minOf { it.y },
                endY = size.height,
            ),
        )
        drawPath(path = linePath, color = lineColor, style = Stroke(width = strokePx))

        // Mark the previous peak — the last interior local maximum — as in the design.
        val peak = coords[previousPeakIndex(points)]
        drawCircle(color = ScanSparklineMarkerFill, radius = markerRadiusPx, center = peak)
        drawCircle(
            color = ChartAxisLine,
            radius = markerRadiusPx - markerBorderPx / 2f,
            center = peak,
            style = Stroke(width = markerBorderPx),
        )
    }
}

/**
 * The previous peak: the last interior local maximum (a point higher than both neighbours),
 * scanning from the end. Falls back to the highest non-final point, then the last point.
 */
private fun previousPeakIndex(points: List<Float>): Int {
    for (i in points.size - 2 downTo 1) {
        if (points[i] >= points[i - 1] && points[i] >= points[i + 1]) return i
    }
    return (0 until points.size - 1).maxByOrNull { points[it] } ?: points.lastIndex
}
