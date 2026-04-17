package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.recovery.DailyRecoveryEntry
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.GridLineGray
import com.hexis.bi.ui.theme.LightBlue
import com.hexis.bi.ui.theme.LightGradientBlue
import com.hexis.bi.utils.constants.RecoveryConstants

@Composable
fun RecoveryBarChart(
    entries: List<DailyRecoveryEntry>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.recovery_bar_chart_height)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.recovery_y_axis_width))
            ) {
                RecoveryConstants.GRID_LINES.forEach { value ->
                    val fraction = RecoveryConstants.mapScoreToFraction(value)
                    Text(
                        text = value.toInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align { size, space, _ ->
                            IntOffset(0, (space.height * fraction - size.height / 2).toInt())
                        }
                    )
                }
            }

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))

            val dashWidth = dimensionResource(R.dimen.dash_width)
            val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
            val dashColor = GridLineGray

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .drawBehind {
                        val dashWidthPx = dashWidth.toPx()
                        val stripeWidthPx = stripeWidth.toPx()
                        val dashEffect =
                            PathEffect.dashPathEffect(floatArrayOf(dashWidthPx, dashWidthPx), 0f)

                        RecoveryConstants.GRID_LINES.forEach { value ->
                            val y = size.height * RecoveryConstants.mapScoreToFraction(value)
                            drawLine(
                                color = dashColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = stripeWidthPx,
                                pathEffect = dashEffect,
                            )
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.recovery_bar_horizontal_padding)),
                ) {
                    entries.forEach { entry ->
                        DayBar(
                            entry = entry,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(R.dimen.spacer_xxs),
                    bottom = dimensionResource(R.dimen.spacer_xxs),
                    start = dimensionResource(R.dimen.spacer_xs)
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val first = entries.firstOrNull()?.dayLabel.orEmpty()
            val last = entries.lastOrNull()?.dayLabel.orEmpty()
            Spacer(Modifier.width(dimensionResource(R.dimen.recovery_y_axis_width)))
            Text(
                text = first,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = last,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun DayBar(
    entry: DailyRecoveryEntry,
    modifier: Modifier = Modifier,
) {
    val barBrush = if (entry.isHighlighted)
        Brush.verticalGradient(listOf(LightGradientBlue, Blue300))
    else Brush.verticalGradient(listOf(LightBlue, LightBlue))

    val barShape = RoundedCornerShape(
        topStart = dimensionResource(R.dimen.recovery_bar_corner),
        topEnd = dimensionResource(R.dimen.recovery_bar_corner),
    )

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (entry.score > 0) Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1f - RecoveryConstants.mapScoreToFraction(entry.score.toFloat()))
                .clip(barShape)
                .background(barBrush)
        )
    }
}