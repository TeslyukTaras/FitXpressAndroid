package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.os.ConfigurationCompat
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.body.BodyChartData
import com.hexis.bi.ui.main.body.BodyTimeRange
import com.hexis.bi.ui.main.body.BodyTrendPhase
import com.hexis.bi.ui.main.body.BodyTrendPoint
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.BodyConstants
import com.hexis.bi.utils.constants.DateFormatConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun BodyTrendChart(
    chart: BodyChartData,
    timeRange: BodyTimeRange,
    onTimeRangeChange: (BodyTimeRange) -> Unit,
    modifier: Modifier = Modifier,
    onOpenClick: (() -> Unit)? = null,
    showSegmentLegend: Boolean = false,
) {
    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val chartHeight = dimensionResource(R.dimen.body_chart_height)
    val dashWidth = dimensionResource(R.dimen.body_chart_dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointRadius = dimensionResource(R.dimen.body_chart_point_radius)
    val pointStrokeWidth = dimensionResource(R.dimen.body_chart_line_stroke)
    val gridStrokeWidth = dimensionResource(R.dimen.border_line)
    val gridDashWidth = dimensionResource(R.dimen.body_chart_grid_dash)

    val snapPoints =
        remember(chart) { chart.points.withIndex().filter { !it.value.isInterpolated } }
    var selectedSnap by remember(chart) { mutableIntStateOf(-1) }
    var chartAreaLeftPx by remember { mutableIntStateOf(0) }
    var chartAreaWidth by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }
    var yAxisWidthPx by remember { mutableIntStateOf(0) }

    val chartColors = NocturnePulseTheme.extendedColors
    val muscleColor = chartColors.chartLeanAdvantage
    val fatColor = chartColors.chartFatAdvantage
    val pointBackgroundColor = MaterialTheme.colorScheme.surface
    val originRingColor = MaterialTheme.colorScheme.onSurface
    val rangeSpan = (chart.rangeEndMillis - chart.rangeStartMillis).coerceAtLeast(1L)

    val pointLabelMeasurer = rememberTextMeasurer()
    val pointLabelStyle = MaterialTheme.typography.bodySmall
    val labeledIndices = remember(chart) {
        if (chart.points.isEmpty()) {
            emptyList()
        } else {
            val atTicks = chart.axisLabels
                .filter { it.text.isNotEmpty() }
                .mapNotNull { label ->
                    chart.points.indices.minByOrNull {
                        abs(chart.points[it].timestamp - label.timestamp)
                    }
                }
            atTicks
                .filter { it > 0 }
                .distinct()
                .sorted()
        }
    }

    val selectedEntry = snapPoints.getOrNull(selectedSnap)?.value
    val showTooltip = selectedEntry != null && chartAreaWidth > 0
    val tooltipEntry = selectedEntry ?: snapPoints.firstOrNull()?.value

    val density = LocalDensity.current
    val yAxisWidth = with(density) { yAxisWidthPx.toDp() }
    val spacerXs = dimensionResource(R.dimen.spacer_xs)
    val tooltipOverlapUpPx = remember(spacerXs, density) { with(density) { spacerXs.roundToPx() } }
    val tooltipOverlapUpDp =
        remember(spacerXs, density) { with(density) { tooltipOverlapUpPx.toDp() } }

    val configuration = LocalConfiguration.current
    val layoutLocale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.ROOT
    val tooltipDateFormat = remember(layoutLocale.toLanguageTag()) {
        SimpleDateFormat(DateFormatConstants.WEEKDAY_MONTH_DAY, layoutLocale)
    }

    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_m),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (showTooltip) 0f else 1f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.body_physique_balance_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = dimensionResource(R.dimen.spacer_m)),
                )
                if (onOpenClick != null) IconButton(
                    onClick = onOpenClick,
                    enabled = !showTooltip,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_normalized))
                        .padding(
                            top = dimensionResource(R.dimen.spacer_3xs),
                            end = dimensionResource(R.dimen.spacer_3xs)
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_open_external),
                        contentDescription = stringResource(R.string.cd_body_physique_balance),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
                    )
                } else BodyRangeSelector(
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.spacer_m),
                        end = dimensionResource(R.dimen.spacer_m)
                    ),
                    selected = timeRange,
                    onSelected = onTimeRangeChange,
                    enabled = !showTooltip,
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = dimensionResource(R.dimen.spacer_m))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight + tooltipOverlapUpDp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                            .align(Alignment.BottomCenter),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .onSizeChanged { yAxisWidthPx = it.width },
                        ) {
                            chart.gridLines.forEach { value ->
                                Text(
                                    text = formatGridValue(value),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        value > 0f -> muscleColor
                                        value < 0f -> fatColor
                                        else -> chartColors.chartZeroLabel
                                    },
                                    modifier = Modifier.align { size, space, _ ->
                                        val fraction =
                                            mapValueToFraction(value, chart.yAxisBound)
                                        IntOffset(
                                            space.width - size.width,
                                            (space.height * fraction - size.height / 2).toInt()
                                        )
                                    },
                                )
                            }
                        }

                        Spacer(Modifier.width(chartGap))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onGloballyPositioned {
                                    chartAreaLeftPx = it.positionInParent().x.roundToInt()
                                }
                                .onSizeChanged { chartAreaWidth = it.width }
                                .drawBehind {
                                    val dashEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f,
                                    )
                                    val gridStrokePx = gridStrokeWidth.toPx()
                                    val centerStrokePx = stripeWidth.toPx()
                                    val verticalDashEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(gridDashWidth.toPx(), gridDashWidth.toPx()),
                                        0f,
                                    )
                                    chart.gridLines.forEach { value ->
                                        val y = size.height * mapValueToFraction(
                                            value,
                                            chart.yAxisBound
                                        )
                                        val isZero = value == 0f
                                        drawLine(
                                            color = if (isZero) {
                                                chartColors.chartCenterLine
                                            } else {
                                                chartColors.chartGridLine
                                            },
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = if (isZero) centerStrokePx else gridStrokePx,
                                        )
                                    }
                                    chart.axisLabels.forEach { label ->
                                        val x =
                                            ((label.timestamp - chart.rangeStartMillis).toFloat() /
                                                    rangeSpan) * size.width
                                        if (x <= 0f || x >= size.width) return@forEach
                                        drawLine(
                                            color = chartColors.chartVerticalLine,
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = gridStrokePx,
                                            pathEffect = verticalDashEffect,
                                        )
                                    }
                                    drawLine(
                                        color = chartColors.chartAxisLine,
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = gridStrokePx,
                                    )
                                    drawLine(
                                        color = chartColors.chartBoundaryLine,
                                        start = Offset(size.width, 0f),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = gridStrokePx,
                                    )

                                    if (chart.points.isEmpty()) return@drawBehind

                                    drawTimeSeries(
                                        points = chart.points,
                                        rangeStart = chart.rangeStartMillis,
                                        rangeSpan = rangeSpan,
                                        yAxisBound = chart.yAxisBound,
                                        color = fatColor,
                                        strokeWidthPx = pointStrokeWidth.toPx(),
                                        dashWidthPx = dashWidth.toPx(),
                                        extractValue = { it.deltaFat },
                                    )
                                    drawTimeSeries(
                                        points = chart.points,
                                        rangeStart = chart.rangeStartMillis,
                                        rangeSpan = rangeSpan,
                                        yAxisBound = chart.yAxisBound,
                                        color = muscleColor,
                                        strokeWidthPx = pointStrokeWidth.toPx(),
                                        dashWidthPx = dashWidth.toPx(),
                                        extractValue = { it.deltaMuscle },
                                    )

                                    val origin = chart.points.firstOrNull()
                                        ?.takeIf { it.phase == BodyTrendPhase.ConfirmedScan }
                                    if (origin != null) {
                                        val originX =
                                            ((origin.timestamp - chart.rangeStartMillis).toFloat() /
                                                    rangeSpan) * size.width
                                        val originY =
                                            size.height * mapValueToFraction(0f, chart.yAxisBound)
                                        drawCircle(
                                            pointBackgroundColor,
                                            pointRadius.toPx() * 0.7f,
                                            Offset(originX, originY)
                                        )
                                        drawCircle(
                                            originRingColor,
                                            pointRadius.toPx() * 0.7f,
                                            Offset(originX, originY),
                                            style = Stroke(width = pointStrokeWidth.toPx()),
                                        )
                                    }

                                    if (showSegmentLegend && chart.points.isNotEmpty()) {
                                        val dotRadiusPx = pointRadius.toPx() * 0.6f
                                        val labelGapPx = pointRadius.toPx() * 0.5f
                                        labeledIndices.forEach { index ->
                                            val point = chart.points[index]
                                            val centerX =
                                                ((point.timestamp - chart.rangeStartMillis).toFloat() /
                                                        rangeSpan) * size.width
                                            drawSeriesLabel(
                                                centerX = centerX,
                                                value = point.deltaMuscle,
                                                color = muscleColor,
                                                above = true,
                                                dotRadiusPx = dotRadiusPx,
                                                gapPx = labelGapPx,
                                                yAxisBound = chart.yAxisBound,
                                                textMeasurer = pointLabelMeasurer,
                                                style = pointLabelStyle,
                                            )
                                            drawSeriesLabel(
                                                centerX = centerX,
                                                value = point.deltaFat,
                                                color = fatColor,
                                                above = false,
                                                dotRadiusPx = dotRadiusPx,
                                                gapPx = labelGapPx,
                                                yAxisBound = chart.yAxisBound,
                                                textMeasurer = pointLabelMeasurer,
                                                style = pointLabelStyle,
                                            )
                                        }
                                    }

                                    val entry = selectedEntry ?: return@drawBehind
                                    val centerX =
                                        ((entry.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * size.width
                                    drawLine(
                                        color = chartColors.chartSelectionLine,
                                        start = Offset(centerX, 0f),
                                        end = Offset(centerX, size.height),
                                        strokeWidth = stripeWidth.toPx(),
                                        pathEffect = dashEffect,
                                    )
                                    val muscleY = size.height * mapValueToFraction(
                                        entry.deltaMuscle,
                                        chart.yAxisBound
                                    )
                                    val fatY =
                                        size.height * mapValueToFraction(
                                            entry.deltaFat,
                                            chart.yAxisBound
                                        )
                                    drawCircle(
                                        pointBackgroundColor,
                                        pointRadius.toPx(),
                                        Offset(centerX, muscleY)
                                    )
                                    drawCircle(
                                        muscleColor,
                                        pointRadius.toPx(),
                                        Offset(centerX, muscleY),
                                        style = Stroke(width = pointStrokeWidth.toPx()),
                                    )
                                    drawCircle(
                                        pointBackgroundColor,
                                        pointRadius.toPx(),
                                        Offset(centerX, fatY)
                                    )
                                    drawCircle(
                                        fatColor,
                                        pointRadius.toPx(),
                                        Offset(centerX, fatY),
                                        style = Stroke(width = pointStrokeWidth.toPx()),
                                    )
                                }
                        ) {
                            if (tooltipEntry != null) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .onSizeChanged { tooltipWidth = it.width }
                                        .offset {
                                            if (chartAreaWidth == 0) return@offset IntOffset.Zero
                                            val centerInChart =
                                                ((tooltipEntry.timestamp - chart.rangeStartMillis).toFloat() /
                                                        rangeSpan) *
                                                        chartAreaWidth
                                            val maxX = chartAreaWidth - tooltipWidth
                                            val targetX =
                                                (centerInChart - tooltipWidth / 2f).coerceIn(
                                                    0f,
                                                    maxX.toFloat().coerceAtLeast(0f),
                                                )
                                            IntOffset(targetX.roundToInt(), -tooltipOverlapUpPx)
                                        }
                                        .alpha(if (showTooltip) 1f else 0f)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHigh,
                                            MaterialTheme.shapes.small,
                                        )
                                        .border(
                                            BorderStroke(
                                                dimensionResource(R.dimen.border_thin),
                                                NocturnePulseTheme.extendedColors.cardBorder,
                                            ),
                                            MaterialTheme.shapes.small,
                                        )
                                        .padding(
                                            horizontal = dimensionResource(R.dimen.spacer_s),
                                            vertical = dimensionResource(R.dimen.spacer_2xs),
                                        ),
                                ) {
                                    val tooltipDate =
                                        tooltipDateFormat.format(Date(tooltipEntry.timestamp))
                                    Text(
                                        text = tooltipDate,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    TooltipDeltaRow(
                                        delta = tooltipEntry.deltaMuscle,
                                        color = muscleColor,
                                    )
                                    TooltipDeltaRow(
                                        delta = tooltipEntry.deltaFat,
                                        color = fatColor,
                                    )
                                }
                            }
                        }
                    }
                }

                XAxisLabels(
                    chart = chart,
                    yAxisWidth = yAxisWidth,
                    chartGap = chartGap,
                    rangeSpan = rangeSpan,
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(chart, rangeSpan, chartAreaLeftPx, chartAreaWidth) {
                        if (snapPoints.isEmpty()) return@pointerInput
                        fun plotStartAndWidth(): Pair<Float, Float> {
                            val startPx = chartAreaLeftPx.toFloat()
                            val plotW = chartAreaWidth.toFloat().coerceAtLeast(1f)
                            return startPx to plotW
                        }

                        fun nearestSnapForX(localXInPlot: Float, plotW: Float): Int {
                            var best = 0
                            var bestDist = Float.MAX_VALUE
                            snapPoints.forEachIndexed { i, p ->
                                val pxAtPoint =
                                    ((p.value.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * plotW
                                val d = abs(pxAtPoint - localXInPlot)
                                if (d < bestDist) {
                                    bestDist = d
                                    best = i
                                }
                            }
                            return best
                        }
                        awaitEachGesture {
                            val (plotStartPx, plotW) = plotStartAndWidth()
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val clampedX = (down.position.x - plotStartPx).coerceIn(0f, plotW)
                            selectedSnap = nearestSnapForX(clampedX, plotW)
                            down.consume()

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUp()) {
                                    selectedSnap = -1
                                    break
                                }
                                val (startPx, w) = plotStartAndWidth()
                                val lx = (change.position.x - startPx).coerceIn(0f, w)
                                selectedSnap = nearestSnapForX(lx, w)
                                change.consume()
                            }
                        }
                    },
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
            ) {
                LegendDot(
                    color = muscleColor,
                    label = stringResource(R.string.body_chart_legend_muscle)
                )
                LegendDot(
                    color = fatColor,
                    label = stringResource(R.string.body_chart_legend_fat)
                )
            }
        }
        if (showSegmentLegend) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            AppHorizontalGradientDivider(modifier = Modifier.padding(end = dimensionResource(R.dimen.spacer_m)))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            SegmentLegendRow(
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacer_m)),
                confirmedColor = MaterialTheme.colorScheme.onSurface,
                predictedColor = MaterialTheme.colorScheme.onSurface,
                futureColor = MaterialTheme.colorScheme.onSurface,
                dashWidth = dashWidth,
            )
        }
    }
}

@Composable
private fun XAxisLabels(
    chart: BodyChartData,
    yAxisWidth: Dp,
    chartGap: Dp,
    rangeSpan: Long,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacer_xs)),
    ) {
        Spacer(Modifier.width(yAxisWidth + chartGap))
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.roundToPx() }
            chart.axisLabels.forEach { label ->
                if (label.text.isEmpty()) return@forEach
                val rawX =
                    ((label.timestamp - chart.rangeStartMillis).toFloat() / rangeSpan) * widthPx
                val measuredWidth =
                    textMeasurer.measure(AnnotatedString(label.text), style = labelStyle).size.width
                val maxLeft = max(0, widthPx - measuredWidth)
                val xLeft = (rawX - measuredWidth / 2f).roundToInt().coerceIn(0, maxLeft)
                Text(
                    text = label.text,
                    style = labelStyle,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.offset { IntOffset(xLeft, 0) },
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
private fun TooltipDeltaRow(delta: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.size_indicator_bigger))
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = formatDelta(delta),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
private fun SegmentLegendRow(
    modifier: Modifier,
    confirmedColor: Color,
    predictedColor: Color,
    futureColor: Color,
    dashWidth: Dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SegmentLegendItem(
            label = stringResource(R.string.body_chart_legend_confirmed_scan),
            color = confirmedColor,
            phase = BodyTrendPhase.ConfirmedScan,
            dashWidth = dashWidth,
        )
        SegmentLegendItem(
            label = stringResource(R.string.body_chart_legend_predicted_drift),
            color = predictedColor,
            phase = BodyTrendPhase.PredictedDrift,
            dashWidth = dashWidth,
        )
        SegmentLegendItem(
            label = stringResource(R.string.body_chart_legend_future_estimate),
            color = futureColor,
            phase = BodyTrendPhase.FutureEstimate,
            dashWidth = dashWidth,
        )
    }
}

@Composable
private fun SegmentLegendItem(
    label: String,
    color: Color,
    phase: BodyTrendPhase,
    dashWidth: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_2xs)),
    ) {
        Box(
            modifier = Modifier
                .width(dimensionResource(R.dimen.body_chart_legend_line_width))
                .height(dimensionResource(R.dimen.border_line))
                .drawBehind {
                    val effect = pathEffectFor(phase, dashWidth.toPx())
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = size.height,
                        pathEffect = effect,
                    )
                },
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private fun DrawScope.drawSeriesLabel(
    centerX: Float,
    value: Float,
    color: Color,
    above: Boolean,
    dotRadiusPx: Float,
    gapPx: Float,
    yAxisBound: Float,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    val y = size.height * mapValueToFraction(value, yAxisBound)
    drawCircle(color, dotRadiusPx, Offset(centerX, y))

    val layout = textMeasurer.measure(
        AnnotatedString(String.format(Locale.US, "%+.1f", value)),
        style.copy(color = color),
    )
    val width = layout.size.width.toFloat()
    val height = layout.size.height.toFloat()
    val x = (centerX - width / 2f).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
    val rawY = if (above) y - dotRadiusPx - gapPx - height else y + dotRadiusPx + gapPx
    val labelY = rawY.coerceIn(0f, (size.height - height).coerceAtLeast(0f))
    drawText(layout, topLeft = Offset(x, labelY))
}

private fun mapValueToFraction(value: Float, bound: Float): Float {
    val b = bound.coerceAtLeast(BodyConstants.CHART_MIN_HALF_RANGE)
    val clamped = value.coerceIn(-b, b)
    return 0.5f - (clamped / (2f * b))
}

private fun formatGridValue(value: Float): String =
    when {
        value == 0f -> GRID_ZERO_LABEL
        value > 0f -> "+${formatOneDecimal(value)}"
        else -> formatOneDecimal(value)
    }

private const val GRID_ZERO_LABEL = "0"

private fun formatOneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)

private fun DrawScope.drawTimeSeries(
    points: List<BodyTrendPoint>,
    rangeStart: Long,
    rangeSpan: Long,
    yAxisBound: Float,
    color: Color,
    strokeWidthPx: Float,
    dashWidthPx: Float,
    extractValue: (BodyTrendPoint) -> Float,
) {
    if (points.isEmpty()) return
    val renderedPoints = if (points.last().timestamp < rangeStart + rangeSpan) {
        points + points.last().copy(
            timestamp = rangeStart + rangeSpan,
            isInterpolated = true,
        )
    } else {
        points
    }
    if (renderedPoints.size == 1) {
        val p = renderedPoints[0]
        val x = ((p.timestamp - rangeStart).toFloat() / rangeSpan) * size.width
        val y = size.height * mapValueToFraction(extractValue(p), yAxisBound)
        drawCircle(color, strokeWidthPx, Offset(x, y))
        return
    }

    val coords = renderedPoints.map { p ->
        val x = ((p.timestamp - rangeStart).toFloat() / rangeSpan) * size.width
        val y = size.height * mapValueToFraction(extractValue(p), yAxisBound)
        Offset(x, y)
    }

    val tangents = monotoneTangents(coords)
    val linePath = buildCubicPath(coords, tangents)
    val zeroY = size.height * mapValueToFraction(0f, yAxisBound)
    val fillPath = Path().apply {
        addPath(linePath)
        lineTo(coords.last().x, zeroY)
        lineTo(coords.first().x, zeroY)
        close()
    }

    // Symmetric drop shadow: opaque at the line's extremes (above AND below zero), fading to
    // transparent at the zero axis. Anchoring the gradient around zeroY (not a single farY) means
    // the fill always falls toward the middle regardless of whether the series rises or drops.
    val maxDist = (coords.maxOfOrNull { abs(it.y - zeroY) } ?: 0f).coerceAtLeast(1f)
    val edgeColor = color.copy(alpha = BodyConstants.CHART_FILL_START_ALPHA * BodyConstants.CHART_FILL_OPACITY)
    val zeroColor = color.copy(alpha = BodyConstants.CHART_FILL_END_ALPHA * BodyConstants.CHART_FILL_OPACITY)
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to edgeColor,   // farthest above the zero axis
                0.5f to zeroColor, // zero axis (transparent toward the middle)
                1f to edgeColor,   // farthest below the zero axis
            ),
            startY = zeroY - maxDist,
            endY = zeroY + maxDist,
        ),
    )
    var strokePhase = segmentPhase(renderedPoints[0], renderedPoints[1])
    var strokePath = Path().apply { moveTo(coords[0].x, coords[0].y) }
    fun drawStroke(path: Path, phase: BodyTrendPhase) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                pathEffect = pathEffectFor(phase, dashWidthPx),
            ),
        )
    }
    for (index in 0 until coords.lastIndex) {
        val phase = segmentPhase(renderedPoints[index], renderedPoints[index + 1])
        if (phase != strokePhase) {
            drawStroke(strokePath, strokePhase)
            strokePhase = phase
            strokePath = Path().apply { moveTo(coords[index].x, coords[index].y) }
        }
        strokePath.cubicSegmentTo(
            coords[index], coords[index + 1], tangents[index], tangents[index + 1],
        )
    }
    drawStroke(strokePath, strokePhase)
}

private fun segmentPhase(start: BodyTrendPoint, end: BodyTrendPoint): BodyTrendPhase = when {
    start.phase == BodyTrendPhase.FutureEstimate || end.phase == BodyTrendPhase.FutureEstimate ->
        BodyTrendPhase.FutureEstimate

    start.phase == BodyTrendPhase.PredictedDrift || end.phase == BodyTrendPhase.PredictedDrift ->
        BodyTrendPhase.PredictedDrift

    else -> BodyTrendPhase.ConfirmedScan
}

private fun pathEffectFor(phase: BodyTrendPhase, dashWidthPx: Float): PathEffect? = when (phase) {
    BodyTrendPhase.ConfirmedScan -> null
    BodyTrendPhase.PredictedDrift -> PathEffect.dashPathEffect(
        floatArrayOf(dashWidthPx, dashWidthPx),
        0f,
    )

    BodyTrendPhase.FutureEstimate -> PathEffect.dashPathEffect(
        floatArrayOf(dashWidthPx * 0.5f, dashWidthPx * 0.5f),
        0f,
    )
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
            if (h > BodyConstants.CHART_MONOTONE_TANGENT_LIMIT) {
                val scale = BodyConstants.CHART_MONOTONE_TANGENT_LIMIT / h
                tangent[i] = scale * a * secant[i]
                tangent[i + 1] = scale * b * secant[i]
            }
        }
    }
    return tangent
}

private fun Path.cubicSegmentTo(
    start: Offset,
    end: Offset,
    startTangent: Float,
    endTangent: Float
) {
    val dx = end.x - start.x
    cubicTo(
        start.x + dx / 3f, start.y + startTangent * dx / 3f,
        end.x - dx / 3f, end.y - endTangent * dx / 3f,
        end.x, end.y,
    )
}

private fun buildCubicPath(points: List<Offset>, tangents: FloatArray): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        path.cubicSegmentTo(points[i], points[i + 1], tangents[i], tangents[i + 1])
    }
    return path
}
