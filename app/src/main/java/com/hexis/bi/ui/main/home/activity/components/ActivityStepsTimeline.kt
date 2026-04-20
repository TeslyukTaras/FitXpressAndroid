package com.hexis.bi.ui.main.home.activity.components

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.HourlyStepEntry
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.GridLineGray
import com.hexis.bi.ui.theme.LightBlue
import com.hexis.bi.ui.theme.LightGradientBlue
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.formatHour
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ActivityStepsTimeline(
    entries: List<HourlyStepEntry>,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    var selectedHour by remember { mutableIntStateOf(-1) }
    val selectedEntry = entries.getOrNull(selectedHour)

    var barsAreaWidth by remember { mutableIntStateOf(0) }
    var tooltipWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val yAxisWidth = dimensionResource(R.dimen.recovery_y_axis_width)
    val chartGap = dimensionResource(R.dimen.spacer_xs)
    val barGap = dimensionResource(R.dimen.spacer_2xs)
    val dashWidth = dimensionResource(R.dimen.dash_width)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val pointerColor = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier.fillMaxWidth()) {
        val showTooltip = selectedEntry != null && barsAreaWidth > 0
        val tooltipEntry = selectedEntry ?: entries.firstOrNull()

        // --- Header / Tooltip Overlay ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (selectedHour in entries.indices && barsAreaWidth > 0) {
                        val gapPx = barGap.toPx()
                        val barWidthPx =
                            (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()
                        val centerOfBarX = selectedHour * (barWidthPx + gapPx) + (barWidthPx / 2f)
                        val absoluteCenterX = (yAxisWidth + chartGap).toPx() + centerOfBarX

                        val dashEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f
                        )

                        val verticalPaddingPx = 5.dp.toPx()

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

                                val safeHour = selectedHour.coerceAtLeast(0)
                                val gapPx = barGap.roundToPx().toFloat()
                                val barWidthPx =
                                    (barsAreaWidth - gapPx * (entries.size - 1)) / entries.size.toFloat()
                                val centerOfBarX =
                                    safeHour * (barWidthPx + gapPx) + (barWidthPx / 2f)
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
                            text = tooltipEntry.hour.formatHour(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Row {
                            Text(
                                text = fmt.format(tooltipEntry.steps),
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
                        text = stringResource(R.string.activity_steps_timeline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = fmt.format(totalSteps),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Blue300,
                    )
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

            // --- Main Chart Area ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.activity_steps_chart_height)),
            ) {

                // Y-Axis
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(yAxisWidth)
                ) {
                    ActivityConstants.STEP_GRID_LINES.forEach { value ->
                        val fraction = 1f - (value / ActivityConstants.STEP_GRID_MAX)
                        Text(
                            text = value.toInt().toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align { size, space, _ ->
                                IntOffset(0, (space.height * fraction - size.height / 2).toInt())
                            },
                        )
                    }
                }

                Spacer(Modifier.width(chartGap))

                // Bars & Grid Lines
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

                                fun getHourForX(x: Float): Int =
                                    (x / slotWidthPx).toInt().coerceIn(0, entries.size - 1)

                                selectedHour = getHourForX(down.position.x)
                                down.consume()

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.changedToUp()) {
                                        selectedHour = -1
                                        break
                                    }
                                    selectedHour = getHourForX(change.position.x)
                                    change.consume()
                                }
                            }
                        }
                        .drawBehind {
                            val dashEffect = PathEffect.dashPathEffect(
                                floatArrayOf(dashWidth.toPx(), dashWidth.toPx()), 0f
                            )

                            ActivityConstants.STEP_GRID_LINES.forEach { value ->
                                val y = size.height * (1f - value / ActivityConstants.STEP_GRID_MAX)
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
                            HourBar(
                                entry = entry,
                                isSelected = index == selectedHour,
                                modifier = Modifier
                                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                                    .width(barWidthDp)
                            )
                        }
                    }
                }
            }
        }

        // --- X-Axis ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacer_xxs)),
        ) {
            Spacer(Modifier.width(yAxisWidth + chartGap))
            Text(
                text = stringResource(R.string.activity_time_start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.activity_time_end),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun HourBar(
    entry: HourlyStepEntry,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val barBrush = if (isSelected)
        Brush.verticalGradient(listOf(LightGradientBlue, Blue300))
    else Brush.verticalGradient(listOf(LightBlue, LightBlue))

    val barShape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.spacer_2xs),
        topEnd = dimensionResource(R.dimen.spacer_2xs),
    )

    val fraction = (entry.steps / ActivityConstants.STEP_GRID_MAX).coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (entry.steps > 0) Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .clip(barShape)
                .background(barBrush),
        )
    }
}