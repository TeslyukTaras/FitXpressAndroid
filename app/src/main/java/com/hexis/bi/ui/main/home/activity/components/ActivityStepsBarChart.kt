package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.BarChartEntry
import com.hexis.bi.ui.theme.AccentBlue
import com.hexis.bi.ui.theme.ActivityMediumTitleStyle
import com.hexis.bi.ui.theme.ChartTooltipFill
import com.hexis.bi.ui.theme.ChartTooltipBorder
import com.hexis.bi.ui.theme.BodyToggleSelectedLabel
import com.hexis.bi.ui.theme.Gray200
import com.hexis.bi.ui.theme.Gray300
import com.hexis.bi.ui.theme.White
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.dark.ActivityVerticalGridLine
import com.hexis.bi.ui.theme.dark.ChartAxisLine
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.utils.constants.ActivityConstants
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ActivityStepsBarChart(
    entries: List<BarChartEntry>,
    totalValue: Int,
    baseYMax: Float,
    yGridStep: Float,
    title: String,
    barGap: Dp,
    modifier: Modifier = Modifier,
    xLabelStartPadding: Dp = 0.dp,
    chartStartPadding: Dp = 0.dp,
    chartEndPadding: Dp = 0.dp,
    xAxisStartLabel: String? = null,
    xAxisEndLabel: String? = null,
    xAxisEdgeLabelColor: Color = Gray300,
    xAxisBarLabelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    highlightedXLabelIndex: Int = -1,
    goalValue: Int? = null,
    showVerticalGridLines: Boolean = false,
    yLabelFormatter: ((Float) -> String)? = null,
) {
    val resolvedYLabelFormatter = yLabelFormatter ?: { value -> formatYAxisLabel(value) }
    val yMax = computeEffectiveYMax(entries, baseYMax, yGridStep)
    val yGridLines = remember(yMax) {
        listOf(0f, yMax / 3f, yMax * 2f / 3f, yMax)
    }
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val selectedEntry = entries.getOrNull(selectedIndex)

    var barsAreaWidth by remember { mutableIntStateOf(0) }
    var yAxisWidthPx by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }
    var tooltipHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val measuredYAxisWidth = with(density) { yAxisWidthPx.toDp() }

    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointerVerticalPadding = dimensionResource(R.dimen.activity_chart_pointer_vertical_padding)
    val pointerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline
    val axisColor = ChartAxisLine
    val axisStrokeWidth = dimensionResource(R.dimen.border_hairline)
    val vGridColor = ActivityVerticalGridLine
    val vGridDash = dimensionResource(R.dimen.activity_chart_vgrid_dash)
    val vGridWidth = dimensionResource(R.dimen.activity_chart_vgrid_width)

    Column(modifier = modifier.fillMaxWidth()) {
        val showTooltip = selectedEntry != null && barsAreaWidth > 0
        val tooltipEntry = selectedEntry ?: entries.firstOrNull()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (selectedIndex in entries.indices && barsAreaWidth > 0) {
                        val gapPx = barGap.toPx()
                        val startPaddingPx = chartStartPadding.toPx()
                        val endPaddingPx = chartEndPadding.toPx()
                        val contentWidthPx =
                            (barsAreaWidth - startPaddingPx - endPaddingPx)
                                .coerceAtLeast(0f)
                        val barWidthPx =
                            (contentWidthPx - gapPx * (entries.size - 1)) / entries.size.toFloat()
                        val centerOfBarX =
                            startPaddingPx + selectedIndex * (barWidthPx + gapPx) + (barWidthPx / 2f)
                        val absoluteCenterX = yAxisWidthPx + chartGap.toPx() + centerOfBarX

                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f
                        )

                        val startY = tooltipHeight.toFloat()
                        val endY = size.height - pointerVerticalPadding.toPx()

                        if (startY < endY) {
                            drawLine(
                                color = pointerColor,
                                start = Offset(absoluteCenterX, startY),
                                end = Offset(absoluteCenterX, endY),
                                strokeWidth = stripeWidth.toPx(),
                                pathEffect = dashEffect,
                            )
                        }
                    }
                },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .alpha(if (showTooltip) 0f else 1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = fmt.format(totalValue),
                                style = ActivityMediumTitleStyle,
                                color = AccentBlue,
                                modifier = Modifier.alignByBaseline(),
                            )
                            if (goalValue != null) Text(
                                text = stringResource(
                                    R.string.activity_goal_value_suffix,
                                    fmt.format(goalValue),
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.activity_steps_chart_height)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .onSizeChanged { yAxisWidthPx = it.width }
                    ) {
                        yGridLines.forEach { value ->
                            val fraction = 1f - (value / yMax)
                            Text(
                                text = resolvedYLabelFormatter(value),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                minLines = 1,
                                modifier = Modifier.align { size, space, _ ->
                                    IntOffset(
                                        space.width - size.width,
                                        (space.height * fraction - size.height / 2).toInt(),
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
                            .onSizeChanged { barsAreaWidth = it.width }
                            .pointerInput(entries.size) {
                                if (entries.isEmpty()) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val gapPx = barGap.toPx()
                                    val startPaddingPx = chartStartPadding.toPx()
                                    val endPaddingPx = chartEndPadding.toPx()
                                    val contentWidthPx =
                                        (size.width - startPaddingPx - endPaddingPx)
                                            .coerceAtLeast(0f)
                                    val barWidthPx =
                                        (contentWidthPx - gapPx * (entries.size - 1)) / entries.size.toFloat()
                                    val slotWidthPx = barWidthPx + gapPx

                                    fun indexForX(x: Float): Int =
                                        ((x - startPaddingPx).coerceIn(0f, contentWidthPx) / slotWidthPx)
                                            .toInt()
                                            .coerceIn(0, entries.size - 1)

                                    selectedIndex = indexForX(down.position.x)
                                    down.consume()

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (change.changedToUp()) {
                                            selectedIndex = -1
                                            break
                                        }
                                        selectedIndex = indexForX(change.position.x)
                                        change.consume()
                                    }
                                }
                            }
                            .drawBehind {
                                val dashEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f
                                )
                                drawLine(
                                    color = axisColor,
                                    start = Offset(0f, -chartGap.toPx()),
                                    end = Offset(0f, size.height + chartGap.toPx()),
                                    strokeWidth = axisStrokeWidth.toPx(),
                                )
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = stripeWidth.toPx(),
                                    pathEffect = dashEffect,
                                )
                                yGridLines.filter { it > 0f }.forEach { value ->
                                    val y = size.height * (1f - value / yMax)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = stripeWidth.toPx(),
                                        pathEffect = dashEffect,
                                    )
                                }
                                if (showVerticalGridLines && entries.isNotEmpty()) {
                                    val vDash = PathEffect.dashPathEffect(
                                        floatArrayOf(vGridDash.toPx(), vGridDash.toPx()), 0f,
                                    )
                                    val startPaddingPx = chartStartPadding.toPx()
                                    val endPaddingPx = chartEndPadding.toPx()
                                    val labelStartPx = xLabelStartPadding.toPx()
                                    val gridGapPx = barGap.toPx()
                                    val contentWidthPx =
                                        (size.width - startPaddingPx - endPaddingPx).coerceAtLeast(0f)
                                    val gridBarWidthPx =
                                        (contentWidthPx - gridGapPx * (entries.size - 1)) / entries.size.toFloat()
                                    entries.forEachIndexed { index, entry ->
                                        if (entry.xLabel == null) return@forEachIndexed
                                        val x = startPaddingPx +
                                                index * (gridBarWidthPx + gridGapPx) + labelStartPx
                                        drawLine(
                                            color = vGridColor,
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = vGridWidth.toPx(),
                                            pathEffect = vDash,
                                        )
                                    }
                                }
                            },
                    ) {
                        if (entries.isNotEmpty() && barsAreaWidth > 0) {
                            val gapPx = with(density) { barGap.toPx() }
                            val startPaddingPx = with(density) { chartStartPadding.toPx() }
                            val endPaddingPx = with(density) { chartEndPadding.toPx() }
                            val contentWidthPx =
                                (barsAreaWidth - startPaddingPx - endPaddingPx).coerceAtLeast(0f)
                            val barWidthPx =
                                (contentWidthPx - gapPx * (entries.size - 1)) / entries.size.toFloat()
                            val barWidthDp = with(density) { barWidthPx.toDp() }

                            entries.forEachIndexed { index, entry ->
                                val offsetX = startPaddingPx + index * (barWidthPx + gapPx)
                                ChartBar(
                                    value = entry.value,
                                    yMax = yMax,
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX.roundToInt(), 0) }
                                        .width(barWidthDp)
                                )
                            }
                        }
                    }
                }
            }

            if (tooltipEntry != null) {
                Column(
                    modifier = Modifier
                        .alpha(if (showTooltip) 1f else 0f)
                        .onSizeChanged {
                            tooltipWidth = it.width
                            tooltipHeight = it.height
                        }
                        .offset {
                            if (barsAreaWidth == 0) return@offset IntOffset.Zero

                            val safeIndex = selectedIndex.coerceAtLeast(0)
                            val gapPx = barGap.roundToPx().toFloat()
                            val startPaddingPx = chartStartPadding.roundToPx().toFloat()
                            val endPaddingPx = chartEndPadding.roundToPx().toFloat()
                            val contentWidthPx =
                                (barsAreaWidth - startPaddingPx - endPaddingPx).coerceAtLeast(0f)
                            val barWidthPx =
                                (contentWidthPx - gapPx * (entries.size - 1)) / entries.size.toFloat()
                            val centerOfBarX =
                                startPaddingPx + safeIndex * (barWidthPx + gapPx) + (barWidthPx / 2f)
                            val absoluteCenterX =
                                yAxisWidthPx + chartGap.roundToPx() + centerOfBarX

                            var targetX = absoluteCenterX - (tooltipWidth / 2f)
                            val maxScroll =
                                yAxisWidthPx + chartGap.roundToPx() + barsAreaWidth - tooltipWidth
                            targetX = targetX.coerceIn(0f, maxScroll.toFloat().coerceAtLeast(0f))

                            IntOffset(targetX.roundToInt(), 0)
                        }
                        .clip(MaterialTheme.shapes.small)
                        .background(ChartTooltipFill)
                        .border(
                            width = dimensionResource(R.dimen.border_hairline),
                            color = ChartTooltipBorder,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(
                            vertical = dimensionResource(R.dimen.activity_chart_tooltip_padding_vertical),
                            horizontal = dimensionResource(R.dimen.activity_chart_tooltip_padding_horizontal)
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tooltipEntry.tooltipLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BodyToggleSelectedLabel,
                        textAlign = TextAlign.Center,
                    )
                    Row {
                        Text(
                            text = fmt.format(tooltipEntry.value.toInt()),
                            style = MaterialTheme.typography.headlineSmall,
                            color = White,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                        Text(
                            text = stringResource(R.string.activity_unit_steps_full),
                            style = MaterialTheme.typography.titleMedium,
                            color = Gray200,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }

        if (entries.any { it.xLabel != null } || xAxisStartLabel != null || xAxisEndLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // drawBehind before padding so the canvas spans the gap, keeping the
                        // vertical grid lines continuous from the chart down to the label.
                        if (showVerticalGridLines && barsAreaWidth > 0 && entries.isNotEmpty()) {
                            val vDash = PathEffect.dashPathEffect(
                                floatArrayOf(vGridDash.toPx(), vGridDash.toPx()), 0f,
                            )
                            val originX = (measuredYAxisWidth + chartGap).toPx()
                            val startPaddingPx = chartStartPadding.toPx()
                            val endPaddingPx = chartEndPadding.toPx()
                            val labelStartPx = xLabelStartPadding.toPx()
                            val gridGapPx = barGap.toPx()
                            val contentWidthPx =
                                (barsAreaWidth - startPaddingPx - endPaddingPx).coerceAtLeast(0f)
                            val gridBarWidthPx =
                                (contentWidthPx - gridGapPx * (entries.size - 1)) / entries.size.toFloat()
                            entries.forEachIndexed { index, entry ->
                                if (entry.xLabel == null) return@forEachIndexed
                                val x = originX + startPaddingPx +
                                        index * (gridBarWidthPx + gridGapPx) + labelStartPx
                                drawLine(
                                    color = vGridColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = vGridWidth.toPx(),
                                    pathEffect = vDash,
                                )
                            }
                        }
                    }
                    .padding(top = dimensionResource(R.dimen.spacer_m)),
            ) {
                Spacer(Modifier.width(measuredYAxisWidth + chartGap))
                Box(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "",
                        style = TitleDimTextStyle,
                        minLines = 1,
                    )
                    if (xAxisStartLabel != null || xAxisEndLabel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = xAxisStartLabel.orEmpty(),
                                style = TitleDimTextStyle,
                                color = xAxisEdgeLabelColor,
                                minLines = 1,
                            )
                            Text(
                                text = xAxisEndLabel.orEmpty(),
                                style = TitleDimTextStyle,
                                color = xAxisEdgeLabelColor,
                                minLines = 1,
                            )
                        }
                    }
                    if (barsAreaWidth > 0 && entries.isNotEmpty()) {
                        val gapPx = with(density) { barGap.toPx() }
                        val startPaddingPx = with(density) { chartStartPadding.toPx() }
                        val endPaddingPx = with(density) { chartEndPadding.toPx() }
                        val labelStartPx = with(density) { xLabelStartPadding.toPx() }
                        val labelLineGapPx = with(density) {
                            if (showVerticalGridLines) dimensionResource(R.dimen.spacer_xxs).toPx()
                            else 0f
                        }
                        val contentWidthPx =
                            (barsAreaWidth - startPaddingPx - endPaddingPx).coerceAtLeast(0f)
                        val barWidthPx =
                            (contentWidthPx - gapPx * (entries.size - 1)) / entries.size.toFloat()

                        entries.forEachIndexed { index, entry ->
                            val label = entry.xLabel ?: return@forEachIndexed
                            val offsetX =
                                startPaddingPx + index * (barWidthPx + gapPx) + labelStartPx +
                                        labelLineGapPx
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (index == highlightedXLabelIndex) Positive
                                else xAxisBarLabelColor,
                                minLines = 1,
                                modifier = Modifier.offset {
                                    IntOffset(offsetX.roundToInt(), 0)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChartBar(
    value: Float,
    yMax: Float,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val barBrush = Brush.verticalGradient(listOf(barColor, barColor))

    val barShape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.spacer_2xs),
        topEnd = dimensionResource(R.dimen.spacer_2xs),
    )

    val fraction = (value / yMax).coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(barShape)
                    .background(barBrush),
            )
        }
    }
}

internal fun computeEffectiveYMax(
    entries: List<BarChartEntry>,
    baseYMax: Float,
    yGridStep: Float,
): Float {
    val actualMax = entries.maxOfOrNull { it.value } ?: 0f
    if (actualMax <= 0f) return baseYMax

    val targetMax = (actualMax * (1f + ActivityConstants.Y_AXIS_HEADROOM_FRACTION))
        .coerceAtLeast(baseYMax)
    val roundingStep = yGridStep.coerceAtLeast(ActivityConstants.Y_AXIS_MIN_GRID_STEP)
    return (ceil(targetMax / roundingStep) * roundingStep).coerceAtLeast(baseYMax)
}

internal fun formatYAxisLabel(value: Float): String {
    return NumberFormat.getNumberInstance(Locale.US)
        .format(value.toInt())
        .replace(',', ' ')
}
