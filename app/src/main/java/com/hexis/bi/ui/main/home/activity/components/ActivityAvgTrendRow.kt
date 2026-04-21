package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.TrendComparison
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Lime200
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.utils.gradientBackground
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ActivityAvgTrendRow(
    avgStepsPerDay: Int,
    trendPercent: Int?,
    trendComparison: TrendComparison,
    trendDescription: String,
    modifier: Modifier = Modifier,
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val trendText = trendPercent?.let {
        stringResource(R.string.activity_trend_percent_signed, it)
    } ?: stringResource(R.string.activity_trend_none_symbol)
    val trendColor = when (trendComparison) {
        TrendComparison.UP -> Green
        TrendComparison.DOWN -> Red100
        TrendComparison.FLAT -> MaterialTheme.colorScheme.secondary
        TrendComparison.NONE -> MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .gradientBackground(
                    brush = Brush.verticalGradient(listOf(Blue300, Blue200)),
                    shape = MaterialTheme.shapes.medium,
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
        ) {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacer_m))) {
                Text(
                    text = stringResource(R.string.activity_avg_steps_per_day),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(dimensionResource(R.dimen.activity_trend_padding)))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = fmt.format(avgStepsPerDay),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Lime200,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
                    Text(
                        text = stringResource(R.string.activity_unit_steps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Lime200,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
        ) {
            Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacer_m))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.activity_trend_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.titleSmall,
                        color = trendColor,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = trendDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
