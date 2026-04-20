package com.hexis.bi.ui.main.home.activity.components

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.ActivityMetric
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.utils.constants.ActivityConstants

@Composable
fun ActivityProgressCard(
    progressPercent: Int,
    stepsProgress: Float,
    distanceProgress: Float,
    caloriesProgress: Float,
    metrics: List<ActivityMetric>,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorColors = ActivityConstants.RING_COLORS.asReversed()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
    ) {
        Column(
            modifier = Modifier.padding(
                top = dimensionResource(R.dimen.spacer_xxs),
                bottom = dimensionResource(R.dimen.spacer_m)
            )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.spacer_m),
                        end = dimensionResource(R.dimen.spacer_xxs)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.activity_progress_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = onInfoClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = stringResource(R.string.cd_info),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                }
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

            // Arc + Metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = dimensionResource(R.dimen.spacer_m)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityCircularProgress(
                    stepsProgress = stepsProgress,
                    distanceProgress = distanceProgress,
                    caloriesProgress = caloriesProgress,
                    progressPercent = progressPercent,
                )

                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.secondaryFixed,
                )

                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

                Column(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_2xl)),
                ) {
                    metrics.forEachIndexed { index, metric ->
                        MetricRow(
                            metric = metric,
                            indicatorColor = indicatorColors.getOrElse(index) { Blue300 },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    metric: ActivityMetric,
    indicatorColor: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(
                    width = dimensionResource(R.dimen.spacer_xxs),
                    height = dimensionResource(R.dimen.spacer_xl),
                )
                .clip(CircleShape)
                .background(indicatorColor),
        )

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

        Text(
            text = stringResource(metric.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = "${metric.value} ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = metric.unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
