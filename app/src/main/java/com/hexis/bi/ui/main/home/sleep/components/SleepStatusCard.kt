package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.hexis.bi.ui.main.home.sleep.SleepQuality
import com.hexis.bi.ui.main.home.sleep.SleepStageData
import com.hexis.bi.utils.formatSleepDuration

@Composable
fun SleepStatusCard(
    modifier: Modifier = Modifier,
    quality: SleepQuality,
    totalSleepMinutes: Int,
    sleepGoalHours: Int,
    progress: Float = 100f,
    stages: List<SleepStageData>,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.sleep_status_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(quality.nameRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SleepArcChart(
                progress = progress,
                totalSleepMinutes = totalSleepMinutes,
                sleepGoalHours = sleepGoalHours,
                stages = stages,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.divider_size)),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
                modifier = Modifier.weight(1f),
            ) {
                stages.forEach { stage ->
                    StageRow(stage = stage)
                }
            }
        }
    }
}

@Composable
private fun StageRow(stage: SleepStageData) {
    val durationText = formatSleepDuration(stage.durationMinutes)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(R.dimen.size_sleep_indicator))
                    .clip(CircleShape)
                    .background(stage.color)
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
            Text(
                text = stringResource(stage.stage.nameRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = durationText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
