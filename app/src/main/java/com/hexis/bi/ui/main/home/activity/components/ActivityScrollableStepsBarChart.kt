package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.BarChartEntry
import com.hexis.bi.ui.theme.ActivityMediumTitleStyle
import com.hexis.bi.ui.theme.NocturnePulseTheme
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ActivityScrollableStepsBarChart(
    entries: List<BarChartEntry>,
    totalValue: Int,
    baseYMax: Float,
    yGridStep: Float,
    title: String,
    barWidth: Dp,
    barGap: Dp,
    modifier: Modifier = Modifier,
    chartStartPadding: Dp = 0.dp,
    chartEndPadding: Dp = 0.dp,
    yLabelFormatter: ((Float) -> String)? = null,
    scrollAlignIndex: Int = entries.lastIndex,
) {
    val resolvedYLabelFormatter = yLabelFormatter ?: { value -> formatYAxisLabel(value) }
    val yMax = computeEffectiveYMax(entries, baseYMax, yGridStep)
    val yGridLines = remember(yMax) {
        listOf(0f, yMax / 3f, yMax * 2f / 3f, yMax)
    }
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val selectedEntry = entries.getOrNull(selectedIndex)
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var yAxisWidthPx by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }
    var tooltipHeight by remember { mutableIntStateOf(0) }
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val measuredYAxisWidth = with(density) { yAxisWidthPx.toDp() }

    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointerVerticalPadding = dimensionResource(R.dimen.activity_chart_pointer_vertical_padding)
    val pointerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline
    val axisColor = NocturnePulseTheme.extendedColors.chartAxisLine
    val axisStrokeWidth = dimensionResource(R.dimen.border_hairline)

    val xLabelStyle = MaterialTheme.typography.bodySmall
    val labelMeasurer = rememberTextMeasurer()
    val effectiveBarWidth = remember(entries, xLabelStyle, barWidth, density) {
        val widestLabelPx = entries.mapNotNull { it.xLabel }
            .maxOfOrNull { labelMeasurer.measure(it, xLabelStyle).size.width } ?: 0
        maxOf(barWidth, with(density) { widestLabelPx.toDp() })
    }

    val slotWidth = effectiveBarWidth + barGap
    val totalBarsWidth =
        if (entries.isEmpty()) 0.dp
        else chartStartPadding + effectiveBarWidth * entries.size + barGap * (entries.size - 1) +
                chartEndPadding

    LaunchedEffect(entries.size, scrollState.maxValue, scrollAlignIndex, chartStartPadding) {
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0 || scrollAlignIndex < 0) return@LaunchedEffect
        val contentWidthPx = with(density) { totalBarsWidth.toPx() }
        val viewportWidthPx = contentWidthPx - maxScroll
        val startPaddingPx = with(density) { chartStartPadding.toPx() }
        val slotPx = with(density) { slotWidth.toPx() }
        val barWidthPx = with(density) { effectiveBarWidth.toPx() }
        val targetRightEdge = startPaddingPx + scrollAlignIndex * slotPx + barWidthPx
        val desired = (targetRightEdge - viewportWidthPx).coerceIn(0f, maxScroll.toFloat())
        scrollState.scrollTo(desired.roundToInt())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val showTooltip = selectedEntry != null
        val tooltipEntry = selectedEntry ?: entries.firstOrNull()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (selectedIndex in entries.indices && viewportWidthPx > 0) {
                        val axisOffsetPx = yAxisWidthPx + chartGap.toPx()
                        val startPaddingPx = chartStartPadding.toPx()
                        val slotPx = slotWidth.toPx()
                        val barWidthPx = effectiveBarWidth.toPx()
                        val centerX = axisOffsetPx +
                                (startPaddingPx + selectedIndex * slotPx + barWidthPx / 2f -
                                        scrollState.value)
                        val startY = tooltipHeight.toFloat()
                        val endY = size.height - pointerVerticalPadding.toPx()
                        if (centerX in axisOffsetPx..(axisOffsetPx + viewportWidthPx) && startY < endY) {
                            val dashEffect = PathEffect.dashPathEffect(
                                floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f,
                            )
                            drawLine(
                                color = pointerColor,
                                start = Offset(centerX, startY),
                                end = Offset(centerX, endY),
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
                        Text(
                            text = fmt.format(totalValue),
                            style = ActivityMediumTitleStyle,
                            color = NocturnePulseTheme.extendedColors.accentBlue,
                        )
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
                            .onSizeChanged { yAxisWidthPx = it.width },
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
                            .onSizeChanged { viewportWidthPx = it.width }
                            .drawBehind {
                                drawLine(
                                    color = axisColor,
                                    start = Offset(0f, -chartGap.toPx()),
                                    end = Offset(0f, size.height + chartGap.toPx()),
                                    strokeWidth = axisStrokeWidth.toPx(),
                                )
                            }
                            .horizontalScroll(scrollState),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalBarsWidth)
                                .fillMaxHeight()
                                .pointerInput(entries.size) {
                                    if (entries.isEmpty()) return@pointerInput
                                    detectTapGestures { tap ->
                                        val startPaddingPx =
                                            with(density) { chartStartPadding.toPx() }
                                        val endPaddingPx = with(density) { chartEndPadding.toPx() }
                                        val contentWidthPx =
                                            (size.width - startPaddingPx - endPaddingPx)
                                                .coerceAtLeast(0f)
                                        val slotPx = with(density) { slotWidth.toPx() }
                                        val idx =
                                            ((tap.x - startPaddingPx).coerceIn(0f, contentWidthPx) /
                                                    slotPx)
                                                .toInt()
                                                .coerceIn(0, entries.size - 1)
                                        selectedIndex = if (selectedIndex == idx) -1 else idx
                                    }
                                }
                                .drawBehind {
                                    val dashEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f,
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
                                },
                        ) {
                            entries.forEachIndexed { index, entry ->
                                val offsetXPx = with(density) {
                                    (chartStartPadding + slotWidth * index).toPx()
                                }
                                ChartBar(
                                    value = entry.value,
                                    yMax = yMax,
                                    modifier = Modifier
                                        .offset { IntOffset(offsetXPx.roundToInt(), 0) }
                                        .width(effectiveBarWidth),
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
                            if (viewportWidthPx == 0 || entries.isEmpty()) return@offset IntOffset.Zero
                            val safeIndex = selectedIndex.coerceAtLeast(0)
                            val startPaddingPx = chartStartPadding.toPx()
                            val slotPx = slotWidth.toPx()
                            val barWidthPx = effectiveBarWidth.toPx()
                            val barCenter = startPaddingPx + safeIndex * slotPx + barWidthPx / 2f
                            val axisOffsetPx = yAxisWidthPx + chartGap.roundToPx()
                            val targetX = axisOffsetPx + (barCenter - scrollState.value) -
                                    tooltipWidth / 2f
                            val minX = axisOffsetPx.toFloat()
                            val maxX = (axisOffsetPx + viewportWidthPx - tooltipWidth)
                                .toFloat().coerceAtLeast(minX)
                            IntOffset(targetX.coerceIn(minX, maxX).roundToInt(), 0)
                        }
                        .clip(MaterialTheme.shapes.small)
                        .background(NocturnePulseTheme.extendedColors.chartTooltipFill)
                        .border(
                            width = dimensionResource(R.dimen.border_hairline),
                            color = NocturnePulseTheme.extendedColors.chartTooltipBorder,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(
                            vertical = dimensionResource(R.dimen.activity_chart_tooltip_padding_vertical),
                            horizontal = dimensionResource(R.dimen.activity_chart_tooltip_padding_horizontal),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tooltipEntry.tooltipLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NocturnePulseTheme.extendedColors.bodyToggleSelectedLabel,
                        textAlign = TextAlign.Center,
                    )
                    Row {
                        Text(
                            text = fmt.format(tooltipEntry.value.toInt()),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                        Text(
                            text = stringResource(R.string.activity_unit_steps_full),
                            style = MaterialTheme.typography.titleMedium,
                            color = NocturnePulseTheme.extendedColors.gray200,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }

        if (entries.any { it.xLabel != null }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacer_m)),
            ) {
                Spacer(Modifier.width(measuredYAxisWidth + chartGap))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = chartStartPadding, end = chartEndPadding)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(barGap),
                ) {
                    entries.forEach { entry ->
                        Box(
                            modifier = Modifier.width(effectiveBarWidth),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry.xLabel.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                minLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
