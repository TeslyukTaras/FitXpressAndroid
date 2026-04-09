package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.sleep.components.SleepBarChartCard
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStagesWeeklyCard

@Composable
fun SleepSummaryContent(
    modifier: Modifier = Modifier,
    state: SleepState,
    onInfoClick: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    // Week navigator
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_small))
                        .graphicsLayer(rotationY = 180f),
                )
            }
            Text(
                modifier = Modifier.defaultMinSize(minWidth = dimensionResource(R.dimen.date_row_min_width)),
                text = state.weekLabel,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(
                onClick = onNextWeek,
                enabled = state.canGoNextWeek,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = if (state.canGoNextWeek)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
        }
    }

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    SleepBarChartCard(entries = state.weeklyEntries)

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

    SleepStagesWeeklyCard(
        stages = state.weeklyStages,
        avgSleepMinutes = state.avgSleepMinutes,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SleepRecoveryBanner(onInfoClick = onInfoClick)
}
