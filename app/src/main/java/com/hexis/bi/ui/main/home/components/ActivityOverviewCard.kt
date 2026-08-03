package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.ActivityOverview

@Composable
internal fun ActivityOverviewCard(
    data: ActivityOverview,
    isSyncing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_card_activity),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AnimatedContent(
                targetState = isSyncing,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "activity-overview-value",
            ) { syncing ->
                Text(
                    text = if (syncing) stringResource(R.string.health_data_syncing) else data.steps,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (syncing) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }

        if (isSyncing) return@BodyGlassCard

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        ActivityMiniBarChart(
            values = data.hourlySteps,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.home_activity_chart_height)),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xs)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.home_activity_start_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.home_activity_end_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
