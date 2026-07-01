package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.recovery.DailyRecoveryEntry
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.utils.constants.RecoveryConstants
import kotlin.math.roundToInt

private fun summaryChartFractionFromTop(score: Float): Float {
    val value = score.coerceIn(0f, RecoveryConstants.MAX_SCORE)
    return when {
        value >= RecoveryConstants.SUMMARY_SCALE_MID_MAX ->
            ((RecoveryConstants.MAX_SCORE - value) /
                    (RecoveryConstants.MAX_SCORE - RecoveryConstants.SUMMARY_SCALE_MID_MAX)) *
                    RecoveryConstants.SUMMARY_SCALE_BAND_FRACTION

        value >= RecoveryConstants.SUMMARY_SCALE_LOW_MAX ->
            RecoveryConstants.SUMMARY_SCALE_BAND_FRACTION +
                    ((RecoveryConstants.SUMMARY_SCALE_MID_MAX - value) /
                            (RecoveryConstants.SUMMARY_SCALE_MID_MAX - RecoveryConstants.SUMMARY_SCALE_LOW_MAX)) *
                    RecoveryConstants.SUMMARY_SCALE_BAND_FRACTION

        else ->
            RecoveryConstants.SUMMARY_SCALE_BAND_FRACTION * 2f +
                    ((RecoveryConstants.SUMMARY_SCALE_LOW_MAX - value) /
                            RecoveryConstants.SUMMARY_SCALE_LOW_MAX) *
                    RecoveryConstants.SUMMARY_SCALE_BAND_FRACTION
    }
}

@Composable
fun RecoveryBarChart(
    entries: List<DailyRecoveryEntry>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val chartGap = dimensionResource(R.dimen.spacer_xxs)
    val barGap = dimensionResource(R.dimen.spacer_s)
    val barHorizontalInset = dimensionResource(R.dimen.spacer_l)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointerVerticalPadding = dimensionResource(R.dimen.activity_chart_pointer_vertical_padding)
    val gridColor = MaterialTheme.colorScheme.outline
    val pointerColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    var selectedIndex by remember { mutableIntStateOf(-1) }
    var yAxisWidthPx by remember { mutableIntStateOf(0) }
    var barsAreaWidth by remember { mutableIntStateOf(0) }
    var chartTopPx by remember { mutableIntStateOf(0) }
    var chartHeightPx by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }
    var tooltipHeight by remember { mutableIntStateOf(0) }
    val selectedEntry = entries.getOrNull(selectedIndex)
    val showTooltip = selectedEntry != null && barsAreaWidth > 0
    val tooltipEntry = selectedEntry ?: entries.firstOrNull()
    val yAxisWidth = with(density) { yAxisWidthPx.toDp() }

    fun barCenterX(index: Int, areaWidthPx: Float, gapPx: Float): Float {
        val barWidthPx = (areaWidthPx - gapPx * (entries.size - 1)) / entries.size.toFloat()
        return index * (barWidthPx + gapPx) + barWidthPx / 2f
    }

    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_l),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_xl),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (showTooltip && selectedIndex in entries.indices && chartHeightPx > 0) {
                        val centerX = yAxisWidthPx.toFloat() +
                                chartGap.toPx() +
                                barHorizontalInset.toPx() +
                                barCenterX(selectedIndex, barsAreaWidth.toFloat(), barGap.toPx())
                        val startY = tooltipHeight.toFloat()
                        val endY = chartTopPx + chartHeightPx - pointerVerticalPadding.toPx()
                        if (startY < endY) {
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
                            .fillMaxWidth()
                            .alpha(if (showTooltip) 0f else 1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.recovery_screen_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = stringResource(R.string.recovery_last_7_days),
                            style = TitleDimTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.recovery_bar_chart_height))
                        .onGloballyPositioned {
                            chartTopPx = it.positionInParent().y.roundToInt()
                        }
                        .onSizeChanged {
                            chartHeightPx = it.height
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .onSizeChanged { yAxisWidthPx = it.width },
                    ) {
                        RecoveryConstants.SUMMARY_GRID_LINES.forEach { value ->
                            val fractionFromTop = summaryChartFractionFromTop(value)
                            Text(
                                text = value.toInt().toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align { size, space, _ ->
                                    IntOffset(
                                        0,
                                        (space.height * fractionFromTop - size.height / 2).toInt()
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
                            .pointerInput(entries.size) {
                                if (entries.isEmpty()) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val gapPx = barGap.toPx()
                                    val insetPx = barHorizontalInset.toPx()
                                    val barWidthPx =
                                        (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()
                                    val slotWidthPx = barWidthPx + gapPx

                                    fun indexForX(x: Float): Int {
                                        val localX =
                                            (x - insetPx).coerceIn(0f, barsAreaWidth.toFloat())
                                        return (localX / slotWidthPx).toInt()
                                            .coerceIn(0, entries.size - 1)
                                    }

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
                                    floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f,
                                )
                                drawLine(
                                    color = axisColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = stripeWidth.toPx(),
                                )
                                drawLine(
                                    color = axisColor,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = stripeWidth.toPx(),
                                )
                                RecoveryConstants.SUMMARY_GRID_LINES.filter { it > 0f }
                                    .forEach { value ->
                                        val y = size.height * summaryChartFractionFromTop(value)
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
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = barHorizontalInset)
                                .onSizeChanged { barsAreaWidth = it.width },
                            horizontalArrangement = Arrangement.spacedBy(barGap),
                        ) {
                            entries.forEach { entry ->
                                DayBar(
                                    entry = entry,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }

            if (tooltipEntry != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (showTooltip) 1f else 0f),
                ) {
                    Column(
                        modifier = Modifier
                            .onSizeChanged {
                                tooltipWidth = it.width
                                tooltipHeight = it.height
                            }
                            .offset {
                                if (barsAreaWidth == 0) return@offset IntOffset.Zero
                                val safeIndex = selectedIndex.coerceAtLeast(0)
                                val absoluteCenterX = (yAxisWidth + chartGap).roundToPx() +
                                        barHorizontalInset.roundToPx() +
                                        barCenterX(
                                            safeIndex,
                                            barsAreaWidth.toFloat(),
                                            barGap.toPx()
                                        )
                                val targetX = (absoluteCenterX - tooltipWidth / 2f).coerceIn(
                                    0f,
                                    ((yAxisWidth + chartGap).roundToPx() + barsAreaWidth - tooltipWidth)
                                        .toFloat(),
                                )
                                IntOffset(targetX.roundToInt(), 0)
                            }
                            .clip(MaterialTheme.shapes.small)
                            .background(NocturnePulseTheme.extendedColors.chartTooltipFill)
                            .border(
                                width = dimensionResource(R.dimen.border_line),
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(
                                vertical = dimensionResource(R.dimen.spacer_2xs),
                                horizontal = dimensionResource(R.dimen.spacer_s),
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = tooltipEntry.tooltipLabel.ifEmpty { tooltipEntry.dayLabel },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Row {
                            Text(
                                text = tooltipEntry.score.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                modifier = Modifier.alignByBaseline(),
                            )
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                            Text(
                                text = stringResource(R.string.recovery_score_unit),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.alignByBaseline(),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        // X-axis day labels, aligned under each bar (today highlighted).
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(yAxisWidth))
            Spacer(Modifier.width(chartGap))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = barHorizontalInset),
                horizontalArrangement = Arrangement.spacedBy(barGap),
            ) {
                entries.forEach { entry ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = entry.dayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entry.isHighlighted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayBar(
    entry: DailyRecoveryEntry,
    modifier: Modifier = Modifier,
) {
    val minBarHeight = dimensionResource(R.dimen.chart_bar_min_height)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        val fraction = 1f - summaryChartFractionFromTop(entry.score.toFloat())
        val fillModifier = if (fraction > 0f) Modifier.fillMaxHeight(fraction)
        else Modifier.height(minBarHeight)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(fillModifier)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
