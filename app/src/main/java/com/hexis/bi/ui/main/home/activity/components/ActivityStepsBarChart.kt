package com.hexis.bi.ui.main.home.activity.components

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import com.hexis.bi.ui.theme.LightBlue
import com.hexis.bi.ui.theme.LightGradientBlue
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Generalized bar chart used across Activity Day/Week/Month/Year tabs.
 *
 * Renders: header (title + total) OR tooltip overlay, y-axis with grid labels,
 * vertical bars aligned in a single row, and per-bar x-axis labels.
 */
@Composable
fun ActivityStepsBarChart(
    entries: List<BarChartEntry>,
    totalValue: Int,
    baseYMax: Float,
    yGridStep: Float,
    title: String,
    barGap: Dp,
    modifier: Modifier = Modifier,
    yAxisWidth: Dp = dimensionResource(R.dimen.recovery_y_axis_width),
    xLabelStartPadding: Dp = 0.dp,
    isLastHighlighted: Boolean = false,
    yLabelFormatter: ((Float) -> String)? = null,
) {
    val context = LocalContext.current
    val resolvedYLabelFormatter = yLabelFormatter ?: { value -> formatYAxisLabel(value, context) }
    val yMax = computeEffectiveYMax(entries, baseYMax, yGridStep)
    val yGridLines = remember(yMax) {
        listOf(0f, yMax / 3f, yMax * 2f / 3f, yMax)
    }
    val highlightedIndex = if (isLastHighlighted) entries.indexOfLast { it.value > 0f } else -1
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val selectedEntry = entries.getOrNull(selectedIndex)

    var barsAreaWidth by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointerVerticalPadding = dimensionResource(R.dimen.activity_chart_pointer_vertical_padding)
    val pointerColor = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier.fillMaxWidth()) {
        val showTooltip = selectedEntry != null && barsAreaWidth > 0
        val tooltipEntry = selectedEntry ?: entries.firstOrNull()

        // Header / tooltip overlay + chart wrap so the selected-index pointer line
        // can span from under the tooltip through the bars area.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (selectedIndex in entries.indices && barsAreaWidth > 0) {
                        val gapPx = barGap.toPx()
                        val barWidthPx =
                            (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()
                        val centerOfBarX = selectedIndex * (barWidthPx + gapPx) + (barWidthPx / 2f)
                        val absoluteCenterX = (yAxisWidth + chartGap).toPx() + centerOfBarX

                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f
                        )

                        val verticalPaddingPx = pointerVerticalPadding.toPx()

                        drawLine(
                            color = pointerColor,
                            start = Offset(absoluteCenterX, verticalPaddingPx),
                            end = Offset(absoluteCenterX, size.height - verticalPaddingPx),
                            strokeWidth = stripeWidth.toPx(),
                            pathEffect = dashEffect,
                        )
                    }
                },
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (tooltipEntry != null) {
                    Column(
                        modifier = Modifier
                            .alpha(if (showTooltip) 1f else 0f)
                            .onSizeChanged { tooltipWidth = it.width }
                            .offset {
                                if (barsAreaWidth == 0) return@offset IntOffset.Zero

                                val safeIndex = selectedIndex.coerceAtLeast(0)
                                val gapPx = barGap.roundToPx().toFloat()
                                val barWidthPx =
                                    (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()
                                val centerOfBarX =
                                    safeIndex * (barWidthPx + gapPx) + (barWidthPx / 2f)
                                val absoluteCenterX =
                                    (yAxisWidth + chartGap).roundToPx() + centerOfBarX

                                var targetX = absoluteCenterX - (tooltipWidth / 2f)
                                val maxScroll =
                                    (yAxisWidth + chartGap).roundToPx() + barsAreaWidth - tooltipWidth
                                targetX = targetX.coerceIn(0f, maxScroll.toFloat())

                                IntOffset(targetX.roundToInt(), 0)
                            }
                            .background(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.shapes.small
                            )
                            .padding(
                                vertical = dimensionResource(R.dimen.spacer_2xs),
                                horizontal = dimensionResource(R.dimen.spacer_s)
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
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

            // Main chart area: y-axis + bars area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.activity_steps_chart_height)),
            ) {
                // Y-axis
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(yAxisWidth)
                ) {
                    yGridLines.forEach { value ->
                        val fraction = 1f - (value / yMax)
                        Text(
                            text = resolvedYLabelFormatter(value),
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

                // Bars + grid lines
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
                                val barWidthPx =
                                    (size.width - gapPx * (entries.size - 1)) / entries.size.toFloat()
                                val slotWidthPx = barWidthPx + gapPx

                                fun indexForX(x: Float): Int =
                                    (x / slotWidthPx).toInt().coerceIn(0, entries.size - 1)

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
                        },
                ) {
                    if (entries.isNotEmpty() && barsAreaWidth > 0) {
                        val gapPx = with(density) { barGap.toPx() }
                        val barWidthPx =
                            (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()
                        val barWidthDp = with(density) { barWidthPx.toDp() }

                        entries.forEachIndexed { index, entry ->
                            val offsetX = index * (barWidthPx + gapPx)
                            ChartBar(
                                value = entry.value,
                                yMax = yMax,
                                isSelected = index == selectedIndex,
                                isHighlighted = index == highlightedIndex,
                                modifier = Modifier
                                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                                    .width(barWidthDp)
                            )
                        }
                    }
                }
            }
        }

        // X-axis labels (per-bar, anchored at the bar's left edge, natural width)
        if (entries.any { it.xLabel != null }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacer_xxs)),
            ) {
                Spacer(Modifier.width(yAxisWidth + chartGap))
                Box(modifier = Modifier.weight(1f)) {
                    // Reserve a line of height up-front so the row doesn't grow
                    // once per-bar labels are positioned after barsAreaWidth is measured.
                    Text(
                        text = "",
                        style = MaterialTheme.typography.bodySmall,
                        minLines = 1,
                    )
                    if (barsAreaWidth > 0 && entries.isNotEmpty()) {
                        val gapPx = with(density) { barGap.toPx() }
                        val labelStartPx = with(density) { xLabelStartPadding.toPx() }
                        val barWidthPx =
                            (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()

                        entries.forEachIndexed { index, entry ->
                            val label = entry.xLabel ?: return@forEachIndexed
                            val offsetX = index * (barWidthPx + gapPx) + labelStartPx
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
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
    isSelected: Boolean,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val minBarHeight = dimensionResource(R.dimen.chart_bar_min_height)
    val barBrush = if (isSelected || isHighlighted)
        Brush.verticalGradient(listOf(LightGradientBlue, Blue300))
    else Brush.verticalGradient(listOf(LightBlue, LightBlue))

    val barShape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.spacer_2xs),
        topEnd = dimensionResource(R.dimen.spacer_2xs),
    )

    val fraction = (value / yMax).coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val fillModifier =
            if (fraction > 0f) Modifier.fillMaxHeight(fraction)
            else Modifier.height(minBarHeight)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(fillModifier)
                .clip(barShape)
                .background(barBrush),
        )
    }
}

internal fun computeEffectiveYMax(
    entries: List<BarChartEntry>,
    baseYMax: Float,
    yGridStep: Float,
): Float {
    val actualMax = entries.maxOfOrNull { it.value } ?: 0f
    val minStep = yGridStep.coerceAtLeast(1f)
    val stepTarget = (maxOf(baseYMax, actualMax) / 3f).coerceAtLeast(minStep)
    val niceStep = ceilNiceStep(stepTarget, minStep)
    return niceStep * 3f
}

internal fun formatYAxisLabel(value: Float, context: Context): String {
    val whole = value.toInt()
    return if (whole >= 10_000) {
        context.getString(R.string.activity_axis_thousands_short, whole / 1_000)
    } else {
        whole.toString()
    }
}

private fun ceilNiceStep(target: Float, minStep: Float): Float {
    val base = 10f.pow(kotlin.math.floor(kotlin.math.log10(target.coerceAtLeast(1f))))
    val multipliers = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 8f, 10f)
    for (m in multipliers) {
        val candidate = base * m
        if (candidate >= target && candidate >= minStep) return candidate
    }
    return base * 10f
}
