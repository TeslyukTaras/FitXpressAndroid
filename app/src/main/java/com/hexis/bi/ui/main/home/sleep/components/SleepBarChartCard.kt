package com.hexis.bi.ui.main.home.sleep.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.sleep.DailySleepEntry
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.LightBlue
import com.hexis.bi.ui.theme.LightBlueBackground
import com.hexis.bi.ui.theme.LightGradientBlue
import com.hexis.bi.utils.constants.SleepConstants

@Composable
fun SleepBarChartCard(
    entries: List<DailySleepEntry>,
    modifier: Modifier = Modifier,
) {
    val maxDuration = entries.maxOfOrNull { it.durationMinutes }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sleep_summary_total_sleep),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.sleep_summary_last_7_days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.sleep_bar_chart_height)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.sleep_bar_horizontal_padding)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sleep_bar_horizontal_padding)),
            ) {
                entries.forEach { entry ->
                    DayBar(
                        entry = entry,
                        maxDuration = maxDuration,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        // Day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.sleep_bar_horizontal_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sleep_bar_horizontal_padding)),
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.dayLabel,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primaryFixed,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DayBar(
    entry: DailySleepEntry,
    maxDuration: Int,
    modifier: Modifier = Modifier,
) {
    val stripeColor = LightBlue
    val barBrush =
        if (entry.isHighlighted) Brush.verticalGradient(listOf(LightGradientBlue, Blue300))
        else Brush.verticalGradient(listOf(LightBlue, LightBlue))
    val textColor = if (entry.isHighlighted) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onBackground
    val barShape = MaterialTheme.shapes.small
    val durationLabel = formatBarLabel(entry.durationMinutes)
    val stripeWidth = dimensionResource(R.dimen.sleep_bar_stripe_width)
    val stripeSpacing = dimensionResource(R.dimen.sleep_bar_stripe_spacing)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(barShape)
            .background(LightBlueBackground.copy(alpha = SleepConstants.BAR_CHART_STRIPE_BG_ALPHA))
            .drawBehind {
                val stripeWidthPx = stripeWidth.toPx()
                val stripeSpacingPx = stripeSpacing.toPx()
                val flatterRatio = SleepConstants.BAR_CHART_STRIPE_FLATTEN
                val horizontalShift = size.height * flatterRatio

                var x = -horizontalShift
                while (x < size.width) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, size.height),
                        end = Offset(x + horizontalShift, 0f),
                        strokeWidth = stripeWidthPx,
                    )
                    x += (stripeWidthPx + stripeSpacingPx * flatterRatio)
                }
            },
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (entry.durationMinutes == 0) Spacer(Modifier.weight(1f))
        else {
            val spacerWeight = (maxDuration - entry.durationMinutes).coerceAtLeast(0).toFloat()
            val barWeight = entry.durationMinutes.toFloat()

            if (spacerWeight > 0f) Spacer(Modifier.weight(spacerWeight))
            Box(
                modifier = Modifier
                    .weight(barWeight)
                    .fillMaxWidth()
                    .clip(barShape)
                    .background(barBrush),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = durationLabel,
                    modifier = Modifier.padding(all = dimensionResource(R.dimen.sleep_bar_label_padding_bottom)),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun formatBarLabel(minutes: Int): String {
    val hours = minutes / SleepConstants.MINUTES_PER_HOUR
    val mins = minutes % SleepConstants.MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.sleep_duration_minutes, mins)
        mins == 0 -> stringResource(R.string.sleep_duration_hours_only, hours)
        else -> stringResource(R.string.sleep_duration_hours_minutes, hours, mins)
    }
}
