package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.recovery.RecoveryTrend
import com.hexis.bi.ui.theme.MeasurementValueStyle
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
fun RecoveryScoreCards(
    avgScore: Int,
    trend: RecoveryTrend,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        // Average recovery score
        BodyGlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            highlighted = true
        ) {
            Text(
                text = stringResource(R.string.recovery_avg_score),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            Text(
                text = stringResource(R.string.recovery_avg_score_value, avgScore),
                style = MeasurementValueStyle,
                color = NocturnePulseTheme.extendedColors.accentBlue,
            )
        }

        // Trend
        BodyGlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.recovery_trend_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(trend.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = trendColor(trend),
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Text(
                text = stringResource(trend.descriptionRes),
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun trendColor(trend: RecoveryTrend): Color = when (trend) {
    RecoveryTrend.Improving -> NocturnePulseTheme.extendedColors.gaugeHigh
    RecoveryTrend.Decreasing -> NocturnePulseTheme.extendedColors.gaugeLow
    RecoveryTrend.Stable -> NocturnePulseTheme.extendedColors.accentBlue
}
