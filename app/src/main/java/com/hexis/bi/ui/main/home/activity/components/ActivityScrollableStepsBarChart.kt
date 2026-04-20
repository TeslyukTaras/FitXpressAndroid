package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.BarChartEntry
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.GridLineGray
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Horizontally-scrollable variant of the bar chart with fixed-width pillars.
 * Y-axis is pinned; bars and x-axis labels share a scroll state and auto-scroll
 * to the end on first composition.
 */
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
    yAxisWidth: Dp = dimensionResource(R.dimen.recovery_y_axis_width),
    yLabelFormatter: (Float) -> String = { it.toInt().toString() },
    isLastHighlighted: Boolean = false,
    scrollAlignIndex: Int = entries.lastIndex,
) {
    val yMax = computeEffectiveYMax(entries, baseYMax, yGridStep)
    val yGridLines = remember(yMax) {
        listOf(0f, yMax / 3f, yMax * 2f / 3f, yMax)
    }
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val selectedEntry = entries.getOrNull(selectedIndex)
    val highlightedIndex = if (isLastHighlighted) entries.indexOfLast { it.value > 0f } else -1
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointerVerticalPadding = dimensionResource(R.dimen.activity_chart_pointer_vertical_padding)
    val pointerColor = MaterialTheme.colorScheme.secondary

    val slotWidth = barWidth + barGap
    val totalBarsWidth =
        if (entries.isEmpty()) 0.dp
        else barWidth * entries.size + barGap * (entries.size - 1)

    LaunchedEffect(entries.size, scrollState.maxValue, scrollAlignIndex) {
        val maxScroll = scrollState.maxValue
        if (maxScroll <= 0 || scrollAlignIndex < 0) return@LaunchedEffect
        val contentWidthPx = with(density) { totalBarsWidth.toPx() }
        val viewportWidthPx = contentWidthPx - maxScroll
        val slotPx = with(density) { slotWidth.toPx() }
        val barWidthPx = with(density) { barWidth.toPx() }
        val targetRightEdge = scrollAlignIndex * slotPx + barWidthPx
        val desired = (targetRightEdge - viewportWidthPx).coerceIn(0f, maxScroll.toFloat())
        scrollState.scrollTo(desired.roundToInt())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val showTooltip = selectedEntry != null
        val tooltipEntry = selectedEntry ?: entries.firstOrNull()

        Box(modifier = Modifier.fillMaxWidth()) {
            if (!showTooltip) Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = dimensionResource(R.dimen.spacer_xxs))
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = Blue300,
                )
            }

            if (tooltipEntry != null) {
                Column(
                    modifier = Modifier
                        .alpha(if (showTooltip) 1f else 0f)
                        .background(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.shapes.small,
                        )
                        .padding(
                            vertical = dimensionResource(R.dimen.spacer_2xs),
                            horizontal = dimensionResource(R.dimen.spacer_s),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = tooltipEntry.tooltipLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Row {
                        Text(
                            text = fmt.format(tooltipEntry.value.toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = Blue300,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                        Text(
                            text = stringResource(R.string.activity_unit_steps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.activity_steps_chart_height)),
        ) {
            // Pinned Y-axis
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(yAxisWidth),
            ) {
                yGridLines.forEach { value ->
                    val fraction = 1f - (value / yMax)
                    Text(
                        text = yLabelFormatter(value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        minLines = 1,
                        modifier = Modifier.align { size, space, _ ->
                            IntOffset(0, (space.height * fraction - size.height / 2).toInt())
                        },
                    )
                }
            }

            Spacer(Modifier.width(chartGap))

            // Scrollable bars area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState),
            ) {
                Box(
                    modifier = Modifier
                        .width(totalBarsWidth)
                        .fillMaxHeight()
                        .pointerInput(entries.size) {
                            if (entries.isEmpty()) return@pointerInput
                            detectTapGestures { tap ->
                                val slotPx = with(density) { slotWidth.toPx() }
                                val idx = (tap.x / slotPx).toInt().coerceIn(0, entries.size - 1)
                                selectedIndex = if (selectedIndex == idx) -1 else idx
                            }
                        }
                        .drawBehind {
                            val dashEffect = PathEffect.dashPathEffect(
                                floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f,
                            )
                            yGridLines.forEach { value ->
                                val y = size.height * (1f - value / yMax)
                                drawLine(
                                    color = GridLineGray,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = stripeWidth.toPx(),
                                    pathEffect = dashEffect,
                                )
                            }
                            if (selectedIndex in entries.indices) {
                                val slotPx = slotWidth.toPx()
                                val barWidthPx = barWidth.toPx()
                                val centerX = selectedIndex * slotPx + barWidthPx / 2f
                                val verticalPaddingPx = pointerVerticalPadding.toPx()
                                drawLine(
                                    color = pointerColor,
                                    start = Offset(centerX, verticalPaddingPx),
                                    end = Offset(centerX, size.height - verticalPaddingPx),
                                    strokeWidth = stripeWidth.toPx(),
                                    pathEffect = dashEffect,
                                )
                            }
                        },
                ) {
                    entries.forEachIndexed { index, entry ->
                        val offsetXPx = with(density) { (slotWidth * index).toPx() }
                        ChartBar(
                            value = entry.value,
                            yMax = yMax,
                            isSelected = index == selectedIndex,
                            isHighlighted = index == highlightedIndex,
                            modifier = Modifier
                                .offset { IntOffset(offsetXPx.roundToInt(), 0) }
                                .width(barWidth),
                        )
                    }
                }
            }
        }

        // X-axis labels centered under each bar, share the same scroll state
        if (entries.any { it.xLabel != null }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacer_xxs)),
            ) {
                Spacer(Modifier.width(yAxisWidth + chartGap))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(barGap),
                ) {
                    entries.forEach { entry ->
                        Box(
                            modifier = Modifier.width(barWidth),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry.xLabel.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                minLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
