package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import com.hexis.bi.R
import com.hexis.bi.ui.main.body.BodyChartData
import com.hexis.bi.ui.main.body.BodyMassUnit
import com.hexis.bi.ui.main.body.BodyTimeRange
import com.hexis.bi.ui.main.body.BodyTrendPoint
import com.hexis.bi.ui.theme.Blue100
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.GridLineLightGray
import com.hexis.bi.utils.constants.BodyConstants
import com.hexis.bi.utils.constants.DateFormatConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
internal fun BodyTrendChart(
    chart: BodyChartData,
    timeRange: BodyTimeRange,
    onTimeRangeChange: (BodyTimeRange) -> Unit,
    massUnit: BodyMassUnit,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    val yAxisWidth = dimensionResource(R.dimen.body_chart_y_axis_width)
    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val chartHeight = dimensionResource(R.dimen.body_chart_height)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointRadius = dimensionResource(R.dimen.body_chart_point_radius)
    val pointStrokeWidth = dimensionResource(R.dimen.body_chart_line_stroke)

    val unitLabel = when (massUnit) {
        BodyMassUnit.Percent -> stringResource(R.string.unit_percent)
        BodyMassUnit.Mass -> stringResource(if (isMetric) R.string.unit_kg else R.string.unit_lb)
    }

    // Pointer snaps only to real scan days, never to gap-fill points.
    val snapPoints =
        remember(chart) { chart.points.withIndex().filter { !it.value.isInterpolated } }
    var selectedSnap by remember(chart) { mutableIntStateOf(-1) }
    var chartAreaWidth by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }

    val muscleColor = Blue100
    val fatColor = Green
    val backgroundColor = MaterialTheme.colorScheme.background
    val rangeSpan = (chart.rangeEndMillis - chart.rangeStartMillis).coerceAtLeast(1L)

    val selectedEntry = snapPoints.getOrNull(selectedSnap)?.value
    val showTooltip = selectedEntry != null && chartAreaWidth > 0
    // Keep the tooltip permanently laid out and toggle it via alpha, so the header↔tooltip swap doesn't resize.
    val tooltipEntry = selectedEntry ?: snapPoints.firstOrNull()?.value

    Column(modifier = modifier.fillMaxWidth()) {
        // Header (title + M/Y switch); the tooltip overlays it while dragging.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (tooltipEntry != null) {
                Column(
                    modifier = Modifier
                        .alpha(if (showTooltip) 1f else 0f)
                        .onSizeChanged { tooltipWidth = it.width }
                        .offset {
                            if (chartAreaWidth == 0) return@offset IntOffset.Zero
                            val centerInChart =
                                ((tooltipEntry.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * chartAreaWidth
                            val absoluteCenter = (yAxisWidth + chartGap).roundToPx() + centerInChart
                            var targetX = absoluteCenter - (tooltipWidth / 2f)
                            val maxX =
                                (yAxisWidth + chartGap).roundToPx() + chartAreaWidth - tooltipWidth
                            targetX = targetX.coerceIn(0f, maxX.toFloat().coerceAtLeast(0f))
                            IntOffset(targetX.roundToInt(), 0)
                        }
                        .background(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.shapes.small,
                        )
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacer_s),
                            vertical = dimensionResource(R.dimen.spacer_2xs),
                        ),
                ) {
                    val tooltipDate = SimpleDateFormat(
                        DateFormatConstants.WEEKDAY_MONTH_DAY,
                        Locale.getDefault(),
                    ).format(Date(tooltipEntry.timestamp))
                    Text(
                        text = tooltipDate,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TooltipDeltaRow(
                        delta = tooltipEntry.deltaMuscle,
                        unit = unitLabel,
                        color = muscleColor
                    )
                    TooltipDeltaRow(
                        delta = tooltipEntry.deltaFat,
                        unit = unitLabel,
                        color = fatColor
                    )
                }
            }

            if (!showTooltip) Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.body_composition_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                BodyRangeSelector(selected = timeRange, onSelected = onTimeRangeChange)
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(yAxisWidth),
            ) {
                chart.gridLines.forEach { value ->
                    Text(
                        text = formatGridValue(value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align { size, space, _ ->
                            val fraction = mapValueToFraction(value, chart.yAxisBound)
                            IntOffset(0, (space.height * fraction - size.height / 2).toInt())
                        },
                    )
                }
            }

            Spacer(Modifier.width(chartGap))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { chartAreaWidth = it.width }
                    .pointerInput(chart) {
                        if (snapPoints.isEmpty()) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            fun nearestSnapForX(x: Float): Int {
                                if (snapPoints.isEmpty()) return -1
                                var best = 0
                                var bestDist = Float.MAX_VALUE
                                snapPoints.forEachIndexed { i, p ->
                                    val pxAtPoint =
                                        ((p.value.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * size.width
                                    val d = abs(pxAtPoint - x)
                                    if (d < bestDist) {
                                        bestDist = d
                                        best = i
                                    }
                                }
                                return best
                            }

                            selectedSnap = nearestSnapForX(down.position.x)
                            down.consume()

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUp()) {
                                    selectedSnap = -1
                                    break
                                }
                                selectedSnap = nearestSnapForX(change.position.x)
                                change.consume()
                            }
                        }
                    }
                    .drawBehind {
                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f,
                        )
                        chart.gridLines.forEach { value ->
                            val y = size.height * mapValueToFraction(value, chart.yAxisBound)
                            drawLine(
                                color = GridLineLightGray,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = stripeWidth.toPx(),
                                pathEffect = dashEffect,
                            )
                        }

                        if (chart.points.isEmpty()) return@drawBehind

                        drawTimeSeries(
                            points = chart.points,
                            rangeStart = chart.rangeStartMillis,
                            rangeSpan = rangeSpan,
                            yAxisBound = chart.yAxisBound,
                            color = fatColor,
                            strokeWidthPx = pointStrokeWidth.toPx(),
                            extractValue = { it.deltaFat },
                        )
                        drawTimeSeries(
                            points = chart.points,
                            rangeStart = chart.rangeStartMillis,
                            rangeSpan = rangeSpan,
                            yAxisBound = chart.yAxisBound,
                            color = muscleColor,
                            strokeWidthPx = pointStrokeWidth.toPx(),
                            extractValue = { it.deltaMuscle },
                        )

                        if (selectedEntry != null) {
                            val centerX =
                                ((selectedEntry.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * size.width
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, size.height),
                                strokeWidth = stripeWidth.toPx(),
                                pathEffect = dashEffect,
                            )
                            val muscleY = size.height * mapValueToFraction(
                                selectedEntry.deltaMuscle,
                                chart.yAxisBound
                            )
                            val fatY = size.height * mapValueToFraction(
                                selectedEntry.deltaFat,
                                chart.yAxisBound
                            )
                            drawCircle(
                                backgroundColor,
                                pointRadius.toPx(),
                                Offset(centerX, muscleY)
                            )
                            drawCircle(
                                muscleColor,
                                pointRadius.toPx(),
                                Offset(centerX, muscleY),
                                style = Stroke(width = pointStrokeWidth.toPx()),
                            )
                            drawCircle(backgroundColor, pointRadius.toPx(), Offset(centerX, fatY))
                            drawCircle(
                                fatColor,
                                pointRadius.toPx(),
                                Offset(centerX, fatY),
                                style = Stroke(width = pointStrokeWidth.toPx()),
                            )
                        }
                    },
            )
        }

        XAxisLabels(
            chart = chart,
            yAxisWidth = yAxisWidth,
            chartGap = chartGap,
            rangeSpan = rangeSpan,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        // Legend sits below the chart and stays visible while dragging (unlike the header).
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
            ) {
                LegendDot(
                    color = muscleColor,
                    label = stringResource(R.string.body_chart_legend_muscle, unitLabel)
                )
                LegendDot(
                    color = fatColor,
                    label = stringResource(R.string.body_chart_legend_fat, unitLabel)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = chart.rangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun XAxisLabels(
    chart: BodyChartData,
    yAxisWidth: androidx.compose.ui.unit.Dp,
    chartGap: androidx.compose.ui.unit.Dp,
    rangeSpan: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacer_xxs)),
    ) {
        Spacer(Modifier.width(yAxisWidth + chartGap))
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            chart.axisLabels.forEach { label ->
                if (label.text.isEmpty()) return@forEach
                val xPx =
                    ((label.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * widthPx
                Text(
                    text = label.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.offset { IntOffset(xPx.roundToInt(), 0) },
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.size_indicator_bigger))
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun TooltipDeltaRow(delta: Float, unit: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.size_indicator_bigger))
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = formatDelta(delta, unit),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

/** Maps a value in [-bound, +bound] to a vertical fraction (0 = top, 1 = bottom). 0 ⇒ 0.5. */
private fun mapValueToFraction(value: Float, bound: Float): Float {
    val b = bound.coerceAtLeast(BodyConstants.CHART_MIN_HALF_RANGE)
    val clamped = value.coerceIn(-b, b)
    return 0.5f - (clamped / (2f * b))
}

private fun formatGridValue(value: Float): String =
    if (value == 0f) GRID_ZERO_LABEL else formatOneDecimal(value)

private const val GRID_ZERO_LABEL = "0"

private fun formatOneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)

/** Time-proportional curve + soft fill; the line is a monotone cubic spline so it never bows past the data or chart band. */
private fun DrawScope.drawTimeSeries(
    points: List<BodyTrendPoint>,
    rangeStart: Long,
    rangeSpan: Long,
    yAxisBound: Float,
    color: Color,
    strokeWidthPx: Float,
    extractValue: (BodyTrendPoint) -> Float,
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        val p = points[0]
        val x = ((p.timestamp - rangeStart).toFloat() / rangeSpan) * size.width
        val y = size.height * mapValueToFraction(extractValue(p), yAxisBound)
        drawCircle(color, strokeWidthPx, Offset(x, y))
        return
    }

    val coords = points.map { p ->
        val x = ((p.timestamp - rangeStart).toFloat() / rangeSpan) * size.width
        val y = size.height * mapValueToFraction(extractValue(p), yAxisBound)
        Offset(x, y)
    }

    val linePath = buildMonotoneCubicPath(coords)
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(coords.last().x, size.height)
        lineTo(coords.first().x, size.height)
        close()
    }

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.02f)),
        ),
    )
    drawPath(
        path = linePath,
        color = color,
        style = Stroke(width = strokeWidthPx),
    )
}

/** Monotone cubic Hermite spline (Fritsch–Carlson) through [points], as Bézier segments — stays between adjacent y-values. */
private fun buildMonotoneCubicPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    val n = points.size
    if (n == 1) return path
    if (n == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    val dx = FloatArray(n - 1)
    val secant = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        dx[i] = points[i + 1].x - points[i].x
        secant[i] = if (dx[i] != 0f) (points[i + 1].y - points[i].y) / dx[i] else 0f
    }

    val tangent = FloatArray(n)
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
            if (h > MONOTONE_TANGENT_LIMIT) {
                val scale = MONOTONE_TANGENT_LIMIT / h
                tangent[i] = scale * a * secant[i]
                tangent[i + 1] = scale * b * secant[i]
            }
        }
    }

    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        path.cubicTo(
            p0.x + dx[i] / 3f, p0.y + tangent[i] * dx[i] / 3f,
            p1.x - dx[i] / 3f, p1.y - tangent[i + 1] * dx[i] / 3f,
            p1.x, p1.y,
        )
    }
    return path
}

/** Fritsch–Carlson cap on |tangent / secant| that keeps each spline segment monotone. */
private const val MONOTONE_TANGENT_LIMIT = 3f
