package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.data.sleep.SleepStage
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.sleep.SleepStageData
import com.hexis.bi.ui.main.home.sleep.nameRes
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.utils.constants.SleepConstants

/** Vertical gap (dimen res) between each stage's marker line and the HRV label below it. */
private fun SleepStage.connectorDistanceRes(): Int = when (this) {
    SleepStage.Light -> R.dimen.sleep_stage_dist_light
    SleepStage.Deep -> R.dimen.sleep_stage_dist_deep
    SleepStage.REM -> R.dimen.sleep_stage_dist_rem
    SleepStage.Awake -> R.dimen.sleep_stage_dist_awake
}

@Composable
fun SleepStatusCard(
    modifier: Modifier = Modifier,
    totalSleepMinutes: Int,
    sleepGoalHours: Int,
    stages: List<SleepStageData>,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_m),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_xl)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(R.string.sleep_stages_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stageHeaderValue(totalSleepMinutes, sleepGoalHours),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StageAxisLabels(Modifier.padding(end = dimensionResource(R.dimen.spacer_xs)))
            val ordered = stageChartOrder(stages)
            ordered.forEachIndexed { index, data ->
                val self = dimensionResource(data.stage.connectorDistanceRes())
                val left = ordered.getOrNull(index - 1)
                    ?.let { dimensionResource(it.stage.connectorDistanceRes()) }
                val right = ordered.getOrNull(index + 1)
                    ?.let { dimensionResource(it.stage.connectorDistanceRes()) }
                // Each boundary is drawn once, by the column with the taller marker, so the
                // longer dashed line wins and overlapping lines never stack (which would darken).
                SleepStageColumn(
                    stage = data,
                    connectorDistance = self,
                    drawLeftEdge = left == null || self > left,
                    drawRightEdge = right == null || self >= right,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun stageHeaderValue(totalSleepMinutes: Int, sleepGoalHours: Int): AnnotatedString {
    val hours = totalSleepMinutes / 60
    val minutes = totalSleepMinutes % 60
    val numberStyle = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = Color.White)
    val unitStyle =
        MaterialTheme.typography.bodyMedium.toSpanStyle()
            .copy(color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f))
    val goalStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
        .copy(color = NocturnePulseTheme.extendedColors.gray200)
    val hourUnit = stringResource(R.string.unit_hours_short)
    val minuteUnit = stringResource(R.string.unit_minutes_short)
    val goalText = stringResource(R.string.sleep_stage_header_goal, sleepGoalHours)
    return buildAnnotatedString {
        withStyle(numberStyle) { append(hours.toString()) }
        withStyle(unitStyle) { append(" $hourUnit") }
        withStyle(numberStyle) { append(" ${minutes.toString().padStart(2, '0')}") }
        withStyle(unitStyle) { append(" $minuteUnit ") }
        withStyle(goalStyle) { append(goalText) }
    }
}

@Composable
private fun stageDurationValue(minutes: Int): AnnotatedString {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / SleepConstants.MINUTES_PER_HOUR
    val mins = safe % SleepConstants.MINUTES_PER_HOUR
    val valueStyle = MaterialTheme.typography.labelMedium.toSpanStyle()
        .copy(color = MaterialTheme.colorScheme.onBackground)
    val unitStyle = MaterialTheme.typography.bodySmall.toSpanStyle()
        .copy(color = NocturnePulseTheme.extendedColors.gray200)
    val hourUnit = stringResource(R.string.unit_hours_short)
    val minuteUnit = stringResource(R.string.unit_minutes_short)
    return buildAnnotatedString {
        if (hours > 0) {
            withStyle(valueStyle) { append(hours.toString()) }
            withStyle(unitStyle) { append(" $hourUnit ") }
        }
        withStyle(valueStyle) { append(mins.toString().padStart(2, '0')) }
        withStyle(unitStyle) { append(" $minuteUnit") }
    }
}

@Composable
private fun StageAxisLabels(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.sleep_metric_hrv),
            style = MaterialTheme.typography.bodySmall,
            color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
        Text(
            text = stringResource(R.string.sleep_metric_rhr_short),
            style = MaterialTheme.typography.bodySmall,
            color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun SleepStageColumn(
    stage: SleepStageData,
    connectorDistance: Dp,
    drawLeftEdge: Boolean,
    drawRightEdge: Boolean,
    modifier: Modifier = Modifier,
) {
    val connectorColor = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f)
    val dashStroke = dimensionResource(R.dimen.sleep_stage_dash_stroke)
    val dashSegment = dimensionResource(R.dimen.sleep_stage_dash_segment)
    val stageColor = rememberSleepStageColors()

    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
        Text(
            text = stageDurationValue(stage.durationMinutes),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacer_xxs))
        )
        Text(
            text = stringResource(stage.stage.nameRes()),
            style = MaterialTheme.typography.bodySmall,
            color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f),
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.spacer_xxs),
                top = dimensionResource(R.dimen.spacer_3xs)
            )
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
        // The marker line plus the dashed connectors running down past the HRV/RHR rows.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = dashStroke.toPx()
                    val dash = PathEffect.dashPathEffect(
                        floatArrayOf(
                            dashSegment.toPx(),
                            dashSegment.toPx()
                        )
                    )
                    if (drawLeftEdge) {
                        val x = strokeWidth / 2f
                        drawLine(
                            connectorColor,
                            Offset(x, size.height),
                            Offset(x, 0f),
                            strokeWidth,
                            pathEffect = dash
                        )
                    }
                    if (drawRightEdge) {
                        val x = size.width - strokeWidth / 2f
                        drawLine(
                            connectorColor,
                            Offset(x, size.height),
                            Offset(x, 0f),
                            strokeWidth,
                            pathEffect = dash
                        )
                    }
                },
        ) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.sleep_stage_line_height))
                        .clip(CircleShape)
                        .background(stageColor(stage.stage))
                )
                Spacer(Modifier.height(connectorDistance))
                Text(
                    text = stringResource(
                        R.string.sleep_metric_value_unit,
                        stage.hrv.coerceAtLeast(0),
                        stringResource(R.string.unit_ms),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.spacer_xxs))
                )
                Text(
                    text = stringResource(
                        R.string.sleep_metric_value_unit,
                        stage.rhr.coerceAtLeast(0),
                        stringResource(R.string.unit_bpm),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = NocturnePulseTheme.extendedColors.gray200.copy(alpha = 0.4f),
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.spacer_xxs),
                        top = dimensionResource(R.dimen.spacer_2xs)
                    )
                )
            }
        }
    }
}

private fun stageChartOrder(stages: List<SleepStageData>): List<SleepStageData> {
    val byStage = stages.associateBy { it.stage }
    return listOf(SleepStage.Light, SleepStage.Deep, SleepStage.REM, SleepStage.Awake)
        .mapNotNull { byStage[it] }
}
