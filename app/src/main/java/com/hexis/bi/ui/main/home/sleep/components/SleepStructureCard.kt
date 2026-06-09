package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.sleep.DailyStructure
import com.hexis.bi.ui.main.home.sleep.StageTrend
import com.hexis.bi.ui.main.home.sleep.WeeklyStageData
import com.hexis.bi.ui.main.home.sleep.nameRes
import com.hexis.bi.ui.theme.Gray200
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.ui.theme.SleepStageAwake
import com.hexis.bi.ui.theme.SleepStageDeep
import com.hexis.bi.ui.theme.SleepStageLight
import com.hexis.bi.ui.theme.SleepStageRem
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.TitleHighlightTextStyle
import com.hexis.bi.ui.theme.dark.ActionTeal
import com.hexis.bi.ui.theme.dark.ChartAxisLine
import com.hexis.bi.ui.theme.dark.ChartGridLineHorizontal
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.ui.theme.dark.TextSecondary
import com.hexis.bi.utils.constants.SleepConstants
import kotlin.math.abs
import kotlin.math.ceil

private const val STRUCTURE_AXIS_TICKS = 3 // labels = 0, max/3, 2·max/3, max
private const val STRUCTURE_FILL_FADE_STOP = 0.2f
private const val STRUCTURE_FILL_TOP_ALPHA = 0.65f
private const val STRUCTURE_FILL_BOTTOM_ALPHA = 0.45f
private val STRUCTURE_STACK_ORDER =
    listOf(SleepStage.Deep, SleepStage.Light, SleepStage.REM, SleepStage.Awake)

@Composable
fun SleepStructureCard(
    structure: List<DailyStructure>,
    stages: List<WeeklyStageData>,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(structure) { mutableIntStateOf(-1) }
    val selectedDay = structure.getOrNull(selectedIndex)
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            vertical = dimensionResource(R.dimen.padding_medium),
            horizontal = dimensionResource(R.dimen.spacer_m)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sleep_structure_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.sleep_summary_last_7_days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

        StructureChart(
            structure = structure,
            selectedIndex = selectedIndex,
            onSelect = { index -> selectedIndex = index },
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        StructureLegend()

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        // A selected bar shows that day's stages (no trends); otherwise the weekly averages.
        val statStages = if (selectedDay != null) {
            SleepStage.entries.map { stage ->
                WeeklyStageData(stage, selectedDay.stageMinutes[stage] ?: 0, trend = null)
            }
        } else {
            stages
        }
        StructureStatsGrid(stages = statStages)

        if (selectedDay == null) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = stringResource(R.string.sleep_summary_trend_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StructureChart(
    structure: List<DailyStructure>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val chartHeight = dimensionResource(R.dimen.sleep_bar_chart_height)
    val gap = dimensionResource(R.dimen.sleep_structure_bar_gap)
    val barInset = dimensionResource(R.dimen.sleep_structure_bar_inset)
    val gridWidth = dimensionResource(R.dimen.sleep_structure_grid_width)
    val gridDash = dimensionResource(R.dimen.sleep_structure_grid_dash)
    val axisMaxHours = adaptiveAxisMaxHours(structure)
    val axisLabels =
        List(STRUCTURE_AXIS_TICKS + 1) { axisMaxHours - axisMaxHours / STRUCTURE_AXIS_TICKS * it }
    val maxMinutes = axisMaxHours * SleepConstants.MINUTES_PER_HOUR
    val xAxisColor = ChartGridLineHorizontal
    val yAxisColor = ChartAxisLine
    // Top label is centred on the chart top, so extend the y-axis up to reach its top edge.
    val axisOvershootPx =
        rememberTextMeasurer().measure("0", MaterialTheme.typography.bodySmall).size.height / 2f
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Row(modifier = Modifier.fillMaxWidth()) {
        StructureAxisLabels(
            labels = axisLabels,
            maxHours = axisMaxHours,
            modifier = Modifier
                .height(chartHeight)
                .padding(end = dimensionResource(R.dimen.spacer_xs)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .pointerInput(structure.size) {
                        if (structure.isEmpty()) return@pointerInput
                        val insetPx = barInset.toPx()
                        val gapPx = gap.toPx()
                        fun indexForX(x: Float): Int {
                            val area = size.width - insetPx * 2
                            val barWidth = (area - gapPx * (structure.size - 1)) / structure.size
                            val localX = (x - insetPx).coerceIn(0f, area)
                            return (localX / (barWidth + gapPx)).toInt()
                                .coerceIn(0, structure.size - 1)
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startIndex = indexForX(down.position.x)
                            val wasSelected = currentSelected == startIndex
                            currentOnSelect(startIndex)
                            down.consume()
                            var dragged = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (change.changedToUp()) {
                                    // Tap toggles and persists; a drag is discarded on lift.
                                    currentOnSelect(
                                        when {
                                            dragged -> -1
                                            wasSelected -> -1
                                            else -> startIndex
                                        }
                                    )
                                    change.consume()
                                    break
                                }
                                if (!dragged &&
                                    abs(change.position.x - down.position.x) > viewConfiguration.touchSlop
                                ) {
                                    dragged = true
                                }
                                if (dragged) currentOnSelect(indexForX(change.position.x))
                                change.consume()
                            }
                        }
                    },
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = gridWidth.toPx()
                    val dash = PathEffect.dashPathEffect(
                        floatArrayOf(gridDash.toPx(), gridDash.toPx()), 0f,
                    )
                    axisLabels.forEach { value ->
                        val rawY = size.height * (1f - value / axisMaxHours.toFloat())
                        val y = rawY.coerceIn(stroke / 2f, size.height - stroke / 2f)
                        drawLine(
                            color = xAxisColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = stroke,
                            pathEffect = if (value == 0) null else dash,
                        )
                    }
                    drawLine(
                        color = yAxisColor,
                        start = Offset(stroke / 2f, -axisOvershootPx),
                        end = Offset(stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = barInset),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    structure.forEachIndexed { index, day ->
                        DayStructureBar(
                            day = day,
                            maxMinutes = maxMinutes,
                            selected = index == selectedIndex,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = barInset),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                structure.forEach { day ->
                    Text(
                        text = day.dayLabel,
                        modifier = Modifier.weight(1f),
                        style = TitleDimTextStyle,
                        color = if (day.isHighlighted) Positive else Gray200,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayStructureBar(
    day: DailyStructure,
    maxMinutes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val corner = dimensionResource(R.dimen.sleep_structure_bar_corner)
    val borderWidth = dimensionResource(R.dimen.sleep_structure_selection_border)
    val total = day.totalMinutes.coerceAtMost(maxMinutes)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(corner, corner)),
    ) {
        val emptyWeight = (maxMinutes - total).toFloat()
        if (emptyWeight > 0f) Spacer(Modifier.weight(emptyWeight))
        if (total > 0) Column(
            modifier = Modifier
                .weight(total.toFloat())
                .clip(RoundedCornerShape(corner, corner))
                .fillMaxWidth()
                .then(
                    if (selected) Modifier.drawWithContent {
                        drawContent()
                        drawOpenBottomTopBorder(
                            color = ActionTeal,
                            strokeWidth = borderWidth.toPx(),
                            cornerRadius = corner.toPx(),
                        )
                    } else Modifier
                ),
        ) {
            STRUCTURE_STACK_ORDER.asReversed().forEach { stage ->
                val minutes = day.stageMinutes[stage] ?: 0
                if (minutes > 0) {
                    Box(
                        modifier = Modifier
                            .weight(minutes.toFloat())
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(corner, corner))
                            .background(
                                Brush.verticalGradient(
                                    0f to stageColor(stage).copy(alpha = STRUCTURE_FILL_TOP_ALPHA),
                                    STRUCTURE_FILL_FADE_STOP to stageColor(stage).copy(alpha = STRUCTURE_FILL_BOTTOM_ALPHA),
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
                }
            }
        }

    }
}

@Composable
private fun StructureAxisLabels(
    labels: List<Int>,
    maxHours: Int,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.bodySmall
    Layout(
        modifier = modifier,
        content = {
            labels.forEach { value ->
                Text(text = value.toString(), style = style, color = TextSecondary)
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = constraints.maxHeight
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val fraction = 1f - labels[index] / maxHours.toFloat()
                val centerY = (height * fraction).toInt()
                placeable.place(0, centerY - placeable.height / 2)
            }
        }
    }
}

/** Smallest multiple of [STRUCTURE_AXIS_TICKS] hours that covers the longest night in the week. */
private fun adaptiveAxisMaxHours(structure: List<DailyStructure>): Int {
    val maxMinutes = structure.maxOfOrNull { it.totalMinutes } ?: 0
    val maxHours = ceil(maxMinutes / SleepConstants.MINUTES_PER_HOUR.toFloat()).toInt()
    val rounded = ceil(maxHours / STRUCTURE_AXIS_TICKS.toFloat()).toInt() * STRUCTURE_AXIS_TICKS
    return rounded.coerceAtLeast(STRUCTURE_AXIS_TICKS)
}

@Composable
private fun StructureLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
    ) {
        STRUCTURE_STACK_ORDER.forEach { stage ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.sleep_legend_dot))
                        .clip(CircleShape)
                        .background(stageColor(stage)),
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = stringResource(stage.nameRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StructureStatsGrid(stages: List<WeeklyStageData>) {
    stages.chunked(2).forEachIndexed { rowIndex, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            row.forEachIndexed { columnIndex, data ->
                StatCell(data = data, modifier = Modifier.weight(1f))
                if (columnIndex < row.size - 1) {
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                    AppVerticalGradientDivider()
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                }
            }
            if (row.size < 2) Spacer(Modifier.weight(1f))
        }
        if (rowIndex < stages.size / 2 - 1) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            AppHorizontalGradientDivider()
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        }
    }
}

@Composable
private fun StatCell(
    data: WeeklyStageData,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(data.stage.nameRes()),
                style = MaterialTheme.typography.bodySmall,
                color = Gray200,
            )
            if (data.trend != null) {
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
                Text(
                    text = if (data.trend == StageTrend.Up) "↑" else "↓",
                    style = TitleHighlightTextStyle,
                    color = if (data.trend == StageTrend.Up) Green else Red100,
                )
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        Text(text = stageDurationText(data.durationMinutes))
    }
}

@Composable
private fun stageDurationText(minutes: Int): AnnotatedString {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / SleepConstants.MINUTES_PER_HOUR
    val mins = safe % SleepConstants.MINUTES_PER_HOUR
    val numberStyle =
        MaterialTheme.typography.headlineMedium.toSpanStyle()
            .copy(color = MaterialTheme.colorScheme.onSurface)
    val unitStyle = MaterialTheme.typography.bodyLarge.toSpanStyle()
        .copy(color = Gray200)
    val hourUnit = stringResource(R.string.unit_hours_short)
    val minuteUnit = stringResource(R.string.unit_minutes_short)
    return buildAnnotatedString {
        if (hours > 0) {
            withStyle(numberStyle) { append(hours.toString()) }
            withStyle(unitStyle) { append(" $hourUnit ") }
            withStyle(numberStyle) { append(mins.toString().padStart(2, '0')) }
        } else {
            withStyle(numberStyle) { append(mins.toString()) }
        }
        withStyle(unitStyle) { append(" $minuteUnit") }
    }
}

private fun stageColor(stage: SleepStage): Color = when (stage) {
    SleepStage.Deep -> SleepStageDeep
    SleepStage.Light -> SleepStageLight
    SleepStage.REM -> SleepStageRem
    SleepStage.Awake -> SleepStageAwake
}

/** Outlines left + rounded top + right edges, leaving the bottom open. */
private fun DrawScope.drawOpenBottomTopBorder(
    color: Color,
    strokeWidth: Float,
    cornerRadius: Float,
) {
    val inset = strokeWidth / 2f
    val w = size.width
    val h = size.height
    val r = cornerRadius
    val path = Path().apply {
        moveTo(inset, h)
        lineTo(inset, inset + r)
        arcTo(Rect(inset, inset, inset + 2f * r, inset + 2f * r), 180f, 90f, false)
        lineTo(w - inset - r, inset)
        arcTo(Rect(w - inset - 2f * r, inset, w - inset, inset + 2f * r), 270f, 90f, false)
        lineTo(w - inset, h)
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
}
