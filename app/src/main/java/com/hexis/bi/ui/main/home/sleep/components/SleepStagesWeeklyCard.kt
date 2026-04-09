package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.sleep.StageTrend
import com.hexis.bi.ui.main.home.sleep.WeeklyStageData
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.utils.formatSleepDuration

@Composable
fun SleepStagesWeeklyCard(
    stages: List<WeeklyStageData>,
    avgSleepMinutes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sleep_weekly_stages_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(
                    R.string.sleep_summary_avg,
                    formatSleepDuration(avgSleepMinutes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        // 2×2 stage grid
        val chunkSize = 2
        stages.chunked(chunkSize).forEachIndexed { indexRow, row ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                row.forEachIndexed { indexColumn, stage ->
                    WeeklyStageCell(
                        data = stage,
                        modifier = Modifier.weight(1f),
                    )
                    if (indexColumn < row.size - 1) {
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            color = MaterialTheme.colorScheme.secondaryFixed,
                        )
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                    }
                }
                // Fill remaining space if row has only 1 item
                if (row.size < chunkSize) Spacer(Modifier.weight(1f))
            }
            if (indexRow < stages.size / chunkSize - 1) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                HorizontalDivider(color = MaterialTheme.colorScheme.secondaryFixed)
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(R.string.sleep_summary_trend_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun WeeklyStageCell(
    data: WeeklyStageData,
    modifier: Modifier = Modifier,
) {
    val trendIcon = if (data.trend == StageTrend.Up) "↑" else "↓"
    val trendTint = if (data.trend == StageTrend.Up) Green else Red100

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(data.stage.nameRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
        Text(
            text = trendIcon,
            style = MaterialTheme.typography.bodyMedium,
            color = trendTint,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = formatSleepDuration(data.durationMinutes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
