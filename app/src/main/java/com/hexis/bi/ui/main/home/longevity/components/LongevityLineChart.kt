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
import com.hexis.bi.ui.theme.Gray300
import com.hexis.bi.ui.theme.dark.ChartAxisLine
import com.hexis.bi.ui.theme.dark.DarkBorderMuted
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.utils.constants.LongevityConstants
import kotlin.math.hypot

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
) {
    val chartHeight = dimensionResource(R.dimen.longevity_chart_height)
    val gridStroke = dimensionResource(R.dimen.longevity_chart_grid_stroke)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val lineStroke = dimensionResource(R.dimen.longevity_chart_line_stroke)
    val chartGap = dimensionResource(R.dimen.spacer_xs)

    val gridColor = DarkBorderMuted
    val axisColor = ChartAxisLine
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
                            drawSmoothLine(points, lineColor, lineStroke.toPx())
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
                        color = if (index == currentLabelIndex) Positive else Gray300,
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

private fun DrawScope.drawSmoothLine(points: List<Float>, color: Color, strokeWidthPx: Float) {
    val coords = points.mapIndexed { index, value ->
        val x = index.toFloat() / (points.size - 1) * size.width
        val y = size.height * mapValueToFraction(value)
        Offset(x, y)
    }
    val tangents = monotoneTangents(coords)
    val linePath = buildCubicPath(coords, tangents)

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

private fun monotoneTangents(points: List<Offset>): FloatArray {
    val n = points.size
    val tangent = FloatArray(n)
    if (n < 2) return tangent

    val dx = FloatArray(n - 1)
    val secant = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        dx[i] = points[i + 1].x - points[i].x
        secant[i] = if (dx[i] != 0f) (points[i + 1].y - points[i].y) / dx[i] else 0f
    }

    tangent[0] = secant[0]
    tangent[n - 1] = secant[n - 2]
    for (i in 1 until n - 1) {
        tangent[i] = if (secant[i - 1] * secant[i] <= 0f) 0f else (secant[i - 1] + secant[i]) / 2f
    }
    for (i in 0 until n - 1) {
        if (secant[i] == 0f) {
            tangent[i] = 0f
            tangent[i + 1] = 0f
        } else {
            val a = tangent[i] / secant[i]
            val b = tangent[i + 1] / secant[i]
            val h = hypot(a, b)
            if (h > LongevityConstants.CHART_MONOTONE_TANGENT_LIMIT) {
                val scale = LongevityConstants.CHART_MONOTONE_TANGENT_LIMIT / h
                tangent[i] = scale * a * secant[i]
                tangent[i + 1] = scale * b * secant[i]
            }
        }
    }
    return tangent
}

private fun buildCubicPath(points: List<Offset>, tangents: FloatArray): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val start = points[i]
        val end = points[i + 1]
        val dx = end.x - start.x
        path.cubicTo(
            start.x + dx / 3f, start.y + tangents[i] * dx / 3f,
            end.x - dx / 3f, end.y - tangents[i + 1] * dx / 3f,
            end.x, end.y,
        )
    }
    return path
}
