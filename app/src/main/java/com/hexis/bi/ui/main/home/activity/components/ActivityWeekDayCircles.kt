package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.activity.WeekDayData
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.ui.theme.dark.StepIndicatorTrack
import com.hexis.bi.utils.constants.ActivityConstants

@Composable
fun ActivityWeekDayCircles(
    days: List<WeekDayData>,
    stepsGoal: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    DayGauge(
                        day = day,
                        stepsGoal = stepsGoal,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayGauge(
    day: WeekDayData,
    stepsGoal: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val gaugeWidth = dimensionResource(R.dimen.activity_day_gauge_width)
    val gaugeHeight = dimensionResource(R.dimen.activity_day_gauge_height)
    val strokeWidth = dimensionResource(R.dimen.activity_day_gauge_stroke)
    val trackColor = ActivityConstants.RING_TRACK_COLOR
    val progressColor = MaterialTheme.colorScheme.primary
    val labelColor = when {
        day.isToday -> Positive
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val highlightColor = StepIndicatorTrack
    val interactionSource = remember { MutableInteractionSource() }
    val progressFraction = if (stepsGoal > 0) {
        (day.steps.toFloat() / stepsGoal).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .then(if (selected) Modifier.background(highlightColor) else Modifier)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_xxs),
                vertical = dimensionResource(R.dimen.spacer_2xs),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(width = gaugeWidth, height = gaugeHeight)) {
            val sw = strokeWidth.toPx()
            val diameter = size.width - sw
            val topLeft = Offset(sw / 2f, sw / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = ActivityConstants.GAUGE_START_ANGLE,
                sweepAngle = ActivityConstants.GAUGE_FULL_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            val sweep = ActivityConstants.GAUGE_FULL_SWEEP * progressFraction
            if (sweep > 0f) drawArc(
                color = progressColor,
                startAngle = ActivityConstants.GAUGE_START_ANGLE,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

        Text(
            text = day.dayLabel,
            style = TitleDimTextStyle,
            color = labelColor,
        )
    }
}
