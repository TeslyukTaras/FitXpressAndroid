package com.hexis.bi.ui.main.home.longevity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleChip
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleTrack
import com.hexis.bi.ui.main.home.longevity.LongevityTab
import com.hexis.bi.ui.main.home.longevity.LongevityTrendData
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.TitleHighlightTextStyle
import com.hexis.bi.ui.theme.NocturnePulseTheme

@Composable
internal fun LongevityTrendCard(
    modifier: Modifier = Modifier,
    selectedTab: LongevityTab,
    onTabSelected: (LongevityTab) -> Unit,
    trendData: LongevityTrendData,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            vertical = dimensionResource(R.dimen.spacer_l),
            horizontal = dimensionResource(R.dimen.spacer_m)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.longevity_trend_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            BodySegmentedToggleTrack {
                LongevityTab.entries.forEachIndexed { index, tab ->
                    if (index > 0) Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                    BodySegmentedToggleChip(
                        label = stringResource(tab.labelRes),
                        isSelected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                        width = dimensionResource(R.dimen.longevity_toggle_chip_width),
                    )
                }
            }
        }

        Text(
            text = trendData.dateLabel,
            style = TitleHighlightTextStyle,
            color = NocturnePulseTheme.extendedColors.accentBlue,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

        LongevityLineChart(
            points = trendData.points,
            axisLabels = trendData.axisLabels,
            currentLabelIndex = trendData.currentLabelIndex,
            xAxisSpanCount = trendData.xAxisSpanCount,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bottomLabel =
                if (selectedTab == LongevityTab.Weekly) R.string.longevity_trend_weekly_label
                else R.string.longevity_trend_today_label

            Text(
                text = stringResource(bottomLabel),
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(trendData.trend.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = NocturnePulseTheme.extendedColors.accentBlue,
            )
        }
    }
}
