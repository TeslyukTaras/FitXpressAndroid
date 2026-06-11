package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.activity.TrendComparison
import com.hexis.bi.ui.theme.AccentBlue
import com.hexis.bi.ui.theme.ActivityMediumTitleStyle
import com.hexis.bi.ui.theme.MeasurementValueStyle
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.dark.Negative
import com.hexis.bi.ui.theme.dark.Positive
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ActivityAvgTrendRow(
    avgStepsPerDay: Int,
    trendPercent: Int?,
    trendComparison: TrendComparison,
    trendTitle: String,
    trendDescription: String,
    separateInsightGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val trendText = trendPercent?.let {
        stringResource(R.string.activity_trend_percent_signed, it)
    } ?: stringResource(R.string.activity_trend_none_symbol)
    val trendColor = when {
        trendPercent != null && trendPercent > 0 -> Positive
        trendPercent != null && trendPercent < 0 -> Negative
        trendComparison == TrendComparison.UP -> Positive
        trendComparison == TrendComparison.DOWN -> Negative
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        BodyGlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            highlighted = true,
        ) {
            Text(
                text = stringResource(R.string.activity_avg_steps_per_day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(dimensionResource(R.dimen.activity_trend_padding)))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = fmt.format(avgStepsPerDay),
                    style = MeasurementValueStyle,
                    color = AccentBlue,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                Text(
                    text = stringResource(R.string.activity_unit_steps),
                    style = ActivityMediumTitleStyle,
                    color = AccentBlue,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        if (separateInsightGlass) Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            BodyGlassCard(modifier = Modifier.fillMaxWidth()) {
                TrendHeaderRow(trendTitle, trendText, trendColor)
            }
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            BodyGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Text(
                    text = trendDescription,
                    style = TitleDimTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else BodyGlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            TrendHeaderRow(trendTitle, trendText, trendColor)
            Spacer(Modifier.weight(1f))
            Text(
                text = trendDescription,
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendHeaderRow(
    title: String,
    trendText: String,
    trendColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = trendText,
            style = MaterialTheme.typography.bodyLarge,
            color = trendColor,
        )
    }
}
