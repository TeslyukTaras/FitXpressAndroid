package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.recovery.RecoveryStatus
import com.hexis.bi.ui.theme.Blue200
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.Lime200
import com.hexis.bi.utils.gradientBackground

@Composable
fun RecoveryScoreCards(
    avgScore: Int,
    trendLabel: String,
    trendDescription: String,
    modifier: Modifier = Modifier,
) {
    val trendStatus = RecoveryStatus.fromScore(avgScore)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
    ) {
        // Avg score card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .gradientBackground(
                    brush = Brush.verticalGradient(listOf(Blue300, Blue200)),
                    shape = MaterialTheme.shapes.medium
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacer_m)),
            ) {
                Text(
                    text = stringResource(R.string.recovery_avg_score),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
                Text(
                    text = stringResource(R.string.recovery_avg_score_value, avgScore),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Lime200,
                )
            }
        }

        // Trend card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.elevation_none)),
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacer_m)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.recovery_trend_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = trendStatus.color,
                    )
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                Text(
                    text = trendDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
