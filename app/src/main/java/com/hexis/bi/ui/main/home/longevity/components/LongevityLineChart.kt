package com.hexis.bi.ui.main.home.longevity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntOffset
import com.hexis.bi.R
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.SmoothLinePath
import com.hexis.bi.utils.constants.LongevityConstants

/**
 * Smooth (monotone-cubic) line chart for the Longevity trend, with dashed horizontal grid lines,
 * a left y-axis label column, and evenly spaced x-axis labels below.
 *
 * [points] are y values in [0, 100], rendered evenly along the x-axis in order.
 */
@Composable
internal fun LongevityLineChart(
    points: List<Float>,
    axisLabels: List<String>,
    modifier: Modifier = Modifier,
    currentLabelIndex: Int = -1,
    xAxisSpanCount: Int? = null,
) {
    val chartHeight = dimensionResource(R.dimen.longevity_chart_height)
    val gridStroke = dimensionResource(R.dimen.longevity_chart_grid_stroke)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val lineStroke = dimensionResource(R.dimen.longevity_chart_line_stroke)
    val chartGap = dimensionResource(R.dimen.spacer_xs)

    val gridColor = MaterialTheme.colorScheme.outline
    val axisColor = NocturnePulseTheme.extendedColors.chartAxisLine
    val lineColor = MaterialTheme.colorScheme.primary
    val gridLines = LongevityConstants.GRID_LINES

    var yAxisWidthPx by remember { mutableIntStateOf(0) }
    val yAxisWidth = with(LocalDensity.current) { yAxisWidthPx.toDp() }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .onSizeChanged { yAxisWidthPx = it.width },
            ) {
                gridLines.forEach { value ->
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align { size, space, _ ->
                            val fraction = mapValueToFraction(value)
                            IntOffset(0, (space.height * fraction - size.height / 2f).toInt())
                        },
                    )
                }
            }

            Spacer(Modifier.width(chartGap))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        val strokePx = gridStroke.toPx()
                        val dashPx = dashWidth.toPx()
                        // Dashed horizontal grid lines, including the x-axis baseline at 0.
                        drawHorizontalGridLines(gridLines, gridColor, strokePx, dashPx)
                        // Solid y-axis.
                        drawLine(
                            color = axisColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = strokePx,
                        )
                        if (points.size >= 2) {
                            // Spread points across the full span (e.g. 24 hours) when given, so a
                            // partial series ends part-way; otherwise spread evenly across the width.
                            val span = (xAxisSpanCount ?: points.size).coerceAtLeast(2)
                            drawSmoothLine(points, lineColor, lineStroke.toPx(), span)
                        }
                    },
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(yAxisWidth + chartGap))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                axisLabels.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == currentLabelIndex) NocturnePulseTheme.extendedColors.positive else NocturnePulseTheme.extendedColors.gray300,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawHorizontalGridLines(
    gridLines: List<Float>,
    color: Color,
    strokeWidthPx: Float,
    dashPx: Float,
) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx), 0f)
    gridLines.forEach { value ->
        val y = size.height * mapValueToFraction(value)
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidthPx,
            pathEffect = dashEffect,
        )
    }
}

private fun DrawScope.drawSmoothLine(
    points: List<Float>,
    color: Color,
    strokeWidthPx: Float,
    xSpan: Int,
) {
    val coords = points.mapIndexed { index, value ->
        val x = index.toFloat() / (xSpan - 1) * size.width
        val y = size.height * mapValueToFraction(value)
        Offset(x, y)
    }
    val linePath = SmoothLinePath.build(coords, LongevityConstants.CHART_MONOTONE_TANGENT_LIMIT)

    val baseline = size.height
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(coords.last().x, baseline)
        lineTo(coords.first().x, baseline)
        close()
    }
    val topY = coords.minOf { it.y }
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                LongevityConstants.CHART_FILL_START_STOP to
                        color.copy(alpha = LongevityConstants.CHART_FILL_START_ALPHA),
                LongevityConstants.CHART_FILL_END_STOP to
                        color.copy(alpha = LongevityConstants.CHART_FILL_END_ALPHA),
            ),
            startY = topY,
            endY = baseline,
        ),
    )
    drawPath(
        path = linePath,
        color = color,
        style = Stroke(width = strokeWidthPx),
    )
}

/**
 * Maps a score to its vertical fraction from the top (0f = top, 1f = bottom), giving each band
 * between grid values (0–25, 25–70, 70–100) the same visual height.
 */
private fun mapValueToFraction(value: Float): Float {
    val v = value.coerceIn(0f, LongevityConstants.MAX_SCORE)
    val band = LongevityConstants.BAND_FRACTION
    return when {
        v >= LongevityConstants.BAND_MID_MAX ->
            (LongevityConstants.MAX_SCORE - v) /
                    (LongevityConstants.MAX_SCORE - LongevityConstants.BAND_MID_MAX) * band

        v >= LongevityConstants.BAND_LOW_MAX ->
            band + (LongevityConstants.BAND_MID_MAX - v) /
                    (LongevityConstants.BAND_MID_MAX - LongevityConstants.BAND_LOW_MAX) * band

        else ->
            band * 2f + (LongevityConstants.BAND_LOW_MAX - v) /
                    LongevityConstants.BAND_LOW_MAX * band
    }
}

